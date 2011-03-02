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

import com.sun.squawk.Address;
import com.sun.squawk.Unsafe;
import com.sun.squawk.VM;
import com.sun.squawk.vm.ChannelConstants;

class SpiMaster implements ISpiMaster {
	private static final Address BASE_ADDRESS = Address.fromPrimitive(0xFFFE0000);

	private static final int SPI_MR				= 4>>2;
	private static final int LOOPBACK_ENABLE	= 1<<7;

	public int sendReceive8 (SpiPcs pcs, int data) {
		return VM.execSyncIO(ChannelConstants.SPI_SEND_RECEIVE_8,
				pcs.getPcsIndex(), pcs.getConfiguration(), data, 0, 0, 0, null, null);
	}

	public int sendReceive8PlusSend16 (SpiPcs pcs, int first, int subsequent) {
		return VM.execSyncIO(ChannelConstants.SPI_SEND_RECEIVE_8_PLUS_SEND_16,
				pcs.getPcsIndex(), pcs.getConfiguration(), first, subsequent, 0, 0, null, null);
	}

	public int sendReceive8PlusSendN (SpiPcs pcs, int first, int size, byte[] subsequent) {
		return VM.execSyncIO(ChannelConstants.SPI_SEND_RECEIVE_8_PLUS_SEND_N,
				pcs.getPcsIndex(), pcs.getConfiguration(), first, size, 0, 0, subsequent, null);
	}

	public void sendAndReceive (SpiPcs pcs, int txSize, byte[] tx, int rxOffset, int rxSize, byte[] rx) {
		VM.execSyncIO(ChannelConstants.SPI_SEND_AND_RECEIVE,
				pcs.getPcsIndex(), pcs.getConfiguration(), 0, txSize, rxSize, rxOffset, tx, rx);
	}

	public void sendAndReceive(SpiPcs pcs, int deviceAddress, int txSize, byte[] tx, int rxOffset, int rxSize, byte[] rx) {
		VM.execSyncIO(ChannelConstants.SPI_SEND_AND_RECEIVE_WITH_DEVICE_SELECT,
				pcs.getPcsIndex(), pcs.getConfiguration(), deviceAddress, txSize, rxSize, rxOffset, tx, rx);
	}
	
	public int sendReceive8PlusReceive16 (SpiPcs pcs, int first) {
		return VM.execSyncIO(ChannelConstants.SPI_SEND_RECEIVE_8_PLUS_RECEIVE_16,
				pcs.getPcsIndex(), pcs.getConfiguration(), first, 0, 0, 0, null, null);
	}

	public int sendReceive8PlusVariableReceiveN (SpiPcs pcs, int first, byte[] subsequent, PIOPin fifo_pin) {
		return VM.execSyncIO(ChannelConstants.SPI_SEND_RECEIVE_8_PLUS_VARIABLE_RECEIVE_N,
				pcs.getPcsIndex(), pcs.getConfiguration(), first, fifo_pin.pin, fifo_pin.pio.getBaseAddress(), 0, null, subsequent);
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.spot.IDriver#name()
	 */
	public String name() {
		return "SpiMaster";
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.spot.ISpiMaster#setLoopback(boolean)
	 */
	public void setLoopback(boolean loopback) {
		int mode = Unsafe.getInt(BASE_ADDRESS, SPI_MR);
		if (loopback) {
			Unsafe.setInt(BASE_ADDRESS, SPI_MR, mode | LOOPBACK_ENABLE);
		} else {
			Unsafe.setInt(BASE_ADDRESS, SPI_MR, mode & ~LOOPBACK_ENABLE);			
		}
	}

	public int getMaxTransferSize() {
		return VM.execSyncIO(ChannelConstants.SPI_GET_MAX_TRANSFER_SIZE, 0);
	}
}
