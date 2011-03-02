/*
 * Copyright 2006-2008 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.spot.peripheral;


/**
 * Interface to flash memory devices on the main Spot board and external boards.
 * 
 * We assume the device has the concepts of sector (unit of erasure) and page (max
 * size of write, alignment boundary). Not all devices have page alignment restrictions,
 * so a special write operation is provided that does not insist on writing at a page
 * boundary. Devices that require alignment should throw an exception if this is called.
 * 
 * For many devices the page size could, in principle, be equal to the sector size,
 * or even larger, but implementors of this interface need to bear in mind:
 * 
 * 1) the page size is used for the size of the RAM buffers in streamed I/O
 * 2) flash writing for devices on the main bus often locks out all other processor
 *    activity, including interrupts. So for devices like that keeping the page size
 *    small is beneficial
 * 
 * @author Syntropy
 */

public interface IFlashMemoryDevice {

	/**
	 * Read data from the flash memory.
	 * 
	 * @param address address in memory to start reading
	 * @param numOfBytes number of bytes to read
	 * @param buffer the hold the data
	 * @param offset offset into buffer for first byte read
	 */
	public void read(int address, int numOfBytes, byte[] buffer, int offset);

	/**
	 * Write data into the flash memory.
	 * 
	 * @param address address in memory to start writing, no need to be page-aligned
	 * @param numOfBytes number of bytes to write, in range 0 to PAGE_SIZE
	 * @param buffer the data to write
	 * @param offset the offset into the buffer of the first byte to write
	 * @throws IllegalStateException if unaligned and this device requires page alignment
	 */
	public void write(int address, int numOfBytes, byte[] buffer, int offset);

	/**
	 * Verify data in the flash memory.
	 * 
	 * @param address address in memory to start verifying, must be page-aligned
	 * @param numOfBytes number of bytes to write, in range 0 to PAGE_SIZE
	 * @param buffer the data to verify against
	 * @return true if data matches
	 */
	public boolean verify(int address, int numOfBytes, byte[] buffer);

	/**
	 * Erase a sector
	 * 
	 * @param address an address within sector to erase - must be even and in range
	 */
	public void eraseSectorAtAddress(int address);

	/**
	 * Check whether a sector is erased.
	 * 
	 * @param address an address within sector to check
	 * @return true if sector is erased
	 */
	public boolean sectorErased(int address);

	/**
	 * Erase all data in the chip
	 */
	public void eraseChip();

	/**
	 * Get the page size for writing. Each call to write can write no more than one page.
	 * 
	 * @return The page size in bytes
	 */
	public int getPageSize();

	/**
	 * Get the address of a sector. The first sector is numbered zero.
	 * 
	 * @param sectorNum The sector whose address is to be returned
	 * @return the sector address
	 */
	public int getSectorAddress(int sectorNum);

	/**
	 * Get the capacity of the device
	 * @return The size of the memory in bytes
	 */
	public int getSize();

	/**
	 * Get the size of a device sector
	 * 
	 * @param sectorNum The sector whose size is to be returned
	 * @return The size of the sector in bytes
	 */
	public int getSectorSize(int sectorNum);
	
	/**
	 * Get the number of sectors in the device.
	 * 
	 * @return Number of sectors.
	 */
	public int getNumberOfSectors();

	/**
	 * Find the sector associated with a given address in the flash.
	 * 
	 * @param addrToFlash The address.
	 * @return The sector number.
	 */
	public int getSectorContainingAddress(int addrToFlash);

	/**
	 * Find the number of sectors in a region of the flash memory.
	 * 
	 * @param addrToFlash The start of the region.
	 * @param dataSize The size of the region.
	 * @return The number of sectors.
	 */
	public int getNumberOfSectorsInRegion(int addrToFlash, int dataSize);

	/**
	 * Answer the last flash sector available to Java.
	 * @return sector number
	 */
	public int getLastSectorAvailableToJava();
}
