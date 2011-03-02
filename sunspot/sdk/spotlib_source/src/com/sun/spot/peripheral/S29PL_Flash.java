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

import com.sun.squawk.Address;
import com.sun.squawk.Unsafe;
import com.sun.squawk.VM;
import com.sun.squawk.vm.ChannelConstants;


class S29PL_Flash implements IFlashMemoryDevice {
	
	public static final int S29PL032J =  62;  // Number of 64k sectors
	public static final int S29PL064J = 126;
	public static final int BIG_SECTOR_SIZE = 0x10000;
	
	private static final int FLASH_BASE_ADDR   = ConfigPage.FLASH_BASE_ADDRESS;
	private static final int PAGE_SIZE         = 1024 * 64; // the max number of bytes in an IFlashMemoryDevice write command

	private static final int NUMBER_OF_SMALL_SECTORS_AT_EACH_END = 8;
	private static final int NUMBER_OF_BIG_SECTORS_IN_COMMON = 62;
	
	private static final int SMALL_SECTOR_SIZE = 0x2000;
	private static final int ADDRESS_OF_FIRST_BIG_SECTOR = FLASH_BASE_ADDR + (NUMBER_OF_SMALL_SECTORS_AT_EACH_END * SMALL_SECTOR_SIZE);
/*
 * Layout of flash for S29PL032J
		// Bank A
		// 8k
		FLASH_BASE_ADDR + 0x000000, //00
		FLASH_BASE_ADDR + 0x002000, //01
		FLASH_BASE_ADDR + 0x004000, //02
		FLASH_BASE_ADDR + 0x006000, //03
		FLASH_BASE_ADDR + 0x008000, //04
		FLASH_BASE_ADDR + 0x00A000, //05
		FLASH_BASE_ADDR + 0x00C000, //06
		FLASH_BASE_ADDR + 0x00E000, //07
		// 64k after this
		FLASH_BASE_ADDR + 0x010000, //08
		FLASH_BASE_ADDR + 0x020000, //09
		FLASH_BASE_ADDR + 0x030000, //10
		FLASH_BASE_ADDR + 0x040000, //11
		FLASH_BASE_ADDR + 0x050000, //12
		FLASH_BASE_ADDR + 0x060000, //13
		FLASH_BASE_ADDR + 0x070000, //14
		// Bank B
		FLASH_BASE_ADDR + 0x080000, //15		
		FLASH_BASE_ADDR + 0x090000, //16		
		FLASH_BASE_ADDR + 0x0A0000, //17		
		FLASH_BASE_ADDR + 0x0B0000, //18		
		FLASH_BASE_ADDR + 0x0C0000, //19		
		FLASH_BASE_ADDR + 0x0D0000, //20		
		FLASH_BASE_ADDR + 0x0E0000, //21		
		FLASH_BASE_ADDR + 0x0F0000, //22		
		FLASH_BASE_ADDR + 0x100000, //23
		FLASH_BASE_ADDR + 0x110000, //24
		FLASH_BASE_ADDR + 0x120000, //25
		FLASH_BASE_ADDR + 0x130000, //26
		FLASH_BASE_ADDR + 0x140000, //27
		FLASH_BASE_ADDR + 0x150000, //28
		FLASH_BASE_ADDR + 0x160000, //29
		FLASH_BASE_ADDR + 0x170000, //30		
		FLASH_BASE_ADDR + 0x180000, //31		
		FLASH_BASE_ADDR + 0x190000, //32		
		FLASH_BASE_ADDR + 0x1A0000, //33		
		FLASH_BASE_ADDR + 0x1B0000, //34		
		FLASH_BASE_ADDR + 0x1C0000, //35		
		FLASH_BASE_ADDR + 0x1D0000, //36		
		FLASH_BASE_ADDR + 0x1E0000, //37		
		FLASH_BASE_ADDR + 0x1F0000, //38
		// Bank C
		FLASH_BASE_ADDR + 0x200000, //39
		FLASH_BASE_ADDR + 0x210000, //40
		FLASH_BASE_ADDR + 0x220000, //41
		FLASH_BASE_ADDR + 0x230000, //42
		FLASH_BASE_ADDR + 0x240000, //43
		FLASH_BASE_ADDR + 0x250000, //44
		FLASH_BASE_ADDR + 0x260000, //45
		FLASH_BASE_ADDR + 0x270000, //46		
		FLASH_BASE_ADDR + 0x280000, //47		
		FLASH_BASE_ADDR + 0x290000, //48		
		FLASH_BASE_ADDR + 0x2A0000, //49		
		FLASH_BASE_ADDR + 0x2B0000, //50		
		FLASH_BASE_ADDR + 0x2C0000, //51		
		FLASH_BASE_ADDR + 0x2D0000, //52		
		FLASH_BASE_ADDR + 0x2E0000, //53		
		FLASH_BASE_ADDR + 0x2F0000, //54		
		FLASH_BASE_ADDR + 0x300000, //55
		FLASH_BASE_ADDR + 0x310000, //56
		FLASH_BASE_ADDR + 0x320000, //57
		FLASH_BASE_ADDR + 0x330000, //58
		FLASH_BASE_ADDR + 0x340000, //59
		FLASH_BASE_ADDR + 0x350000, //60
		FLASH_BASE_ADDR + 0x360000, //61
		FLASH_BASE_ADDR + 0x370000, //62
		// Bank D
		FLASH_BASE_ADDR + 0x380000, //63		
		FLASH_BASE_ADDR + 0x390000, //64		
		FLASH_BASE_ADDR + 0x3A0000, //65		
		FLASH_BASE_ADDR + 0x3B0000, //66		
		FLASH_BASE_ADDR + 0x3C0000, //67		
		FLASH_BASE_ADDR + 0x3D0000, //68		
		FLASH_BASE_ADDR + 0x3E0000, //69
		// 8K after this
		FLASH_BASE_ADDR + 0x3F0000, //70		
		FLASH_BASE_ADDR + 0x3F2000, //71
		FLASH_BASE_ADDR + 0x3F4000, //72
		FLASH_BASE_ADDR + 0x3F6000, //73
		FLASH_BASE_ADDR + 0x3F8000, //74
		FLASH_BASE_ADDR + 0x3FA000, //75
		FLASH_BASE_ADDR + 0x3FC000, //76
		FLASH_BASE_ADDR + 0x3FE000, //77
*/
	private int numberOfBigSectors;
	private int addressOfFirstSmallSectorAtTop;

