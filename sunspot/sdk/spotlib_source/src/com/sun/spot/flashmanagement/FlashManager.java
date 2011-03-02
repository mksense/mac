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
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import com.sun.spot.peripheral.ConfigPage;
import com.sun.spot.peripheral.SpotFatalException;
import com.sun.spot.util.Utils;
import com.sun.squawk.VM;
import com.sun.squawk.peripheral.INorFlashSector;
import com.sun.squawk.peripheral.InsufficientFlashMemoryException;
import com.sun.squawk.vm.ChannelConstants;

/**
 * FlashManager
 *
 */
class FlashManager implements IFlashManager {

	// These three are package visibility to aid testing
	Hashtable fileDescriptors;
	FreeSectorsList freeSectorsRecord;
	Vector needsDeleting = new Vector();
	private IAddressableNorFlashSector fatSector;
	private INorFlashSectorFactory flashSectorFactory;
	private int fatWriteOffset;
	private int lowestSectorInFilingSystem;
	private int highestSectorInFilingSystem;
	
	FlashManager(int lowestSectorInFilingSystem, int highestSectorInFilingSystem) {
		this.lowestSectorInFilingSystem = lowestSectorInFilingSystem;
		this.highestSectorInFilingSystem = highestSectorInFilingSystem;
	}

	public void initFilingSystem(INorFlashSectorFactory factory) {
		flashSectorFactory = factory;
		resetFAT();
		fileDescriptors = new Hashtable();
		freeSectorsRecord = new FreeSectorsList(lowestSectorInFilingSystem, highestSectorInFilingSystem, factory);
	}

	private void resetFAT() {
		getFatSector().erase();
		byte[] buffer = new byte[4];
		Utils.writeBigEndInt(buffer, 0, FlashFile.FAT_IDENTIFIER_V3);
		getFatSector().setBytes(0, buffer, 0, buffer.length);
		fatWriteOffset = buffer.length;
	}
	
	public void initFromStoredFAT(INorFlashSectorFactory factory) throws IOException {
		flashSectorFactory = factory;
		initFrom(new FlashFileInputStream(getFatSector()), factory);
	}

	/**
	 * This function is split out solely to aid unit testing
	 */
	void initFrom(InputStream is, INorFlashSectorFactory factory) throws IOException {
		flashSectorFactory = factory;

		fileDescriptors = new Hashtable();

		DataInputStream dis = new DataInputStream(is);
		
		int identifierInt = dis.readInt();
		if (identifierInt != FlashFile.FAT_IDENTIFIER_V3) {
			throw new SpotFatalException("This is not a FAT 0x" + Integer.toHexString(identifierInt));
		}
		
		freeSectorsRecord = new FreeSectorsList();
		int fatOffset = 4; // length of identifier int

		short recordStatus = dis.readShort();
		while (recordStatus != FATRecord.UNUSED_FAT_RECORD_STATUS) {
			short recordSize = dis.readShort();
			switch (recordStatus) {
				case FATRecord.DELETED_FAT_RECORD_STATUS:
					dis.skip(recordSize-FATRecord.FAT_RECORD_HEADER_SIZE);
					break;

				case FATRecord.CURRENT_FAT_RECORD_STATUS:
					byte recordType = dis.readByte();
					switch (recordType) {
						case FATRecord.FILE_FAT_RECORD_TYPE:
							readFATFileRecord(factory, dis, fatOffset);
							break;
						default:
							throw new IOException("[FlashManager] FAT contains bad record type " + recordType);
					}
					break;

				default:
					throw new IOException("[FlashManager] FAT contains bad record status " + recordStatus);
			}
			fatOffset += recordSize;
			if (recordSize % 2 != 0) {
				dis.skip(1);
				fatOffset += 1;
			}
			recordStatus = dis.readShort();
		}
		fatWriteOffset = fatOffset;
		
		Enumeration e = fileDescriptors.elements();
		while (e.hasMoreElements()) {
			FlashFileDescriptor descriptor = (FlashFileDescriptor) e.nextElement();
			IAddressableNorFlashSector[] sectors = descriptor.getSectors();
			for (int i = 0; i < sectors.length; i++) {
				highestSectorInFilingSystem = Math.max(highestSectorInFilingSystem, sectors[i].getSectorNumber());
			}
		}
		boolean[] sectorsInUse = new boolean[highestSectorInFilingSystem+1];
		e = fileDescriptors.elements();
		while (e.hasMoreElements()) {
			FlashFileDescriptor descriptor = (FlashFileDescriptor) e.nextElement();
			IAddressableNorFlashSector[] sectors = descriptor.getSectors();
			for (int i = 0; i < sectors.length; i++) {
				sectorsInUse[sectors[i].getSectorNumber()] = true;
			}
		}
		for (int i = lowestSectorInFilingSystem; i < sectorsInUse.length; i++) {
			if (!sectorsInUse[i]) {
				freeSectorsRecord.addSectorNumber(flashSectorFactory.create(i, INorFlashSector.SYSTEM_PURPOSED));
			}
		}
	}

