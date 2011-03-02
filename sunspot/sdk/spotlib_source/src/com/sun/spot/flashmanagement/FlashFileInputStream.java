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
import java.io.InputStream;
import java.util.Vector;

/**
 * FlashFileInputStream represents an input stream over some flash memory. There are two uses for this.<br /> 
 * <br />
 * The first is to stream over the flash memory allocated to an instance of a {@link com.sun.spot.flashmanagement.FlashFile}.
 * In this case, you should construct the input stream accordingly, for example:<br />
 * <p><blockquote><pre>
 * FlashFileInputStream ffis = new FlashFileInputStream(new FlashFile("my file"));<br />
 * </pre></blockquote></p>
 * In this case, the input stream will signal end of file when the reader reaches the end of the 
 * data that has been written to the FlashFile.<br />
 * <br />
 * The second use of FlashFileInputStream is to stream over the flash memory in an arbitrary sector of the 
 * flash memory. In this case, construct the input stream like this:<br />
 * <p><blockquote><pre>
 * FlashFileInputStream ffis = new FlashFileInputStream(new NorFlashSector(mySectorNumber));<br />
 * </pre></blockquote></p>
 * In this case, the input stream will signal end of file when the reader reaches the end of the
 * physical sector.
 */
public class FlashFileInputStream extends InputStream {

	private byte[] singleByteBuffer = new byte[1];
	private int offsetInSector;
	private IAddressableNorFlashSector currentSector;
	private FlashFileDescriptor fileDescriptor;
	private int offsetInFile;

	/**
	 * Construct an input stream over the data previously written to a FlashFile 
	 * @param file The FlashFile to read data from
	 * @throws IOException If the file does not exist
	 */
	public FlashFileInputStream(FlashFile file) throws IOException {
		this(file.getFileDescriptor());
	}
		
	/**
	 * Construct a input stream over the raw data in a sector of the flash memory
	 * @param sector The sector to read data from
	 */
	public FlashFileInputStream(final IAddressableNorFlashSector sector) {
		this(new FlashFileDescriptor(
				"temp",
				sector.getSize(),
				new Vector(1) {{
					addElement(sector);
				}},
				0, // virtual address is ignored for this usage
				0,
				"",
				(short)0));

	}
	
	FlashFileInputStream(FlashFileDescriptor flashFileDescriptor) {
		fileDescriptor = flashFileDescriptor;
		offsetInFile = offsetInSector = 0;
		currentSector = fileDescriptor.getFirstSector();
	}

	/**
	 * @see java.io.InputStream#read(byte[], int, int)
	 */
	public int read(byte[] b, int off, int len) throws IOException {
		int numberOfBytesToReturn = Math.min(len, fileDescriptor.length()-offsetInFile);
		if (numberOfBytesToReturn == 0) {
			//EOF
			return -1;
		}
		int numberOfBytesToRead = numberOfBytesToReturn;
		while (numberOfBytesToRead > 0) {
			if (offsetInSector == currentSector.getSize()) {
				currentSector = fileDescriptor.getNextSector(currentSector);
				offsetInSector = 0;
			}
			int lenToReadFromThisSector = Math.min(numberOfBytesToRead, currentSector.getSize()-offsetInSector);
			currentSector.getBytes(offsetInSector, b, off, lenToReadFromThisSector);
			off += lenToReadFromThisSector;
			numberOfBytesToRead -= lenToReadFromThisSector;
			offsetInSector += lenToReadFromThisSector;
			offsetInFile += lenToReadFromThisSector;
		}
		return numberOfBytesToReturn;
	}

	/**
	 * @see java.io.InputStream#read()
	 */
	public int read() throws IOException {
		if (offsetInFile == fileDescriptor.length()) {
			// EOF
			return -1;
		}
		if (offsetInSector == currentSector.getSize()) {
			currentSector = fileDescriptor.getNextSector(currentSector);
			offsetInSector = 0;
		}
		currentSector.getBytes(offsetInSector++, singleByteBuffer, 0, 1);
		offsetInFile++;
		return singleByteBuffer[0] & 0xFF;
	}
}

