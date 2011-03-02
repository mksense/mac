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
 * An enumerated type that defines the different sharing schemes for shared resources.
 */
public abstract class ResourceSharingScheme {

	/**
	 * The requestor requires fully exclusive access to the resource. If granted no other
	 * request for access will succeed until the first requestor unlocks the resource.
	 */
	public static final ResourceSharingScheme EXCLUSIVE = new ResourceSharingScheme("ex", 40) {
		boolean compatibleWith(ResourceSharingScheme scheme) {return false;}};

	/**
	 * The requestor requires exclusive write access to the resource, but concurrent READ access
	 * is permitted. READ access for the requestor is implied.
	 */
	public static final ResourceSharingScheme EXCLUSIVE_WRITE = new ResourceSharingScheme("ew", 30) {
		boolean compatibleWith(ResourceSharingScheme scheme) {return scheme==READ;}};

	/**
	 * The requestor requires write access to the resource, but concurrent READ and/or
	 * WRITE access is permitted. READ access for the requestor is implied.
	 */
	public static final ResourceSharingScheme WRITE = new ResourceSharingScheme("wr", 20) {
		boolean compatibleWith(ResourceSharingScheme scheme) {return scheme==this || scheme==READ;}};

	/**
	 * The requestor requires read access to the resource.
	 */
	public static final ResourceSharingScheme READ = new ResourceSharingScheme("rd", 10) {
		boolean compatibleWith(ResourceSharingScheme scheme) {return scheme==this || scheme==WRITE || scheme==EXCLUSIVE_WRITE;}};

	private String id;
	private int rank;

	private ResourceSharingScheme(String id, int rank) {
		this.id = id;
		this.rank = rank;
	}
	
	abstract boolean compatibleWith(ResourceSharingScheme scheme);
	
	public String toString() {
		return "ResourceSharingScheme:"+id;
	}

	int getRank() {
		return rank;
	}

	/**
	 * Return the instance that matches the argument. We need this to allow schemes to be
	 * passed between isolates
	 * @param scheme the scheme to be matched
	 * @return the matching scheme
	 */
	public static ResourceSharingScheme schemeMatching(ResourceSharingScheme scheme) {
		if (scheme.id.equals(EXCLUSIVE.id)) return EXCLUSIVE;
		if (scheme.id.equals(EXCLUSIVE_WRITE.id)) return EXCLUSIVE_WRITE;
		if (scheme.id.equals(WRITE.id)) return WRITE;
		return READ;
	}
}
