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

package com.sun.spot.peripheral.driver.proxy;

import java.util.Hashtable;
import java.util.Vector;

import com.sun.spot.interisolate.InterIsolateServer;
import com.sun.spot.interisolate.ReplyEnvelope;
import com.sun.spot.interisolate.RequestSender;
import com.sun.spot.peripheral.IDriver;
import com.sun.spot.peripheral.IDriverLookup;
import com.sun.spot.peripheral.IDriverRegistry;
import com.sun.spot.util.Utils;
import com.sun.squawk.Isolate;
import com.sun.squawk.VM;
import com.sun.squawk.io.mailboxes.NoSuchMailboxException;

public class ProxyDriverRegistry implements IDriverRegistry, IDriverLookup {
	public static final String DRIVER_REGISTRY_SERVER = "DRIVER_REGISTRY_SERVER";
	public static final String DRIVER_SERVER = "DRIVER_SERVER";
	private RequestSender requestSender;
	private Hashtable namesToDrivers = new Hashtable();
	private Hashtable driversToNames = new Hashtable();
	private int uniqueNumber = 0;
	
	public ProxyDriverRegistry() {
		try{
			requestSender = RequestSender.lookup(DRIVER_REGISTRY_SERVER);
		} catch (NoSuchMailboxException e) {
			throw new RuntimeException(e.getMessage());
		}
		// This server runs at MAX_SYS_PRIORITY. This is because call backs 
		// will come from SleepManager during setup and teardown, when normal priorities
		// will be blocked
		InterIsolateServer.run(getDriverServerName(), this, VM.MAX_SYS_PRIORITY);
	}
	
	/*
	 * For testing only
	 */
	ProxyDriverRegistry(RequestSender requestSender) {
		this.requestSender = requestSender;
	}

	public void add(IDriver driver) {
		String remoteName = (String) driversToNames.get(driver);
		
		if (remoteName == null) {
			remoteName = createRemoteDriverName(driver);
			namesToDrivers.put(remoteName, driver);
			driversToNames.put(driver, remoteName);
		}
		ReplyEnvelope resultEnvelope = requestSender.send(new AddCommand(remoteName, getDriverServerName()));
		resultEnvelope.checkForRuntimeException();
	}

	private String getDriverServerName() {
		return DRIVER_SERVER + Isolate.currentIsolate().getId();
	}

	private String createRemoteDriverName(IDriver driver) {
		return "[child  " + Isolate.currentIsolate().getId() + ":" + 
			(uniqueNumber ++) + "] " + driver.getDriverName();
	}
	
	/*
	 * This method is provided to allow a shutdown for drivers in this isolate only. It
	 * should be called by the isolate exit hook.
	 */
	synchronized void notifyShutdown() {
        Vector v = getDrivers();
        
        for (int i = 0; i < v.size(); i++) {
			IDriver driver = (IDriver)(v.elementAt(i));
			remove(driver);
			Utils.log("Shutting down " + driver.getDriverName());
			driver.shutDown();
		}
	}
	
	public IDriver getDriverNamed(String name) {
		return (IDriver) namesToDrivers.get(name);
	}

	public void remove(IDriver driver) {
		String remoteName = (String) driversToNames.get(driver);

		if (remoteName == null) {
			remoteName = createRemoteDriverName(driver);
		}
			
		ReplyEnvelope resultEnvelope = requestSender.send(new RemoveCommand(remoteName));
		namesToDrivers.remove(remoteName);
		driversToNames.remove(driver);
		resultEnvelope.checkForRuntimeException();
	}

	Vector getDrivers() {
		return Utils.enumToVector(namesToDrivers.elements());
	}
}
