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
import com.sun.spot.peripheral.radio.RadioPacket;
import com.sun.spot.util.Queue;


public class MCPSDataIndicationCommand extends MACCommand {

	protected static final int INCOMING_QUEUE_HIGH_WATER = 1000;
	protected static final int INCOMING_QUEUE_LOW_WATER  = 900;
	private Queue incomingPackets;
	private RadioPacket radioPacket;
	protected boolean stopped = false;

	public MCPSDataIndicationCommand with(final I802_15_4_MAC mac) {
		incomingPackets = new Queue();
		new Thread() {
			public void run() {
				while (true) {
					RadioPacket radioPacket = RadioPacket.getDataPacket();
					mac.mcpsDataIndication(radioPacket);
					incomingPackets.put(radioPacket);
					int size = incomingPackets.size();
					if (size > INCOMING_QUEUE_HIGH_WATER) {
						System.err.println("[reached HIGH_WATER]");
						mac.mlmeSet(I802_15_4_MAC.MAC_RX_ON_WHEN_IDLE, I802_15_4_MAC.FALSE);
						mac.mlmeRxEnable(0);
						stopped = true;
					}
				}
			};
		}.start();
		return this;
	}

	protected void prepareResultOrExecute(I802_15_4_MAC mac) throws InterruptedException {
		radioPacket = (RadioPacket) incomingPackets.get();
		int size = incomingPackets.size();
		if (stopped && size < INCOMING_QUEUE_LOW_WATER) {
			System.err.println("[reached LOW_WATER]");
			mac.mlmeSet(I802_15_4_MAC.MAC_RX_ON_WHEN_IDLE, I802_15_4_MAC.TRUE);
			stopped = false;
		}			
	}
	
	protected int writePreparedResult(byte[] outputBuffer, int startingOffset) throws IOException {
		if (radioPacket == null) {
			System.err.println("[MCPSDataIndicationCommand] null packet");
			return 0; // should be resetting if packet is null
		} else {
			return radioPacket.writeWithoutTimestampOnto(outputBuffer, startingOffset);
		}
	}
	
	protected Object readResultFrom(DataInputStream dataInputStream) throws IOException {
		RadioPacket packet = RadioPacket.getDataPacket();
		packet.readWithoutTimestampFrom(dataInputStream);
		return packet;
	}
	
	protected byte classIndicatorByte() {
		return MCPSDataIndicationCommand;
	}

	public void reset() {
		Queue oldQueue = incomingPackets;
		incomingPackets = new Queue();
		oldQueue.stop(); // free up any waiting MACProxyWorkerThreads
	}
}
