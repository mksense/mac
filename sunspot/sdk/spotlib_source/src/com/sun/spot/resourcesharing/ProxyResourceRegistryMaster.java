/*
 * Copyright 2007-2008 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.spot.resourcesharing;

import com.sun.spot.interisolate.BooleanReplyEnvelope;
import com.sun.spot.interisolate.ReplyEnvelope;
import com.sun.spot.interisolate.RequestSender;
import com.sun.spot.peripheral.SpotFatalException;
import com.sun.squawk.io.mailboxes.NoSuchMailboxException;

public class ProxyResourceRegistryMaster implements IResourceRegistryMaster {

	public static final String RESOURCE_REGISTRY_SERVER = "RESOURCE_REGISTRY_SERVER";
	private RequestSender requestSender;

	public ProxyResourceRegistryMaster() {
		try{
			requestSender = RequestSender.lookup(RESOURCE_REGISTRY_SERVER);						
		} catch (NoSuchMailboxException ex) {
			throw new RuntimeException(ex.getMessage());
		}
	}

	public boolean lock(int isolateId, String resourceName, ResourceSharingScheme scheme) throws ResourceUnavailableException {
		ReplyEnvelope resultEnvelope = requestSender.send(new LockCommand(isolateId, resourceName, scheme));
		try {
			resultEnvelope.checkForThrowable();
		} catch (RuntimeException e) {
			throw e;
		} catch (ResourceUnavailableException e) {
			throw e;
		} catch (Throwable e) {
			throw new SpotFatalException("Unexpected exception: " + e);
		}
		return ((BooleanReplyEnvelope)resultEnvelope).getBooleanContents();
	}

	public void register(String resourceName) {
		ReplyEnvelope resultEnvelope = requestSender.send(new RegisterCommand(resourceName));
		resultEnvelope.checkForRuntimeException();
	}

	public void unlock(int isolateId, String resourceName, ResourceSharingScheme scheme) {
		ReplyEnvelope resultEnvelope = requestSender.send(new UnlockCommand(isolateId, resourceName, scheme));
		resultEnvelope.checkForRuntimeException();
	}

	public void unlockAllResourcesHeldByIsolate(int isolateId) {
		ReplyEnvelope resultEnvelope = requestSender.send(new UnlockAllCommand(isolateId));
		resultEnvelope.checkForRuntimeException();
	}

	public void adjustLock(int isolateId, String resourceName, ResourceSharingScheme oldScheme, ResourceSharingScheme newScheme) throws ResourceUnavailableException {
		ReplyEnvelope resultEnvelope = requestSender.send(new AdjustLockCommand(isolateId, resourceName, oldScheme, newScheme));
		try {
			resultEnvelope.checkForThrowable();
		} catch (RuntimeException e) {
			throw e;
		} catch (ResourceUnavailableException e) {
			throw e;
		} catch (Throwable e) {
			throw new SpotFatalException("Unexpected exception: " + e);
		}
	}

}
