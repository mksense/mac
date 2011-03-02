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
 * A PIOPin is a convenient way of manipulating an individual PIO pin. It's maybe
 * not as efficient as going direct to the PIO but it's simpler.
 * 
 * The normal way of using PIOPin is:
 * 
 * 1. Create a PIOPin or get one from Spot.getInstance().getSpotPins()
 * 2. Claim the pin
 * 3. Open it for input or output
 * 4. Set it high or low
 */
public class PIOPin {

	/**
	 * multiplex option that puts the pin under control of the PIO
	 * (this is the default for PIOPins)
	 */
	public static final int IO = 0;
	/**
	 * multiplex option that puts the pin under control of PERIPHERAL A
	 */
	public static final int PERIPHERAL_A = 1;
	/**
	 * multiplex option that puts the pin under control of PERIPHERAL B
	 */
	public static final int PERIPHERAL_B = 2;

	/**
	 * The PIO pin mask for this pin
	 */
	public int pin;
	/**
	 * The PIO for this pin
	 */
	public IAT91_PIO pio;
	/**
	 * The multiplex selection for this pin
	 */
	public int multiplex;

	/**
	 * @param pio the PIO that controls this pin
	 * @param pin the pin mask (i.e. only one bit set)
	 * @param multiplex the multiplex option
	 */
	public PIOPin(IAT91_PIO pio, int pin, int multiplex) {
		this.pio = pio;
		this.pin = pin;
		this.multiplex = multiplex;
	}

	/**
	 * Create a PIOPin that's controlled by a PIO
	 * @param pio the PIO that controls this pin
	 * @param pin the pin mask
	 */
	public PIOPin(IAT91_PIO pio, int pin) {
		this(pio, pin, IO);
	}

	/**
	 * Attempt to claim this pin
	 */
	public void claim() {
		pio.claim(pin, multiplex == IO ? pin : 0, multiplex == PERIPHERAL_A);
	}

	/**
	 * Release the claim on this pin
	 */
	public void release() {
		pio.release(pin);
	}

	/**
	 * Open this pin for output
	 */
	public void openForOutput() {
		pio.open(pin, IAT91_PIO.OUTPUT);
	}

	/**
	 * Open this pin for input
	 */
	public void openForInput() {
		pio.open(pin, IAT91_PIO.INPUT);
	}

	/**
	 * Set this pin to the specified state
	 * @param state if true set to high, else low
	 */
	public void setState(boolean state) {
		pio.write(pin, state ? IAT91_PIO.SET_OUT : IAT91_PIO.CLEAR_OUT);
	}

	/**
	 * Set this pin high
	 */
	public void setHigh() {
		pio.write(pin, IAT91_PIO.SET_OUT);
	}

	/**
	 * Set this pin low
	 */
	public void setLow() {
		pio.write(pin, IAT91_PIO.CLEAR_OUT);
	}

	/**
	 * @return true if this pin is high
	 */
	public boolean isHigh() {
		return (pio.read() & pin) != 0;
	}

	/**
	 * @return true if this pin is low
	 */
	public boolean isLow() {
		return (pio.read() & pin) == 0;
	}
	
	/**
	 * Enable pin change interrupts on this pin
	 */
	public void enableIrq() {
		pio.enableIrq(pin);
	}
	
	/**
	 * Wait for a pin change interrupt on this pin
	 * @throws InterruptedException 
	 */
	public void waitForIrq() throws InterruptedException {
		pio.waitForIrq(pin);
	}

	/**
	 * Disable pin change interrupts on this pin
	 */
	public void disableIrq() {
		pio.disableIrq(pin);
	}
	
	public String toString() {
		return "PIOPin-"+pio.getDriverName()+"-"+pin;
	}
}

