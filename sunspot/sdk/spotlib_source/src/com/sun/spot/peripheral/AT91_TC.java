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

import com.sun.squawk.Address;
import com.sun.squawk.Unsafe;
 
/**
 * @author Syntropy
 * 
 * Timer counter
 *  
 */
class AT91_TC implements TimerCounterBits, IAT91_TC {
	/*------------------------------------*/
	/* Timer Counter Register Definitions */
	/*------------------------------------*/
    private static final Address[] BLOCK_BASE_ADDRESS = {
    	Address.fromPrimitive(0xFFFA0000), // base address for first timer counter block
    	Address.fromPrimitive(0xFFFA4000)  // base address for second timer counter block
    };
 
    // offsets for the registers that operate on the whole block
    private static final int TC_BCR = 0xC0 >> 2;     /* Block control register */
    private static final int TC_BMR = 0xC4 >> 2;	    /* Block mode register */
    
    // offsets for base of each timer counter
    private static final int TC_CHAN0 = 0 >> 2;
    private static final int TC_CHAN1 = 0x40 >> 2;
    private static final int TC_CHAN2 = 0x80 >> 2;
    
    private static final int tcBases[]  = {TC_CHAN0, TC_CHAN1, TC_CHAN2};

    // offsets for registers of each timer counter
    private static final int TC_CCR = 0 >> 2;        /* Control Register */
    private static final int TC_CMR = 4 >> 2;        /* Mode Register */
    private static final int TC_CVR = 0x10 >> 2;     /* Counter value */
    private static final int TC_RA  = 0x14 >> 2;     /* Register A */
    private static final int TC_RB  = 0x18 >> 2;     /* Register B */
    private static final int TC_RC  = 0x1C >> 2;     /* Register C */
    private static final int TC_SR  = 0x20 >> 2;     /* Status Register */
    private static final int TC_IER = 0x24 >> 2;     /* Interrupt Enable Register */
    private static final int TC_IDR = 0x28 >> 2;     /* Interrupt Disable Register */
    private static final int TC_IMR = 0x2C >> 2;     /* Interrupt Mask Register */

	/* masks for XC configuration in TC_BMR */
	private static final int XC_MASK[]	= {TC_TC0XC0S, TC_TC1XC1S, TC_TC2XC2S};
	
	/* interrupts for the 6 TCs */
	private static final int INT_MASK[]	= {
		IAT91_Peripherals.TC0_ID_MASK,
		IAT91_Peripherals.TC1_ID_MASK,
		IAT91_Peripherals.TC2_ID_MASK,
		IAT91_Peripherals.TC3_ID_MASK,
		IAT91_Peripherals.TC4_ID_MASK,
		IAT91_Peripherals.TC5_ID_MASK
	};

	/* instance variables */
	private Address blockBaseAddress;
	private int channelBaseAddress;
	private int index;
	private IAT91_AIC aic;
	private IAT91_PowerManager powerManager;
	private ISpotPins spotPins;

	
	/**
	 * @param index - the index of the tc to create (0, 1 or 2)
	 * @param pio
	 */
	public AT91_TC(int index, IAT91_AIC aic, IAT91_PowerManager pm, ISpotPins spotPins) {
		if ((IAT91_Peripherals.PERIPHERALS_ACCESSIBLE_FROM_JAVA & INT_MASK[index]) == 0) {
			throw new SpotFatalException("Timer-Counter " + index + " is not accessible from Java");
		}
		this.index = index;
		blockBaseAddress = BLOCK_BASE_ADDRESS[index / 3];
		channelBaseAddress = tcBases[index % 3];
        this.aic = aic;
        this.spotPins = spotPins;
        powerManager = pm;
        disable();
        disableIrq(0xFF); // all sources
		setUp();
	}
	
	/**
	 * Configure TC channel
	 * @param mask - bits to set in TC_CMR
	 */
	public void configure(int mask) {
		Unsafe.setInt(blockBaseAddress, channelBaseAddress + TC_CMR, mask);
	}
	
	/**
	 * Enable counter and cause software trigger
	 */
	public void enableAndReset() {
		Unsafe.setInt(blockBaseAddress, channelBaseAddress + TC_CCR, TC_CLKEN | TC_SWTRG);
	}
	
	/**
	 * Enable counter
	 */
	public void enable() {
		Unsafe.setInt(blockBaseAddress, channelBaseAddress + TC_CCR, TC_CLKEN);
	}

