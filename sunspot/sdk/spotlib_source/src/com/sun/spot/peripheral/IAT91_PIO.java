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
 * Interface to the AT91 Parallel I/O Controller.
 * 
 * @author Syntropy
 */
public interface IAT91_PIO {
	/**
	 * Symbols identifying the four PIOs
	 */
	public static final int PIOA = 0;
	public static final int PIOB = 1;
	public static final int PIOC = 2;
	public static final int PIOD = 3;

	/**
	 * A single bit that can be used to test against
	 */
	public static final int SENSE_BIT = 0x1;

	/**
	 * In calls to {@link IAT91_PIO#open(int, int)}, defines the selected pins to be outputs.
	 */
	public static final int OUTPUT = 0x1;

	/**
	 * In calls to {@link IAT91_PIO#open(int, int)}, defines the selected pins to be inputs.
	 */
	public static final int INPUT = 0x0;

	/**
	 * In calls to {@link IAT91_PIO#write(int, int)}, causes the selected pins to be set to 1.
	 */
	public static final int SET_OUT = 0x0;

	/**
	 * In calls to {@link IAT91_PIO#write(int, int)}, causes the selected pins to be set to 0.
	 */
	public static final int CLEAR_OUT = 0x1;

	/**
	 * Return the mask of available pins. An available pin can be claimed.
	 * @return mask
	 */
	public abstract int available();

	/**
	 * Lay claim to certain PIO pins.
	 * 
	 * @param mask claimed pins
	 * @param drive bits set to 1 for pins to be controlled by PIO. Bits set to 0 default to peripheral A.
	 */
	public abstract void claim(int mask, int drive);

	/**
	 * Lay claim to certain PIO pins.
	 * 
	 * @param mask claimed pins
	 * @param drive bits set to 1 for pins to be controlled by PIO
	 * @param claimForPeriphA if true assign undriven pins to peripheral A; if false to peripheral B
	 */
	// TODO: consider changing this to 3 state as in PIOPin.multiplex
	public abstract void claim(int mask, int drive, boolean claimForPeriphA);

	/**
	 * Release claim to certain PIO pins
	 * 
	 * @param mask released pins
	 */
	public abstract void release(int mask);

	/**
	 * Configure the direction of selected pins.
	 * @param mask The pins to be configured
	 * @param config Indicates whether these pins are to be input ({@link #INPUT}) or output ({@link #OUTPUT})
	 */
	public abstract void open(int mask, int config);

	/**
	 * Set the state of selected output pins
	 * @param mask The pins to be set or cleared
	 * @param state Indicates whether these pins are to be set ({@link #SET_OUT}) or cleared ({@link #CLEAR_OUT})
	 */
	public abstract void write(int mask, int state);

	/**
	 * Read the instantaneous state of the pio pins
	 * @return The current state as a 32 bit mask
	 */
	public abstract int read();

	/**
	 * Enable glitch filters for the specified pin(s)
	 * @param mask the pio pin mask
	 */
	public abstract void enableGlitchFilter(int mask);

	/**
	 * Disable glitch filters for the specified pin(s)
	 * @param mask the pio pin mask
	 */
	public abstract void disableGlitchFilter(int mask);

	/**
	 * Enable PIO interrupts for the specified pin
	 * @param mask the pio pin mask
	 */
	public abstract void enableIrq(int mask);

	/**
	 * Wait for the specified PIO pin to generate an interrupt.
	 * On exit, the pin is masked so that it will not generate another interrupt.
	 * @param irq A mask with a single bit set indicating the interrupt awaited
	 * @throws InterruptedException 
	 */
	public abstract void waitForIrq(int irq) throws InterruptedException;

	/**
	 * Disable PIO interrupts for the specified pin
	 * @param irq A mask with a single bit set indicating the interrupt
	 */
	public abstract void disableIrq(int irq);

	/**
	 * Get the base address of this PIO
	 * @return the base address
	 */
	public abstract int getBaseAddress();

	/**
	 * @return the name of this PIO
	 */
	public abstract String getDriverName();
}
