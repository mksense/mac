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

import com.sun.spot.flashmanagement.IAddressableNorFlashSector;
import com.sun.squawk.Address;
import com.sun.squawk.peripheral.INorFlashSector;

/**
 * Define a segment of memory which is defined as being of type NOR Flash.
 * The read-only mode of NOR memories is similar to reading from a common memory,
 * provided address and data bus is mapped correctly, so NOR flash memory is much
 * like any address-mapped memory. NOR flash memories can be used as execute-in-place
 * memory (XIP), meaning it behaves as a ROM memory mapped to a certain address.
 * A NOR flash sector can be completely erased, setting all bits to 1s.  Writing simply
 * sets some bits from 1 to 0.  Setting a bit from 0 to 1, requires the complete sector
 * to be erased.
 * 
 * @author Eric Arseneau
 *
 */
public class NorFlashSector implements IAddressableNorFlashSector, INorFlashSector {
	private int purpose;
	private int size;
	private int startAddress;
	private int virtualStartAddress;
	private IFlashMemoryDevice flashMem;
	private int sectorNumber;

    protected NorFlashSector() {
    }
    
    public NorFlashSector(IFlashMemoryDevice flashMem, int sectorNumber, int purpose) {
        this.flashMem = flashMem;
        this.sectorNumber = sectorNumber;
		this.startAddress = flashMem.getSectorAddress(sectorNumber);
		this.size = flashMem.getSectorSize(sectorNumber);
		this.purpose = purpose;
    }

    public void erase() {
    	flashMem.eraseSectorAtAddress(startAddress);
    }
    
    public void getBytes(int memoryOffset, byte[] buffer, int bufferOffset, int length) {
        if (length == 0) {
            return;
        }
        ensureInBounds(memoryOffset, buffer, bufferOffset, length);
        flashMem.read(startAddress+memoryOffset, length, buffer, bufferOffset);
    }
    
    public byte getErasedValue() {
        return (byte) 0xFF;
    }
    
    public int getPurpose() {
        return purpose;
    }
    
    public int getSize() {
        return size;
    }
    
    public Address getStartAddress() {
        return Address.fromPrimitive(startAddress);
    }

	public int getStartAddressAsInt() {
		return startAddress;
	}

    public int getVirtualStartAddressAsInt() {
        return virtualStartAddress;
    }
    
    public void setBytes(int memoryOffset, byte[] buffer, int bufferOffset, int length) {
        if (length == 0) {
            return;
        }
        if ((memoryOffset & 1) == 1) {
            throw new IndexOutOfBoundsException("memory offset must be even");
        }
        ensureInBounds(memoryOffset, buffer, bufferOffset, length);
        int pageSize = flashMem.getPageSize();
        int numPages = length / pageSize;
        int address = startAddress + memoryOffset;
        for (int i=0; i < numPages; i++) {
            flashMem.write(address, pageSize, buffer, bufferOffset);
            address += pageSize;
            bufferOffset += pageSize;

        }
        if (length % pageSize > 0) {
        	flashMem.write(address, length % pageSize, buffer, bufferOffset);
        }
    }
    
    public int getSectorNumber() {
    	return sectorNumber;
    }
    
    public void setVirtualAddress(int virtualAddress) {
    	virtualStartAddress = virtualAddress;
    }
    
    void ensureInBounds(int memoryOffset, byte[] buffer, int bufferOffset, int length) {
        if (memoryOffset > size || memoryOffset < 0) {
            throw new IndexOutOfBoundsException("sectorSize: " + size + " memoryOffset: " + memoryOffset);
        }
        if ((memoryOffset + length) > size) {
            throw new IndexOutOfBoundsException("sectorSize: " + size + " memoryOffset: " + memoryOffset + " length: " + length);
        }
        int bufferSize = buffer.length;
        if (bufferOffset > bufferSize || bufferOffset < 0) {
            throw new IndexOutOfBoundsException("bufferSize: " + bufferSize + " bufferOffset: " + bufferOffset);
        }
        if ((bufferOffset + length) > bufferSize) {
            throw new IndexOutOfBoundsException("bufferSize: " + bufferSize + " bufferOffset: " + bufferOffset + " length: " + length);
        }
    }
}
