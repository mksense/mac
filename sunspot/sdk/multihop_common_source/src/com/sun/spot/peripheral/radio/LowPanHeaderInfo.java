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
 * LowPanHeaderInfo.java
 *
 * This class provides access
 * to information that is stored in headers that are not existing any more after
 * the decapsulation process that takes place in the low pan layer. 
 * @author Jochen Furthmueller
 */
public class LowPanHeaderInfo extends HeaderInfoBase{
    
    //    private long sourceAddress;
    public int rssi;
    public int corr;
    public int linkQuality;
    public long originator;
    public long timestamp;
    private int destinationPanID;
    private int sourcePanID;
    private long finalDestination;
    private long hopCount;
    private boolean fragmented;
    private boolean meshDelivery;
    /**
     * Creates a new instance of LowPanHeaderInfo
     */
    public LowPanHeaderInfo(long destinationAddress, long sourceAddress,
            int rssi, int corr, int linkQuality,
            int destinationPanID, int sourcePanID, boolean fragmented,
            long originator, long finalDestination, byte hopCount, long timestamp) {
        super(destinationAddress, sourceAddress);
	//        this.sourceAddress = sourceAddress;
        this.rssi = rssi;
        this.corr = corr;
        this.linkQuality = linkQuality;
        this.destinationPanID = destinationPanID;
        this.sourcePanID = sourcePanID;
        this.fragmented = fragmented;
        this.meshDelivery = true;
        this.originator = originator;
        this.finalDestination = finalDestination;
        this.hopCount = hopCount;
        this.timestamp = timestamp;
    }
    
    /** 
     * Gives access to properties of headers that have been stripped away during
     * processing of the packet. This method should be overwritten by extending
     * classes, according to their header fields.
     * @return property string representation of the property that was asked for
     */   
    public String getProperty(String key) {
        if (key.toLowerCase().equals("sourceaddress")) {
            return IEEEAddress.toDottedHex(sourceAddress);
        }
        if (key.toLowerCase().equals("destinationaddress")) {
            return IEEEAddress.toDottedHex(destinationAddress);
        }
        if (key.toLowerCase().equals("rssi")) {
            return Integer.toString(rssi);
        }
        if (key.toLowerCase().equals("corr")) {
            return Integer.toString(corr);
        }
        if (key.toLowerCase().equals("linkquality")) {
            return Integer.toString(linkQuality);
        }
        if (key.toLowerCase().equals("destinationpanid")) {
            return Integer.toString(destinationPanID);
        }
        if (key.toLowerCase().equals("sourcepanid")) {
            return Integer.toString(sourcePanID);
        }
        if (key.toLowerCase().equals("originator")) {
            return IEEEAddress.toDottedHex(originator);
        }
        if (key.toLowerCase().equals("finaldestination")) {
            return IEEEAddress.toDottedHex(finalDestination);
        }
        if (key.toLowerCase().equals("hopcount")&meshDelivery) {
            return Long.toString(hopCount);
        }
        if (key.toLowerCase().equals("meshdelivery")) {
            if (meshDelivery) return "true"; else return "false";
        }
        if (key.toLowerCase().equals("fragmented")) {
            if (fragmented) return "true"; else return "false";
        }
        throw new IllegalArgumentException("RadioMACHeaderInfo.getProperty: "
                +key+ " is no legal argument. Valid arguments: sourceAddress, "
                +"destinationAddress, rssi, corr, linkQuality, "
                +"destinationPanID, sourcePanID, meshDelivery, fragmented, "
                +"originator, finalDestination or hopCount");
    }    
}
