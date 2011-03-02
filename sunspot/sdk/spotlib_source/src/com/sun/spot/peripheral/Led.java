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


/**
 * Class that implements the behaviour of simple LEDs, as found on the Spot board and the testboard.<br><br>
 * 
 * This class is not for general use. Access LEDs via the Spot or TestBoard classes.
 */
class Led implements ILed, IDriver { 

    private PIOPin pin;
    // SENSE = true means AT91_PIO.SET_OUT turns the LED on
    private boolean sense;
    private boolean isOn = false;

    /**
     * @param piopin The pio pin to be used for this LED
     * @param sense The logical sense of this LED
     */
    public Led(PIOPin piopin, boolean sense) {
    	this.pin = piopin;
    	this.sense = sense;
    	setUp();
    }

    public void setOn() {
    	setOn(true);
    }

    public void setOff() {
    	setOn(false);
    }

	public void setOn(boolean on) {
		isOn = on;
    	pin.pio.write(pin.pin, (sense ^ isOn) ? IAT91_PIO.CLEAR_OUT : IAT91_PIO.SET_OUT);
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.spot.IDriver#name()
	 */
	public String getDriverName() {
		return "LED on " + pin;
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.spot.IDriver#tearDown()
	 */
	public boolean tearDown() {
		pin.release();
		return true;
	}

	public void shutDown() {
		setOff();
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.spot.IDriver#setUp()
	 */
	public void setUp() {
    	pin.claim(); // claim it and ask PIO to drive it
    	setOn(isOn);
        pin.openForOutput();
	}

	public boolean isOn() {
		return isOn;
	}
}