	/**
	 * Answer a sector address in the area that is common to all S29PL flash devices used in eSPOTs
	 * @param sectorNumber
	 * @return the address of the sector
	 */
	public static int getCommonSectorAddress(int sectorNumber) {
		if (sectorNumber >= (NUMBER_OF_SMALL_SECTORS_AT_EACH_END + NUMBER_OF_BIG_SECTORS_IN_COMMON)) {
			throw new IllegalArgumentException("Sector number " + sectorNumber + " is beyond common part");
		} else {
			return primGetSectorAddress(sectorNumber, NUMBER_OF_BIG_SECTORS_IN_COMMON);
		}
	}
	
	/**
	 * Answer the total size in bytes of the area that is common to all S29PL flash devices used in eSPOTs
	 * @return the flash size
	 */
	public static int getCommonSize() {
		return (NUMBER_OF_BIG_SECTORS_IN_COMMON * BIG_SECTOR_SIZE) + (NUMBER_OF_SMALL_SECTORS_AT_EACH_END * SMALL_SECTOR_SIZE);
	}


	/**
	 * Answer the number of the last large sector that appears on all S29PL flash devices used in eSPOTs
	 * @return the sector number
	 */
	public static int getCommonLastLargeSector() {
		return NUMBER_OF_BIG_SECTORS_IN_COMMON + NUMBER_OF_SMALL_SECTORS_AT_EACH_END - 1;
	}

