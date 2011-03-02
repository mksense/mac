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

import java.util.Enumeration;
import java.util.Vector;

import com.sun.spot.util.Utils;
import com.sun.squawk.Isolate;
import com.sun.squawk.VM;

/**
 * Acts as a registry for drivers that need to be aware of deep sleep. The Spot singleton holds a singleton
 * of this class.
 * 
 * Drivers that require to be informed/asked about deep sleep must be registered with the the DriverRegistry
 * by calling add().
 */

public class DriverRegistry implements IDriverRegistry {

	private boolean tracing;
	private Vector drivers;
	private String lastVeto;
	
	DriverRegistry() {
		this.tracing = false;
		drivers = new Vector();
		Isolate.LifecycleListener shutDownListener = new Isolate.LifecycleListener() {
			public void handleLifecycleListenerEvent(Isolate iso, int eventKind) {
				if (eventKind == Isolate.SHUTDOWN_EVENT_MASK) {
					DriverRegistry.this.shutDown();
				}
			}
		};
		Isolate.currentIsolate().addLifecycleListener(shutDownListener, Isolate.SHUTDOWN_EVENT_MASK);
	}
	
	/**
	 * Add a driver to the registry. The driver will now be informed/asked about deep sleep
	 * @param driver The driver to add, which must not already be registered
	 */
	public synchronized void add(IDriver driver) {
		if (! drivers.contains(driver)) {
			if (tracing) Utils.log("Registering " + driver.getDriverName());
			drivers.addElement(driver);
		} else {
			throw new SpotFatalException("Driver " + driver.getDriverName() + " is attempting to register twice");
		}
	}
	
	/**
	 * Remove a driver from the registry
	 * @param driver The driver to remove, which must be registered
	 */
	public synchronized void remove(IDriver driver) {
		if (drivers.contains(driver)) {
			if (tracing) Utils.log("Deregistering " + driver.getDriverName());
			drivers.removeElement(driver);
		} else {
			throw new SpotFatalException("Driver " + driver.getDriverName() + " is attempting to deregister before registering");
		}
	}

	/**
	 * Display a list of all the registered drivers
	 */
	public synchronized String[] getRegisteredDriverNames() {
		String[] result = new String[drivers.size()];
		for (int i = 0; i < drivers.size(); i++) {
			result[i] = ((IDriver) drivers.elementAt(i)).getDriverName();
		}
		return result;
	}

	/**
	 * Activate all registered drivers following a deep sleep
	 */
	synchronized void setUp() {
		for (int i = 0; i < drivers.size(); i++) {
			IDriver driver = (IDriver) drivers.elementAt(i);
			if (tracing) {
				Utils.log("Setting up " + driver.getDriverName());
			}
			driver.setUp();
		}
	}

	/**
	 * Try to deactivate all the drivers, most recent first
	 * @return true if all the drivers report they deactivated successfully, false if any driver couldn't deactivate, in which case the others have been reactivated
	 */
	synchronized boolean tearDown() {
		for (int i = drivers.size() - 1; i >= 0; i--) {
			IDriver driver = ((IDriver) drivers.elementAt(i));
			if (tracing) {
				Utils.log("Tearing down " + driver.getDriverName());
			}
			if (! driver.tearDown()) {
				lastVeto = driver.getDriverName();
				if (tracing) {
					Utils.log(driver.getDriverName()+" driver vetoed tearDown");
				}
				while (i<drivers.size()-1) {
					++i;
					((IDriver) drivers.elementAt(i)).setUp();
				}
				return false;
			}
		}
			
		return true;
	}
	
	synchronized String getLastVeto() {
		return lastVeto;
	}
	
	void setTracing(boolean tracing) {
		this.tracing = tracing;
	}

	/*
	 * Test support only
	 */
	void removeDriverNamed(String nameOfRegisteredDriver) {
		Enumeration dr = drivers.elements();
		while (dr.hasMoreElements()) {
			IDriver driver = (IDriver) dr.nextElement();
			if (driver.getDriverName().equals(nameOfRegisteredDriver)) {
				drivers.removeElement(driver);
			}
		}
	}

	synchronized void shutDown() {
        for (int i = drivers.size() - 1; i >= 0; i--) {
			IDriver driver = (IDriver) drivers.elementAt(i);
			if (tracing) {
				Utils.log("Shutting down " + driver.getDriverName());
			}
			driver.shutDown();
		}		
	}
}
