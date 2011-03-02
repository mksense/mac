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

import com.sun.spot.peripheral.radio.mhrp.aodv.Constants;

/**
 * @author Allen Ajit George
 * @version 0.1
 */
public class RequestTableCleaner extends Thread {
        
    private RequestTable requestTable;
    private boolean keepRunning = true;
    
    /**
     * This thread is responsible for calling the clean table method() of the
     * request table all Constants.REQUEST_TABLE_CLEANER_SLEEP_TIME miliseconds
     * (in the current implementation this is 8000 milisecs
     */
    public RequestTableCleaner(RequestTable requestTable) {
    	super("RequestTableCleaner");
        this.requestTable = requestTable;
    }
    
    public void run() {
        while (keepRunning) {
            try {
            	requestTable.waitUntilTableNotEmpty();
                if (keepRunning) {
                    requestTable.cleanTable();
                }
                if (keepRunning) {
                    Thread.sleep(Constants.REQUEST_TABLE_CLEANER_SLEEP_TIME);
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
