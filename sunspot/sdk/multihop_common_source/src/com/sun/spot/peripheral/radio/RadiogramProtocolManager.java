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


import com.sun.spot.interisolate.InterIsolateServer;
import com.sun.spot.peripheral.ChannelBusyException;
import com.sun.spot.peripheral.NoAckException;
import com.sun.spot.peripheral.NoRouteException;
import com.sun.spot.peripheral.radio.LowPanHeader;
import com.sun.spot.peripheral.radio.proxy.IRadioServerContext;
import com.sun.spot.peripheral.radio.proxy.ProxyRadiogramProtocolManager;


/* (non-Javadoc)
 * @see com.sun.squawk.peripheral.radio.IPortBasedProtocolManager#addConnection(long, byte, boolean)
 */

public class RadiogramProtocolManager extends RadioProtocolManager implements IProtocolManager, IRadiogramProtocolManager {
    /*
      * Note on synchronization:
      * All methods that rely on there being no external changes to the connections hashtable
      * during their execution are synchronized.
      */

    public static final byte PROTOCOL_NUMBER = 105;
    public static final String PROTOCOL_NAME = "radiogram";
    private static IRadiogramProtocolManager theInstance;

    public static void main(String[] args) {
        InterIsolateServer.run(ProxyRadiogramProtocolManager.CHANNEL_IDENTIFIER,
                new IRadioServerContext() {
                    public IRadioProtocolManager getRadioProtocolManager() {
                        return RadiogramProtocolManager.getInstance();
                    }
                });
    }

    public synchronized static IRadiogramProtocolManager getInstance() {
        if (theInstance == null) {
            if (RadioFactory.isMasterIsolate()) {
                theInstance = new RadiogramProtocolManager();
            } else {
                theInstance = new ProxyRadiogramProtocolManager(PROTOCOL_NUMBER, PROTOCOL_NAME);
            }
        }
        return theInstance;
    }

    RadiogramProtocolManager(ILowPan lowpan, IRadioPolicyManager radioPolicyManager) {
        super(lowpan, radioPolicyManager);
    }

    /**
     * Construct an instance to manage the given protocol number.
     */
    public RadiogramProtocolManager() {
        this(LowPan.getInstance(), RadioFactory.getRadioPolicyManager());
        lowpan.registerProtocol(PROTOCOL_NUMBER, this);
    }

    public long send(ConnectionID cid, long toAddress, byte[] payload, int length) throws NoAckException, ChannelBusyException, NoRouteException {
        /* Note: no need to sync this method because the connections hashtable is itself
           * thread-safe, and it is accessed only once here.
           */
        if (!cid.canSend())
            throw new IllegalArgumentException(cid.toString() + " cannot be used for sending");

        if (toAddress == 0) {
            throw new IllegalArgumentException("Cannot send to address 0");
        }

        payload[PORT_OFFSET] = cid.getPortNo();
     //   System.out.println("Sending data to " + cid.getMacAddress() + " with length " + length + "isBdc " + cid.isBroadcast());
        if (cid.isBroadcast()) {
            return lowpan.sendBroadcast(PROTOCOL_NUMBER, payload, 0, length, cid.getMaxBroadcastHops());
        } else {
            return lowpan.send(LowPanHeader.DISPATCH_SPOT, PROTOCOL_NUMBER, toAddress, payload, 0, length);
        }
    }

    public synchronized void processIncomingData(byte[] payload, LowPanHeaderInfo headerInfo) {
        // System.out.println("Processing incomimg data from " + sourceAddress + " " + Utils.stringify(payload));

        // First see if we have a matching server connection, because if we have all input goes there
        byte portNumber = payload[PORT_OFFSET];
        ConnectionState destinationCS = getConnectionState(0, SERVER, portNumber);
        if (destinationCS == null) {
            // System.out.println("no matching server");
            destinationCS = getConnectionState(headerInfo.originator, INPUT, portNumber);
        }
        if (destinationCS == null) {
            // System.out.println("discarding packet with key " + needle);
        } else {
            destinationCS.addToQueue(new IncomingData(payload, headerInfo));
        }
    }

    public String getName() {
        return PROTOCOL_NAME;
    }

    public synchronized ConnectionID addServerConnection(byte portNo) {
        return addConnection(true, new ConnectionID(0, portNo, SERVER));
    }

    public synchronized ConnectionID addBroadcastConnection(byte portNo) {
        ConnectionID cid = new ConnectionID(-1, portNo, BROADCAST);

        BroadcastConnectionState foundState = (BroadcastConnectionState) connectionIDTable.get(cid);
        if (foundState != null) {
            foundState.incrementReferenceCount();
            return cid;
        }
        return addConnection(false, cid);
    }

    int getNumberOfEntriesInConnectionStateTable() {
        return connectionIDTable.size();
    }
}
