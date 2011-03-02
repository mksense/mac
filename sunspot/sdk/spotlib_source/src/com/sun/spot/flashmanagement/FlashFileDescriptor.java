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
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Vector;

import com.sun.spot.peripheral.SpotFatalException;

class FlashFileDescriptor extends FATRecord implements IExtendedFlashFileInfo {

	private static final short OBSOLETE_FLAG_MASK = 0x1;
	private int fileSize;
	private Vector sectors;
	private String name;
	private int virtualAddress;
	private long lastModifiedMillis;
	private String comment;
	private short flags;
	private int allocatedSpace;
	/**
	 * @return Returns the comment.
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * @param comment The comment to set.
	 */
	public void setComment(String comment) {
		this.comment = comment;
		needsWriting = true;
	}

	/**
	 * @return Returns the lastModifiedMillis.
	 */
	public long lastModified() {
		return lastModifiedMillis;
	}

	/**
	 * @param lastModifiedMillis The lastModifiedMillis to set.
	 */
	public void setLastModifiedMillis(long lastModifiedMillis) {
		this.lastModifiedMillis = lastModifiedMillis;
		needsWriting = true;
	}

	/**
	 * @return Returns the virtualAddress.
	 */
	public int getVirtualAddress() {
		return virtualAddress;
	}

	/**
	 * @param virtualAddress The virtualAddress to set.
	 */
	public void setVirtualAddress(int virtualAddress) {
		this.virtualAddress = virtualAddress;
		needsWriting = true;
	}

	/**
	 * Use this constructor to make a descriptor for a new file, or a temporary descriptor
	 */
	public FlashFileDescriptor(String name, int fileSize, Vector sectorsForFile, int virtualAddress, long lastModifiedMillis, String comment, short flags) {
		super(NOT_WRITTEN_YET_OFFSET);
		this.flags = flags;
		this.virtualAddress = virtualAddress;
		sectors = sectorsForFile;
		this.name = name;
		this.fileSize = fileSize;
		this.lastModifiedMillis = lastModifiedMillis;
		this.comment = comment;
		calculateAllocatedSpace();
	}
	
	/**
	 * Use this constructor to make a descriptor for an existing file
	 */
	public FlashFileDescriptor(INorFlashSectorFactory factory, DataInputStream dis, int fatOffset) throws IOException {
		super(fatOffset);
		flags = dis.readShort();
		virtualAddress = dis.readInt();
		sectors = readSectorList(factory, dis);
		name = dis.readUTF();
		fileSize = dis.readInt();
		lastModifiedMillis = dis.readLong();
		comment = dis.readUTF();
		calculateAllocatedSpace();
	}

	/**
	 * @param currentSector
	 * @return
	 */
	public IAddressableNorFlashSector getNextSector(IAddressableNorFlashSector sector) {
		for (int i = 0; i < sectors.size(); i++) {
			IAddressableNorFlashSector s = (IAddressableNorFlashSector) sectors.elementAt(i);
			if (s == sector) {
				if (i == sectors.size() - 1) {
					throw new SpotFatalException("FlashFileDescriptor has been asked for a sector beyond the allocated space");
				}
				return (IAddressableNorFlashSector) sectors.elementAt(i+1);
			}
		}
		throw new SpotFatalException("FlashFileDescriptor: sector not found in getNextSector");
	}

	/**
	 * @return the size of the file
	 */
	public int length() {
		return fileSize;
	}

	/**
	 * @return the number of bytes allocated for this file
	 */
	public int getAllocatedSpace() {
		return allocatedSpace;
	}

	private void calculateAllocatedSpace() {
		allocatedSpace = 0;
		for (int i = 0; i < sectors.size(); i++) {
			allocatedSpace += ((IAddressableNorFlashSector)sectors.elementAt(i)).getSize();
		}
	}

	/**
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name The name to set.
	 */
	public void setName(String name) {
		this.name = name;
		needsWriting = true;
	}

