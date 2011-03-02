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

import com.sun.spot.interisolate.BooleanReplyEnvelope;
import com.sun.spot.interisolate.ReplyEnvelope;
import com.sun.spot.interisolate.RequestSender;
import com.sun.spot.peripheral.IDriver;
import com.sun.spot.peripheral.Spot;
import com.sun.squawk.io.mailboxes.NoSuchMailboxException;

final class ProxyDriver implements IDriver {
	private String name;
	public RequestSender requestSender;

	public ProxyDriver(String name, String driverServerName) throws NoSuchMailboxException {
		this.name = new String(name); // construct a string to avoid cross-isolate reference
		requestSender = RequestSender.lookup(driverServerName);
	}
	
	public ProxyDriver(String name) {
		this.name = new String(name); // construct a string to avoid cross-isolate reference
	}

	public String getDriverName() {
		return name;
	}

	public boolean tearDown() {
		if (isChildIsolateStillThere()) {
			ReplyEnvelope resultEnvelope = requestSender.send(new TearDownCommand(name));
			resultEnvelope.checkForRuntimeException();
			return ((BooleanReplyEnvelope)resultEnvelope).getBooleanContents();
		} else {
			deregister();
			return true;
		}
	}

	public void shutDown() {
		if (isChildIsolateStillThere()) {
			ReplyEnvelope resultEnvelope = requestSender.send(new ShutDownCommand(name));
			resultEnvelope.checkForRuntimeException();
		}
	}

	private boolean isChildIsolateStillThere() {
		return requestSender.isOpen();
	}

	private void deregister() {
		Spot.getInstance().getDriverRegistry().remove(this);
	}

	public void setUp() {
		ReplyEnvelope resultEnvelope = requestSender.send(new SetUpCommand(name));
		resultEnvelope.checkForRuntimeException();	
	}
	
	public boolean equals(Object arg0) {
		return arg0 instanceof ProxyDriver && 
			((ProxyDriver)arg0).name.equals(name);
	}
	
	public int hashCode() {
		return name.hashCode();
	}
}
