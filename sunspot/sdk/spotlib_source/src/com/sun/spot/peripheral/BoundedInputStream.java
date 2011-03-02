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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Wraps a parent {@link InputStream} with an input stream on the assumptions
 * that the first four bytes of the parent's content are a Java int which tells us
 * how many more valid bytes remain in the parent.
 */
public class BoundedInputStream extends InputStream {
	private int length;
	private int count;
	private InputStream in;
	
	/**
	 * Construct an instance based on the supplied parent {@link InputStream}.
	 * 
	 * @param in the parent input stream (see class comment)
	 * @throws IOException
	 */
	public BoundedInputStream(InputStream in) throws IOException {
		this.in = in;
		length = Math.max(new DataInputStream(in).readInt(), 0);
		count = 0;
	}

	/* (non-Javadoc)
	 * @see java.io.InputStream#read()
	 */
	public int read() throws IOException {
		if (count >= length) {
			return -1;
		} else {
			count++;
			return in.read();
		}
	}

	/* (non-Javadoc)
	 * @see java.io.InputStream#close()
	 */
	public void close() throws IOException {
        in.close();
	}

	/* (non-Javadoc)
	 * @see java.io.InputStream#available()
	 */
	public int available() throws IOException {
		return length - count;
	}
}
