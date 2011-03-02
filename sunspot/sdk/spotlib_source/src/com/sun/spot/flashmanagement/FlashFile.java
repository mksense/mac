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

import com.sun.spot.globals.SpotGlobals;
import com.sun.spot.peripheral.ConfigPage;
import com.sun.spot.peripheral.Spot;
import com.sun.spot.peripheral.SpotFatalException;
import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.squawk.peripheral.InsufficientFlashMemoryException;

/**
 * FlashFile
 * <p>
 * This class is the main entry point to the flash memory filing system. This class is modelled on java.io.File
 * from j2se. As for File in j2se, a FlashFile does not represent the file itself, but merely a handle or name for
 * a file. Creating a FlashFile object does not create the file.
 * </p><p>
 * A flash-based file comprises one or more sectors of flash memory. An underlying flash manager takes care of
 * allocating sectors for files. Allocation is recorded in a File Allocation Table (FAT) managed by the flash manager.
 * The flash manager, which is not publicly accessible, also holds file descriptors for each file.
 * </p><p>
 * One important difference between File and FlashFile is that when creating a new file the size of the file must be
 * specified, and files are <b>not</b> extended automatically.
 * </p><p>
 * For some uses (e.g. storing suites) it is important that a file appears to occupy a contiguous range of
 * memory addresses and starts at a specific address. Such a file is called a "mapped" file, and the flash filing
 * system uses the MMU to map the file's sectors to the correct addresses. To make a file mapped
 * use {@link #setVirtualAddress(int)} and then {@link #map()}.
 * </p><p>
 * Rather than deleting a file and thereby releasing the sectors it occupies, a file can be marked as "obsolete".
 * The space occupied by obsolete files is reclaimed at the next reboot. This is useful if the file contains the
 * bytecodes of an active suite.
 * </p><p>
 * <b>CAVEATS</b>
 * </p><p>
 * The current implementation does not provide file locking: it is possible for two users simultaneously to access
 * the same file, possibly with disastrous consequences.
 * </p><p>
 * A FlashFile object does not hold on to a pointer to the underlying file descriptor but gets it from the flash manager
 * each time it is required. Acquiring a reference to a file descriptor is an expensive operation (taking about 15ms
 * on an eSPOT), so you should avoid repeatedly calling FlashFile operations that need access to it, such as
 * {@link #getAllocatedSpace()} or {@link #length()}.
 * </p>
 */
public class FlashFile {
	
	/**
	 * The index used with SpotGlobals to access the globally shared FlashManager
	 */
	public static final int SPOT_GLOBAL_FLASH_MANAGER = 1;
	
	/**
	 * The index used with SpotGlobals to access the globally shared NorFlashSectorFactory
	 */
	public static final int SPOT_GLOBAL_NOR_FLASH_SECTOR_FACTORY = 2;

        /**
	 * The size of the virtual address space allocated to a mapped file
	 */
	public static final int VIRTUAL_ADDRESS_FILE_SPACING = 1024*1024;
	
	/**
	 * The lowest valid virtual address that can be allocated to a file
	 */
	public static final long FIRST_FILE_VIRTUAL_ADDRESS = 0x10800000L;
	
	static final int VIRTUAL_ADDRESS_FILE_COUNT = 8;
	
	/**
	 * The highest valid virtual address that can be allocated to a file
	 */
	public static final long LAST_FILE_VIRTUAL_ADDRESS = FIRST_FILE_VIRTUAL_ADDRESS + (VIRTUAL_ADDRESS_FILE_COUNT-1)*VIRTUAL_ADDRESS_FILE_SPACING;

	private String name;

	public static final int FAT_IDENTIFIER_V1 = 0x12345678;
	public static final int FAT_IDENTIFIER_V2 = 0x12345679;
	public static final int FAT_IDENTIFIER_V3 = 0x1234567A;

	/**
	 * Set the INorFlashSectorFactory to be used by FlashFile when creating a FlashManager.
	 * It is only necessary to call this method if you want a factory other than the default one
	 * (which allocates sectors that are read and written using the standard flash memory driver).
	 * 
	 * @param norFlashSectorFactory the factory to use
	 * @throws IOException
	 */
	public static void setNorFlashSectorFactory(INorFlashSectorFactory norFlashSectorFactory) throws IOException {
		synchronized (SpotGlobals.getMutex()) {
			SpotGlobals.setGlobal(SPOT_GLOBAL_NOR_FLASH_SECTOR_FACTORY, norFlashSectorFactory);
			SpotGlobals.setGlobal(SPOT_GLOBAL_FLASH_MANAGER, null);
		}
	}

	/**
	 * Overwrite the existing FAT with a new empty one. Use with care as existing FlashFiles will be
	 * in an inconsistent state
	 * 
	 * @throws IOException
	 */
	public static void resetFAT() throws IOException {
		synchronized (SpotGlobals.getMutex()) {
			int highestSectorNumberKnownOnDevice = ConfigPage.LAST_COMMON_FILE_SYSTEM_SECTOR;
				
			FlashManager theFlashManager = new FlashManager(ConfigPage.FIRST_FILE_SYSTEM_SECTOR, highestSectorNumberKnownOnDevice);
			theFlashManager.initFilingSystem(getNorFlashSectorFactory());
			SpotGlobals.setGlobal(SPOT_GLOBAL_FLASH_MANAGER, theFlashManager);
		}
	}

