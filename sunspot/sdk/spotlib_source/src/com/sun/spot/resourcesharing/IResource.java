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

/**
 * This interface must be implemented by any object that is to be a shared resource.
 * Only objects that implement this interface can be registered with the resource registry.
 * 
 * To make a shared resource available it must be registered in every isolate in which it
 * is to be available using {@link IResourceRegistry#register(String, IResource)}.
 * 
 * When access to a resource is requested using {@link IResourceRegistry#getResource(String, ResourceSharingScheme)}
 * the registry will check across all isolates that the requested access is compatible with the current
 * locks and if so it will call {@link #getHandle(ResourceSharingScheme, boolean)}.
 * 
 * {@link SimpleResource} is an implementation of this interface that can used for simple cases, or extended
 * for more complex ones.
 */
public interface IResource {

	/**
	 * Generate a handle for this resource. This method is called on the resource by the resource
	 * registry when the registry has determined that a handle should be generated according to
	 * the rules of the requested scheme, taking into account locks held in all isolates.
	 * The receiver should generate a handle or throw an exception if it is unable to do so.
	 * The handle generated should not be the same object as any other handle for this resource
	 * that is still in use.
	 * @param scheme The scheme that the requestor intends to follow for this handle
	 * @param isLockedInADifferentIsolate true if a lock for this resource already exists in another isolate
	 * @return the new handle
	 * @throws ResourceSharingException if the scheme is not supported or there is some other problem
	 * @throws ResourceUnavailableException if, despite the checks already performed by the registry, the 
	 * resource still thinks the handle shouldn't be generated because it would conflict with other usage
	 */
	IResourceHandle getHandle(ResourceSharingScheme scheme, boolean isLockedInADifferentIsolate)
					throws ResourceSharingException, ResourceUnavailableException;

	/**
	 * A previously generated handle has been unlocked, and the handle can be reused. This method is
	 * called by the registry when it receives the unlock(handle) call.
	 * @param handle The handle that has been unlocked.
	 * @throws ResourceSharingException if the handle is not recognised or there is some other problem
	 */
	void unlocked(IResourceHandle handle) throws ResourceSharingException;

	/**
	 * @return the name of the this resource
	 */
	String getResourceName();
	
	/**
	 * A previously generated handle is adjusting its ResourceSharingScheme. Returns a
	 * handle that has the new lock status (which might or might not be the same handle passed
	 * as the input parameter).
	 * @param handle
	 * @param oldScheme
	 * @param newScheme
	 * @return the handle that reflects the adjusted lock
	 * @throws ResourceSharingException if the scheme is not supported or there is some other problem
	 * @throws ResourceUnavailableException if, despite the checks already performed by the registry, the 
	 * resource still thinks the scheme shouldn't be adjusted because it would conflict with other usage
	 */
	IResourceHandle lockAdjusted(IResourceHandle handle, ResourceSharingScheme oldScheme, ResourceSharingScheme newScheme) throws ResourceSharingException, ResourceUnavailableException;
}
