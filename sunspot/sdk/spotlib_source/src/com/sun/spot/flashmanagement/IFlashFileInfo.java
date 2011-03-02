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

package com.sun.spot.flashmanagement;


/**
 * IFlashFileInfo provides a read-only interface to files in the flash filing system
 */
public interface IFlashFileInfo {

	/**
	 * Get the length of the file with this name
	 * @return the length in bytes of the valid portion of the file with this name
	 */
	public abstract int length();

	/**
	 * Get the virtual address of a mapped file
	 * @return the virtual address of the file, or 0 if it is not a mapped file
	 */
	public abstract int getVirtualAddress();

	/**
	 * Get the comment for this file
	 * @return the comment attached to the file's descriptor
	 */
	public abstract String getComment();

	/**
	 * Get the time at which this file was last modified
	 * @return the time (as returned by System.currentTimeMillis()) at which this file was last modified.
	 */
	public abstract long lastModified();

	/**
	 * Check whether the file with this name is obsolete
	 * @return true if this file is marked obsolete, false if it is not
	 */
	public abstract boolean isObsolete();

	/**
	 * Get the name of this file
	 * @return the name of this file
	 */
	public abstract String getName();
}
