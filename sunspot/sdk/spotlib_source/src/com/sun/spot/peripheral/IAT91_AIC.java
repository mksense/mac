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
 * The interface to the AT91 Interrupt Controller.<br><br>
 * 
 * The Java interrupt handling mechanism works like this:<br><br>
 * 
 * At startup the main() in os.c calls set_up_java_interrupts() in eb40a-io.c which
 * puts the address of the generic asm interrupt handler in the Source Vector Register for
 * every interrupt source available to Java. All other set up is done in Java.<br><br>
 * 
 * The asm interrupt handler is java_irq_hndl in java_irq_hndl.s.<br><br>
 * 
 * Communication between the asm interrupt handler and the C code is via an unsigned int
 * called java_irq_status. Each bit represents a different irq. The handler sets
 * the appropriate bit every time it handles an interrupt. The handler also masks
 * the interrupt that has just occurred in the AIC to prevent another interrupt until
 * the first has been serviced. The asm handler then signals "end of interrupt" to the
 * AIC and returns.<br><br>
 * 
 * A Java thread detects an interrupt by calling {@link IAT91_AIC#waitForInterrupt(int)}.
 * This method performs a Channel IO request. If the appropriate bit in java_irq_status
 * is set the bit is cleared and the request returns immediately. If not the calling thread
 * is blocked until the interrupt occurs.<br><br>
 * 
 * To handle an interrupt in Java you must:<br><br>
 * 
 * 1) Call {@link IAT91_AIC#configure(int, int, int)} to set the interrupt priority and trigger mode<br>
 * 2) Configure the source device so that it will generate an interrupt request<br>
 * 3) Call {@link IAT91_AIC#enableIrq(int)} to allow the AIC to receive the request<br>
 * 4) Call {@link IAT91_AIC#waitForInterrupt(int)} to wait for the interrupt.<br>
 * 5) Do whatever processing is necessary to clear the request<br>
 * 6) Call {@link IAT91_AIC#enableIrq(int)} to allow another interrupt.<br>
 * 7) Repeat from 4)<br><br>
 *
 * If you don't want more interrupts then don't call {@link IAT91_AIC#enableIrq(int)}.<br>
  * 
 * @author Syntropy
 */
public interface IAT91_AIC extends IDriver {
	/**
	 * Select positive edge triggered interrupt source
	 */
	public static final int SRCTYPE_POSITIVE_EDGE = 0x60;
	/**
	 * Select high level triggered interrupt source
	 */
	public static final int SRCTYPE_HIGH_LEVEL = 0x40;
	/**
	 * Select negative edge triggered interrupt source (external sources only)
	 */
	public static final int SRCTYPE_EXT_NEGATIVE_EDGE = 0x20;
	/**
	 * Select low level triggered interrupt source (external sources only)
	 */
	public static final int SRCTYPE_EXT_LOW_LEVEL = 0x0;

	/**
	 * Normal priority value (pri=4)
	 */
	public static final int AIC_IRQ_PRI_NORMAL = 4;


	/**
	 * Configure an interrupt for java handling.
	 * @see IAT91_Peripherals for constants that define the interrupts.
	 * Select one of the modes defined by the SRCTYPE constants in this file
	 * @param irq irq mask (ie one bit set according to which interrupt we are handling)
	 * @param pri priority (0 to 7, with 7 being the highest)
	 * @param mode word to write into AIC_SMR
	 */
	public abstract void configure(int irq, int pri, int mode);

	/**
	 * Enable interrupts from selected source.
	 * @param irq irq mask (ie one bit set according to which interrupt we are handling)
	 */
	public abstract void enableIrq(int irq);

	/**
	 * Suspend this thread until the selected interrupt occurs.
	 * @param irq irq mask (ie one bit set according to which interrupt we are handling)
	 */
	public abstract void waitForInterrupt(int irq);

	/**
	 * Disable interrupts from the selected source.
	 * @param irq irq mask (ie one bit set according to which interrupt we are handling)
	 */
	public abstract void disableIrq(int irq);
	
	/**
	 * Clear any pending interrupt request for the specified IRQ. Note that this only
	 * clears an interrupt that is pending in the AIC. If the interrupt has already been
	 * detected by the low-level handler then a subsequent waitForInterrupt() will still
	 * return immediately.
	 * @param irq irq mask (ie one bit set according to which interrupt we are handling)
	 */
	public abstract void clearIrq(int irq);

	/**
	 * Check whether the specified interrupt is enabled in the AIC hardware. Note that
     * the interrupt will be disabled automatically when an interrupt occurs.
	 * @param irq irq mask (ie one bit set according to which interrupt we are handling)
     * @return true if the interrupt is enabled in the AIC hardware
	 */
	public abstract boolean isEnabled(int irq);
}
