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

package com.sun.spot.peripheral.radio.routing;

import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Utils;

/**
 * An object that describes a basic route entry.  It contains the final destination, 
 * next hop and number of hops between this node and the destination
 * @author Allen Ajit George
 * @version 0.1
 */
public class RouteInfo {
    
    /**
     * The final destination address
     */
    public long destination;
    /**
     * next node on the route to the destination
     */
    public long nextHop;
    /**
     * total number of hops between this node and the destination
     */
    public int hopCount;
    
    /**
     * Create a RouteInfo object using all three components: destination, next hop and total hop count
     * @param destination The IEEE Address (as a long) of the route destination
     * @param nextHop The IEEE Address (as a long) of the next hop to the detination address
     * @param hopCount number of hops to the final destination of this route
     */
    public RouteInfo(long destination, long nextHop, int hopCount) {
        this.destination = destination;
        this.nextHop = nextHop;
        this.hopCount = hopCount;
    }
    
    // DO NOT REMOVE! This function needed as a workaround against a verifier bug
    /**
     * return the destination for this RouteInfo object
     * @return the destination address for this route entry
     */
    public long getDestination() {
        return destination;
    }
    
    /**
     * create a string represenation of this object
     * @return the string representation of this destination, next hop, hop count tuple.
     */
    public String toString() {
    	return new IEEEAddress(destination).asDottedHex()+":"+
    		new IEEEAddress(nextHop).asDottedHex()+":"+
    		hopCount;
    }
    
    public static RouteInfo fromString(String s) {
        String[] parts = Utils.split(s, ':');
        return new RouteInfo(new IEEEAddress(parts[0]).asLong(),
                        new IEEEAddress(parts[1]).asLong(),
                        Integer.parseInt(parts[2]));
    }
  
}