	private void readFATFileRecord(INorFlashSectorFactory factory, DataInputStream dis, int offsetInFAT) throws IOException {
		FlashFileDescriptor flashFileDescriptor = new FlashFileDescriptor(factory, dis, offsetInFAT);
		if (!flashFileDescriptor.isObsolete()) {
			if (flashFileDescriptor.getVirtualAddress() != 0) {
				mapSectors(flashFileDescriptor);
			}
			fileDescriptors.put(flashFileDescriptor.getName(), flashFileDescriptor);
		} else {
			addToNeedsDeleting(flashFileDescriptor);
		}
	}

	public synchronized FlashFileDescriptor createFile(String name, int size) throws InsufficientFlashMemoryException {
		if (exists(name)) {
			throw new SpotFatalException("Cannot create file " + name + " - already exists");
		}
		Vector sectors = freeSectorsRecord.allocateSectors(name, size);
		FlashFileDescriptor flashFileDescriptor = new FlashFileDescriptor(name, 0, sectors, 0, System.currentTimeMillis(), "", (short) 0);
		fileDescriptors.put(name, flashFileDescriptor);
		return flashFileDescriptor;
	}

	public synchronized int allocateVirtualAddress() {
		long guessedAddress = FlashFile.FIRST_FILE_VIRTUAL_ADDRESS;
		
		while (guessedAddress <= FlashFile.LAST_FILE_VIRTUAL_ADDRESS) {
			if (!isVirtualAddressInUse((int) guessedAddress)) {
				return (int) guessedAddress;
			}
			guessedAddress += FlashFile.VIRTUAL_ADDRESS_FILE_SPACING;
		}
		throw new SpotFatalException("Virtual address space exhausted");
	}

	private boolean isVirtualAddressInUse(int virtualAddress) {
		Enumeration e = fileDescriptors.elements();
		while (e.hasMoreElements()) {
			FlashFileDescriptor descriptor = (FlashFileDescriptor) e.nextElement();
			if (descriptor.getVirtualAddress() == virtualAddress && !descriptor.isObsolete()) {
				return true;
			}
		}
		return false;				
	}
	
	public boolean exists(String name) {
		return fileDescriptors.containsKey(name);
	}

	public FlashFileDescriptor getFileDescriptorFor(String name) throws FlashFileNotFoundException {
		if (exists(name)) {
			return (FlashFileDescriptor) fileDescriptors.get(name);
		}
		throw new FlashFileNotFoundException("File not found " + name);
	}

	public synchronized void deleteFile(String name) {
		if (exists(name)) {
			FlashFileDescriptor descriptor = (FlashFileDescriptor) fileDescriptors.remove(name);
			addToNeedsDeleting(descriptor);
			freeSectorsRecord.addSectors(descriptor.getSectors());
		}
	}

