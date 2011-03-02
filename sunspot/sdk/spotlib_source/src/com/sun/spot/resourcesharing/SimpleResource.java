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
 * A simple implementation of the {@link IResource} interface that can be used as it is, or 
 * extended for a more complex use. By default uses {@link SimpleResourceHandle}: if
 * an extension needs to use a different {@link IResourceHandle} then it should override
 * {@link #createNewHandle()}. 
 */
public class SimpleResource implements IResource {
	private String name;

	public SimpleResource(String name) {
		this.name = name;
	}

	public IResourceHandle getHandle(ResourceSharingScheme scheme, boolean isLockedInADifferentIsolate) throws ResourceUnavailableException {
		return createNewHandle();
	}

	public String getResourceName() {
		return name;
	}

	public void unlocked(IResourceHandle handle) {
		// no-op
	}
	
	public IResourceHandle lockAdjusted(IResourceHandle handle, ResourceSharingScheme oldScheme, ResourceSharingScheme newScheme) throws ResourceSharingException, ResourceUnavailableException {
		return handle;
	}

	protected IResourceHandle createNewHandle() {
		return new SimpleResourceHandle(this);
	}
}
