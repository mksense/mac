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

package com.sun.spot.dmamemory;

import java.util.Hashtable;

import com.sun.spot.peripheral.SpotFatalException;
import com.sun.squawk.VM;
import com.sun.squawk.vm.ChannelConstants;

/**
 * Manages a chunk of uncached memory to be used for DMA buffers - see {@link IDMAMemoryManager}
 */
public class DMAMemoryManager implements IDMAMemoryManager {

	private MemoryBlock freeBlocks = new MemoryBlock(0, 0, "DUMMY");
	private MemoryBlock allocatedBlocks = new MemoryBlock(0, 0, "DUMMY");

	/**
	 * For use by the SPOT library only - to get a handle to a DMA Memory Manager use
	 * Spot.getInstance().getDMAMemoryManager()
	 */
	public DMAMemoryManager() {
		int size = VM.execSyncIO(ChannelConstants.GET_DMA_BUFFER_SIZE, 0);
		int startAddress = VM.execSyncIO(ChannelConstants.GET_DMA_BUFFER_ADDRESS, 0);
		freeBlocks.next = new MemoryBlock(startAddress, size, "");
	}

	public synchronized int getBuffer(int size, String comment) throws NotEnoughDMAMemoryException {
		if (size <= 0) throw new SpotFatalException("Request for DMA buffer of size " + size);
		MemoryBlock block = freeBlocks;
		while (block.next != null) {
			if (block.next.size < size) {
				// too small, move on to next
				block = block.next;
			} else {
				int result = block.next.startAddress;
				block.next.comment = comment;
				block.next.allocate(block, size);
				return result;
			}
		}
		throw new NotEnoughDMAMemoryException("Cannot allocate DMA buffer of size " + size + ", biggest available is " + getMaxAvailableBufferSize());
	}

	public synchronized int getMaxAvailableBufferSize() {
		MemoryBlock b = freeBlocks;
		while (b.next != null) {
			b = b.next;
		}
		return b.size;
	}

	public synchronized void releaseBuffer(int memAddr) {
		MemoryBlock allocatedBlock = removeBlock(allocatedBlocks, memAddr);
		if (allocatedBlock == null) {
			throw new SpotFatalException("Attempt to release unallocated DMA buffer at address " + memAddr);
		}
		insertFreeBlock(allocatedBlock);
	}
	
	public synchronized Hashtable getAllocationDetails() {
		Hashtable result = new Hashtable();
		MemoryBlock block = allocatedBlocks.next;
		while (block != null) {
			result.put(block.comment, new Integer(block.size));
			block = block.next;
		}
		return result;
	}
	
	private MemoryBlock removeBlock(MemoryBlock root, int memAddr) {
		MemoryBlock block = root;
		while (block.next != null) {
			if (block.next.startAddress == memAddr) {
				MemoryBlock result = block.next;
				block.next = block.next.next;
				return result;
			}
			block = block.next;
		}
		return null;
	}

	private void insertFreeBlock(MemoryBlock block) {
		MemoryBlock b = removeBlock(freeBlocks, block.startAddress-block.size);
		if (b != null) {
			// the block of memory just before this one was free - merge them
			b.size = b.size + block.size;
			insertFreeBlock(b);
			return;
		}
		b = removeBlock(freeBlocks, block.startAddress+block.size);
		if (b != null) {
			// the block of memory just after this one was free - merge them
			block.size = b.size + block.size;
		}
		b = freeBlocks;
		while (b.next != null) {
			if (b.next.size < block.size) {
				b = b.next;
			} else {
				break;
			}
		}
		block.next = b.next;
		b.next = block;
	}

	private class MemoryBlock {
		MemoryBlock next = null;
		int startAddress;
		int size;
		String comment;

		public MemoryBlock(int startAddress, int size, String comment) {
			this.startAddress = startAddress;
			this.size = size;
			this.comment = comment;
		}

		/*
		 * Allocate all or part of this block
		 */
		public void allocate(MemoryBlock previousBlock, int requestedSize) {
			previousBlock.next = next;
			next = allocatedBlocks.next;
			allocatedBlocks.next = this;
			if (size != requestedSize) {
				insertFreeBlock(new MemoryBlock(startAddress+requestedSize, size-requestedSize, ""));
			}
			size = requestedSize;
		}
	}
}
