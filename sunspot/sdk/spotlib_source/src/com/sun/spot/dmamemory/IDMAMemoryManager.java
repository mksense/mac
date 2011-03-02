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

package com.sun.spot.dmamemory;

import java.util.Hashtable;


/**
 * Interface to a manager of uncached memory suitable for use as DMA buffers.
 */
public interface IDMAMemoryManager {

	/**
	 * Try to obtain memory for a DMA buffer.
	 * 
	 * @param size The size of the required buffer
	 * @param comment A string that describes the usage (so we can tell who's using what)
	 * @return The address of the allocated buffer
	 * @throws NotEnoughDMAMemoryException
	 */
	int getBuffer(int size, String comment) throws NotEnoughDMAMemoryException;

	/**
	 * Find out the size of the biggest available DMA memory buffer
	 * 
	 * @return The size of the biggest buffer that is available
	 */
	int getMaxAvailableBufferSize();

	/**
	 * Release a previously allocated buffer
	 * 
	 * @param memAddr The address of the buffer being released
	 */
	void releaseBuffer(int memAddr);

	/**
	 * Get information about current memory allocations. Each entry in the table
	 * represents an allocation. The key is the comment string provided when the
	 * buffer was allocated; the value is the size of the buffer.
	 * 
	 * @return The table of allocations
	 */
	Hashtable getAllocationDetails();

}
