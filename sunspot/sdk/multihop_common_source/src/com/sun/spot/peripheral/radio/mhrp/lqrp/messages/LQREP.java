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
import com.sun.spot.util.Utils;

/**
 *
 * @author Ron Goldman
 */
public class LQREP extends LQRPMessage {

    private byte[] message;
    private double aveLQ;

    /**
     * constructs a new link quality reply message
     *
     * @param request the link quality request that caused this link quality reply
     * @param cost the average link quality for messages received on this link
     */
    public LQREP(LQREQ request, double cost) {
        type = Constants.LQREP_TYPE;
        message = new byte[28];
        destAddress = request.getDestAddress();
        origAddress = request.getOrigAddress();
        aveLQ = cost;
    }

    /**
     * constructs a new route request message
     * @param newMessage
     */
    public LQREP(byte[] newMessage){
        type = Constants.LQREP_TYPE;
        message = newMessage;
        destAddress = Utils.readBigEndLong(message, 4);
        origAddress = Utils.readBigEndLong(message, 12);
        aveLQ = Double.longBitsToDouble(Utils.readBigEndLong(message, 20));
    }

    /**
     * writes the properties into the appropriate bytes of the buffer
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


    public double getRouteCost() {
        return aveLQ;
    }

}