	/**
	 * @return
	 */
	public IAddressableNorFlashSector getFirstSector() {
		return (IAddressableNorFlashSector) sectors.firstElement();
	}

	/**
	 * @param numOfBytesInFile
	 */
	public void setFileSize(int numOfBytesInFile) {
		fileSize = numOfBytesInFile;
	}

	/**
	 * @return the sectors allocated to this file
	 */
	public IAddressableNorFlashSector[] getSectors() {
		IAddressableNorFlashSector[] addressableNorFlashSectors = new IAddressableNorFlashSector[sectors.size()];
		sectors.copyInto(addressableNorFlashSectors);
		return addressableNorFlashSectors;
	}

	public boolean isMapped() {
		if (!isAddressed()) {
			return false;
		}
		int offset = 0;
		for (int i = 0; i < sectors.size(); i++) {
			IAddressableNorFlashSector addressableNorFlashSector = (IAddressableNorFlashSector)sectors.elementAt(i);
			if (virtualAddress + offset != addressableNorFlashSector.getVirtualStartAddressAsInt()) {
				return false;
			}
			offset += addressableNorFlashSector.getSize();
		}
		return true;
	}

	public void setObsolete(boolean b) {
		if (b)
			flags |= OBSOLETE_FLAG_MASK;
		else
			flags &= ~OBSOLETE_FLAG_MASK;
	}

	public boolean isObsolete() {
		return (flags & OBSOLETE_FLAG_MASK) != 0;
	}

	short getFlags() {
		return flags;
	}

	public boolean isAddressed() {
		return virtualAddress != 0;
	}

	public void addSector(IAddressableNorFlashSector sector) {
		sectors.addElement(sector);
		calculateAllocatedSpace();
		needsWriting = true;
	}

	public void removeSector(IAddressableNorFlashSector sector) {
		if (!sectors.removeElement(sector)) {
			throw new SpotFatalException("Sector " + sector.getSectorNumber() + 
					" is not part of " + name);
		}
		calculateAllocatedSpace();
		needsWriting = true;
	}

	public boolean exists() {
		return true;
	}

	public int[] getSectorNumbers() {
		int[] result = new int[sectors.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = ((IAddressableNorFlashSector)sectors.elementAt(i)).getSectorNumber();
		}
		return result;
	}

	public String toString() {
		String lineSeparator = System.getProperty("line.separator");
		StringBuffer sb = new StringBuffer();
		sb.append(getName());
		if (isObsolete()) {
			sb.append(" [obsolete]");
		}
		sb.append(getVirtualAddress() == 0 ? " [unmapped]" : " [mapped at 0x" + Integer.toHexString(getVirtualAddress()) + "]");
		sb.append(lineSeparator);
		String string = "    sectors:";
		int[] sectors = getSectorNumbers();
		for (int j = 0; j < sectors.length; j++) {
			string += " 0x" + Integer.toHexString(sectors[j]);
		}
		sb.append(string);
		sb.append(lineSeparator);
		sb.append("    comment: " + getComment());
		sb.append(lineSeparator);
		sb.append("    last modified: " + new Date(lastModified()));
		sb.append(lineSeparator);
		sb.append("    size: " + length());
		sb.append(lineSeparator);
		return sb.toString();
	}

	public byte getRecordType() {
		return FATRecord.FILE_FAT_RECORD_TYPE;
	}

	public void writeRecord(DataOutputStream dos) throws IOException {
		dos.writeShort(getFlags());
		dos.writeInt(getVirtualAddress());
		IAddressableNorFlashSector[] sectors = getSectors();
		dos.writeShort(sectors.length);
		for (int i = 0; i < sectors.length; i++) {
			dos.writeShort(sectors[i].getSectorNumber());
		}
		dos.writeUTF(getName());
		dos.writeInt(length());
		dos.writeLong(lastModified());
		dos.writeUTF(getComment());
	}
}
