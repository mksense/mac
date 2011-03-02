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

package com.sun.spot.peripheral.radio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

public class SpotSerialPipe {
	
	/**
	 * Number of bytes that you must leave blank in your byte array for SpotSerialPipe to use for its own purposes
	 */
	public static final int PAYLOAD_OFFSET = 2;

	private static final byte MAGIC_BYTE = (byte)0xDE;
	private InputStream inputStream;
	private OutputStream outputStream;
	private byte[] scratchBuffer = new byte[1];

	public SpotSerialPipe() throws IOException {
		StreamConnection c = (StreamConnection)Connector.open("serial://");
		inputStream = c.openInputStream();
		outputStream = c.openOutputStream();
		while (inputStream.available()>0) {
			inputStream.read();
		}
	}

	public void receive(byte[] in) {
		try {
			int length;
			do {
				skipToMagicByte();
				length = inputStream.read();
				if (length > in.length) {
					System.err.println("[SP] bad len " + length);
				}
			} while (length > in.length);
			int offset = 0;
			while (offset < length) {
				offset = offset + inputStream.read(in, offset, length - offset);
			}
		} catch (Exception e) {
			System.err.println("receiving exception: " + e.getMessage());
		}
		//System.err.println("Received byte array: " + Utils.stringify(in));
	}

	private void skipToMagicByte() throws IOException {
		int skippedBytes = 0;
		do {
			inputStream.read(scratchBuffer, 0, 1);
			skippedBytes++;
		} while (scratchBuffer[0] != MAGIC_BYTE);
		
		if (skippedBytes > 1) {
			System.err.println("skipped " + (skippedBytes-1));
		}
	}

	public void send(byte[] out, int length) {
		out[0] = MAGIC_BYTE;
		out[1] = (byte)length;
		// System.err.println("About to send a byte array of len " + (length+PAYLOAD_OFFSET) + ": " + Utils.stringify(out));
		try {
			// it seems that if you send 64 bytes the host app won't see it until some more data is sent
			// so if the length is 64 (or a multiple) tack a null on the end
			// TODO - hack here which assumes there will be a free byte at the end
			int actualLength = length + PAYLOAD_OFFSET;
			if (actualLength % 64 == 0) out[actualLength++] = 0;
			outputStream.write(out, 0, actualLength);
			outputStream.flush();
		} catch (Exception e) {
			System.err.println("receiving exception: " + e.getMessage());
		}
	}

	public void reset() throws IOException {
		// We do this to prompt the RXTX comms into life on the MAC
		// otherwise a small write upstream never makes it to the host-side Java.
		outputStream.write(new byte[128]);
	}
}
