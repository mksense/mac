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

public class PeripheralChipSelect {
	
	public static final PeripheralChipSelect SPI_PCS_0 = new PeripheralChipSelect(0);
	public static final PeripheralChipSelect SPI_PCS_BD_SEL1 = SPI_PCS_0;
	public static final PeripheralChipSelect SPI_PCS_1 = new PeripheralChipSelect(1);
	public static final PeripheralChipSelect SPI_PCS_BD_SEL2 = SPI_PCS_1;
	public static final PeripheralChipSelect SPI_PCS_2 = new PeripheralChipSelect(2);
	public static final PeripheralChipSelect SPI_PCS_CC2420 = SPI_PCS_2;
	public static final PeripheralChipSelect SPI_PCS_3 = new PeripheralChipSelect(3);
	public static final PeripheralChipSelect SPI_PCS_POWER_CONTROLLER = SPI_PCS_3;

	private int pcsIndex;

	private PeripheralChipSelect(int pcsIndex) {
		this.pcsIndex = pcsIndex;
	}
	
	public int getPcsIndex() {
		return pcsIndex;
	}
	
	public String toString() {
		switch (pcsIndex) {
		case 0:
			return "PCS_BD_SEL1";
		case 1:
			return "PCS_BD_SEL2";
		case 2:
			return "PCS_CC2420";
		case 3:
			return "PCS_PCTRL";
		default:
			return "PCS_" + pcsIndex;
		}
	}
}
