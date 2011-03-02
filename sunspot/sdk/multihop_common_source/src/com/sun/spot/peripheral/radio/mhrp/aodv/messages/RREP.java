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


import com.sun.spot.peripheral.radio.mhrp.aodv.AODVManager;
import com.sun.spot.peripheral.radio.mhrp.aodv.Constants;
import com.sun.spot.util.Utils;

/**
 * @author Allen Ajit George
 * @version 0.1
 */
public class RREP extends AODVMessage {
    
    private short hopCount;
    private byte[] message;
    
    private static AODVManager aodvManager = AODVManager.getInstance();
    /**
     * constructs a new route reply message
     */
    public RREP() {
        this.type = Constants.RREP_TYPE;
        this.message = new byte[28];
    }
    /**
     * constructs a new route reply message
     * @param message the route request that caused this route reply
     */
    public RREP(RREQ message) {
        this.type = Constants.RREP_TYPE;
        this.message = new byte[28];
        this.hopCount = 0;
        this.destAddress = message.getDestAddress();
        
        // FIXME
        int givenSequenceNumber = message.getDestSeqNum();
        int currentSequenceNumber = aodvManager.getCurrentSequenceNumber();
        if (givenSequenceNumber >= currentSequenceNumber) {
            this.destSeqNum = aodvManager.getNextSequenceNumber(givenSequenceNumber);
        } else {
            this.destSeqNum = currentSequenceNumber;
        }
        
        this.origAddress = message.getOrigAddress();
        this.origSeqNum = message.getOrigSeqNum();
    }
    
    /**
     * constructs a new route reply message
     * @param newMessage
     */
    public RREP(byte[] newMessage) {
        this.type = Constants.RREP_TYPE;
        this.message = newMessage;
        hopCount = message[3];
        destAddress = Utils.readBigEndLong(message, 4);
        destSeqNum = Utils.readBigEndInt(message, 12);
        origAddress = Utils.readBigEndLong(message, 16);
        origSeqNum = Utils.readBigEndInt(message, 24);
    }
    /**
     * writes the properties into the appropriate bytes of the buffer
     * @return message
     */
    public byte[] writeMessage() {
        message[0] = type;
        // RREQ flags written already
        message[3] = (byte) hopCount;
        Utils.writeBigEndLong(message, 4, destAddress);
        Utils.writeBigEndInt(message, 12, destSeqNum);
        Utils.writeBigEndLong(message, 16, origAddress);
        Utils.writeBigEndInt(message, 24, origSeqNum);
        
        return message;
    }
    /**
     * tells the status of the ackRequiredFlag
     * @return ackRequired  
     */
    public boolean getACKRequiredFlag() {
        return (message[1] & 0x80) != 0;
    }
    
    /**
     * sets the ackRequiredFlag
     */
    public void setACKRequiredFlag(boolean flag) {
        if (flag) {
            message[1] |= 0x80;
        } else {
            message[1] &= 0x7F;
        }
    }
    
    /**
     * read the hop count of this route reply
     * @return hopCount
     */
    public short getHopCount() {
        return hopCount;
    }
    
    /**
     * increment the hop count
     * @return hopCount
     */
    public short incrementHopCount() {
        hopCount++;
        return hopCount;
    }
    
    /**
     * get the route cost
     * @return route cost
     */
    public short getRouteCost() {
        throw new IllegalStateException(RREP.class.getName()
        + ": method not implemented");
    }
}
