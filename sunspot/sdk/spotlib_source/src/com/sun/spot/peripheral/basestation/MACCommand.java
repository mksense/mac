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

package com.sun.spot.peripheral.basestation;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.sun.spot.peripheral.radio.I802_15_4_MAC;

public abstract class MACCommand implements ICommand {
	private static final int SUCCESS = 1;
	private static final int FAILURE = 0;

	public static final byte GetNullPacketAfterAckWaitCommand = 0;
	public static final byte GetChannelAccessFailureCommand   = 1;
    public static final byte GetNoAckCommand                  = 2;
	public static final byte GetRadioPropertyCommand          = 3;
	public static final byte GetWrongAckCommand               = 4;
	public static final byte GetRxErrorCommand                = 5;
	public static final byte ResetErrorCountersCommand        = 6;
	public static final byte MCPSDataIndicationCommand        = 7;
	public static final byte MCPSDataRequestCommand           = 8;
	public static final byte MLMEGetCommand                   = 9;
	public static final byte MLMEResetCommand                 = 10;
	public static final byte MLMERxEnableCommand              = 11;
	public static final byte MLMESetCommand                   = 12;
	public static final byte MLMEStartCommand                 = 13;
	public static final byte SetPLMETransmitPowerCommand      = 14;
	public static final byte SetPLMEChannelCommand            = 15;
	
	public static final byte ResetProxyCommand                = 101;
	public static final byte ExitCommand                      = 102;
	

	private int uid;	

	public final void writeOnto(DataOutputStream dataOutputStream) throws IOException {
		dataOutputStream.writeByte(classIndicatorByte());
		writeParametersOnto(dataOutputStream);
	}

	protected abstract byte classIndicatorByte();

	protected void writeParametersOnto(DataOutputStream dataOutputStream) throws IOException {
		//default for no-arg commands
	}

	public void populateFrom(byte[] inputBuffer, int startingOffset) throws IOException {
		//default for no-arg commands
	}

	public int writeResultOnto(byte[] outputBuffer, int startingOffset, I802_15_4_MAC mac) throws IOException {
		try {
			prepareResultOrExecute(mac);
			outputBuffer[startingOffset] = SUCCESS;
			return writePreparedResult(outputBuffer, startingOffset+1) + 1; // 1 for SUCCESS byte
		} catch (Throwable ex) {
			ex.printStackTrace();
			outputBuffer[startingOffset] = FAILURE;
			int exceptionLength = writeExceptionDetails(outputBuffer, startingOffset, ex);
			return exceptionLength + 1; // 1 for FAILURE byte
		}
	}

	private int writeExceptionDetails(byte[] outputBuffer, int startingOffset, Throwable ex) throws IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
		
		dataOutputStream.writeUTF(ex.getClass().getName());
		dataOutputStream.writeUTF(ex.getMessage() == null ? "" : ex.getMessage());
		
		byte[] exceptionMsgContent = byteArrayOutputStream.toByteArray();
		System.arraycopy(exceptionMsgContent, 0, outputBuffer, startingOffset+1, exceptionMsgContent.length);
		return exceptionMsgContent.length;
	}

	protected abstract void prepareResultOrExecute(I802_15_4_MAC mac) throws Throwable;
	
	protected int writePreparedResult(byte[] outputBuffer, int startingOffset) throws IOException {
		//default for void commands
		return 0;
	}

	public final Object resultFrom(DataInputStream dataInputStream, ICreateExceptions exceptionCreator) throws Throwable {
		boolean isOk = dataInputStream.readBoolean();
		if (isOk) {
			return readResultFrom(dataInputStream);
		} else {
			String exceptionClassName = dataInputStream.readUTF();
			String exceptionMessage = dataInputStream.readUTF();
			return exceptionCreator.throwException(exceptionClassName, exceptionMessage);
		}
	}

	protected Object readResultFrom(DataInputStream dataInputStream) throws IOException {
		//default for void commands
		return null;
	}
	
	public int getUID() {
		return this.uid;
	}
	
	public long getTimeout() {
		// by default, wait for ever
		return 0;
	}

	public void setUID(int uid) {
		this.uid = uid;
	}
}
