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

package com.sun.spot.peripheral.radio;

import java.util.Hashtable;

import com.sun.spot.peripheral.SpotFatalException;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Utils;

public abstract class RadioProtocolManager implements IRadioProtocolManager {
	static final byte CTRL_NEW_CONN = 1;

	IRadioPolicyManager radioPolicyManager;
	ILowPan lowpan;
	Hashtable connectionIDTable;
	ConnectionID needle;

	RadioProtocolManager(ILowPan lowpan, IRadioPolicyManager radioPolicyManager) {
		connectionIDTable = new Hashtable();
		needle = new ConnectionID(0, (byte)0, 0);
		this.lowpan = lowpan;
		this.radioPolicyManager = radioPolicyManager;
	}

	public IncomingData receivePacket(ConnectionID cid) {
		ConnectionState cs = (ConnectionState)connectionIDTable.get(cid);
		if (cs == null) return null;
		return cs.getQueuedPacket();
	}

	public IncomingData receivePacket(ConnectionID cid, long timeout) {
		ConnectionState cs = (ConnectionState)connectionIDTable.get(cid);
		if (cs == null) return null;
		return cs.getQueuedPacket(timeout);
	}

	public boolean packetsAvailable(ConnectionID connectionID) {
		ConnectionState cs = (ConnectionState)connectionIDTable.get(connectionID);
		return cs.packetsAvailable();
	}

	public synchronized void closeConnection(ConnectionID cidToClose) {
		ConnectionState cs = (ConnectionState)connectionIDTable.get(cidToClose);
		if (cs == null) {
			throw new IllegalArgumentException("Attempt to close unknown connection " + cidToClose.toString());
		}
		if (cs.close()) {
			connectionIDTable.remove(cidToClose);
			log("Removing: " + cidToClose.toString());
		}
	}

	public synchronized ConnectionID addOutputConnection(long macAddress, byte portNo) {
		return addConnection(false, new ConnectionID(macAddress, portNo, OUTPUT));
	}

	protected ConnectionID addConnection(boolean canReceive, ConnectionID cid) {
		if (cid.portNo == 0) {
			cid.portNo = FIRST_USER_PORT;
			while (connectionIDTable.containsKey(cid)) {
				cid.portNo++;
				if (cid.portNo == 0) { // wrapped round
					throw new SpotFatalException("Run out of port numbers for remote address " + IEEEAddress.toDottedHex(cid.getMacAddress()));
				}
			}
		}
		
		if (connectionIDTable.containsKey(cid)) {
			throw new IllegalArgumentException("Attempt to open connection twice for " + cid.toString());
		}
		
		ConnectionState cs = ConnectionState.newInstance(canReceive, cid);
		connectionIDTable.put(cs.id, cs);
		log(" Adding: " + cid.toString());
		return cs.id;
	}

	protected ConnectionState getConnectionState(long macAddress, int connectionType, byte portNumber) {
		needle.macAddress = macAddress;
		needle.connectionType = connectionType;
		needle.portNo = portNumber;
		return (ConnectionState)connectionIDTable.get(needle);
	}
	
	protected abstract String getName();

	private void log(String message) {
		if (Utils.isOptionSelected("spot.log.connections", false)) {
			System.out.println("["+getName()+"]" + message);
		}
	}

	public synchronized ConnectionID addInputConnection(long macAddress, byte portNo) {
		return addConnection(true, new ConnectionID(macAddress, portNo, INPUT));
	}

//	private void dumpConnectionIDTable() {
//		Enumeration x = connectionIDTable.keys();
//		while (x.hasMoreElements()) {
//			ConnectionID element = (ConnectionID) x.nextElement();
//			System.out.println("CS: " + element);
//		}
//	}
}
