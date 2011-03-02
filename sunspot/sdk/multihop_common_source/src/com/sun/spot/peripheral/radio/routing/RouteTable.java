/*
 * Copyright 2007-2008 Sun Microsystems, Inc. All Rights Reserved.
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
import java.util.Enumeration;
import java.util.Hashtable;

/**
 *
 * @author Pete St. Pierre
 */
public class RouteTable {
    private Hashtable table;  //A hashtable of RouteInfo objects, keyed on destination
    
    /** Creates a new instance of RouteTable */
    public RouteTable() {
        table = new Hashtable();
    }
    
    public void addEntry(RouteInfo ri) {
        table.put(new Long(ri.destination), ri);
    }
    
    
    public RouteInfo getEntry(long address) {
        RouteInfo ri = null;
        ri = (RouteInfo)table.get(new Long(address));
        return ri;
    }
    
    public Enumeration getAllEntries() {
        return table.elements();
    }
    public int getSize() {
        return table.size();
    }
    
    public String toString() {
        Enumeration e = getAllEntries();
        String output = "   Destination\t\tNext Hop\tHops\n";
        while (e.hasMoreElements()) {
            RouteInfo ri = ((RouteInfo)e.nextElement());
            output += IEEEAddress.toDottedHex(ri.destination) + "\t  "
                    + IEEEAddress.toDottedHex(ri.nextHop) + "\t"
                    + ri.hopCount +  "\n";
        }
        return output;
    }
}