	/**
	 * Get a read-only representation of the FAT
	 * @return a read-only representation of the FAT
	 */
	public static IFAT getFAT() {
		return getFlashManager();
	}
	
	/**
	 * Returns an unused virtual address.
	 * @return The virtual address
	 */
	public static int getUnusedVirtualAddress() throws IOException {
		return getFlashManager().allocateVirtualAddress();
	}
	
	/**
	 * Create a FlashFile with the specified name. Note that this does <b>not</b> create the file itself -
	 * call {@link #createNewFile(int)} to do that.
	 * 
	 * @param name the name of the file
	 */
	public FlashFile(String name) {
		this.name = name;
	}
	
	/**
	 * Create a new file of the specified size, whose name is the name of this FlashFile object.
	 * @param size the size in bytes of the file to create
	 * @return true if the file was created or false if it could not be created because it already existed
	 * @throws InsufficientFlashMemoryException if the space requested cannot be allocated
	 */
	public boolean createNewFile(int size) throws InsufficientFlashMemoryException {
		if (exists()) return false;
		getFlashManager().createFile(name, size);
		return true;
	}

	/**
	 * Check whether a file with this name exists
	 * @return true if a file with this name exsits, false otherwise
	 */
	public boolean exists() {
		return getFlashManager().exists(name);
	}

	/**
	 * Delete the file with this name
	 * @throws IOException
	 */
	public void delete() throws IOException {
		getFlashManager().deleteFile(name);
		getFlashManager().writeFAT();
	}

	/**
	 * Get the length of the file with this name. Warning: this operation accesses the file
	 * descriptor and will be slow - see class comment.
	 * @return the length in bytes of the valid portion of the file with this name
	 * @throws IOException
	 */
	public int length() throws IOException {
		if (!exists()) {
			return 0;
		} else {
			return getFileDescriptor().length();
		}
	}

	/**
	 * Ensure all changes to this file's descriptor are written persistently. Note that this
	 * operation does not force data written to a FlashFileOutputStream to be written persistently:
	 * the FlashFileOutputStream must be flushed or closed separately.
	 * @throws IOException
	 */
	public void commit() throws IOException {
		getFileDescriptor().setLastModifiedMillis(System.currentTimeMillis());
		getFlashManager().writeFAT();
	}

	/**
	 * Set the virtual address that the file gets mapped to. This mapping will take 
	 * place at the next reboot or when {@link #map()} is invoked. The virtual address must be one
	 * of the valid addresses defined by {@link #FIRST_FILE_VIRTUAL_ADDRESS}, {@link #LAST_FILE_VIRTUAL_ADDRESS}
	 * and {@link #VIRTUAL_ADDRESS_FILE_SPACING}.
	 * @param virtualAddress the virtual address for the file, or 0 if it should not be a mapped file
	 * @throws IOException
	 */
	public void setVirtualAddress(int virtualAddress) throws IOException {
		if (getFileDescriptor().getVirtualAddress() != 0) {
			throw new IllegalStateException("File already has a virtual address");
		}
		if (virtualAddress == getFileDescriptor().getVirtualAddress())
			return;
		getFlashManager().validateVirtualAddress(virtualAddress);
		getFileDescriptor().setVirtualAddress(virtualAddress);
		getFileDescriptor().setLastModifiedMillis(System.currentTimeMillis());
	}

	/**
	 * Set a comment for this file
	 * @param comment the comment to be attached to the descriptor of the file
	 * @throws IOException
	 */
	public void setComment(String comment) throws IOException {
		getFileDescriptor().setLastModifiedMillis(System.currentTimeMillis());
		getFileDescriptor().setComment(comment);
	}

	/**
	 * Get the virtual address of a mapped file
	 * @return the virtual address of the file, or 0 if it is not a mapped file
	 * @throws IOException
	 */
	public int getVirtualAddress() throws IOException {
		return getFileDescriptor().getVirtualAddress();
	}

	/**
	 * Get the comment for this file
	 * @return the comment attached to the file's descriptor
	 * @throws IOException
	 */
	public String getComment() throws IOException {
		return getFileDescriptor().getComment();
	}

	/**
	 * Get the time at which this file was last modified
	 * @return the time (as returned by System.currentTimeMillis()) at which this file was last modified.
	 * @throws IOException
	 */
	public long lastModified() throws IOException {
		return getFileDescriptor().lastModified();
	}

	/**
	 * Set whether or not the file with this name is obsolete. The space occupied by an obsolete file
	 * is reclaimed at the next reboot.
	 * @param b true if this file is to be marked obsolete, false if it is not
	 * @throws IOException
	 */
	public void setObsolete(boolean b) throws IOException {
		getFileDescriptor().setLastModifiedMillis(System.currentTimeMillis());
		getFileDescriptor().setObsolete(b);
	}

