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

import java.util.Enumeration;
import java.util.Hashtable;

import com.sun.spot.peripheral.SpotFatalException;
import com.sun.squawk.Isolate;

public class ResourceRegistryChild implements IResourceRegistry {

	private int isolateId;
	private IResourceRegistryMaster masterRegistry;
	private Hashtable resourceTable = new Hashtable();

	private class ExitHook implements Isolate.LifecycleListener {
		public void handleLifecycleListenerEvent(Isolate iso, int eventKind) {
			masterRegistry.unlockAllResourcesHeldByIsolate(isolateId);
		}
	};

	public ResourceRegistryChild(int isolateId, IResourceRegistryMaster masterRegistry) {
		this.isolateId = isolateId;
		this.masterRegistry = masterRegistry;
		Isolate.currentIsolate().addLifecycleListener(new ExitHook(), Isolate.SHUTDOWN_EVENT_MASK | Isolate.HIBERNATE_EVENT_MASK );
	}

	public synchronized IResourceHandle getResource(String resourceName, ResourceSharingScheme scheme) throws ResourceUnavailableException {
		HandleControl selectedResource = (HandleControl)resourceTable.get(resourceName);
		if (selectedResource != null) {
			boolean otherIsolateHasLock = masterRegistry.lock(isolateId, resourceName, scheme);
			try {
				return selectedResource.allocateResource(scheme, otherIsolateHasLock);
			} catch (ResourceUnavailableException e) {
				masterRegistry.unlock(isolateId, resourceName, scheme);
				throw e;
			} catch (RuntimeException e) {
				masterRegistry.unlock(isolateId, resourceName, scheme);
				throw e;
			}
		} else {
			throw new ResourceSharingException("unknown resource: " + resourceName);
		}
	}

	public synchronized String[] getResourceNames() {
		Enumeration keys = resourceTable.keys();
		String[] result = new String[resourceTable.size()];
		int i = 0;
		while (keys.hasMoreElements()) {
			result[i++] = (String) keys.nextElement();
		}
		return result;
	}

	public synchronized void register(String resourceName, IResource resource) throws ResourceSharingException {
		if (resourceTable.containsKey(resourceName)) {
			throw new ResourceSharingException("resource already registered: " + resourceName);
		}
		masterRegistry.register(resourceName);
		resourceTable.put(resourceName, new HandleControl(resource));
	}

	public synchronized void unlock(IResourceHandle handle) throws ResourceSharingException {
		HandleControl selectedResource = (HandleControl)resourceTable.get(handle.getResourceName());
		if (selectedResource != null) {
			masterRegistry.unlock(isolateId, handle.getResourceName(), selectedResource.getSchemeForHandle(handle));
			selectedResource.unlock(handle);
		} else {
			throw new ResourceSharingException("attempt to unlock unknown resource: " + handle.getResourceName());
		}
	}

	public void unlockAllResourcesHeldByIsolate(int isolateId) {
		masterRegistry.unlockAllResourcesHeldByIsolate(isolateId);
	}

	public synchronized IResourceHandle adjustLock(IResourceHandle handle, ResourceSharingScheme newScheme) throws ResourceUnavailableException {
		HandleControl selectedResource = (HandleControl)resourceTable.get(handle.getResourceName());
		if (selectedResource != null) {
			ResourceSharingScheme oldScheme = selectedResource.getSchemeForHandle(handle);
			if (newScheme == oldScheme) {
				return handle;
			}
			if (!newScheme.compatibleWith(oldScheme)) {
				// upgrading lock, so adjust with master first
				masterRegistry.adjustLock(isolateId, handle.getResourceName(), oldScheme, newScheme);
				try {
					return selectedResource.lockAdjusted(handle, newScheme);
				} catch (ResourceUnavailableException e) {
					masterRegistry.adjustLock(isolateId, handle.getResourceName(), newScheme, oldScheme);
					throw e;
				} catch (RuntimeException e) {
					masterRegistry.adjustLock(isolateId, handle.getResourceName(), newScheme, oldScheme);
					throw e;
				}
			} else {
				// downgrading lock, so check with resource first
				IResourceHandle newHandle = selectedResource.lockAdjusted(handle, newScheme);
				try {
					masterRegistry.adjustLock(isolateId, handle.getResourceName(), oldScheme, newScheme);
				} catch (ResourceUnavailableException e) {
					throw new SpotFatalException("Internal error: attempt to downgrade lock failed");
				}
				return newHandle;
			}
		} else {
			throw new ResourceSharingException("attempt to unlock unknown resource: " + handle.getResourceName());
		}
	}

}
