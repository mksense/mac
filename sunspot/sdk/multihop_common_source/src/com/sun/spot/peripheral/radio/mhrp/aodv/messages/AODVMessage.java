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

/**
 *
 *
 * @author Allen Ajit George
 * @version 0.1
 */
public abstract class AODVMessage {
    
    protected byte type;
    protected int destSeqNum;
    protected long destAddress;
    protected int origSeqNum;
    protected long origAddress;
    
    public abstract byte[] writeMessage();
    
    /**
     * get the type of this message.
     * @return type can be RREQ_TYPE = 0x01, RREP_TYPE = 0x02 or RERR_TYPE = 0x03
     */
    public byte getType() {
        return type;
    }
    
    /**
     * get the destination sequence number of this message.
     * @return destSeqNum
     */
    public int getDestSeqNum() {
        return destSeqNum;
    }
    
    /**
     * get the destination address of this message
     * @return destinationAddress
     */
    public long getDestAddress() {
        return destAddress;
    }
    
    /**
     * get the originator sequence number.
     * @return origSeqNum
     */
    public int getOrigSeqNum() {
        return origSeqNum;
    }
    
    /**
     * get the originator's address.
     * @return origAddress
     */
    
    public long getOrigAddress() {
        return origAddress;
    }
}
