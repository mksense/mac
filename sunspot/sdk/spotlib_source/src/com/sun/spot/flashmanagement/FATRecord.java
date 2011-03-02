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

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

import com.sun.spot.util.Utils;
import com.sun.squawk.peripheral.INorFlashSector;

/**
 * FATRecord
 *
 */
abstract class FATRecord {
	static final int NOT_WRITTEN_YET_OFFSET = -1;
	static final byte FILE_FAT_RECORD_TYPE = 0;
	static final short UNUSED_FAT_RECORD_STATUS = (short) 0xFFFF;
	static final short DELETED_FAT_RECORD_STATUS = 0x0000;
	static final short FAT_RECORD_HEADER_SIZE = 4;
	static final short CURRENT_FAT_RECORD_STATUS = 0x00FF;
	static final byte[] DELETED_FAT_RECORD_STATUS_AS_BYTE_ARRAY = new byte[] {0, 0};

	private int offsetInFAT;
	protected boolean needsWriting;

	public FATRecord(int offsetInFAT) {
		this.offsetInFAT = offsetInFAT;
		needsWriting = offsetInFAT == NOT_WRITTEN_YET_OFFSET;
	}

	/**
	 * @return Returns the offsetInFAT.
	 */
	public int getOffsetInFAT() {
		return offsetInFAT;
	}

	/**
	 * @param offsetInFAT The offsetInFAT to set.
	 */
	public void setOffsetInFAT(int offsetInFAT) {
		this.offsetInFAT = offsetInFAT;
	}

	public byte[] asFATRecord() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		dos.writeShort(FATRecord.CURRENT_FAT_RECORD_STATUS);
		dos.writeShort(0); // placeholder for length
		dos.write(getRecordType());

		writeRecord(dos);
		
		boolean recordHasOddLength = baos.size() % 2 != 0;
		if (recordHasOddLength) {
			dos.write(0xFF); // padding as record is an odd number of bytes long
		}
		byte[] result = baos.toByteArray();
		Utils.writeBigEndShort(result, 2, recordHasOddLength ? result.length-1 : result.length);
		return result;
	}

	public boolean needsWriting() {
		return needsWriting;
	}

	public boolean needsDeleting() {
		return needsWriting && offsetInFAT != NOT_WRITTEN_YET_OFFSET;
	}

	public void setClean() {
		needsWriting = false;
	}

	abstract void writeRecord(DataOutputStream dos) throws IOException;

	abstract byte getRecordType();
	
	Vector readSectorList(INorFlashSectorFactory factory, DataInputStream dis) throws IOException {
		Vector sectors = new Vector();
		int freeSectorCount = dis.readShort();
		for (int i = 0; i < freeSectorCount; i++) {
			sectors.addElement(factory.create(dis.readShort(), INorFlashSector.SYSTEM_PURPOSED));
		}
		return sectors;
	}
}
