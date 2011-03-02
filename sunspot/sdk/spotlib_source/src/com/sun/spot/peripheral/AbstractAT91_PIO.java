/*
 * Copyright 2007-2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * AbstractAT91_PIO
 * @see IAT91_PIO
 *
 */
public abstract class AbstractAT91_PIO implements IAT91_PIO {

	protected static final int PIO_OER	= 4;           /* Output Enable Register */
    protected static final int PIO_ODR	= 5;           /* Output Disable Register */
    protected static final int PIO_IFER	= 8;           /* Input Filter Enable Register */
    protected static final int PIO_IFDR	= 9;           /* Input Filter Disable Register */
    protected static final int PIO_SODR	= 12;          /* Set Output Data Register */
    protected static final int PIO_CODR	= 13;          /* Clear Output Data Register */
    protected static final int PIO_PDSR	= 15;          /* Pin Data Status Register */

    static final int[] BASE_ADDRESS = {
    	0xFFFFF400,
    	0xFFFFF600,
    	0xFFFFF800,
    	0xFFFFFA00
    };

	protected Address baseAddress;
	private int baseAddressAsInt;
	protected int pioSelector;

	protected AbstractAT91_PIO(int pioSelector) {
		this.pioSelector = pioSelector;
    	baseAddressAsInt = BASE_ADDRESS[pioSelector];
    	baseAddress = Address.fromPrimitive(baseAddressAsInt);
	}

    /**
     * @param mask
     * @param state
     */
    public void write(int mask, int state) {
        //Utils.log("AT91_PIO::write("+mask+","+state+")");
        if (state == CLEAR_OUT )
        {
            //* Clear PIOs with data at 0 in CODR (Clear Output Data Register)
        	Unsafe.setInt(baseAddress, PIO_CODR,mask);
        }
        else
        {
            //* Set PIOs with data at 1 in SODR (Set Output Data Register)
        	Unsafe.setInt(baseAddress, PIO_SODR,mask);
        }
    }

    /**
     * @param mask
     * @param config
     */
    public void open(int mask, int config) {
        //Utils.log("AT91_PIO::open("+mask+","+config+")");

    	checkOwned(mask);

        //* If PIOs required to be output
        if ((config & SENSE_BIT) != 0 )
        {
            //* Defines the PIOs as output
        	Unsafe.setInt(baseAddress, PIO_OER,mask);
        }
        else
        {
            //* Defines the PIOs as input
        	Unsafe.setInt(baseAddress, PIO_ODR,mask) ;
        }
    }

    public int read() {
        return Unsafe.getInt(baseAddress, PIO_PDSR);
    }

	public int getBaseAddress() {
		return baseAddressAsInt;
	}

	public void enableGlitchFilter(int mask) {
		Unsafe.setInt(baseAddress, PIO_IFER, mask);
	}

	public void disableGlitchFilter(int mask) {
		Unsafe.setInt(baseAddress, PIO_IFDR, mask);
	}

    /**
     * Lay claim to certain PIO pins
     * 
     * @param mask - claimed pins
     * @param drive - bits set to 1 for pins to be controlled by PIO. Bits set to 0 default to peripheral A.
     */
    public void claim(int mask, int drive) {
    	claim(mask, drive, true);
    }

	public String getDriverName() {
		return "PIO-"+(char)('A'+pioSelector);
	}

	/* Abstract methods for dealing with pin claiming */
	public abstract int available();
	public abstract void claim(int mask, int drive, boolean claimForPeriphA);
	public abstract void release(int mask);

	/* Abstract methods for dealing with interrupts */
	public abstract void disableIrq(int irq);
	public abstract void enableIrq(int mask);
	public abstract void waitForIrq(int irq) throws InterruptedException;
	
	/* Abstract support methods */
	protected abstract void checkOwned(int mask);
}
