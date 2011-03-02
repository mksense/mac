/*
 * Copyright 2006-2009 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.spot.io.j2me.radiostream;

import java.io.IOException;
import java.io.OutputStream;

import com.sun.spot.peripheral.ChannelBusyException;
import com.sun.spot.peripheral.IRadioControl;
import com.sun.spot.peripheral.NoAckException;
import com.sun.spot.peripheral.NoRouteException;
import com.sun.spot.peripheral.radio.IRadioPolicyManager;
import com.sun.spot.peripheral.radio.RadioPolicy;
import com.sun.spot.peripheral.radio.IRadiostreamProtocolManager;
import com.sun.spot.peripheral.radio.ConnectionID;
import com.sun.spot.peripheral.radio.NoMeshLayerAckException;
import com.sun.spot.peripheral.radio.IncomingData;
import com.sun.spot.peripheral.radio.RadioPacket;
import com.sun.spot.peripheral.radio.ILowPan;
import com.sun.spot.peripheral.radio.LowPanHeader;


/**
 * Helper class for "radiostream:" connections. This class provides an OutputStream
 * to stream data to another Spot. You should NOT normally instantiate this
 * class directly, but rather via the GCF framework: see the first reference below
 * for more details.
 * 
 * @see RadiostreamConnection
 */
public class RadioOutputStream extends OutputStream implements IRadioControl {
	
	// Our aim is to avoid fragmentation and its inefficiencies, so set the buffer size
	// to the max that avoids fragmentation.
	private static final int BUFFER_SIZE = RadioPacket.MIN_PAYLOAD_LENGTH - ILowPan.MAC_PAYLOAD_OFFSET - LowPanHeader.MAX_UNFRAG_HEADER_LENGTH;
	
	private byte[] payload;
	private int payloadIndex;
	private IRadiostreamProtocolManager protMgr;
	private int flushThreshold;
	private ConnectionID connectionID;

	private IRadioPolicyManager radioPolicyManager;

	private boolean closed;

	/**
	 * Construct a RadioOutputStream
	 * @param dispatcher the PortBasedProtocolManager that will dispatch packets
	 * @param cid the ConnectionID object to be used
	 * @param radioPolicyManager 
	 * @param initialPolicy 
	 */
	public RadioOutputStream(IRadiostreamProtocolManager dispatcher, ConnectionID cid, RadioPolicy initialPolicy, IRadioPolicyManager radioPolicyManager) {
		connectionID = cid;
		this.protMgr = dispatcher;
		this.radioPolicyManager = radioPolicyManager;
		payload = new byte[BUFFER_SIZE];
		payloadIndex = IRadiostreamProtocolManager.DATA_OFFSET;
		flushThreshold = BUFFER_SIZE;
		radioPolicyManager.registerConnection(connectionID);
		radioPolicyManager.policyHasChanged(connectionID, initialPolicy);
		closed = false;
	}

	/* (non-Javadoc)
	 * @see java.io.OutputStream#write(int)
	 */
	public synchronized void write(int arg0) throws NoAckException, ChannelBusyException, NoRouteException, NoMeshLayerAckException {
		payload[payloadIndex++] = (byte)arg0;
		if (payloadIndex == flushThreshold) {
			sendPayload();
		}
	}
	
	public synchronized void flush() throws NoAckException, ChannelBusyException, NoRouteException, NoMeshLayerAckException {
		if (payloadIndex > IRadiostreamProtocolManager.DATA_OFFSET) {
			sendPayload();
		}
		protMgr.waitForAllAcks(connectionID);
	}

	private void sendPayload() throws NoMeshLayerAckException, NoAckException, ChannelBusyException, NoRouteException {
		int len = payloadIndex;
        payloadIndex = IRadiostreamProtocolManager.DATA_OFFSET; // reset the packet
        protMgr.send(connectionID, connectionID.getMacAddress(), payload, len);
	}

	/* (non-Javadoc)
	 * @see java.io.OutputStream#close()
	 */
	public synchronized void close() throws IOException {
		if (!closed) {
			try {
				flush();
			} finally {
				payload = null;
				protMgr.closeConnection(connectionID);
				radioPolicyManager.deregisterConnection(connectionID);
				closed = true;
			}
		}
		super.close();
	}

	/**
	 * Get the flush threshold for this stream. Data written to the stream is only sent to the remote Spot
	 * when the flush threshold is reached, or when the current radio packet is full. 
	 * @return number of bytes written to receiver before a radio packet is sent to the remote spot.
	 */
	public int getFlushThreshold() {
		return flushThreshold - IRadiostreamProtocolManager.DATA_OFFSET;
	}

	/**
	 * Set the flush threshold for this stream. Data written to the stream is only sent to the remote Spot
	 * when the flush threshold is reached. This method should only be called before writing any data to the stream.
	 * @param ft - number of bytes to write to receiver before a radio packet is sent to the remote spot - should
	 * be in the range 1 to ({@link com.sun.spot.peripheral.radio.RadioPacket#getMaxMacPayloadSize()}
	 * - {@link com.sun.spot.peripheral.radio.IRadiostreamProtocolManager#DATA_OFFSET})
	 */
	public void setFlushThreshold(int ft) {
		int maxFlushThreshold = BUFFER_SIZE - IRadiostreamProtocolManager.DATA_OFFSET;
		if (ft > 0 && ft <= maxFlushThreshold) {
			flushThreshold = ft+IRadiostreamProtocolManager.DATA_OFFSET;
		} else {
			throw new IllegalArgumentException("Flush threshold of " + ft + " out of range; should be > 0 and <= "+maxFlushThreshold);
		}
	}

	public void setRadioPolicy(RadioPolicy selection) {
		if (!closed) {
			radioPolicyManager.policyHasChanged(connectionID, selection);
		}
	}

	public byte getLocalPort() {
		return connectionID.getPortNo();
	}
}