	/**
	 * Check whether the file with this name is obsolete
	 * @return true if this file is marked obsolete, false if it is not
	 * @throws IOException
	 */
	public boolean isObsolete() throws IOException {
		return getFileDescriptor().isObsolete();
	}

	/**
	 * Cause the file with this name to become a mapped file. The file's sectors will be
	 * mapped so that the file occupies a contiguous range of memory addresses, starting with its
	 * defined virtual address. If {@link #map()} is called before a virtual address has been specified
	 * using {@link #setVirtualAddress(int)} then a free virtual address will be allocated (and can be
	 * discovered after the {@link #map()} call using {@link #getVirtualAddress()}.
	 * @throws IOException
	 */
	public void map() throws IOException {
		if (getFileDescriptor().isMapped()) {
			throw new IllegalStateException("Cannot map a file that is already mapped");
		}
		getFlashManager().remap(name);
	}

	/**
	 * Check whether this file is mapped.
	 * @return true if this file is mapped to a virtual address
	 * @throws IOException
	 */
	public boolean isMapped() throws IOException {
		return getFileDescriptor().isMapped();
	}

	/**
	 * Check whether this file has a virtual address.
	 * @return true if this file has a virtual address
	 * @throws IOException
	 */
	public boolean isAddressed() throws IOException {
		return getFileDescriptor().isAddressed();
	}

	/**
	 * Determine the space allocated for the file with this name. Warning: this operation accesses
	 * the file descriptor and will be slow - see class comment.
	 * @return the space in bytes allocated for this file
	 * @throws IOException
	 */
	public int getAllocatedSpace() throws IOException {
		return getFileDescriptor().getAllocatedSpace();
	}

	/**
	 * Get the name of this file
	 * @return the name of this file
	 */
	public String getName() {
		return name;
	}

	/**
	 * Rename the file with this name so that it has a different name
	 * @param dest the new name for the file
	 * @return true if the file was renamed, false if a file with the new name already existed
	 * @throws IOException
	 */
	public boolean renameTo(FlashFile dest) throws IOException {
		boolean result = getFlashManager().rename(getFileDescriptor(), dest.getName());
		if (result) {
			getFlashManager().writeFAT();
		}
		return result;
	}

	/**
	 * For test purposes only
	 */
	public int getFirstSectorBaseAddress() throws IOException {
		return getFileDescriptor().getSectors()[0].getStartAddressAsInt();
	}

	IAddressableNorFlashSector getExtraSector() throws IOException {
		IAddressableNorFlashSector extraSector = getFlashManager().getExtraSector(getFileDescriptor());
		getFlashManager().writeFAT();
		return extraSector;
	}

	void releaseSector(IAddressableNorFlashSector sector) throws IOException {
		getFlashManager().releaseSector(getFileDescriptor(), sector);
		getFlashManager().writeFAT();
	}
	
	static IFlashManager getFlashManager() {
		IFlashManager theFlashManager;
		synchronized (SpotGlobals.getMutex()) {
			theFlashManager = (IFlashManager) SpotGlobals.getGlobal(SPOT_GLOBAL_FLASH_MANAGER);
			if (theFlashManager == null) {
				try {
					int highestSectorNumberKnownOnDevice = (RadioFactory.isRunningOnHost()) ?  
							ConfigPage.LAST_COMMON_FILE_SYSTEM_SECTOR :
							Spot.getInstance().getFlashMemoryDevice().getLastSectorAvailableToJava();
						
					theFlashManager = new FlashManager(ConfigPage.FIRST_FILE_SYSTEM_SECTOR, highestSectorNumberKnownOnDevice);
					((FlashManager) theFlashManager).initFromStoredFAT(getNorFlashSectorFactory());
					SpotGlobals.setGlobal(SPOT_GLOBAL_FLASH_MANAGER, theFlashManager);
				} catch (IOException e) {
					throw new SpotFatalException("Unexpected IOException while creating FlashManager");
				}
			}
		}		
		return theFlashManager;
	}
	
	FlashFileDescriptor getFileDescriptor() throws FlashFileNotFoundException {
		return getFlashManager().getFileDescriptorFor(name);
	}

	private static INorFlashSectorFactory getNorFlashSectorFactory() {
		INorFlashSectorFactory norFlashSectorFactory;
		synchronized (SpotGlobals.getMutex()) {
			norFlashSectorFactory = (INorFlashSectorFactory) SpotGlobals.getGlobal(SPOT_GLOBAL_NOR_FLASH_SECTOR_FACTORY);
			if (norFlashSectorFactory == null) {
				norFlashSectorFactory = new NorFlashSectorFactory(Spot.getInstance().getFlashMemoryDevice());
				SpotGlobals.setGlobal(SPOT_GLOBAL_NOR_FLASH_SECTOR_FACTORY, norFlashSectorFactory);
			}
		}		
		return norFlashSectorFactory;
	}
}

