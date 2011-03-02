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
 * This interface provides symbolic access to the RM9200 peripheral ids.
 * The ids are held in the form of a bit mask, with one bit set in the position
 * corresponding to the id of the peripheral (eg if the id is 4, bit 4 is set).
 * These ids are used to specify the required device when enabling interrupts or
 * enabling peripheral clocks.
 * 
 * This interface also defines which peripherals are accessible from Java.
 */
public interface IAT91_Peripherals {
	
	public static final int FIQ_ID_MASK			= 1 << 0;
	public static final int SYSIRQ_ID_MASK		= 1 << 1;
	public static final int PIOA_ID_MASK		= 1 << 2;
	public static final int PIOB_ID_MASK		= 1 << 3;
	public static final int PIOC_ID_MASK		= 1 << 4;
	public static final int PIOD_ID_MASK		= 1 << 5;
	public static final int US0_ID_MASK			= 1 << 6;
	public static final int US1_ID_MASK			= 1 << 7;
	public static final int US2_ID_MASK			= 1 << 8;
	public static final int US3_ID_MASK			= 1 << 9;
	public static final int MCI_ID_MASK			= 1 << 10;
	public static final int UDP_ID_MASK			= 1 << 11;
	public static final int TWI_ID_MASK			= 1 << 12;
	public static final int SPI_ID_MASK			= 1 << 13;
	public static final int SSC0_ID_MASK		= 1 << 14;
	public static final int SSC1_ID_MASK		= 1 << 15;
	public static final int SSC2_ID_MASK		= 1 << 16;
	public static final int TC0_ID_MASK			= 1 << 17;
	public static final int TC1_ID_MASK			= 1 << 18;
	public static final int TC2_ID_MASK			= 1 << 19;
	public static final int TC3_ID_MASK			= 1 << 20;
	public static final int TC4_ID_MASK			= 1 << 21;
	public static final int TC5_ID_MASK			= 1 << 22;
	public static final int UHP_ID_MASK			= 1 << 23;
	public static final int EMAC_ID_MASK		= 1 << 24;
	public static final int IRQ0_ID_MASK		= 1 << 25;
	public static final int IRQ1_ID_MASK		= 1 << 26;
	public static final int IRQ2_ID_MASK		= 1 << 27;
	public static final int IRQ3_ID_MASK		= 1 << 28;
	public static final int IRQ4_ID_MASK		= 1 << 29;
	public static final int IRQ5_ID_MASK		= 1 << 30;
	public static final int IRQ6_ID_MASK		= 1 << 31;
	
	/* C code manages:
	 *   usarts 0 and 1 and the usb device port (which it uses for communications)
	 *   spi (which it needs to talk to the power controller)
	 *   timer counters 4 and 5 (which underpin delay.c).
	 *   What is SYSIRQ?
	 */
	
	// This list should match that in system.h
	public static final int PERIPHERALS_NOT_ACCESSIBLE_FROM_JAVA =
		SYSIRQ_ID_MASK | US0_ID_MASK | US1_ID_MASK | UDP_ID_MASK | SPI_ID_MASK | TC4_ID_MASK | TC5_ID_MASK | IRQ4_ID_MASK | IRQ5_ID_MASK;
	
	public static final int PERIPHERALS_ACCESSIBLE_FROM_JAVA = 0xFFFFFFFF & ~PERIPHERALS_NOT_ACCESSIBLE_FROM_JAVA;
		
		
}
