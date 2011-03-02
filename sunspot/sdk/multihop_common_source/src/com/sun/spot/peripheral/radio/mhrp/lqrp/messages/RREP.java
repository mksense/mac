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

package com.sun.spot.peripheral.radio.mhrp.lqrp.messages;


import com.sun.spot.peripheral.radio.mhrp.lqrp.LQRPManager;
import com.sun.spot.peripheral.radio.mhrp.lqrp.Constants;
import com.sun.spot.peripheral.radio.mhrp.lqrp.linkParams.ConfigLinkParams;
import com.sun.spot.peripheral.radio.mhrp.lqrp.linkParams.NodeLifeAndLinkMonitor;
import com.sun.spot.util.Utils;

/**
 * @author Allen Ajit George modified by Pradip De
 * @version 0.1
 */
public class RREP extends LQRPMessage {
    
    private short hopCount;
    private short lowLQlinkCount;
    private byte[] message;
    
    private double routeCostToDest;
    private double revLastHopCost; //Link Cost to go to last hop from current node
    private static LQRPManager lqrpManager = LQRPManager.getInstance();
    
    private NodeLifeAndLinkMonitor linkM = NodeLifeAndLinkMonitor.getInstance();

    /**
     * constructs a new route reply message
     * @param request the route request that caused this route reply
     */
    public RREP(RREQ request) {
        type = Constants.RREP_TYPE;
        message = new byte[48];
        hopCount = 0;
        routeCostToDest = 0.0; //The fwd cost from the src is built here; filled after nextHop is known  
        revLastHopCost = 0.0;//The nextLinkCost is filled here when the nextHop is identified
        lowLQlinkCount = 0;//Count of links in path with LQ below threshold
        destAddress = request.getDestAddress();
        
        // FIXME
        int givenSequenceNumber = request.getDestSeqNum();
        int currentSequenceNumber = lqrpManager.getCurrentSequenceNumber();
        if (givenSequenceNumber >= currentSequenceNumber) {
            destSeqNum = lqrpManager.getNextSequenceNumber(givenSequenceNumber);
        } else {
            destSeqNum = currentSequenceNumber;
        }
        
        origAddress = request.getOrigAddress();
        origSeqNum = request.getOrigSeqNum();
    }
    
    /**
     * constructs a new route reply message
     * @param newMessage
     */
    public RREP(byte[] newMessage) {
        type = Constants.RREP_TYPE;
        message = newMessage;
        hopCount = message[3];
        destAddress = Utils.readBigEndLong(message, 4);
        destSeqNum = Utils.readBigEndInt(message, 12);
        origAddress = Utils.readBigEndLong(message, 16);
        origSeqNum = Utils.readBigEndInt(message, 24);
        routeCostToDest = Double.longBitsToDouble(Utils.readBigEndLong(message, 28));
        revLastHopCost = Double.longBitsToDouble(Utils.readBigEndLong(message, 36));
        lowLQlinkCount = message[44];
    }
    /**
     * writes the properties into the appropriate bytes of the buffer
     * @return message
     */
    public byte[] writeMessage() {
        message[0] = type;
        // RREQ flags written already
        message[3] = (byte) hopCount;
        Utils.writeBigEndLong(message,  4, destAddress);
        Utils.writeBigEndInt(message,  12, destSeqNum);
        Utils.writeBigEndLong(message, 16, origAddress);
        Utils.writeBigEndInt(message,  24, origSeqNum);
        Utils.writeBigEndLong(message, 28, Double.doubleToLongBits(routeCostToDest));
        Utils.writeBigEndLong(message, 36, Double.doubleToLongBits(revLastHopCost));
        message[44] = (byte) lowLQlinkCount;
        
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
     * update the route cost metric of this message
     * @return route cost to destination
     */
    public double updateRouteCostToDest(long nextHop) {
        double cost;
        double linkLQ = linkM.getCurrNormLQForAddress(nextHop);
        if (linkLQ < ConfigLinkParams.THRESH_LOW_LQI) {
            lowLQlinkCount++;
        }
        cost = 1.0/(ConfigLinkParams.SIGMA * linkLQ +
                       (1.0 - ConfigLinkParams.SIGMA) * linkM.getNodeLifetime().getNormNodeLifetime());
        //System.out.println("UpdateRouteCostToDest : " + cost);
        routeCostToDest += cost;
        revLastHopCost = cost;
        return cost;
    }
    
    
    /**
     * get the route cost
     * @return route cost
     */
    public double getRouteCostToDest() {
        return routeCostToDest;
    }
    
    /**
     * get the link cost for the last hop 
     * @return revLastHopCost
     */
    public double getRevLastHopCost() {
        return revLastHopCost;
    }

    public int getLowLQlinkCount() {
        return lowLQlinkCount;
    }
}