	/**
	 * Read current counter value
	 * @return TC_CV
	 */
	public int counter() {
		return Unsafe.getInt(blockBaseAddress, channelBaseAddress + TC_CVR);
	}

	/**
	 * Read current status
	 * @return TC_SR
	 */
	public int status() {
		return Unsafe.getInt(blockBaseAddress, channelBaseAddress + TC_SR);
	}

	/**
	 * Disable PIO use of shared TIOA line
	 */
	public void claimTIOA() {
		PIOPin pin = spotPins.getTC_TIOA(index);
		pin.release();
		pin.claim();
	}

	public void unclaimTIOA() {
		PIOPin pin = spotPins.getTC_TIOA(index);
		pin.release();
	}

	/**
	 * Disable PIO use of shared TIOB line
	 */
	public void claimTIOB() {
		PIOPin pin = spotPins.getTC_TIOB(index);
		pin.release();
		pin.claim();
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.AT91_TC#claimTCLK()
	 */
	public void claimTCLK() {
		PIOPin pin = spotPins.getTC_TCLK(index);
		pin.release();
		pin.claim();
	}

	/**
	 * Read current Reg A value
	 * @return TC_RA
	 */
	public int regA() {
		return Unsafe.getInt(blockBaseAddress, channelBaseAddress + TC_RA);
	}

	/**
	 * Read current Reg B value
	 * @return TC_RB
	 */
	public int regB() {
		return Unsafe.getInt(blockBaseAddress, channelBaseAddress + TC_RB);
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.AT91_TC#setRegC(int)
	 */
	public void setRegC(int i) {
		Unsafe.setInt(blockBaseAddress, channelBaseAddress + TC_RC, i);
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.AT91_TC#setRegA(int)
	 */
	public void setRegA(int i) {
		Unsafe.setInt(blockBaseAddress, channelBaseAddress + TC_RA, i);
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.AT91_TC#disable()
	 */
	public void disable() {
		Unsafe.setInt(blockBaseAddress, channelBaseAddress + TC_CCR, TC_CLKDIS);
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.AT91_TC#configureXC(int)
	 */
	public void configureXC(int xcMask) {
		// check that only bits relevant to this TC are being set
		if ((xcMask & ~XC_MASK[index % 3]) != 0) {
			throw new IllegalArgumentException ("XC mask of " + xcMask + " not valid for TC " + index);
		}
		// set bits in TC_BMR
		int bmr = Unsafe.getInt(blockBaseAddress, TC_BMR);
		Unsafe.setInt(blockBaseAddress, TC_BMR, (bmr & ~XC_MASK[index % 3]) | xcMask);
	}

	public void blockSync() {
		Unsafe.setInt(blockBaseAddress, TC_BCR, 1);
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.AT91_TC#enableIrq(int)
	 */
	public void enableIrq(int mask) {
		aic.enableIrq(INT_MASK[index]);
		Unsafe.setInt(blockBaseAddress, channelBaseAddress + TC_IER, mask);
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.AT91_TC#waitForIrq()
	 */
	public void waitForIrq() {
		aic.waitForInterrupt(INT_MASK[index]);
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.AT91_TC#disableIrq(int)
	 */
	public void disableIrq(int mask) {
		Unsafe.setInt(blockBaseAddress, channelBaseAddress + TC_IDR, mask);
	}

	public String getDriverName() {
		return "TC-"+index;
	}

	public boolean tearDown() {
		if ((status() & TC_CLKSTA) == 0) {
			aic.disableIrq(INT_MASK[index]);
			powerManager.disablePeripheralClock(INT_MASK[index]);
			return true;
		}
		return false;
	}

	public void shutDown() {
        disable();
        tearDown();
	}

	public void setUp() {
		// turn on the correct timer counter clock
		powerManager.enablePeripheralClock(INT_MASK[index]);
		// configure interrupt in AIC
		aic.configure(INT_MASK[index], IAT91_AIC.AIC_IRQ_PRI_NORMAL, IAT91_AIC.SRCTYPE_HIGH_LEVEL);
		// unmask interrupt in AIC
		aic.enableIrq(INT_MASK[index]);
	}
	
	public static int getSystemTicks() {
		return Unsafe.getInt(BLOCK_BASE_ADDRESS[1], tcBases[1] + TC_CVR);
	}

}
