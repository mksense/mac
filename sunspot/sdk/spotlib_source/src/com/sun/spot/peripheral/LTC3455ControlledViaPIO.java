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


class LTC3455ControlledViaPIO implements ILTC3455, IDriver {

	private PIOPin usbHpPin;
	private PIOPin usbEnPin;

	/**
	 * @param pins
	 */
	public LTC3455ControlledViaPIO(ISpotPins spotPins) {
		usbHpPin = spotPins.getUSB_HP();
		usbEnPin = spotPins.getUSB_EN();
		setUp();
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.spot.ILTC3455#setHighPower(boolean)
	 */
	public void setHighPower(boolean state) {
		usbHpPin.setState(state);
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.spot.ILTC3455#isHighPower()
	 */
	public boolean isHighPower() {
		return usbHpPin.isHigh();
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.spot.ILTC3455#setSuspended(boolean)
	 */
	public void setSuspended(boolean state) {
		usbEnPin.setState(state);
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.spot.ILTC3455#isSuspended()
	 */
	public boolean isSuspended() {
		return usbEnPin.isHigh();
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.spot.IDriver#name()
	 */
	public String getDriverName() {
		return "LTC3455";
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.spot.IDriver#tearDown()
	 */
	public boolean tearDown() {
		usbEnPin.release();
		return true;
	}
	
	public void shutDown() {
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.spot.IDriver#setUp()
	 */
	public void setUp() {
		usbEnPin.claim();
		usbEnPin.setLow();
		usbEnPin.openForOutput();
	}

}
