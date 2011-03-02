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

package com.sun.spot.peripheral.external;

import com.sun.spot.peripheral.IFlashMemoryDevice;
import com.sun.spot.peripheral.ISpiMaster;

/**
 * Driver for the M25P05 flash memory chip as fitted to Sun SPOT external boards
 * 
 */
public class M25P05 implements IFlashMemoryDevice {
	
	public static final int SPI_CONFIG = (ISpiMaster.CSR_NCPHA | ISpiMaster.CSR_BITS_8 | ISpiMaster.CSR_SCBR_250K | ISpiMaster.CSR_DLYBCT_200);

	private static final int PAGE_SIZE = 256;

	private byte[] rxBufferSize2 = new byte[2];
	private byte[] commandBuffer = new byte[PAGE_SIZE + 4]; // 4 bytes to hold command and address
	private byte[] emptyArray = new byte[0];

	private static final byte M25P05_COMMAND_RDSR         = 5;
	private static final byte M25P05_COMMAND_RDID         = 0x15;
	private static final byte M25P05_COMMAND_WRSR         = 1;
	private static final byte M25P05_COMMAND_READ         = 3;
	private static final byte M25P05_COMMAND_WREN         = 6;
	private static final byte M25P05_COMMAND_WRDI         = 4;
	private static final byte M25P05_COMMAND_PROGRAM      = 2;
	private static final byte M25P05_COMMAND_SECTOR_ERASE = (byte)0xD8;
	private static final byte M25P05_COMMAND_CHIP_ERASE   = (byte)0xC7;

	private static final int[] M25P05_SECTOR_ADDRESSES = new int[] {0x000000, 0x008000};
	private static final int M25P05_SIZE = 0x010000;
	private static final int M25P05_SECTOR_SIZE = 0x8000;

	private static final byte WRITE_PROTECTION_BITS = 0x0C;

	private ISPI spi;

	public M25P05(ISPI spi) {
		this.spi = spi;
	}

	/**
	 * Read data from the M25P05 flash memory.
	 * 
	 * @param address address in memory to start reading, in range 0 to 0xFFFF
	 * @param numOfBytes number of bytes to read, in range 0 to (0x10000-address)
	 * @param buffer the hold the data
	 * @param offset offset into buffer for first byte read
	 */
	public synchronized void read(int address, int numOfBytes, byte[] buffer, int offset) {
		byte[] rxbuffer = new byte[numOfBytes];
		if (!validAddress(address)) throw new IllegalArgumentException("Illegal address: 0x" + Integer.toHexString(address));
		if (!validAddress(address+numOfBytes-1)) throw new IllegalArgumentException("Attempt to read beyond end of memory: 0x" + Integer.toHexString(address+numOfBytes-1));
		commandBuffer[0] = M25P05_COMMAND_READ;
		commandBuffer[1] = (byte)((address >> 16) & 0xFF);		
		commandBuffer[2] = (byte)((address >> 8) & 0xFF);		
		commandBuffer[3] = (byte)(address & 0xFF);
		spi.sendSPICommand(commandBuffer, 4, rxbuffer, numOfBytes, 4);
		System.arraycopy(rxbuffer, 0, buffer, offset, numOfBytes);
	}

	public void write(int address, int numOfBytes, byte[] buffer, int offset) {
		if (!validAddress(address)) throw new IllegalArgumentException("Illegal address: 0x" + Integer.toHexString(address));
		if ((address % PAGE_SIZE) != 0) throw new IllegalArgumentException("Attempt to write to unaligned address: 0x" + Integer.toHexString(address));
		if (numOfBytes > PAGE_SIZE) throw new IllegalArgumentException("Attempt to write more than one page: " + numOfBytes);

		enableWriting();
		commandBuffer[0] = M25P05_COMMAND_PROGRAM;
		commandBuffer[1] = (byte)((address >> 16) & 0xFF);		
		commandBuffer[2] = (byte)((address >> 8) & 0xFF);		
		commandBuffer[3] = (byte)(address & 0xFF);
		for (int i = 0; i < numOfBytes; i++) {
			commandBuffer[i+4] = buffer[offset+i];
		}
		spi.sendSPICommand(commandBuffer, numOfBytes+4, emptyArray, 0);
		waitForWriteToComplete();
	}

