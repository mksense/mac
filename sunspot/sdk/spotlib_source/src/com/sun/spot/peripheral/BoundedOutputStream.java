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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;


/**
 * An output stream that writes the length of the data at the start.
 * Compatible with BoundedInputStream.
 */
public class BoundedOutputStream extends OutputStream {

	private ByteArrayOutputStream buffer;
	
	private OutputStream output;
	
	public BoundedOutputStream(OutputStream os) {
		buffer = new ByteArrayOutputStream();
		this.output = os;
	}

	public void close() throws IOException {
		buffer.flush();
		int length = buffer.size();
		DataOutputStream dos = new DataOutputStream(output);
		dos.writeInt(length);
		dos.write(buffer.toByteArray());
		dos.flush();
		dos.close();
	}

	public void flush() throws IOException {
		buffer.flush();
	}

	public void write(byte[] b) throws IOException {
		buffer.write(b);
	}

	public void write(byte[] b, int off, int len) throws IOException {
		buffer.write(b, off, len);
	}

	public void write(int b) throws IOException {
		buffer.write(b);
	}
}
