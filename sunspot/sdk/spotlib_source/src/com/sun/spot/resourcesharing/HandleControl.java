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

import java.util.Hashtable;

class HandleControl {

	private IResource resource;
	private Hashtable handles = new Hashtable();

	public HandleControl(IResource resource) {
		this.resource = resource;
	}

	public IResourceHandle allocateResource(ResourceSharingScheme scheme, boolean otherIsolateHasLock) throws ResourceUnavailableException, ResourceSharingException {
		IResourceHandle handle = resource.getHandle(scheme, otherIsolateHasLock);
		if (handles.containsKey(handle)) {
			throw new ResourceSharingException("resource " + resource.getResourceName() + " has issued a handle that is still in use");
		}
		handles.put(handle, scheme);
		return handle;
	}
	
	public ResourceSharingScheme getSchemeForHandle(IResourceHandle handle) throws ResourceSharingException {
		checkHandle(handle);
		return (ResourceSharingScheme) handles.get(handle);
	}

	public void unlock(IResourceHandle handle) throws ResourceSharingException {
		checkHandle(handle);
		handles.remove(handle);
		resource.unlocked(handle);
	}

	private void checkHandle(IResourceHandle handle) {
		if (!handles.containsKey(handle)) {
			throw new ResourceSharingException("Attempt to manipulate unregistered handle to " + handle.getResourceName());			
		}
	}

	public IResourceHandle lockAdjusted(IResourceHandle handle, ResourceSharingScheme newScheme) throws ResourceSharingException, ResourceUnavailableException {
		checkHandle(handle);
		IResourceHandle newHandle = resource.lockAdjusted(handle, getSchemeForHandle(handle), newScheme);
		handles.remove(handle);
		handles.put(newHandle, newScheme);
		return newHandle;
	}
	
}