	/**
	 * Verify data in the M25P05 flash memory.
	 * 
	 * @param address address in memory to start verifying, in range 0 to 0xFF00 but must be page-aligned
	 * @param numOfBytes number of bytes to write, in range 0 to PAGE_SIZE
	 * @param buffer the data to verify against
	 * @return true if data matches
	 */
	public synchronized boolean verify(int address, int numOfBytes, byte[] buffer) {
		if (!validAddress(address)) throw new IllegalArgumentException("Illegal address: 0x" + Integer.toHexString(address));
		if ((address % PAGE_SIZE) != 0) throw new IllegalArgumentException("Attempt to verify unaligned address: 0x" + Integer.toHexString(address));
		if (numOfBytes > PAGE_SIZE) throw new IllegalArgumentException("Attempt to verify more than one page: " + numOfBytes);

		// bit naughty, but we'll use the command buffer to hold the data read
		read(address, numOfBytes, commandBuffer, 0);
		for (int i = 0; i < numOfBytes; i++) {
			if (commandBuffer[i] != buffer[i]) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Erase a sector
	 * 
	 * @param address an address within sector to erase
	 */
	public synchronized void eraseSectorAtAddress(int address) {
		if (!validAddress(address)) throw new IllegalArgumentException("Illegal address: 0x" + Integer.toHexString(address));
		enableWriting();
		commandBuffer[0] = M25P05_COMMAND_SECTOR_ERASE;
		commandBuffer[1] = (byte)((address >> 16) & 0xFF);		
		commandBuffer[2] = (byte)((address >> 8) & 0xFF);		
		commandBuffer[3] = (byte)(address & 0xFF);
		spi.sendSPICommand(commandBuffer, 4, emptyArray, 0);
		waitForWriteToComplete();
	}

	/**
	 * Check whether a sector is erased.
	 * 
	 * @param address an address within sector to check
	 * @return true if sector is erased
	 */
	public synchronized boolean sectorErased(int address) {
		if (!validAddress(address)) throw new IllegalArgumentException("Illegal address: 0x" + Integer.toHexString(address));
		// determine which sector this is
		int sectorNum = getSectorContainingAddress(address);
		int checkAddress = M25P05_SECTOR_ADDRESSES[sectorNum];
		int topAddress = checkAddress + M25P05_SECTOR_SIZE;
		while (checkAddress < topAddress) {
			// read a page
			// bit naughty, but we'll use the command buffer to hold the data read
			read(checkAddress, PAGE_SIZE, commandBuffer, 0);
			// loop through page checking erased
			for (int j = 0; j < PAGE_SIZE; j++) {
				if (commandBuffer[j] != (byte)0xFF) {
					return false;
				}
			}
			checkAddress += PAGE_SIZE;
		}
		return true;
	}

	/**
	 * Erase all data in the chip
	 */
	public synchronized void eraseChip() {
		enableWriting();
		commandBuffer[0] = M25P05_COMMAND_CHIP_ERASE;
		spi.sendSPICommand(commandBuffer, 1, emptyArray, 0);
		waitForWriteToComplete();
	}

	/**
	 * Get the page size for writing. Each call to write can write no more than one page.
	 * 
	 * @return The page size in bytes
	 */
	public int getPageSize() {
		return PAGE_SIZE;
	}
	
	/**
	 * Set or clear the write protection
	 * 
	 * @param b If b is true the device becomes write protected; if b is false the device becomes writable.
	 */
	public void setWriteProtection(boolean b) {
		setStatusReg(b ? WRITE_PROTECTION_BITS : 0);
	}

	/**
	 * Check whether the device is write protected
	 * 
	 * @return true if it is write protected
	 */
	public boolean isWriteProtected() {
		return (getStatusReg() & WRITE_PROTECTION_BITS) != 0;
	}

	/**
	 * Get the capacity of the device
	 * @return The size of the memory in bytes
	 */
	public int getSize() {
		return M25P05_SIZE;
	}

	/**
	 * Get the size of a device sector
	 * 
	 * @param sectorNum The sector whose size is to be returned
	 * @return The size of a sector in bytes
	 */
	public int getSectorSize(int sectorNum) {
		return M25P05_SECTOR_SIZE;
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.spot.IFlashMemoryDevice#getNumberOfSectors()
	 */
	public int getNumberOfSectors() {
		return M25P05_SECTOR_ADDRESSES.length;
	}

	public int getLastSectorAvailableToJava() {
		return M25P05_SECTOR_ADDRESSES.length - 1;
	}
	
	public int getSectorAddress(int sectorNum) {
		return M25P05_SECTOR_ADDRESSES[sectorNum];
	}

	synchronized int getStatusReg() {
		commandBuffer[0] = M25P05_COMMAND_RDSR;
		spi.sendSPICommand(commandBuffer, 1, rxBufferSize2, 1, 1);
		return rxBufferSize2[0] & 0xFF;
	}

	synchronized void setStatusReg(byte regValue) {
		enableWriting();
		commandBuffer[0] = M25P05_COMMAND_WRSR;
		commandBuffer[1] = regValue;
		spi.sendSPICommand(commandBuffer, 2, emptyArray, 0);
		waitForWriteToComplete();
	}


	
	public int getSectorContainingAddress(int addr) {
		int sectorNum;
		for (sectorNum = M25P05_SECTOR_ADDRESSES.length-1; sectorNum >= 0 ; sectorNum--) {
			if (M25P05_SECTOR_ADDRESSES[sectorNum] <= addr) {
				break;
			}
		}
		return sectorNum;
	}

	private boolean validAddress(int address) {
		return address >=0 && address < M25P05_SIZE;
	}

	private int waitForWriteToComplete() {
		int regValue;
		do {
			regValue = getStatusReg();
		} while ((regValue & 1) != 0);
		return regValue; 
	}

	private void enableWriting() {
		commandBuffer[0] = M25P05_COMMAND_WREN;
		spi.sendSPICommand(commandBuffer, 1, emptyArray, 0);
	}

	public int getNumberOfSectorsInRegion(int startAddress, int length) {
		return 1+getSectorContainingAddress(startAddress+length-1) - getSectorContainingAddress(startAddress);
	}
}