	public synchronized void writeFAT() throws IOException {
		Hashtable recordsToWrite = new Hashtable();
		Enumeration e = fileDescriptors.elements();
		int totalSize = 0;

		while (e.hasMoreElements()) {
			FATRecord record = (FATRecord) e.nextElement();
			if (record.needsWriting()) {
				byte[] fatRecord = record.asFATRecord();
				recordsToWrite.put(record, fatRecord);
				totalSize += fatRecord.length;
			}
		}
		
		if (getFatSector().getSize() - fatWriteOffset - 2 >= totalSize) {
			// room in FAT just to write
			e = needsDeleting.elements();
			while (e.hasMoreElements()) {
				FATRecord record = (FATRecord) e.nextElement();
				getFatSector().setBytes(record.getOffsetInFAT(), FATRecord.DELETED_FAT_RECORD_STATUS_AS_BYTE_ARRAY, 0, FATRecord.DELETED_FAT_RECORD_STATUS_AS_BYTE_ARRAY.length);
			}

			e = fileDescriptors.elements();
			while (e.hasMoreElements()) {
				FATRecord record = (FATRecord) e.nextElement();
				if (record.needsDeleting()) {
					getFatSector().setBytes(record.getOffsetInFAT(), FATRecord.DELETED_FAT_RECORD_STATUS_AS_BYTE_ARRAY, 0, FATRecord.DELETED_FAT_RECORD_STATUS_AS_BYTE_ARRAY.length);
				}
				record.setClean();
			}
			e = recordsToWrite.keys();
			while (e.hasMoreElements()) {
				FATRecord record = (FATRecord) e.nextElement();
				byte[] recordAsByteArray = (byte[]) recordsToWrite.get(record);
				getFatSector().setBytes(fatWriteOffset, recordAsByteArray, 0, recordAsByteArray.length);
				record.setOffsetInFAT(fatWriteOffset);
				fatWriteOffset += recordAsByteArray.length;
			}
		} else {
			resetFAT();
			e = fileDescriptors.elements();
			while (e.hasMoreElements()) {
				FATRecord record = (FATRecord) e.nextElement();
				byte[] recordAsByteArray = record.asFATRecord();
				if (getFatSector().getSize() - fatWriteOffset - 2 < recordAsByteArray.length) {
					throw new SpotFatalException("FAT is full");
				}
				getFatSector().setBytes(fatWriteOffset, recordAsByteArray, 0, recordAsByteArray.length);
				record.setOffsetInFAT(fatWriteOffset);
				fatWriteOffset += recordAsByteArray.length;				
				record.setClean();
			}
		}
		needsDeleting = new Vector();
	}

	FlashFileDescriptor[] getFileDescriptors() {
		FlashFileDescriptor[] result = new FlashFileDescriptor[fileDescriptors.size()];
		Enumeration e = fileDescriptors.elements();
		int index = 0;
		while (e.hasMoreElements()) {
			result[index++] = (FlashFileDescriptor) e.nextElement();	
		}
		return result;
	}

	Vector getFreeSectors() {
		return freeSectorsRecord.getFreeSectors();
	}

	public synchronized void remap(String filename) {
		remap((FlashFileDescriptor) fileDescriptors.get(filename));
	}

	private void remap(FlashFileDescriptor flashFileDescriptor) {
		if (!flashFileDescriptor.isAddressed()) {
			flashFileDescriptor.setVirtualAddress(allocateVirtualAddress());
		}
		mapSectors(flashFileDescriptor);
		reprogramMMU();
	}

	private void mapSectors(FlashFileDescriptor descriptor) {
		IAddressableNorFlashSector[] sectors = descriptor.getSectors();
		int virtualAddress = descriptor.getVirtualAddress();
		for (int i = 0; i < sectors.length; i++) {
			sectors[i].setVirtualAddress(virtualAddress);
			virtualAddress = virtualAddress + sectors[i].getSize();
		}
	}

