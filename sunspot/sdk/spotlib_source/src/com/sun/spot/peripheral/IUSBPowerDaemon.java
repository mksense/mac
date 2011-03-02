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

public interface IUSBPowerDaemon {

	/**
	 * No power applied to USB socket
	 */
	static final int STATE_UNCONNECTED = 0;
	/**
	 * Power detected, waiting to see if enumeration occurs
	 */
	static final int STATE_AWAITING_ENUM = 1;
	/**
	 * Enumerated
	 */
	static final int STATE_ENUMERATED = 2;
	/**
	 * Power detected but enumeration did not occur
	 */
	static final int STATE_BATTERY = 3;
	
	/**
	 * Get the current state of the USB connection
	 * @return a state as defined in IUSBPowerDaemon
	 */
	public abstract int getCurrentState();

	/**
	 * @return true if power is being applied to the USB socket
	 */
	public abstract boolean isUsbPowered();

	/**
	 * @return true if the host has enumerated the USB device
	 */
	public abstract boolean isUsbEnumerated();
	
	/**
	 * @return true if a host process is connected to the USB (may not work for all host OSs)
	 */
	public abstract boolean isUsbInUse();
}
