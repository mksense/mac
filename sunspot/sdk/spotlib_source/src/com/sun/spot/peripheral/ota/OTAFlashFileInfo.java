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

package com.sun.spot.peripheral.ota;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;

import com.sun.spot.flashmanagement.FlashFile;
import com.sun.spot.flashmanagement.IFlashFileInfo;

/**
 * OTAFlashFileInfo records information about a FlashFile instance so that it can be
 * serialised for transmission to a host process.
 *
 */
public class OTAFlashFileInfo implements IFlashFileInfo {
	private String comment;
	private String name;
	private int virtualAddress;
	private boolean isObsolete;
	private long lastModified;
	private int length;

	/**
	 * Reconstruct an {@link OTAFlashFileInfo} from its serialised representation (see {@link #toByteArray()}
	 * @param rawInfo
	 * @throws IOException
	 */
	public OTAFlashFileInfo(byte[] rawInfo) throws IOException {
		ByteArrayInputStream bais = new ByteArrayInputStream(rawInfo);
		DataInputStream dis = new DataInputStream(bais);
		name = dis.readUTF();
		comment = dis.readUTF();
		isObsolete = dis.readBoolean();
		lastModified = dis.readLong();
		length = dis.readInt();
		virtualAddress = dis.readInt();
		dis.close();
	}

	/**
	 * Construct a new {@link OTAFlashFileInfo} from a {@link FlashFile}
	 * @param file the {@link FlashFile} to copy information from
	 * @throws IOException
	 */
	public OTAFlashFileInfo(FlashFile file) throws IOException {
		name = file.getName();
		comment = file.getComment();
		virtualAddress = file.getVirtualAddress();
		isObsolete = file.isObsolete();
		lastModified = file.lastModified();
		length = file.length();
	}

	/**
	 * This constructor is intended for test use only.
	 * 
	 * @param comment
	 * @param name
	 * @param virtualAddress
	 * @param isObsolete
	 * @param lastModified
	 * @param length
	 * @param exists
	 */
	public OTAFlashFileInfo(String comment, String name, int virtualAddress, boolean isObsolete, long lastModified, int length, boolean exists) {
		super();
		this.comment = comment;
		this.name = name;
		this.virtualAddress = virtualAddress;
		this.isObsolete = isObsolete;
		this.lastModified = lastModified;
		this.length = length;
	}

	/**
	 * @see com.sun.spot.flashmanagement.IFlashFileInfo#getComment()
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * @see com.sun.spot.flashmanagement.IFlashFileInfo#isObsolete()
	 */
	public boolean isObsolete() {
		return isObsolete;
	}

	/**
	 * @see com.sun.spot.flashmanagement.IFlashFileInfo#lastModified()
	 */
	public long lastModified() {
		return lastModified;
	}

	/**
	 * @see com.sun.spot.flashmanagement.IFlashFileInfo#length()
	 */
	public int length() {
		return length;
	}

	/**
	 * @see com.sun.spot.flashmanagement.IFlashFileInfo#getName()
	 */
	public String getName() {
		return name;
	}

	/**
	 * @see com.sun.spot.flashmanagement.IFlashFileInfo#getVirtualAddress()
	 */
	public int getVirtualAddress() {
		return virtualAddress;
	}
	
	/**
	 * @return a serialised version of the info for transmission to a host process
	 * @throws IOException
	 */
	public byte[] toByteArray() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		dos.writeUTF(name);
		dos.writeUTF(comment);
		dos.writeBoolean(isObsolete);
		dos.writeLong(lastModified);
		dos.writeInt(length);
		dos.writeInt(virtualAddress);
		dos.flush();
		dos.close();
		return baos.toByteArray();
	}
	
	public String toString() {
		String lineSeparator = System.getProperty("line.separator");
		StringBuffer sb = new StringBuffer();
		sb.append(getName());
		if (isObsolete()) {
			sb.append(" [obsolete]");
		}
		sb.append(getVirtualAddress() == 0 ? " [unmapped]" : " [mapped at 0x" + Integer.toHexString(getVirtualAddress()) + "]");
		sb.append(lineSeparator);
		sb.append("    comment: " + getComment());
		sb.append(lineSeparator);
		sb.append("    last modified: " + new Date(lastModified()));
		sb.append(lineSeparator);
		sb.append("    size: " + length());
		sb.append(lineSeparator);
		return sb.toString();
	}
}