	void reprogramMMU() {
		VM.execSyncIO(ChannelConstants.REPROGRAM_MMU, 0, 0, 0, 0, 0, 0, null, null);
	}

	public void validateVirtualAddress(int virtualAddress) {
		long longVirtualAddress = virtualAddress & 0xFFFFFFFFL;
		if (longVirtualAddress < FlashFile.FIRST_FILE_VIRTUAL_ADDRESS)
			throw new IllegalArgumentException("Virtual address 0x" + Integer.toHexString(virtualAddress) + " is too low");
		if (longVirtualAddress > FlashFile.LAST_FILE_VIRTUAL_ADDRESS)
			throw new IllegalArgumentException("Virtual address 0x" + Integer.toHexString(virtualAddress) + " is too high");
		if (virtualAddress % FlashFile.VIRTUAL_ADDRESS_FILE_SPACING != 0)
			throw new IllegalArgumentException("Virtual address 0x" + Integer.toHexString(virtualAddress) + " is not on a valid boundary");
		if (isVirtualAddressInUse(virtualAddress))
			throw new IllegalArgumentException("Virtual address 0x" + Integer.toHexString(virtualAddress) + " is already allocated");
	}

	/**
	 * @return Returns the fatSector.
	 */
	private IAddressableNorFlashSector getFatSector() {
		if (fatSector == null) {
			fatSector = flashSectorFactory.create(ConfigPage.FAT_SECTOR_NUMBER, INorFlashSector.SYSTEM_PURPOSED);
		}
		return fatSector;
	}

	public synchronized boolean rename(FlashFileDescriptor fileDescriptor, String newName) throws IOException {
		if (exists(newName)) {
			return false;
		} else {
			fileDescriptors.remove(fileDescriptor.getName());
			fileDescriptor.setName(newName);
			fileDescriptors.put(newName, fileDescriptor);
			return true;
		}
	}

	/**
	 * @param fileDescriptor
	 * @return sector found.
	 * @throws InsufficientFlashMemoryException 
	 */
	public synchronized IAddressableNorFlashSector getExtraSector(FlashFileDescriptor fileDescriptor) throws InsufficientFlashMemoryException {
		boolean isMapped = fileDescriptor.isMapped();
		IAddressableNorFlashSector sector = freeSectorsRecord.allocateSector(fileDescriptor.getName()); 
		fileDescriptor.addSector(sector);
		if (isMapped) {
			remap(fileDescriptor);
		}
		return sector;
	}

	/**
	 * @param fileDescriptor
	 * @param sector
	 */
	public synchronized void releaseSector(FlashFileDescriptor fileDescriptor, IAddressableNorFlashSector sector) {
		boolean mapped = fileDescriptor.isMapped();
		fileDescriptor.removeSector(sector);
		freeSectorsRecord.addSectors(new IAddressableNorFlashSector[] {sector});
		if (mapped) {
			remap(fileDescriptor);
		}
	}

	public IFlashFileInfo[] getFileInfos() {
		return getFileDescriptors();
	}

	public synchronized int[] getFreeSectorIndices() {
		return freeSectorsRecord.getFreeSectorIndices();
	}
	
	public String toString() {
		String lineSeparator = System.getProperty("line.separator");
		IFlashFileInfo[] fileInfos = getFileInfos();
		StringBuffer sb = new StringBuffer(100);
		sb.append("FAT contains " + fileInfos.length + " files.");
		sb.append(lineSeparator);
		for (int i = 0; i < fileInfos.length; i++) {
			sb.append(" " + i + ": " + fileInfos[i].toString());
		}
		return sb.toString();
	}

	private void addToNeedsDeleting(FATRecord record) {
		if (record.getOffsetInFAT() != -1) {
			needsDeleting.addElement(record);
		}
	}
}

