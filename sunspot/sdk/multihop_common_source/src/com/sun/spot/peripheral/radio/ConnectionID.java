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

package com.sun.spot.peripheral.radio;

import com.sun.spot.peripheral.radio.IConnectionID;
import com.sun.spot.util.IEEEAddress;

public class ConnectionID implements IConnectionID {
	/**
	 * The macAddress to/from which radio packets are sent/received. Set to zero for 
	 * server connections (which handle all traffic for a port number), and to -1
	 * for broadcast connections.
	 */
	long macAddress;
	
	/**
	 * The type of connection (SERVER, BROADCAST, INPUT, OUTPUT).
	 */
	int connectionType;

	/**
	 * The port number associated with thie connection ID.
	 */
	byte portNo;
        
    /**
     * The maximum number of hops a message will make 0=no forwarding
     */
    private byte maxBroadcastHops;
	
	public ConnectionID(){
	}

        public ConnectionID(long macAddress, byte portNo, int connectionType) {
            this.macAddress = macAddress;
            this.portNo = portNo;
            this.connectionType = connectionType;
            if (this.connectionType == IRadiogramProtocolManager.BROADCAST) {
                this.maxBroadcastHops = 0; // by default we don't forward broadcasts
            } else {
                this.maxBroadcastHops = ILowPan.DEFAULT_HOPS;
            }           
        }

	public long getMacAddress() {
		return macAddress;
	}

	public boolean isBroadcast() {
		return connectionType == IRadiogramProtocolManager.BROADCAST;
	}

	public boolean isServer() {
		return connectionType == IRadiogramProtocolManager.SERVER;
	}

	public boolean isInput() {
		return connectionType == IRadiogramProtocolManager.INPUT;
	}

	public boolean isOutput() {
		return connectionType == IRadiogramProtocolManager.OUTPUT;
	}

	public byte getPortNo() {
		return portNo;
	}

        public byte getMaxBroadcastHops() {
            return maxBroadcastHops;
        }

        public void setMaxBroadcastHops(byte maxHops) {
            this.maxBroadcastHops = maxHops;
        }
        
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return ((byte)macAddress << 8) + portNo;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object c) {
		if (c == null) return false;
		//TODO check class
		ConnectionID cid = (ConnectionID)c;
		return (cid.getMacAddress() == macAddress) &&
				(cid.getPortNo() == portNo) &&
				(cid.getConnectionType() == connectionType);
	}
	
	public int getConnectionType() {
		return connectionType;
	}

	public String toString() {
		String name;
		if (isServer()) {
			name = "Server on port " + (portNo & 0xFF);
		} else if (isBroadcast()) {
			name = "Broadcast on port " + (portNo & 0xFF);
		} else if (isInput()) {
			name = "Input from " + IEEEAddress.toDottedHex(macAddress) + " on port " + (portNo & 0xFF);
		} else {
			name = "Output to " + IEEEAddress.toDottedHex(macAddress) + " on port " + (portNo & 0xFF);
		}
		return name;
	}

	public boolean canReceive() {
		return isServer() || isInput();
	}

	public boolean canSend() {
		return isOutput() || isBroadcast() || isServer();
	}

	public void copyFrom(Object o) {
		ConnectionID other = (ConnectionID) o;
		this.connectionType = other.connectionType;
		this.macAddress = other.macAddress;
		this.portNo = other.portNo;
                this.maxBroadcastHops = other.maxBroadcastHops;
	}

    public byte getMaxHops() {
        return maxBroadcastHops;
    }

    public void setMaxHops(byte maxHops) {
        this.maxBroadcastHops = maxHops;
    }
}
