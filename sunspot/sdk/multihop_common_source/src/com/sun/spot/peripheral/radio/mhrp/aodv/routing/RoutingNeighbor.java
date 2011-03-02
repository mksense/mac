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

package com.sun.spot.peripheral.radio.mhrp.aodv.routing;

import com.sun.spot.peripheral.radio.ILowPan;
import com.sun.spot.peripheral.radio.mhrp.aodv.Constants;
import com.sun.spot.peripheral.radio.mhrp.aodv.Sender;
import com.sun.spot.peripheral.radio.mhrp.aodv.messages.RREP;
import com.sun.spot.peripheral.radio.mhrp.aodv.messages.RREQ;
import com.sun.spot.util.IEEEAddress;

/**
 * @author Pete St. Pierre
 * @version 1.0
 */
public class RoutingNeighbor extends Thread {
        
    private ILowPan lowPan;
    private long ourAddress;
    private boolean keepRunning = true;
    
    /**
     * Constructs a new routing table advertiser
     */
    public RoutingNeighbor(ILowPan lp, long address) {
    	super("Routing Neighbor");
        this.lowPan = lp;
        this.ourAddress = address;
        
    }
    
    /**
     * Periodically broadcast a new route advertisement (RREP)
     * to keep our route active in neighbors.
     */
    public void run() {
        RREQ reqMessage;
        RREP advert;

        reqMessage = new RREQ((long)0xffff, ourAddress);
        advert = new RREP(reqMessage);
        byte buffer[] = advert.writeMessage();
        while (keepRunning) {
            try {
                lowPan.sendBroadcast(Constants.AODV_PROTOCOL_NUMBER, buffer, 0, buffer.length, 1);
               
                Thread.sleep((int)(Constants.ACTIVE_ROUTE_TIMEOUT * .85));
            } catch (Exception e) {
                // ignore & continue
            }
        }
    }
    
    public void stopThread() {
        keepRunning = false;
        this.interrupt();
    }
}
