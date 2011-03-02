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

package com.sun.spot.peripheral.radio.mhrp.aodv.request;

import com.sun.spot.peripheral.radio.routing.interfaces.Comparable;
import com.sun.spot.peripheral.radio.routing.interfaces.RouteEventClient;

/**
 * @author Allen Ajit George
 * @version 0.1
 */
public class RequestEntry implements Comparable {
    
    public Long key;
    public int requestID;
    public Long originatorMACAddress;
    public long expiryTime;
    public boolean activityFlag;
    public RouteEventClient client;
    public Object uniqueKey;
    /**
     * compares to request entries
     * @param object entry with which this entry is to be compared
     * @return expiration -1 if the expiry time of this entry is lower than the one 
     * of the one that was passed as argument, 0 if they are equal, 1 if it is greater
     */
    public int compare(Object object) {
        RequestEntry entry = (RequestEntry) object;
        
        if (this.expiryTime < entry.expiryTime) {
            return -1;
        } else if (this.expiryTime == entry.expiryTime) {
            return 0;
        } else {
            return 1;
        }
    }
}
