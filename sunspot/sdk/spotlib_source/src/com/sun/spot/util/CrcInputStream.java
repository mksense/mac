/*
 * Copyright 2007-2009 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.spot.util;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

class CrcInputStream extends InputStream {

	/**
	 * Internal class used by CrcOutputStream
	 */
	private static final int PROBE_RESPONSE = 0xC0000000;
	private static final long PROBE_TIMEOUT_PERIOD = 200;
	private byte[] buffer = new byte[0];
	private DataInputStream inData;
	private int byteIndex = 0;
	private int outstandingAck;
	private CrcOutputStream crcOutputStream;
	private byte[] pendingBuffer;
	private int pendingChecksum;

	public CrcInputStream(InputStream inputStream, CrcOutputStream crcOutputStream) {
		inData = new DataInputStream(inputStream);
		this.crcOutputStream = crcOutputStream;
	}

	public int available() throws IOException {
		return (buffer.length - byteIndex) + (pendingBuffer == null ? 0 : pendingBuffer.length);
	}

	public int read() throws IOException {
		ensureDataAvailable();
		return buffer[byteIndex++] & 0xFF;
	}

	public int read(byte[] b, int off, int len) throws IOException {
		if (len == 0) {
			return 0;
		}
		ensureDataAvailable();
		int bytesToRead = Math.min(buffer.length-byteIndex, len);
		System.arraycopy(buffer, byteIndex, b, off, bytesToRead);
		byteIndex += bytesToRead;
		return bytesToRead;
	}

	public void close() throws IOException {
		inData.close();
	}
	
	private void ensureDataAvailable() throws IOException {
		while (byteIndex == buffer.length) {
			while (pendingBuffer == null) {
				doRead(false);
				Thread.yield(); // TODO
								// bug 1137: it is important that we give another thread that wants to pick up an ACK
								// a chance to get in, otherwise it may spend its life blocked trying to
								// enter doRead. Using Thread.yield to achieve this is very ugly - it might be better
								// if CrcInputStream had its own reading thread.
			}
			movePendingToCurrent();
		}
	}
	

	public int getAck() throws IOException {
		while (outstandingAck == 0) {
			doRead(true);
			Thread.yield(); // TODO
							// bug 1137: it is important that we give another thread that wants to pick up data
							// a chance to get in, otherwise it may spend its life blocked trying to
							// enter doRead. Using Thread.yield to achieve this is very ugly - it might be better
							// if CrcInputStream had its own reading thread.
		}
		int returnValue = outstandingAck & 0x7FFFFFFF;
		outstandingAck = 0;
		return returnValue;
	}

	private synchronized void doRead(boolean ackRequired) throws IOException {
		if (ackRequired && outstandingAck != 0) {
			return;
		}
		if (!ackRequired && pendingBuffer != null) {
			return;
		}
		int length = inData.readInt();

		if (length == 0) { // length == 0 means the remote end is probing to see if we're here
			Utils.log("[CrcInputStream] Responding to probe");
			crcOutputStream.writeAck(PROBE_RESPONSE);
		} else if (length < 0) {
			if (outstandingAck != 0) {
				throw new IOException("Received new incoming ack before last ack was read");
			}
			outstandingAck = length;
		} else if (length > (1024 * 1024)) {
            int len = inData.available();
            byte[] rawBytesBuffer = new byte[len+4];
            Utils.writeBigEndInt(rawBytesBuffer, 0, length);
            inData.readFully(rawBytesBuffer, 4, len);
            String stuff = new String(rawBytesBuffer);
            throw new IOException("Attempt to read unlikely checked byte array size: " + length + ". Raw data follows:\n" + stuff);
		} else {
			if (pendingBuffer != null) {
				throw new IOException("Received new incoming data before last data was acknowledged");
			}
			byte[] newPendingBuffer = new byte[length];
			inData.readFully(newPendingBuffer);
			pendingChecksum = inData.readShort() & 0xFFFF;
			pendingBuffer = newPendingBuffer;
		}
	}

	private void movePendingToCurrent() throws IOException {
		int crc = CRC.crc(pendingBuffer, 0, pendingBuffer.length) & 0xFFFF;
		crcOutputStream.writeAck(crc | 0x80000000);
		if (crc != pendingChecksum) {
			pendingBuffer = null;
			throw new IOException("Checksum for received checked byte array is incorrect");
		}
		buffer = pendingBuffer;
		pendingBuffer = null;
		byteIndex = 0;
	}

	synchronized boolean getProbeResponse() throws IOException {
		long timeout = System.currentTimeMillis() + PROBE_TIMEOUT_PERIOD;
		byte[] result = new byte[4];
		int offset = 0;
		while (System.currentTimeMillis() < timeout && offset < result.length) {
			if (inData.available() > 0) {
				offset += inData.read(result, offset, result.length-offset);
			}
			Utils.sleep(10);
		}
		if (offset == result.length) {
			int response = Utils.readBigEndInt(result, 0);
//			System.out.println("[CrcInputStream] Received probe response of " + response);
			return response == PROBE_RESPONSE;
		} else {
//			int available = inData.available();
//			System.out.println("[CrcInputStream] Timed out waiting for probe response with " + available + " available");
			return false;
		}
	}

	void clear() throws IOException {
		while (inData.available() > 0) {
//			System.out.println("CrcInputStream.clear() skipped ascii value " + inData.read());
			inData.skip(inData.available());
		}
	}
}
