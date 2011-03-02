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

import com.sun.spot.peripheral.TimeoutException;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * CrcOutputStream provides CRC checking and flow control for two Java programs 
 * communicating with two input and output stream pairs.
 * 
 * To use this class, open a CrcOutputStream around the InputStream and OutputStream at each end.
 * Then use the accessor {@link #getInputStream()} to obtain an InputStream and do further io using
 * the CrcOutputStream and the InputStream obtained from {@link #getInputStream()}. Data is sent over
 * the output stream in blocks whose size can be specified in CrcOutputStream constructor. A smaller
 * block is sent whenever a flush is done on the CrcOutputStream.
 * 
 * If data corruption occurs, IOExceptions will be thrown on both the sending and receiving threads.
 * Subsequent read and writes will still be valid.
 * 
 * The streams are not thread-safe in the presence of multiple simultaneous accessors; each stream should be
 * accessed from one thread.
 *
 */
public class CrcOutputStream extends OutputStream {

	static final int DEFAULT_BLOCK_SIZE = 100;
	private static final int PROTOCOL_OVERHEAD = 6; // 4 for the length and 2 for the CRC
	private byte[] buffer;
	private byte[] ackBuffer = new byte[4];
	private DataOutputStream outData;
	private int byteCount = 0;
	private CrcInputStream inputStream;
	private int blockSize;

	/**
	 * Open an output stream that will add CRC checking and flow control to the outgoing data stream.
	 * 
	 * @param outputStream the underlying output stream on which to write data. 
	 * @param inputStream to receive data from the remote CRCOutputStream, and acknowledgements and CRC
	 * hashes from the remote CRC InputStream. 
	 */
	public CrcOutputStream(OutputStream outputStream, InputStream inputStream) {
		this(outputStream, inputStream, DEFAULT_BLOCK_SIZE);
	}

	/**
	 * Open an output stream that will add CRC checking and flow control to the outgoing data stream.
	 * 
	 * @param outputStream the underlying output stream on which to write data. 
	 * @param inputStream to receive data from the remote CRCOutputStream, and acknowledgements and CRC
	 * hashes from the remote CRC InputStream. 
	 * @param blockSize the maximum number of bytes of data to be written before waiting for a CRC confirmation
	 * from the remote device. Also, the remote device will never buffer more than (2 * blockSize) bytes
	 * of data awaiting reading from the containing CRC InputStream.
	 */
	public CrcOutputStream(OutputStream outputStream, InputStream inputStream, int blockSize) {
		buffer = new byte[blockSize+PROTOCOL_OVERHEAD];
		this.blockSize = blockSize;
		outData = new DataOutputStream(outputStream);
		this.inputStream = new CrcInputStream(inputStream, this);
	}

	/**
	 * @return a CRC InputStream. This supplies data received from the remote device's CrcOutputStream
	 * whose integrity has been checked using a CRC hash. No more than (2 * blockSize) bytes of data will
	 * be buffered locally.
	 */
	public InputStream getInputStream() {
		return inputStream;
	}

	/**
	 * @see java.io.OutputStream#write(int)
	 */
	public void write(int b) throws IOException {
		buffer[byteCount++ + 4] = (byte) b;
		if (byteCount == blockSize) {
			writeBuffer();
		}
	}

	/**
	 * @see java.io.OutputStream#write(byte[], int, int)
	 */
	public void write(byte[] b, int off, int len) throws IOException {
		while (len > 0) {
			int bytesToWriteThisLoop = Math.min(len, blockSize - byteCount);
			System.arraycopy(b, off, buffer, byteCount+4, bytesToWriteThisLoop);
			byteCount += bytesToWriteThisLoop;
			if (byteCount == blockSize) {
				writeBuffer();
			}
			len -= bytesToWriteThisLoop;
			off += bytesToWriteThisLoop;
		}
	}

	/**
	 * @see java.io.OutputStream#flush()
	 */
	public void flush() throws IOException {
		writeBuffer();
	}
	
	/**
	 * @see java.io.OutputStream#close()
	 */
	public void close() throws IOException {
        try {
            flush();
        } finally {
            outData.close();
        }
	}


	/**
	 * Returns true if there's a CrcOutputStream at the far end, and someone there is waiting
	 * on a read. This method can only be called when no-one is waiting on a local read, and will 
	 * hang if this condition is not met.
	 * @return true if there's a reader at the far end
	 * @throws IOException
	 */
	public synchronized boolean probe() throws IOException {
		// try again if the first go returns false
		if (probeOnce()) {
			return true;
		} else {
			boolean secondTryResult = probeOnce();
			inputStream.clear(); 
			return secondTryResult;
		}
	}

	private boolean probeOnce() throws IOException {
		inputStream.clear(); 
		outData.writeInt(0);
		outData.flush();
		return inputStream.getProbeResponse();
	}
	
	synchronized void writeAck(int i) throws IOException {
		Utils.writeBigEndInt(ackBuffer, 0, i);
		outData.write(ackBuffer);
		outData.flush();
	}

	private void writeBuffer() throws IOException {
		int crc;
		synchronized (this) {
			if (byteCount == 0)
				return;

			Utils.writeBigEndInt(buffer, 0, byteCount);
			crc = CRC.crc(buffer, 4, byteCount) & 0xFFFF;
			Utils.writeBigEndShort(buffer, byteCount+4, crc);
            try {
                outData.write(buffer, 0, byteCount + PROTOCOL_OVERHEAD);
                outData.flush();
            } finally {
                byteCount = 0;
            }
		}
        try {
            int ackValue = inputStream.getAck();
            if (crc != ackValue) {
                throw new IOException("Received incorrect CRC ack of 0x" + Integer.toHexString(ackValue) + " (expected 0x" + Integer.toHexString(crc) + ")");
            }
        } catch (TimeoutException ex) {
            throw new IOException("CrcOutputStream received timeout while waiting for Ack");
        }

	}
}
