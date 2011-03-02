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

import com.sun.spot.util.IEEEAddress;

/**
 * HeaderInfoBase.java
 *
 * This class is to be extended according to the used mac layer. See for example 
 * the LowPanHeaderInfo. This class and also extending classes provide access
 * to information that is stored in headers that are not existing any more after
 * the decapsulation process that takes place in the low pan layer. 
 *
 * @author Jochen Furthmueller
 */
public class HeaderInfoBase {
    
    public long destinationAddress;
    public long sourceAddress;
    /**
     * creates a new instance of the class HeaderInfoBase
     */
    public HeaderInfoBase(long dest, long src) {
        this.destinationAddress = dest;
	this.sourceAddress = src;
    }
    
    /**
     * Set the destination address of this header info. This is used for spotgrams
     * that are supposed to be sent on a server connection.
     */
    public void setDestinationAddress(long addr) {
        this.destinationAddress = addr;
    }

    public void setSourceAddress(long addr) {
        this.sourceAddress = addr;
    }
    
    /** 
     * Gives access to properties of headers that have been stripped away during
     * processing of the packet. This method should be overwritten by extending
     * classes, according to their header fields.
     * @return property string representation of the property that was asked for
     */
    public String getProperty(String key){
        if (key.toLowerCase().equals("destinationaddress")) {
            return IEEEAddress.toDottedHex(destinationAddress);
        }
        if (key.toLowerCase().equals("sourceaddress")) {
            return IEEEAddress.toDottedHex(sourceAddress);
        }
        throw new IllegalArgumentException("RadioMACHeaderInfo.getProperty: "
                +key+ " is no legal argument. Valid argument: destinationAddress");
    };
}
