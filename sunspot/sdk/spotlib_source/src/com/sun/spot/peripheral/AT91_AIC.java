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

import java.io.IOException;

import com.sun.spot.util.Utils;
import com.sun.squawk.Address;
import com.sun.squawk.Unsafe;
import com.sun.squawk.VM;

/*
 * This class is the Java interface to the AT91 Advanced Interrupt Controller
 * 
 * The Java interrupt handling mechanism works like this:
 * 
 * At startup the main() in os.c calls set_up_java_interrupts() in eb40a-io.c which
 * puts the address of the asm interrupt handler in the Source Vector Register for
 * every interrupt source available to Java. All other set up is done in Java.
 * 
 * The asm interrupt handler is java_irq_hndl in java_irq_hndl.s.
 * 
 * Communication between the asm interrupt handler and the C code is via an unsigned int
 * called java_irq_status. Each bit represents a different irq. The handler sets
 * the appropriate bit every time it handles an interrupt. The handler also masks
 * the interrupt that has just occurred in the AIC to prevent another interrupt until
 * the first has been serviced. The asm handler then signals "end of interrupt" to the
 * AIC and returns.
 * 
 * A Java thread detects an interrupt by calling aic.waitForInterrupt. This method performs a
 * Channel IO request. If the appropriate bit in java_irq_status is set the bit is
 * cleared and the request returns immediately. If not the calling thread is blocked
 * until the interrupt occurs.
 * 
 * To handle an interrupt in Java you must:
 * 
 * 1) Call aic.configure() to set the interrupt priority and trigger mode
 * 2) Configure the source device so that it will generate an interrupt request
 * 3) Call aic.enableIrq() to allow the AIC to receive the request
 * 4) Call aic.waitForInterrupt() to wait for the interrupt.
 * 5) Do whatever processing is necessary to clear the request
 * 6) Call aic.enableIrq() to allow another interrupt.
 * 7) Repeat from 4)
 *
 * If you don't want more interrupts then don't call aic.enableIrq().
 *  
 */
class AT91_AIC implements IAT91_AIC {

	private static final Address AIC_BASE_ADDR = Address.fromPrimitive(0xFFFFF000);
	private static final int AIC_SMR =      0;
	private static final int AIC_SVR =   0x80 >> 2;
	private static final int AIC_IMR =  0x110 >> 2;
	private static final int AIC_IECR = 0x120 >> 2; // expressed as 32-bit word offset
	private static final int AIC_IDCR = 0x124 >> 2;
	private static final int AIC_ICCR = 0x128 >> 2;
	
	/**
	 * Configure an interrupt for java handling
	 * @param irq irq mask (ie one bit set according to which interrupt we are handling)
	 * @param pri priority (0 to 7, with 7 being the highest)
	 * @param mode word to write into AIC_SMR
	 */
	public void configure(int irq, int pri, int mode) {
		if ((IAT91_Peripherals.PERIPHERALS_ACCESSIBLE_FROM_JAVA & irq) == 0) {
			throw new SpotFatalException("The peripheral with irq mask " + irq + " is not accessible from Java");
		}
		
		// Calculate the zero-based irq index based on the mask
		int irqIndex = 0;
		int irqTemp = irq >> 1;
		while (irqTemp != 0) {
			irqIndex++;
			irqTemp = irqTemp >> 1;
		}
		
	    // Disable the interrupt on the interrupt controller
		Unsafe.setInt(AIC_BASE_ADDR, AIC_IDCR, irq) ;
		
		// Clear the interrupt on the interrupt controller
		Unsafe.setInt(AIC_BASE_ADDR, AIC_ICCR, irq);

	    // Set the mode 
		Unsafe.setInt(AIC_BASE_ADDR, AIC_SMR + irqIndex, mode | pri);
	}

	/**
	 * @param irq irq mask (ie one bit set according to which interrupt we are handling)
	 */
	public void enableIrq(int irq) {
		Unsafe.setInt(AIC_BASE_ADDR, AIC_IECR, irq);
	}

	/**
	 * @param irq irq mask (ie one bit set according to which interrupt we are handling)
	 */
	public void waitForInterrupt(int irq) {
        try {
			VM.waitForInterrupt(irq);
		} catch (IOException e) {
			throw new SpotFatalException(e.getMessage());
		}
	}

	/**
	 * @param irq
	 */
	public void disableIrq(int irq) {
		Unsafe.setInt(AIC_BASE_ADDR, AIC_IDCR, irq);
	}

	public void clearIrq(int irq) {
		Unsafe.setInt(AIC_BASE_ADDR, AIC_ICCR, irq);
	}

	public boolean tearDown() {
		int enabledInterrupts = getEnabledInterrupts();
		Utils.log("AIC at tear down - enabled interrupts are " + enabledInterrupts);
		return enabledInterrupts == 0;
	}

	public void setUp() {
	}

	public void shutDown() {
	}

	public String getDriverName() {
		return "AIC";
	}

	public boolean isEnabled(int irq) {
		return (getEnabledInterrupts() & irq) != 0;
	}

	private int getEnabledInterrupts() {
		return Unsafe.getInt(AIC_BASE_ADDR, AIC_IMR) & IAT91_Peripherals.PERIPHERALS_ACCESSIBLE_FROM_JAVA;
	}

}
