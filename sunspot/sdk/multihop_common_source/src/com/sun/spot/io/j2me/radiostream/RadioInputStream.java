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

package com.sun.spot.io.j2me.radiostream;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;

import com.sun.spot.peripheral.IRadioControl;
import com.sun.spot.peripheral.TimeoutException;
import com.sun.spot.peripheral.radio.IRadioPolicyManager;
import com.sun.spot.peripheral.radio.RadioPolicy;
import com.sun.spot.peripheral.radio.LowPanHeaderInfo;
import com.sun.spot.peripheral.radio.IRadiostreamProtocolManager;
import com.sun.spot.peripheral.radio.IncomingData;
import com.sun.spot.peripheral.radio.ConnectionID;

/**
 * Helper class for "radiostream:" connections. This class provides an InputStream
 * over the data received from another Spot. It also provides a small set of accessors
 * to get information about the quality of the link. You should NOT normally instantiate this
 * class directly, but rather via the GCF framework: see the first reference below
 * for more details.
 * 
 * @see RadiostreamConnection
 */
public class RadioInputStream extends InputStream implements IRadioControl {
	private IRadioPolicyManager radioPolicyManager;
	private static final int BUFFER_SIZE = 256;
	
	private byte[] payload;
	private int payloadIndex;
	private int endOfDataIndex;
	private LowPanHeaderInfo headerInfo;
	private IRadiostreamProtocolManager protMgr;
	private ConnectionID connectionID;
	private long timeout;
	private boolean closed;
	
	/**
     * Construct a RadioInputStream
     * @param initialPolicy radio policy assoicated with this connection
     * @param radioPolicyManager the RadioPolicyManager associated with this inputstream
     * @param dispatcher the PortBasedProtocolManager that will dispatch radio packets
     * @param cid the ConnectionID object being used
     * @param timeout the timeout to use when waiting for input
     */
	public RadioInputStream(IRadiostreamProtocolManager dispatcher, ConnectionID cid, long timeout, RadioPolicy initialPolicy, IRadioPolicyManager radioPolicyManager) {
		connectionID = cid;
		this.timeout = timeout;
		this.protMgr = dispatcher;
		this.radioPolicyManager = radioPolicyManager;
		payload = new byte[BUFFER_SIZE];
		payloadIndex = IRadiostreamProtocolManager.DATA_OFFSET;
		endOfDataIndex = payloadIndex;
		radioPolicyManager.registerConnection(connectionID);
		radioPolicyManager.policyHasChanged(connectionID, initialPolicy);
		closed = false;
	}

	/* 
         * This method behaves like java.io.InputStream#read(), other than returning -1 if at end of stream.
         * radiostream does not support the underlying semantics to determine if the other end of the stream
         * has terminated gracefully, or otherwise. 
         *
         * A higher level semantic should be implemented on top of radiostream to signal end of stream.
         * 
	 * @see java.io.InputStream#read()
	 */
	public int read() throws IOException {
		while (payloadIndex >= endOfDataIndex) {
			getPacket();
		}
		return payload[payloadIndex++] & 0xFF;
	}

	/**
     * Return the number of bytes available to be read from the stream without blocking.
     * Returns the number of bytes available in the current underlying radio packet -
     * more packets might be coming but we just don't know.
     * @return the number of bytes available right now
     * @throws java.io.IOException 
     */
	public int available() throws IOException {
		while (payloadIndex >= endOfDataIndex) {
			//nothing in current packet
			if (protMgr.packetsAvailable(connectionID)) {
				getPacket();
			} else {
				return 0;
			}
		}
		return endOfDataIndex - payloadIndex;
	}

	/**
	 * Link Quality Indication (LQI) is a characterization of the quality of a
	 * received packet. Its value is computed from the CORR, correlation value. The
	 * LQI ranges from 0 (bad) to 255 (good).
	 *  
	 * @return linkQuality - range 0 to 0xFF
	 * @see com.sun.spot.peripheral.radio.RadioPacket#getLinkQuality()
	 */
	public int getLinkQuality() {
		return headerInfo.linkQuality;
	}

	/**
	 * CORR measures the average correlation value of the first 4 bytes of the packet
	 * header. A correlation value of ~110 indicates a maximum quality packet while a
	 * value of ~50 is typically the lowest quality packet detectable by the SPOT's
	 * receiver.
	 * 
	 * @return - correlation value
	 * @see com.sun.spot.peripheral.radio.RadioPacket#getCorr()
	 */
	public int getCorr() {
		return headerInfo.corr;
	}

	/**
	 * RSSI (received signal strength indicator) measures the strength (power) of the
	 * signal for the packet. It ranges from +60 (strong) to -60 (weak). To convert it
	 * to decibels relative to 1 mW (= 0 dBm) subtract 45 from it, e.g. for an RSSI of
	 * -20 the RF input power is approximately -65 dBm.
	 * 
	 * @return - RSSI value
	 * @see com.sun.spot.peripheral.radio.RadioPacket#getRssi()
	 */
	public int getRssi() {
		return headerInfo.rssi;
	}

	private void getPacket() throws IOException {
		IncomingData receivedData;
		if (timeout >= 0) {
			receivedData = protMgr.receivePacket(connectionID, timeout);
			if (receivedData == null) {
				throw new TimeoutException("Radio receive timeout");
			}
		} else {
			receivedData = protMgr.receivePacket(connectionID);
			if (receivedData == null) {
				throw new InterruptedIOException("Connection was closed");
			}
		}
		System.arraycopy(receivedData.payload, 0, payload, 0, receivedData.payload.length);
		endOfDataIndex = receivedData.payload.length;
		headerInfo = receivedData.headerInfo;
		payloadIndex = IRadiostreamProtocolManager.DATA_OFFSET;
	}

	public void close() throws IOException {
		if (!closed) {
			protMgr.closeConnection(connectionID);
			radioPolicyManager.deregisterConnection(connectionID);
			closed = true;
		}
		super.close();
	}

	public void setRadioPolicy(RadioPolicy selection) {
		if (!closed) {
			radioPolicyManager.policyHasChanged(connectionID, selection);
		}
	}

	public byte getLocalPort() {
		return connectionID.getPortNo();
	}
        
    /**
     * change the timeout associated with this input stream
     * @param time time, in milliseconds, to wait for data on this input stream
     */
        public void setTimeout(long time) {
            timeout = time;           
        }
    /**
     * returns the time, in milliseconds, this connection will wait for data before throwing a timeout exception
     * @return input timeout, in milliseconds
     */
        public long getTimeout() {
            return timeout;
        }
}
