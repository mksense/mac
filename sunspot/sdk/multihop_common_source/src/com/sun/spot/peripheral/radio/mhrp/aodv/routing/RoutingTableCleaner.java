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

package com.sun.spot.peripheral.radio.mhrp.aodv.routing;

import com.sun.spot.peripheral.radio.mhrp.aodv.Constants;

/**
 * @author Allen Ajit George
 * @version 0.1
 */
public class RoutingTableCleaner extends Thread {
        
    private RoutingTable routingTable;
    private boolean keepRunning = true;
    
    /**
     * constructs a new routing table cleaner
     */
    public RoutingTableCleaner(RoutingTable routingTable) {
    	super("RoutingTableCleaner");
        this.routingTable = routingTable;
        
    }
    
    /**
     * calls the routingTable.cleanTable() method every 
     * Constants.ROUTING_TABLE_CLEANER_SLEEP_TIME. This is 8000 milisec in the
     * current implementation
     */
    public void run() {
        while (keepRunning) {
            try {
            	routingTable.waitUntilTableNotEmpty();
                if (keepRunning) {
                    long sleepTime = routingTable.cleanTable();
                    if (keepRunning && sleepTime > 0) {
                        Thread.sleep(sleepTime);
                    }
                }
            } catch (InterruptedException e) {
                // ignore & continue
            }
        }
    }
    
  public void stopThread() {
      keepRunning = false;
      this.setPriority(Thread.MIN_PRIORITY);
      this.interrupt();
  }
}