	private static int primGetSectorAddress(int sectorNumber, int numberOfBigSectors) {
		if (sectorNumber < NUMBER_OF_SMALL_SECTORS_AT_EACH_END) {
			return ConfigPage.FLASH_BASE_ADDRESS + (sectorNumber * SMALL_SECTOR_SIZE);
		} else if (sectorNumber < NUMBER_OF_SMALL_SECTORS_AT_EACH_END + numberOfBigSectors){
			return ConfigPage.FLASH_BASE_ADDRESS + (NUMBER_OF_SMALL_SECTORS_AT_EACH_END * SMALL_SECTOR_SIZE) + (sectorNumber-NUMBER_OF_SMALL_SECTORS_AT_EACH_END)*BIG_SECTOR_SIZE;
		} else {
			return ConfigPage.FLASH_BASE_ADDRESS +
						(NUMBER_OF_SMALL_SECTORS_AT_EACH_END * SMALL_SECTOR_SIZE) + 
						(numberOfBigSectors * BIG_SECTOR_SIZE) +
						(sectorNumber-NUMBER_OF_SMALL_SECTORS_AT_EACH_END-numberOfBigSectors)*SMALL_SECTOR_SIZE;
		}
	}

	S29PL_Flash(int model) {
		numberOfBigSectors = model;
		addressOfFirstSmallSectorAtTop = ADDRESS_OF_FIRST_BIG_SECTOR + (BIG_SECTOR_SIZE * numberOfBigSectors);
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.spot.IFlashMemory#eraseSectorAtAddress(int)
	 */
	public void eraseSectorAtAddress(int address) {
		validateAddress(address);
		int count = 0;
		while (!(eraseSectorPrim(address) && sectorErased(address))) {
			if (count++ > 5) throw new SpotFatalException("Failed to erase flash at address " + address);
		}
	}

	/**
	 * @param address
	 * @return
	 */
	public boolean sectorErased(int address) {
		Address baseAddress = Address.fromPrimitive(FLASH_BASE_ADDR);
		validateAddress(address);
		// determine which sector this is
		int sectorNum = getSectorContainingAddress(address);
		// determine sector top
		int sectorAddress = getSectorAddress(sectorNum);
		int sectorTop = sectorAddress + getSectorSize(sectorNum);
		// loop through sector checking erased
		for (int j = sectorAddress; j < sectorTop; j += 8) {
			if (Unsafe.getLongAtWord(baseAddress, (j-FLASH_BASE_ADDR)>>2) != -1) {
				return false;
			}
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.external.IFlashMemoryDevice#eraseChip()
	 */
	public void eraseChip() {
		throw new SpotFatalException("The eraseChip operation is not supported for the S29PL032J device");
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.external.IFlashMemoryDevice#getPageSize()
	 */
	public int getPageSize() {
		return PAGE_SIZE;
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.external.IFlashMemoryDevice#getSectorSize()
	 */
	public int getSectorSize(int sectorNum) {
		validateSectorNumber(sectorNum);
		if (sectorNum < NUMBER_OF_SMALL_SECTORS_AT_EACH_END || sectorNum >= (NUMBER_OF_SMALL_SECTORS_AT_EACH_END + numberOfBigSectors)) {
			return SMALL_SECTOR_SIZE;
		} else {
			return BIG_SECTOR_SIZE;
		}
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.external.IFlashMemoryDevice#getSize()
	 */
	public int getSize() {
		return (numberOfBigSectors * BIG_SECTOR_SIZE) + (2 * NUMBER_OF_SMALL_SECTORS_AT_EACH_END * SMALL_SECTOR_SIZE);
	}

	public void read(int address, int numOfBytes, byte[] buffer, int offset) {
		validateAddressInRange(address);
		if (!isInRange(address+numOfBytes-1)) throw new IllegalArgumentException("Attempt to read beyond end of memory: 0x" + Integer.toHexString(address+numOfBytes-1));		
		Address addr = Address.fromPrimitive(address);
		Unsafe.getBytes(addr, 0, buffer, offset, numOfBytes);
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.external.IFlashMemoryDevice#verify(int, int, byte[])
	 */
	public boolean verify(int address, int numOfBytes, byte[] buffer) {
		validateAddressInRange(address);
		if ((address % PAGE_SIZE) != 0) throw new IllegalArgumentException("Attempt to verify unaligned address: 0x" + Integer.toHexString(address));
		if (numOfBytes > PAGE_SIZE) throw new IllegalArgumentException("Attempt to verify more than one page: " + numOfBytes);
		Address addr = Address.fromPrimitive(address);
		for (int i = 0; i < numOfBytes; i++) {
			if (buffer[i] != (byte)(Unsafe.getByte(addr, i) & 0xFF)) {
				return false;
			}
		}
		return true;
	}


	public int getSectorAddress(int sectorNum) {
		return primGetSectorAddress(sectorNum, numberOfBigSectors);
	}

	public void write(int address, int numOfBytes, byte[] buffer, int offset) {
		validateAddress(address);
		validateAddressInRange(address + numOfBytes - 1);
		if (numOfBytes > PAGE_SIZE) throw new IllegalArgumentException("Attempt to write more than one page: " + numOfBytes);
		boolean writtenOk = writePrim(address, buffer, numOfBytes, offset);
		if (! writtenOk)
			throw new SpotFatalException("Failed to write flash at address " + address);
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.spot.IFlashMemoryDevice#getNumberOfSectors()
	 */
	public int getNumberOfSectors() {
		return numberOfBigSectors + (2 * NUMBER_OF_SMALL_SECTORS_AT_EACH_END);
	}

	public int getSectorContainingAddress(int addr) {
		validateAddressInRange(addr);
		if (addr < ADDRESS_OF_FIRST_BIG_SECTOR) {
			return (addr - FLASH_BASE_ADDR) / SMALL_SECTOR_SIZE;
		} else if (addr < addressOfFirstSmallSectorAtTop) {
			return ((addr - ADDRESS_OF_FIRST_BIG_SECTOR) / BIG_SECTOR_SIZE) + NUMBER_OF_SMALL_SECTORS_AT_EACH_END;
		} else {
			return ((addr - addressOfFirstSmallSectorAtTop) / SMALL_SECTOR_SIZE) + NUMBER_OF_SMALL_SECTORS_AT_EACH_END + numberOfBigSectors;
		}
	}


	public int getLastSectorAvailableToJava() {
		return NUMBER_OF_SMALL_SECTORS_AT_EACH_END + numberOfBigSectors - 1;
	}

	public int getNumberOfSectorsInRegion(int startAddress, int length) {
		return 1+getSectorContainingAddress(startAddress+length-1) - getSectorContainingAddress(startAddress);
	}

	private boolean eraseSectorPrim(int address) {
		int result = VM.execSyncIO(ChannelConstants.FLASH_ERASE, FLASH_BASE_ADDR, address, 0,0,0,0,null,null);
		return result==1;
	}

	private boolean writePrim(int address, byte[] data, int size, int offset) {
		int result = VM.execSyncIO(ChannelConstants.FLASH_WRITE, address, size, offset,0,0,0,data,null);
		return result==1;
	}

	private void validateAddressInRange(int address) {
		if (!isInRange(address)) {
			throw new IllegalArgumentException("Address " + address + " is out of range of flash memory");
		}
	}
	
	private void validateAddress(int address) {
		validateAddressInRange(address);
		if (address % 2 != 0) {
			throw new IllegalArgumentException("Address " + address + " is not even.");
		}
	}

	private boolean isInRange(int address) {
		return !(address < FLASH_BASE_ADDR || address >= FLASH_BASE_ADDR + getSize());
	}

	private void validateSectorNumber(int sectorNum) {
		if (sectorNum < 0 || sectorNum > (numberOfBigSectors + (2 * NUMBER_OF_SMALL_SECTORS_AT_EACH_END))) {
			throw new IllegalArgumentException("Sector number " + sectorNum + " is not valid");
		}
	}
}
