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
import com.sun.squawk.Address;
import com.sun.squawk.Unsafe;
import com.sun.squawk.VM;
import com.sun.squawk.vm.ChannelConstants;


class AT91_PowerManager implements IAT91_PowerManager {
	
	private static final int BASE_ADDR = 0xFFFFFC00;
	private static final int PS_PCER = 0x4; // NB 32-bit unit offsets
	private static final int PS_PCDR = 0x5;
	private static final int PS_PCSR = 0x6;
	private static final int PMC_SR = 0x68 >> 2;
	private static final int CKGR_PLLBR = 0x2C >> 2;

	private int normalPLLBRegisterValue = Unsafe.getInt(Address.fromPrimitive(BASE_ADDR), CKGR_PLLBR);
	
	public void enablePeripheralClock(int mask) {
		if ((IAT91_Peripherals.PERIPHERALS_ACCESSIBLE_FROM_JAVA & mask) == 0) {
			throw new SpotFatalException("The peripheral with mask " + mask + " is not accessible from Java");
		}
		Unsafe.setInt(Address.fromPrimitive(BASE_ADDR), PS_PCER, mask);		
	}

	public void disablePeripheralClock(int mask) {
		if ((IAT91_Peripherals.PERIPHERALS_ACCESSIBLE_FROM_JAVA & mask) == 0) {
			throw new SpotFatalException("The peripheral with mask " + mask + " is not accessible from Java");
		}
		Unsafe.setInt(Address.fromPrimitive(BASE_ADDR), PS_PCDR, mask);		
	}

	public int getEnabledPeripheralClocks() {
		return Unsafe.getInt(Address.fromPrimitive(BASE_ADDR), PS_PCSR) & IAT91_Peripherals.PERIPHERALS_ACCESSIBLE_FROM_JAVA;
	}
	
	public String getDriverName() {
		return "PowerManager";
	}

	public boolean tearDown() {
		int enabledPeripheralClocks = getEnabledPeripheralClocks();
		Utils.log(getDriverName() + ": Enabled clocks are " + enabledPeripheralClocks);
		return enabledPeripheralClocks == 0;
	}

	public void setUp() {
	}

	public void shutDown() {
	}

	public void setUsbEnable(boolean enable) {
		boolean currentlyEnabled = (Unsafe.getInt(Address.fromPrimitive(BASE_ADDR), PS_PCSR) & IAT91_Peripherals.UDP_ID_MASK) != 0;
		
		if (enable && !currentlyEnabled) {
			Unsafe.setInt(Address.fromPrimitive(BASE_ADDR), CKGR_PLLBR, normalPLLBRegisterValue);
			while ((Unsafe.getInt(Address.fromPrimitive(BASE_ADDR), PMC_SR) & 1<<2) == 0);
			Unsafe.setInt(Address.fromPrimitive(BASE_ADDR), PS_PCER, IAT91_Peripherals.UDP_ID_MASK);
		} else if (!enable && currentlyEnabled) {
			Unsafe.setInt(Address.fromPrimitive(BASE_ADDR), PS_PCDR, IAT91_Peripherals.UDP_ID_MASK);
			Unsafe.setInt(Address.fromPrimitive(BASE_ADDR), CKGR_PLLBR, 0);
		}
	}

	public void setUsartEnable(boolean enable) {
		if (enable) {
			Unsafe.setInt(Address.fromPrimitive(BASE_ADDR), PS_PCER, IAT91_Peripherals.US0_ID_MASK | IAT91_Peripherals.US1_ID_MASK);		
		} else {
			Unsafe.setInt(Address.fromPrimitive(BASE_ADDR), PS_PCDR, IAT91_Peripherals.US0_ID_MASK | IAT91_Peripherals.US1_ID_MASK);		
		}
	}

	public void setShallowSleepClockMode(int mode) {
            System.err.println("Cannot change shallow sleep clock speed."); // due to bug 1386
            // VM.execSyncIO(ChannelConstants.SET_SHALLOW_SLEEP_CLOCK_MODE, mode);
	}

}
