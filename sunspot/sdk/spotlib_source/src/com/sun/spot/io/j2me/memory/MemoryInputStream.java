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

package com.sun.spot.io.j2me.memory;

import java.io.IOException;
import java.io.InputStream;

import com.sun.squawk.Address;
import com.sun.squawk.Unsafe;
import com.sun.squawk.io.j2me.memory.*;

/**
 * An {@link InputStream} that streams over the device memory. You should not
 * normally construct instance of this class directly, but instead, use the GCF
 * framework. See {@link Protocol} for more details.
 */
public class MemoryInputStream extends InputStream {
	private Address startAddress;
	private int currentIndex;
	
	/**
	 * Construct an instance to read from a given memory address.
	 * 
	 * @param startAddress -- the memory address at which to start reading
	 */
	public MemoryInputStream(int startAddress) {
		this.startAddress = Address.fromPrimitive(startAddress);
		currentIndex = 0;
	}

	/* (non-Javadoc)
	 * @see java.io.InputStream#read()
	 */
	public int read() {
		return Unsafe.getByte(startAddress, currentIndex++) & 0xFF;
	}

	public int read(byte[] b, int off, int len) throws IOException {
		Unsafe.getBytes(startAddress, currentIndex, b, off, len);
		currentIndex += len;
		return len;
	}


	/* (non-Javadoc)
	 * @see java.io.InputStream#close()
	 */
	public void close() {
	}

	/* (non-Javadoc)
	 * @see java.io.InputStream#skip()
	 */
	public long skip(long x) {
		currentIndex += x;
		return x;
	}

	/**
	 * Returns the number of bytes available to be read from the stream. 
	 * @return always returns 1 since the stream never blocks and we don't know its size
	 */
	public int available() {
		return 1;
	}
}
