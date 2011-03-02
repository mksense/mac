/*
 * Copyright 2007-2008 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.spot.flashmanagement;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Vector;

/**
 * FlashFileOutputStream represents an output stream over some flash memory. There are two uses for this.<br /> 
 * <br />
 * The first is to stream over the flash memory allocated to an instance of a {@link com.sun.spot.flashmanagement.FlashFile}.
 * In this case, you should construct the output stream accordingly, for example:<br />
 * <p><blockquote><pre>
 * FlashFile myFile = new FlashFile("my file");
 * myFile.createNewFile(requiredSize);
 * FlashFileOutputStream ffos = new FlashFileOutputStream(myFile);
 * </pre></blockquote></p>
 * In this case, the output stream will throw an IOException if you attempt to write beyond the end of the last
 * sector allocated to the FlashFile. Note that this will allow more than requiredSize bytes to be written to the file
 * when requiredSize is not equal to a whole number of flash memory sectors.<br />
 * <br />
 * The second use of FlashFileOutputStream is to stream over the flash memory in an arbitrary sector of the 
 * flash memory. In this case, construct the output stream like this:<br />
 * <p><blockquote><pre>
 * FlashFileOutputStream ffos = new FlashFileOutputStream(new NorFlashSector(mySectorNumber));<br />
 * </pre></blockquote></p>
 * In this case, the output stream will throw an IOException if you attempt to write beyond the end of the 
 * sector.
 */

public class FlashFileOutputStream extends OutputStream {
	
	/**
	 * The default size of the output buffer. This can be overridden when opening a stream.
	 */
	public static final int DEFAULT_BUFFER_SIZE = 512; // size must be even
	
	private byte[] buffer;
	private int numOfBytesWrittenInSector;
	private IAddressableNorFlashSector currentSector;
	private FlashFileDescriptor fileDescriptor;
	private int numOfBytesInFile;
	private int numOfBytesInBuffer;
	private FlashFile file = null;

	/**
	 * Construct an output stream over the space allocated to a previously created FlashFile 
	 * @param file The FlashFile to write data to
	 * @throws FlashFileNotFoundException 
	 */
	public FlashFileOutputStream(FlashFile file) throws FlashFileNotFoundException {
		this(file, DEFAULT_BUFFER_SIZE);
	}

	/**
	 * Construct an output stream over the space allocated to a previously created FlashFile,
	 * specifying a non-standard buffer size. The output stream will flush its contents to
	 * flash memory when either this buffer is full, or when {@link #flush()} or {@link #close()}
	 * are called. Thus varying the buffer size affects the performance characteristics of the
	 * output stream. The default value is {@value #DEFAULT_BUFFER_SIZE};
	 * @param file The FlashFile to write data to
	 * @param bufferSize The size - in bytes - for the FlashFile's output buffer.
	 * @throws FlashFileNotFoundException 
	 */
	public FlashFileOutputStream(FlashFile file, int bufferSize) throws FlashFileNotFoundException {
		this(file.getFileDescriptor(), bufferSize);
		this.file = file;
	}
	
	/**
	 * Construct an output stream over a sector of the flash memory.
	 * @param sector the sector to write data to.
	 * @throws FlashFileNotFoundException 
	 */
	public FlashFileOutputStream(final IAddressableNorFlashSector sector) throws FlashFileNotFoundException {
		this(new FlashFileDescriptor(
			"temp",
			0,
			new Vector(1) {{
				addElement(sector);
			}},
			0, // virtual address is ignored for this usage
			0,
			"",
			(short)0),
			DEFAULT_BUFFER_SIZE);
	}

	private FlashFileOutputStream(FlashFileDescriptor fileDescriptor, int bufferSize) throws FlashFileNotFoundException {
		buffer = new byte[bufferSize];
		this.fileDescriptor = fileDescriptor;
		numOfBytesInFile = numOfBytesWrittenInSector = numOfBytesInBuffer = 0;
		currentSector = fileDescriptor.getFirstSector();
		fileDescriptor.setFileSize(0);
		IAddressableNorFlashSector[] sectors = fileDescriptor.getSectors();
		for (int i = 0; i < sectors.length; i++) {
			sectors[i].erase();
		}
	}

	/**
	 * @see java.io.OutputStream#write(int)
	 */
	public void write(int b) throws IOException {
		if (numOfBytesInFile == fileDescriptor.getAllocatedSpace()) {
			throw new IOException("File " + fileDescriptor.getName() + " is full");
		}
		buffer[numOfBytesInBuffer++] = (byte)b;
		if (numOfBytesInBuffer == buffer.length) {
			// buffer is full
			writeOutBuffer();
		}
		numOfBytesInFile++;
	}

	/**
	 * @see java.io.OutputStream#flush()
	 */
	public void flush() throws IOException {
		int numOfBytesInBufferOnEntry = numOfBytesInBuffer;
		writeOutBuffer();
		// numOfBytesInBuffer is now 0
		if (numOfBytesInBufferOnEntry % 2 != 0) {
			buffer[numOfBytesInBuffer++] = buffer[numOfBytesInBufferOnEntry-1];
			numOfBytesInBuffer = 1;
			numOfBytesWrittenInSector--;
		}
		fileDescriptor.setFileSize(numOfBytesInFile);
		if (file != null) {
			file.commit();
		}
	}
	
	/**
	 * @see java.io.OutputStream#close()
	 */
	public void close() throws IOException {
		flush();
		super.close();
	}

	private void writeOutBuffer() {
		int unusedBytesInSector = currentSector.getSize()-numOfBytesWrittenInSector;
		if (numOfBytesInBuffer > unusedBytesInSector) {
			currentSector.setBytes(numOfBytesWrittenInSector, buffer, 0, unusedBytesInSector);
			currentSector = fileDescriptor.getNextSector(currentSector);
			currentSector.setBytes(0, buffer, unusedBytesInSector, numOfBytesInBuffer-unusedBytesInSector);
			numOfBytesWrittenInSector = numOfBytesInBuffer-unusedBytesInSector;
		} else {
			currentSector.setBytes(numOfBytesWrittenInSector, buffer, 0, numOfBytesInBuffer);
			numOfBytesWrittenInSector+= numOfBytesInBuffer;
		}
		numOfBytesInBuffer = 0;
	}
}

