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

import com.sun.spot.peripheral.radio.mhrp.lqrp.Constants;
import com.sun.spot.peripheral.radio.mhrp.lqrp.linkParams.ConfigLinkParams;
import com.sun.spot.util.Utils;

/**
 *
 * @author Ron Goldman
 */
public class LQREQ extends LQRPMessage {

    private byte[] message;
    private double aveLQ;
    private int rcvdLQ;

    /**
     * constructs a new link quality request message
     *
     * @param origSourceAddress
     * @param destinationMACaddress how well can this node hear our packets
     * @param cost
     */
    public LQREQ(long origSourceAddress, long destinationMACaddress, double cost) {
        type = Constants.LQREQ_TYPE;
        message = new byte[28];
        destAddress = destinationMACaddress;
        origAddress = origSourceAddress;
        aveLQ = cost;
    }

    /**
     * constructs a new route request message
     *
     * @param newMessage
     * @param lqi 
     */
    public LQREQ(byte[] newMessage, int lqi){
        type = Constants.LQREQ_TYPE;
        message = newMessage;
        destAddress = Utils.readBigEndLong(message, 4);
        origAddress = Utils.readBigEndLong(message, 12);
        aveLQ = Double.longBitsToDouble(Utils.readBigEndLong(message, 20));
        rcvdLQ = lqi;
    }

    /**
     * writes the properties into the appropriate bytes of the buffer
     * 
     * @return message
     */
    public byte[] writeMessage() {
        message[0] = type;
        // LQREQ flags written already
        Utils.writeBigEndLong(message,  4, destAddress);
        Utils.writeBigEndLong(message, 12, origAddress);
        Utils.writeBigEndLong(message, 20, Double.doubleToLongBits(aveLQ));

        return message;
    }

    public void updateRequestorAddress(long newAddr) {
        origAddress = newAddr;
    }

    public double getRouteCost() {
        return aveLQ;
    }

    public double getReceivedCost() {
        return ((double)rcvdLQ) / ConfigLinkParams.MAX_LQ;
    }

}
