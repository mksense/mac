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

package com.sun.spot.peripheral.radio.mhrp.aodv.messages;

import com.sun.spot.peripheral.Spot;
import com.sun.spot.peripheral.radio.mhrp.aodv.AODVManager;
import com.sun.spot.peripheral.radio.mhrp.aodv.Constants;
import com.sun.spot.peripheral.radio.mhrp.aodv.request.RequestTable;
import com.sun.spot.peripheral.radio.mhrp.aodv.routing.RoutingTable;
import com.sun.spot.util.Utils;

/**
 * @author Allen Ajit George
 * @version 0.1
 */
public class RREQ extends AODVMessage {
    
    private byte[] message;
    private int requestID;
    private short hopCount;
    
    private static AODVManager routingManager = AODVManager.getInstance();
    private static RoutingTable routingTable = RoutingTable.getInstance();
    
    /**
     * constructs a new route request message
     * @param destinationMACaddress node to which a route shall be discovered
     */
    public RREQ(long origAddress, long destinationMACaddress) {
        this.type = Constants.RREQ_TYPE;
        this.message = new byte[32];
        this.hopCount = 0;
        this.requestID = RequestTable.getNextRREQID();
        this.destAddress = destinationMACaddress;
        this.destSeqNum = routingTable.getDestinationSequenceNumber(destinationMACaddress);
        this.origAddress = origAddress;
        this.origSeqNum = routingManager.getNextSequenceNumber();
    }
    
    /**
     * constructs a new route request message
     * @param newMessage
     */
    public RREQ(byte[] newMessage){
        this.type = Constants.RREQ_TYPE;
        this.message = newMessage;
        this.hopCount = message[3];
        this.requestID = Utils.readBigEndInt(message, 4);
        this.destAddress = Utils.readBigEndLong(message, 8);
        this.destSeqNum = Utils.readBigEndInt(message, 16);
        this.origAddress = Utils.readBigEndLong(message, 20);
        this.origSeqNum = Utils.readBigEndInt(message, 28);
    }
        /**
     * writes the properties into the appropriate bytes of the buffer
     * @return message
     */
    public byte[] writeMessage() {
        message[0] = type;
        // RREQ flags written already
        message[3] = (byte) (hopCount & 0xff);
        Utils.writeBigEndInt(message, 4, requestID);
        Utils.writeBigEndLong(message, 8, destAddress);
        Utils.writeBigEndInt(message, 16, destSeqNum);
        Utils.writeBigEndLong(message, 20, origAddress);
        Utils.writeBigEndInt(message, 28, origSeqNum);
        
        return message;
    }
    
    /**
     * get the state of the destination only flag
     * @return destinationOnlyFlag
     */
    public boolean getDestinationOnlyFlag() {
        return (message[1] & 0x80) != 0;
    }
    
    /**
     * set the destination only flag
     * @param flag
     */
    public void setDestinationOnlyFlag(boolean flag) {
        if (flag) {
            message[1] |= 0x80;
        } else {
            message[1] &= 0x7F;
        }
    }
    
    /**
     * get the state of the gratuitous route reply flag
     * @return gratuitousRREPFlag
     */
    public boolean getGratuitousRREPFlag() {
        return (message[1] & 0x40) != 0;
    }
    
    /**
     * set the gratuitous route reply flag
     * @param flag
     */
    public void setGratuitousRREPFlag(boolean flag) {
        if (flag) {
            message[1] |= 0x40;
        } else {
            message[2] &= 0xBF;
        }
    }
    
    /**
     * get the ID of this request
     * @return requestID
     */
    public int getRequestID() {
        return requestID;
    }
    
    /**
     * set ID of this request
     * @param requestID
     */
    public void setRequestID(int requestID) {
        this.requestID = requestID;
    }
    
    /**
     * get the hop count of this message
     * @return hop count
     */
    public short getHopCount() {
        return hopCount;
    }
    
    /**
     * increment the hop count of this message
     * @return hop count
     */
    public short incrementHopCount() {
        hopCount++;
        return hopCount;
    }
    
    public short getRouteCost() {
        throw new IllegalStateException(RREQ.class.getName() 
                + ": method not implemented");
    }

    public short setRouteCost() {
        throw new IllegalStateException(RREQ.class.getName() 
                + ": method not implemented");
    }
    
    /**
     * set the destination sequence number of this route request
     * @param destinationSequenceNumber
     */
    public void setDestinationSequenceNumber(int destinationSequenceNumber) {
        this.destSeqNum = destinationSequenceNumber;
    }
}
