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
 * Interface for all device drivers participating in the deep sleep
 * setUp/tearDown process controlled by the DriverRegistry.
 * 
 * @author Syntropy
 */

public interface IDriver {

	/**
	 * An identifying name for the driver (e.g. "AIC" for the AIC driver).
	 * 
	 * @return The driver name
	 */
	String getDriverName();
	
	/**
	 * Deactivate the driver (usually in preparation for deep sleep). The driver should
	 * store any important state and release all resources it has claimed from other drivers.<br />
	 * <br />
	 * Drivers are torn down in the inverse order to that in which they registered, so that in general
	 * user drivers will be torn down before any underlying system drivers.
	 * 
	 * @return True if the driver is able to deactivate, false if it cannot deactivate (e.g.
	 * due to being busy with a data transfer). If any driver returns false, no deep sleep occurs
	 * and all other drivers will be reactivated.
	 */
	boolean tearDown();

	/**
	 * Activate or reactivate the driver (after a deep sleep or when another driver refused to deep sleep).
	 * The driver should claim all the resources it needs from other drivers and reinitialize its hardware.
	 * It should also restore any state saved before tearDown.<br />
	 * <br />
	 * Drivers are set up in the order that they registered, so that in general user drivers will be set up
	 * after any underlying system drivers.
	 */
	void setUp();
	
	/**
	 * Notify the driver that the VM is about to exit.
	 */
	void shutDown();

}
