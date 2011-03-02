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

public class ResourceRegistryMaster implements IResourceRegistryMaster {

	private Hashtable resourceTable = new Hashtable();

	public synchronized void register(String resourceName) throws ResourceSharingException {
		if (!resourceTable.containsKey(resourceName)) {
			resourceTable.put(resourceName, new ResourceControl(resourceName));
		}
	}

	public synchronized void unlock(int isolateId, String resourceName, ResourceSharingScheme scheme) throws ResourceSharingException {
		ResourceControl selectedResource = (ResourceControl)resourceTable.get(resourceName);
		if (selectedResource != null) {
			selectedResource.unlock(isolateId, scheme);
		} else {
			throw new ResourceSharingException("attempt to unlock unknown resource: " + resourceName);
		}
	}

	public synchronized boolean lock(int isolateId, String resourceName, ResourceSharingScheme scheme) throws ResourceUnavailableException {
		ResourceControl selectedResource = (ResourceControl)resourceTable.get(resourceName);
		if (selectedResource != null) {
			return selectedResource.lock(isolateId, scheme);
		} else {
			throw new ResourceSharingException("unknown resource: " + resourceName);
		}
	}

	public synchronized void unlockAllResourcesHeldByIsolate(int isolateId) {
		Enumeration resources = resourceTable.elements();
		while (resources.hasMoreElements()) {
			ResourceControl resource = (ResourceControl) resources.nextElement();
			if (resource.isLockedBy(isolateId)) {
				try {
					resource.unlockAll(isolateId);
				} catch (ResourceSharingException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public synchronized void adjustLock(int isolateId, String resourceName, ResourceSharingScheme oldScheme, ResourceSharingScheme newScheme) throws ResourceUnavailableException {
		unlock(isolateId, resourceName, oldScheme);
		try {
			lock(isolateId, resourceName, newScheme);
		} catch (ResourceUnavailableException e) {
			lock(isolateId, resourceName, oldScheme);
			throw e;
		}
	}

}
