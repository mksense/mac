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

import java.io.DataInputStream;
import java.io.IOException;

import com.sun.spot.peripheral.radio.I802_15_4_MAC;
import com.sun.spot.peripheral.radio.IProprietaryMAC;
import com.sun.spot.util.Utils;

public abstract class NoArgIntParameterCommand extends MACCommand {
	private int preparedResult;
	
	protected final void prepareResultOrExecute(I802_15_4_MAC mac) {
		preparedResult = getIntResult((IProprietaryMAC)mac);
	}

	protected abstract int getIntResult(IProprietaryMAC mac);

	protected final int writePreparedResult(byte[] outputBuffer, int startingOffset) throws IOException {
		Utils.writeBigEndInt(outputBuffer, startingOffset, preparedResult);
		return Utils.SIZE_OF_INT;
	}
	
	protected final Object readResultFrom(DataInputStream dataInputStream) throws IOException {
		return new Integer(dataInputStream.readInt());
	}
}
