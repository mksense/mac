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
 * The interface to the resource registry that controls access to shared resources. A reference to the registry is
 * obtained by calling Spot.getInstance().getResourceRegistry().
 * 
 * The resource registry is used to control access to shared resources. First, resources that are to be made
 * available are registered with the resource registry by calling {@link #register(String, IResource)}. Then
 * an application that wants to use a resource asks the registry for a handle to it by calling
 * {@link #getResource(String, ResourceSharingScheme)}. A list of registered resources can be obtained by
 * calling {@link #getResourceNames()}.
 * 
 * When requesting access to a resource the requestor must specify their intended or required usage by
 * nominating a {@link ResourceSharingScheme}.
 * 
 * When Isolates exit or hibernate, all their resource locks are released. An implication of this is that
 * when Isolates are unhibernated, they must obtain any locks they need again. This is the responsibility
 * of the application or driver writer and is not done automatically by the resource registry.
 * 
 * See {@link IResource} for information about implementing a shared resource.
 */
public interface IResourceRegistry {

	/**
	 * Inform the registry of a resource that is available for sharing. Note that this call must be made
	 * in the same isolate in which users of the resource reside. If a resource is to be available in
	 * multiple isolates then this call must be made - using the same resource name - in each isolate.
	 * @param resourceName The name of the resource
	 * @param resource The resource object
	 * @throws ResourceSharingException If the resource has already been registered.
	 */
	void register(String resourceName, IResource resource) throws ResourceSharingException;

	/**
	 * @return An array of the names of the resources available to be shared.
	 */
	String[] getResourceNames();

	/**
	 * Request access to a resource. The caller intends to use the resource in accordance with
	 * the scheme specified in the request.
	 * @param resourceName The name of the resource.
	 * @param scheme The scheme that defines the intended or required usage.
	 * @return A handle that provides access to the resource.
	 * @throws ResourceSharingException If the resource is unknown or there is some other problem
	 * @throws ResourceUnavailableException If the resource is currently unavailable
	 */
	IResourceHandle getResource(String resourceName, ResourceSharingScheme scheme)
			throws ResourceSharingException, ResourceUnavailableException;

	/**
	 * Notify the registry that that access to the resource, as defined by the specified handle,
	 * is no longer required.
	 * @param handle The handle being unlocked
	 * @throws ResourceSharingException
	 */
	void unlock(IResourceHandle handle) throws ResourceSharingException;

	/**
	 * Attempt to change the {@link ResourceSharingScheme} associated with the handle. Returns a
	 * handle that has the new lock status (which might or might not be the same handle passed
	 * as the input parameter). If the existence of other locks make the change impossible,
	 * throw {@link ResourceUnavailableException}.
	 * @param handle The handle to adjust the lock of
	 * @param scheme The required resource sharing scheme
	 * @return the handle that reflects the adjusted lock
	 * @throws ResourceUnavailableException
	 */
	IResourceHandle adjustLock(IResourceHandle handle, ResourceSharingScheme scheme) throws ResourceUnavailableException;

}
