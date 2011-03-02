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
 * INorFlashSectorFactory creates instances of {@link IAddressableNorFlashSector}.
 * It is used within the implementation of the FlashFile system and should not normally be referred to
 * from outside the com.sun.spot.flashmanagement package.
 *
 */
public interface INorFlashSectorFactory {

	/**
	 * Create an {@link IAddressableNorFlashSector}
	 * 
	 * @param sectorNumber The unique number to associate with the new sector
	 * @param purpose The intended use of the new sector. Normally one of 
	 * {@link com.sun.squawk.peripheral.INorFlashSector#RMS_PURPOSED},
	 * {@link com.sun.squawk.peripheral.INorFlashSector#SYSTEM_PURPOSED} or
	 * {@link com.sun.squawk.peripheral.INorFlashSector#USER_PURPOSED}
	 * @return the new {@link IAddressableNorFlashSector}
	 */
	IAddressableNorFlashSector create(int sectorNumber, int purpose);
}

