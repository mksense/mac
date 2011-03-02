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
 * Interface to LTC3455 power control chip on the Spot.
 * This regulates Spot power and charges the battery
 * if USB power is available.
 * 
 * @author Syntropy
 */

public interface ILTC3455 {

	/**
	 * Set high power state. If the Spot is connected to USB and high power
	 * (500mA) has been negotiated with the host the battery can be charged
	 * using a higher current.
	 * 
	 * @param state whether high power is available
	 */
	public void setHighPower (boolean state);

	/**
	 * Read the current high power state.
	 * 
	 * @return the high power state
	 */
	public boolean isHighPower();

	/**
	 * Set USB suspended mode (low power). Normally done in response to a USB
	 * host requesting a suspend for power saving.
	 */
	public void setSuspended (boolean state);

	/**
	 * Read the current USB suspended state.
	 * 
	 * @return the suspend state
	 */
	public boolean isSuspended();
}
