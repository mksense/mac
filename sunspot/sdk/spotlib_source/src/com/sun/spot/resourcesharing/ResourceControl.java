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


class ResourceControl {

	private Lock highestLock;
	private String resourceName;

	public ResourceControl(String resourceName) {
		this.resourceName = resourceName;
	}

	public boolean lock(int isolateId, ResourceSharingScheme scheme) throws ResourceUnavailableException {
		if (highestLock != null && !scheme.compatibleWith(highestLock.scheme)) {
			throw new ResourceUnavailableException("resource " + resourceName + " is already locked by isolate " + highestLock.isolateId);
		}
		boolean result = false;
		Lock nextLock = highestLock;
		while (nextLock != null) {
			if (nextLock.isolateId != isolateId) {
				result = true;
				break;
			}
			nextLock = nextLock.nextLock;
		}
		
		Lock newLock = new Lock();
		newLock.scheme = scheme;
		newLock.isolateId = isolateId;
		insertIntoList(newLock);
		return result;
	}

	public void unlockAll(int isolateId) throws ResourceSharingException {
		Lock lock;
		while ((lock = findLock(isolateId)) != null) {
			removeLock(lock);
		}
	}
	
	public void unlock(int isolateId, ResourceSharingScheme scheme) throws ResourceSharingException {
		Lock lock = findLock(isolateId, scheme);
		if (lock == null) {
			throw new ResourceSharingException("cannot find lock for " + resourceName + " for isolate " + isolateId);			
		}
		removeLock(lock);
	}
	
	public boolean isLockedBy(int isolateId) {
		return findLock(isolateId) != null;
	}
	
	private void removeLock(Lock lock) {
		if (lock == highestLock) {
			highestLock = lock.nextLock;
		} else {
			Lock nextLock = highestLock;
			while (nextLock != null) {
				if (nextLock.nextLock == lock) {
					break;
				}
				nextLock = nextLock.nextLock;
			}
			nextLock.nextLock = lock.nextLock;
		}
	}

	private Lock findLock(int isolateId) {
		Lock nextLock = highestLock;
		while (nextLock != null) {
			if (nextLock.isolateId == isolateId) {
				break;
			}
			nextLock = nextLock.nextLock;
		}
		return nextLock;
	}

	private Lock findLock(int isolateId, ResourceSharingScheme scheme) {
		Lock nextLock = highestLock;
		while (nextLock != null) {
			if (nextLock.isolateId == isolateId && nextLock.scheme == scheme) {
				break;
			}
			nextLock = nextLock.nextLock;
		}
		return nextLock;
	}

	private void insertIntoList(Lock newLock) {
		if (highestLock == null) {
			highestLock = newLock;
		} else if (highestLock.scheme.getRank() < newLock.scheme.getRank()) {
			// the new lock becomes the head
			newLock.nextLock = highestLock;
			highestLock = newLock;
		} else {
			// insert new lock in correct place
			Lock nextLock = highestLock;
			while (nextLock.nextLock != null && nextLock.nextLock.scheme.getRank() >= newLock.scheme.getRank()) {
				nextLock = nextLock.nextLock;
			}
			newLock.nextLock = nextLock.nextLock;
			nextLock.nextLock = newLock;
		}
	}

	private static class Lock {
		Lock nextLock;
		ResourceSharingScheme scheme;
		int isolateId;
	}
	
}
