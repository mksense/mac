/*
 * Copyright 2006-2008 Sun Microsystems, Inc. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 * 
 * This code is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * only, as published by the Free Software Foundation.
 * 
 * This code is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License version 2 for more details (a copy is
 * included in the LICENSE file that accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License
 * version 2 along with this work; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA
 * 
 * Please contact Sun Microsystems, Inc., 16 Network Circle, Menlo
 * Park, CA 94025 or visit www.sun.com if you need additional
 * information or have any questions.
 */

package com.sun.spot.peripheral;

import com.sun.spot.util.Utils;
import com.sun.squawk.Unsafe;

 
/**
 * @author Syntropy
 *  
 */
 class AT91_PIO extends AbstractAT91_PIO implements IAT91_PIO, IDriver {

	
    // Register offsets, expressed in 32-bit units
    private static final int PIO_PER	= 0;           /* PIO Enable Register */
    private static final int PIO_PDR	= 1;           /* PIO Disable Register */
//    private static final int PIO_PSR	= 2;           /* PIO Status Register */
//    private static final int PIO_OSR	= 6;           /* Output Status Register */
//    private static final int PIO_IFSR	= 10;          /* Input Filter Status Register */
//    private static final int PIO_ODSR	= 14;          /* Output Data Status Register */
    private static final int PIO_IER	= 16;          /* Interrupt Enable Register */
    private static final int PIO_IDR	= 17;          /* Interrupt Disable Register */
    private static final int PIO_IMR	= 18;          /* Interrupt Mask Register */
    private static final int PIO_ISR	= 19;          /* Interrupt Status Register */
    
//    private static final int PIO_MDER	= 20;          /* Multi Driver Enable Register */
//    private static final int PIO_MDDR	= 21;          /* Multi Driver Disable Register */
//    private static final int PIO_MDSR	= 22;          /* Multi Driver Status Register */

//    private static final int PIO_PUDR	= 24;          /* Pull Up Disable Register */
//    private static final int PIO_PUER	= 25;          /* Pull Up Driver Enable Register */
//    private static final int PIO_PUSR	= 26;          /* Pull Up Driver Status Register */

    private static final int PIO_ASR	= 28;          /* Peripheral A Select Register */
    private static final int PIO_BSR	= 29;          /* Peripheral B Select Register */
//    private static final int PIO_ABSR	= 30;          /* A B Status Register */
        
	private static int[] PIO_IDS = {
			IAT91_Peripherals.PIOA_ID_MASK,
			IAT91_Peripherals.PIOB_ID_MASK,
			IAT91_Peripherals.PIOC_ID_MASK,
			IAT91_Peripherals.PIOD_ID_MASK};

    // per-PIO data
    
    private int claims;
	private IAT91_AIC aic;
	private IAT91_PowerManager powerManager;
	private int outstandingIrqs;
	private int unavailablePins;

	private Object syncObjectForWaitForIrq;

	AT91_PIO(int pioSelector, IAT91_AIC aic, IAT91_PowerManager powerManager, int unavailablePins) {
		super(pioSelector);
	    this.unavailablePins = claims = unavailablePins;
	    outstandingIrqs = 0;
	    this.aic = aic;
	    this.powerManager = powerManager;
	    disableIrq(~0); // all pins
	    setUp();
	}

    /**
     * Return the mask of available pins
     * @return - mask
     */
    public int available() {
    	return ~claims;
    }
    
	/**
	 * Lay claim to certain PIO pins.
	 * 
	 * @param mask claimed pins
	 * @param drive bits set to 1 for pins to be controlled by PIO
	 * @param claimForPeriphA if true assign undriven pins to peripheral A; if false to peripheral B
	 */
    public void claim(int mask, int drive, boolean claimForPeriphA) {
    	if ((mask | drive) != mask) {
    		throw new IllegalArgumentException ("Request to drive pins not claimed");
    	}
    	if ((claims & mask) != 0) {
    		throw new IllegalArgumentException ("At least one pin in mask 0x" + Integer.toHexString(mask) + " is already claimed" );
    	}
    	claims = claims | mask;
    	Unsafe.setInt(baseAddress, PIO_PER, drive);
    	if ((mask ^ drive) != 0) {
    		if (claimForPeriphA) {
    			// select peripheral A
        		Unsafe.setInt(baseAddress, PIO_ASR, mask ^ drive);
    		} else {
    			// select peripheral B
        		Unsafe.setInt(baseAddress, PIO_BSR, mask ^ drive);    			
    		}
    	}
    	Unsafe.setInt(baseAddress, PIO_PDR, mask ^ drive);
    }

    /**
     * Release claim to certain PIO pins
     * 
     * @param mask - released pins
     */
    public void release(int mask) {
    	claims = claims & ~mask;
    	//set unclaimed pins to be inputs (safest)
    	Unsafe.setInt(baseAddress, PIO_ODR, mask);
    }


	/**
	 * Enable PIO interrupts for the specified pin
	 * @param mask - the pio mask
	 */
	public void enableIrq(int mask) {
		// force a read of PIO_ISR to clear any outstanding change indications
		updateOutstandingIrqs();
		// enable interrupt in PIO
		Unsafe.setInt(baseAddress, PIO_IER, mask);
	}

	/**
	 * Wait for the specified PIO pin to generate an interrupt.
	 * On exit, the pin is masked so that it will not generate another interrupt.
	 * @param irq	a bit mask of the pin to wait on
	 * @throws InterruptedException 
	 */
	public void waitForIrq(int irq) throws InterruptedException {
		while (!myPinHasChanged(irq)) {
			if (syncObjectAvailable()) {
				aic.waitForInterrupt(PIO_IDS[pioSelector]);
				updateOutstandingIrqs();
				boolean myPinChanged = myPinHasChanged(irq);
				Object syncObject = syncObjectForWaitForIrq;
				if (myPinChanged) {
					disableIrq(irq);
				}
				releaseSyncObject();
				aic.enableIrq(PIO_IDS[pioSelector]);
				synchronized (syncObject) {
					syncObject.notifyAll();
				}
			} else {
				waitOnSyncObject();
				if (myPinHasChanged(irq)) {
					disableIrq(irq);
				}
			}
		}
		// clear the corresponding bit in the outstandingIrqs
		outstandingIrqs = outstandingIrqs & ~irq;
	}
	
	public void disableIrq(int irq) {
		// disaable interrupt in PIO
		Unsafe.setInt(baseAddress, PIO_IDR, irq);
	}

	public void setUp() {
		powerManager.enablePeripheralClock(PIO_IDS[pioSelector]);
	    // select all available lines to be inputs
		Unsafe.setInt(baseAddress, PIO_ODR, ~unavailablePins) ;
	    // select PIO control for all available lines
    	Unsafe.setInt(baseAddress, PIO_PER, ~unavailablePins);
		// configure interrupt in AIC
		aic.configure(PIO_IDS[pioSelector], IAT91_AIC.AIC_IRQ_PRI_NORMAL, IAT91_AIC.SRCTYPE_HIGH_LEVEL);
		// unmask interrupt in AIC
		aic.enableIrq(PIO_IDS[pioSelector]);
	}

	public boolean tearDown() {
		Utils.log(getDriverName() + ": Claims are" + claims);
		if (claims == unavailablePins) {
			aic.disableIrq(PIO_IDS[pioSelector]);
			powerManager.disablePeripheralClock(PIO_IDS[pioSelector]);
			return true;
		}
		return false;
	}

	public void shutDown() {
	}

	protected void checkOwned(int mask) {
		if ((claims & mask) != mask) {
    		throw new IllegalArgumentException ("Attempt to open pins that are not claimed");
    	}
	}

	private boolean myPinHasChanged(int irq) {
		return (outstandingIrqs & irq) != 0;
	}

	private void waitOnSyncObject() throws InterruptedException {
		synchronized (syncObjectForWaitForIrq) {
			syncObjectForWaitForIrq.wait();
		}
	}

	private synchronized boolean syncObjectAvailable() {
		if (syncObjectForWaitForIrq == null) {
			syncObjectForWaitForIrq = new Object();
			return true;
		} else {
			return false;
		}
	}
	private synchronized void releaseSyncObject() {
		syncObjectForWaitForIrq = null;
	}
	
	private void updateOutstandingIrqs() {
		outstandingIrqs = outstandingIrqs | (Unsafe.getInt(baseAddress, PIO_ISR) & Unsafe.getInt(baseAddress, PIO_IMR));
	}

}
