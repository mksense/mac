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

import java.io.IOException;

import com.sun.spot.peripheral.ConfigPage;
import com.sun.spot.peripheral.SpotFatalException;
import com.sun.squawk.peripheral.INorFlashSector;
import com.sun.squawk.peripheral.INorFlashSectorAllocator;
import com.sun.squawk.peripheral.InsufficientFlashMemoryException;

/**
 * @see com.sun.squawk.peripheral.INorFlashSectorAllocator
 */
public class NorFlashSectorAllocator implements INorFlashSectorAllocator {

	private static final String RMS_FILE_NAME = "rms";

	/**
	 * @see com.sun.squawk.peripheral.INorFlashSectorAllocator#getExtraSector(int)
	 */
	public synchronized INorFlashSector getExtraSector(int purpose) throws InsufficientFlashMemoryException {
		FlashFile rmsFile = new FlashFile(RMS_FILE_NAME);
		try {
			IAddressableNorFlashSector extraSector = rmsFile.getExtraSector();
			extraSector.erase();
			return (INorFlashSector) extraSector;
		} catch (IOException e) {
			throw new SpotFatalException("Unexpected IOException in getExtraSector: " + e.getMessage());
		}
	}

	/**
	 * @see com.sun.squawk.peripheral.INorFlashSectorAllocator#getInitialSectors(int)
	 */
	public INorFlashSector[] getInitialSectors(int purpose) throws IOException {
		checkPurpose(purpose);
		FlashFile rmsFile = new FlashFile(RMS_FILE_NAME);
		if (!rmsFile.exists()) {
			rmsFile.createNewFile(ConfigPage.LARGE_SECTOR_SIZE * ConfigPage.DEFAULT_SECTOR_COUNT_FOR_RMS);
			rmsFile.commit();
			IAddressableNorFlashSector[] sectors = rmsFile.getFileDescriptor().getSectors();
			for (int i = 0; i < sectors.length; i++) {
				sectors[i].erase();
			}
		}
		
		IAddressableNorFlashSector[] sectors = rmsFile.getFileDescriptor().getSectors();
		INorFlashSector[] result = new INorFlashSector[sectors.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = (INorFlashSector) sectors[i];
		}
		return result;
	}

	/**
	 * @see com.sun.squawk.peripheral.INorFlashSectorAllocator#releaseSector(com.sun.squawk.peripheral.INorFlashSector, int)
	 */
	public void releaseSector(INorFlashSector sector, int purpose) {
		FlashFile rmsFile = new FlashFile(RMS_FILE_NAME);
		try {
			rmsFile.releaseSector((IAddressableNorFlashSector) sector);
		} catch (IOException e) {
			throw new SpotFatalException("Unexpected IOException in releaseSector: " + e.getMessage());
		}
	}

	private void checkPurpose(int purpose) {
		if (purpose != INorFlashSector.RMS_PURPOSED) {
			throw new SpotFatalException("NorFlashSectorAllocator expects to deal only with RMS");			
		}
	}
	
}
