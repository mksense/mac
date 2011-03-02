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

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import com.sun.squawk.peripheral.INorFlashSector;
import com.sun.squawk.peripheral.InsufficientFlashMemoryException;

/**
 * FreeSectorsFATRecord
 *
 */
class FreeSectorsList {

	private Vector freeSectors = new Vector();
	
	public FreeSectorsList(int lowestFreeSector, int highestFreeSector, INorFlashSectorFactory factory) {
		freeSectors = new Vector();
		
		for (int i = lowestFreeSector; i <= highestFreeSector; i++) {
			freeSectors.addElement(factory.create(i, INorFlashSector.SYSTEM_PURPOSED));
		}
	}

	public FreeSectorsList() throws IOException {
	}

	public void addSectors(INorFlashSectorFactory factory, DataInputStream dis) throws IOException {
		int freeSectorCount = dis.readShort();
		for (int i = 0; i < freeSectorCount; i++) {
			freeSectors.addElement(factory.create(dis.readShort(), INorFlashSector.SYSTEM_PURPOSED));
		}
	}
	
	public void addSectors(IAddressableNorFlashSector[] sectors) {
		for (int j = 0; j < sectors.length; j++) {
			freeSectors.addElement(sectors[j]);
		}
	}

	public void addSectorNumber(IAddressableNorFlashSector sector) {
		freeSectors.addElement(sector);
	}

	public Vector allocateSectors(String name, int size) throws InsufficientFlashMemoryException {
		Vector sectors = new Vector();
		int sizeSoFar = 0;
		int freeSectorIndex = 0;
		while (sizeSoFar < size) {
			if (freeSectorIndex < freeSectors.size()) {
				IAddressableNorFlashSector s = (IAddressableNorFlashSector) freeSectors.elementAt(freeSectorIndex++);
				sectors.addElement(s);
				sizeSoFar += s.getSize();
			} else {
				throw new InsufficientFlashMemoryException("Not enough space to create file " + name + " (wants " + size + ", " + sizeSoFar + " available)");
			}
		}
		Enumeration e = sectors.elements();
		while (e.hasMoreElements()) {
			freeSectors.removeElement(e.nextElement());
		}
		return sectors;
	}

	// For test only
	public Vector getFreeSectors() {
		return freeSectors;
	}

	public IAddressableNorFlashSector allocateSector(String name) throws InsufficientFlashMemoryException {
		if (freeSectors.isEmpty()) {
			throw new InsufficientFlashMemoryException("Not enough space to allocate sector for " + name);
		}
		IAddressableNorFlashSector sector = (IAddressableNorFlashSector) freeSectors.elementAt(0);
		freeSectors.removeElementAt(0);
		return sector;
	}

	public int[] getFreeSectorIndices() {
		int[] result = new int[freeSectors.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = ((IAddressableNorFlashSector)freeSectors.elementAt(i)).getSectorNumber();
		}
		return result;
	}
}

