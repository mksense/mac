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

package com.sun.spot.io.j2me.radiogram;


import java.io.IOException;

import javax.microedition.io.Connection;
import javax.microedition.io.Datagram;

import com.sun.spot.io.j2me.radiostream.RadiostreamConnection;
import com.sun.spot.peripheral.ChannelBusyException;
import com.sun.spot.peripheral.NoAckException;
import com.sun.spot.peripheral.NoRouteException;
import com.sun.spot.peripheral.RadioConnectionBase;
import com.sun.spot.peripheral.SpotFatalException;
import com.sun.spot.peripheral.radio.ConnectionID;
import com.sun.spot.peripheral.radio.IRadioPolicyManager;
import com.sun.spot.peripheral.radio.IRadiogramProtocolManager;
import com.sun.spot.peripheral.radio.IncomingData;
import com.sun.spot.peripheral.radio.NoMeshLayerAckException;
import com.sun.spot.peripheral.radio.RadioPolicy;
import com.sun.spot.peripheral.radio.RadiogramProtocolManager;
import com.sun.spot.peripheral.radio.ILowPan;
import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.util.IEEEAddress;


/**
 * This class provides the "radiogram" protocol for accessing the SPOT radio using datagrams.
 * It is an implementor of {@link RadiostreamConnection}
 * 
 * @see RadiostreamConnection
 */
public class RadiogramConnImpl extends RadioConnectionBase implements RadiogramConnection {

	ConnectionID sendConnectionID;
	ConnectionID receiveConnectionID;

	private static IRadiogramProtocolManager protocolManager;
	private static IRadioPolicyManager radioPolicyManager;

	private static synchronized IRadiogramProtocolManager getProtocolManager() {
		if (protocolManager == null) {
			protocolManager = RadiogramProtocolManager.getInstance();
		}
		return protocolManager;
	}

	public static void setProtocolManager(IRadiogramProtocolManager protocolManager) {
		RadiogramConnImpl.protocolManager = protocolManager;
	}

	public static synchronized IRadioPolicyManager getRadioPolicyManager() {
		if (radioPolicyManager == null) {
			radioPolicyManager = RadioFactory.getRadioPolicyManager();
		}
		return radioPolicyManager;
	}
	
	public static void setRadioPolicyManager(IRadioPolicyManager manager) {
		radioPolicyManager = manager;
	}

	/**
	 * DO NOT USE THIS CONSTRUCTOR - connections should be created using Connector.open(...)
	 */
	public RadiogramConnImpl(String addr, byte portNo, boolean isServer, boolean timeouts) {
		if (isServer) {
			receiveConnectionID = sendConnectionID = getProtocolManager().addServerConnection(portNo);
		} else {
			if (addr.toLowerCase().equals("broadcast")) {
				sendConnectionID = getProtocolManager().addBroadcastConnection(portNo);
                                sendConnectionID.setMaxBroadcastHops(ILowPan.DEFAULT_HOPS);
				receiveConnectionID = null;
			} else {
                long macAddress;
                //Conver 16bit address to long
                if(addr.length() == 4){
                   addr = IEEEAddress.MAC_PREFIX + addr;
                }
				macAddress = IEEEAddress.toLong(addr);
				sendConnectionID = getProtocolManager().addOutputConnection(macAddress, portNo);
				receiveConnectionID = getProtocolManager().addInputConnection(macAddress, portNo);
			}
		}

		if (receiveConnectionID != null) {
			getRadioPolicyManager().registerConnection(receiveConnectionID);
			getRadioPolicyManager().policyHasChanged(receiveConnectionID, RadioPolicy.ON);
		}
		if (timeouts) {
			setTimeout(DEFAULT_TIMEOUT);
		}
	}

	/**
	 * DO NOT USE THIS CONSTRUCTOR - connections should be created using Connector.open(...)
	 */
	public RadiogramConnImpl() {}
	
	public void close() throws IOException {
		if (receiveConnectionID != null) {
			getProtocolManager().closeConnection(receiveConnectionID);
			getRadioPolicyManager().deregisterConnection(receiveConnectionID);
			receiveConnectionID = null;
			if (isServer()) {
				// if it's a server, then sendConnectionID == receiveConnectionID so don't deregister it twice
				sendConnectionID = null;
			}
		}
		if (sendConnectionID != null) {
			getProtocolManager().closeConnection(sendConnectionID);
			sendConnectionID = null;
		}
		super.close();
	}

	public int getMaximumLength() {
		return Radiogram.MAX_LENGTH;
	}

	public int getNominalLength() {
		return Radiogram.MAX_LENGTH;
	}

	public void send(Datagram dgram) throws NoAckException, ChannelBusyException, NoRouteException, NoMeshLayerAckException {
		Radiogram rg = (Radiogram)dgram;
		if (rg.getConnection() == this) {
			((Radiogram)dgram).send();
		} else {
			throw new IllegalArgumentException("Attempt to send radiogram on unassociated connection");
		}
	}

	public void receive(Datagram dgram) throws IOException {
		if (isBroadcast()) {
			throw new IllegalStateException("Can't receive on broadcast connection");
		}
		Radiogram rg = (Radiogram)dgram;
		if (rg.getConnection() == this) {
			((Radiogram)dgram).receive();
		} else {
			throw new IllegalArgumentException("Attempt to receive radiogram on unassociated connection");
		}
	}

	public Datagram newDatagram(int size) {
		return new Radiogram(size, this);
	}

	public Datagram newDatagram(int size, String addr) {
		return new Radiogram(size, this, addr);
	}

	public Datagram newDatagram(byte[] buf, int size) {
		throw new IllegalStateException ("Method newDatagram(byte[] buf, int size) is not implemented");
	}

	public Datagram newDatagram(byte[] buf, int size, String addr) {
		throw new IllegalStateException ("Method newDatagram(byte[] buf, int size, String addr) is not implemented");
	}

	public boolean isBroadcast() {
		return sendConnectionID.isBroadcast();
	}

	public boolean isPointToPoint() {
		return sendConnectionID.isOutput();
	}

	public boolean isServer() {
		return sendConnectionID.isServer();
	}
	
	public long getMacAddress() {
		return sendConnectionID.getMacAddress();
	}

	public void setRadioPolicy(RadioPolicy policy) {
		if (receiveConnectionID != null) {
			getRadioPolicyManager().policyHasChanged(receiveConnectionID, policy);
		} else {
			throw new IllegalStateException("Can't set radio policy for output-only connections");
		}
	}
	
	public Connection open(String arg0, String arg1, int arg2, boolean arg3) throws IOException {
		throw new SpotFatalException("cannot reopen a connection");
	}
	
	long send(byte[] payload, long toAddress, int length) throws NoAckException, ChannelBusyException, NoRouteException, NoMeshLayerAckException {
		return getProtocolManager().send(sendConnectionID, toAddress, payload, length);
	}

	IncomingData receivePacket(long timeout) {
		return getProtocolManager().receivePacket(receiveConnectionID, timeout);
	}

	public IncomingData receivePacket() {
		return getProtocolManager().receivePacket(receiveConnectionID);
	}

	public byte getLocalPort() {
		return sendConnectionID.getPortNo();
	}

	public boolean packetsAvailable() {
		return getProtocolManager().packetsAvailable(receiveConnectionID);
	}
        
        public void setMaxBroadcastHops(int hops) {
            if (hops > 255) hops = 255;
            sendConnectionID.setMaxBroadcastHops((byte)(hops & 0xff));
        }
        
        public int getMaxBroadcastHops() {
            return sendConnectionID.getMaxBroadcastHops();           
        }
}
