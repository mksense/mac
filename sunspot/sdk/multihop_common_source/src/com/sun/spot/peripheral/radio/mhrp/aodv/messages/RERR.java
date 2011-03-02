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

import com.sun.spot.peripheral.radio.mhrp.aodv.Constants;
import com.sun.spot.util.Utils;

/**
 * @author Allen Ajit George
 * @version 0.1
 */
public class RERR extends AODVMessage {
        
    private byte destCount;
    private byte[] message;
    
    /**
     * constructs a new route error message
     * @param originator 
     * @param destination
     */
    public RERR(long originator, long destination) {
        message = new byte[20];
        destCount = 1;
        this.type = Constants.RERR_TYPE;
        this.origAddress = originator;
        this.destAddress = destination;
    }
    
    /** 
     * constructs a new route error message
     * @param newMessage
     */
    public RERR(byte[] newMessage) {
        this.type = Constants.RERR_TYPE;
        this.message = newMessage;
        this.destCount = message[3];
        this.destAddress = Utils.readBigEndLong(message, 4);
        this.origAddress = Utils.readBigEndLong(message, 12);
    }
    
    /**
     * writes the properties into the appropriate bytes of the buffer
     * @return message
     */
    public byte[] writeMessage() {
        message[0] = type;
        // Reserved (2 bytes)
        message[3] = destCount;
        Utils.writeBigEndLong(message, 4, destAddress);
        Utils.writeBigEndLong(message, 12, origAddress);
        
        return message;
    }
    
    public int getOrigSeqNum() {
        throw new IllegalStateException(RERR.class.getName() 
                + ": method not implemented");
    }
    
    public int getDestSeqNum() {
        throw new IllegalStateException(RERR.class.getName() 
                + ": method not implemented");
    }
}
