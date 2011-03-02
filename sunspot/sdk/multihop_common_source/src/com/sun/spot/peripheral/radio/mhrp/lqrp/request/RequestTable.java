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

package com.sun.spot.peripheral.radio.mhrp.lqrp.request;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.peripheral.radio.routing.RouteInfo;
import com.sun.spot.peripheral.radio.routing.SortedList;
import com.sun.spot.peripheral.radio.mhrp.lqrp.Constants;
import com.sun.spot.peripheral.radio.mhrp.lqrp.messages.RREP;
import com.sun.spot.peripheral.radio.mhrp.lqrp.messages.RREQ;
import com.sun.spot.peripheral.radio.routing.interfaces.RouteEventClient;
import java.util.Random;

/**
 * @author Allen Ajit George
 * @version 0.1
 */
public class RequestTable {
    
    private final Hashtable table;
    private final SortedList timeoutList;
    
    private static final Object RREQlock = new Integer(0);
    private static int currentRREQID;
    private long ourAddress;
    private Object cleanerMonitor;
    private static RequestTable instance;
    private RequestTableCleaner cleaner;
    
    /**
     * constructs new request table
     */
    private RequestTable() {
        Random rnd = new Random();
        timeoutList = new SortedList();
        table = new Hashtable();
        currentRREQID = rnd.nextInt(65535) + 1;  // start with a limited random seq number in [1,65535]
        cleanerMonitor = new Object();
    }

    
    public void start() {
        cleaner = new RequestTableCleaner(this);
        RadioFactory.setAsDaemonThread(cleaner);
        cleaner.start();
    }
    
    public void stop() {
        cleaner.stopThread();
    }
    

    /**
     * provides the instance of this singleton
     */
    public synchronized static RequestTable getInstance() {
        if (instance == null) {
            instance = new RequestTable();
        }
        
        return instance;
    }
    
    /**
     * get the next route request ID
     * @return nextRREQID
     */
    public static int getNextRREQID() {
        int returnVal;
        
        synchronized(RREQlock) {
            currentRREQID++;
            if (currentRREQID == 65536)  // we roll over @ a 16 bit boundry
                currentRREQID = 1;
            returnVal = currentRREQID;
        }
        
        return returnVal;
    }
    
    /**
     * add a route request to the request table
     * @param message the rreq that should be added
     * @param eventClient the instance that caused this route request
     * @param uniqueKey identifier for this rreq
     * @return true when ready
     */
    // FIXME See if I can improve synchronization
    public boolean addRREQ(RREQ message, RouteEventClient eventClient,
            Object uniqueKey) {
        Long wantedDestination = new Long(message.getDestAddress());
        RequestEntry entry = new RequestEntry();
        entry.activityFlag = true;
        entry.key = wantedDestination;
        entry.requestID = message.getRequestID();
        entry.expiryTime = System.currentTimeMillis() + Constants.PATH_DISCOVERY_TIME;
        entry.originatorMACAddress = new Long(message.getOrigAddress());
        entry.client = eventClient;
        entry.uniqueKey = uniqueKey;
        synchronized (table) {
            Object o = table.get(wantedDestination);
            if (o != null) {                               
                if (o instanceof Vector) {
                    // if there are already 2 or more entries, we must add it to
                    // the vector
                    Vector v = (Vector) o;
                    timeoutList.insertElement(entry);
                    v.addElement(entry);
                    //Debug.print("addRREQ: added request entry " + v.size(), 1);
                } else {
                    // if there is only one, than we must create a vector and
                    // add the new one
                    Vector v = new Vector();
                    v.addElement(table.remove(wantedDestination));
                    v.addElement(entry);
                    timeoutList.insertElement(entry);
                    table.put(wantedDestination, v);
                    //Debug.print("addRREQ: added request entry 2", 1);
                }
            } else {
                //if there is no entry so far, just add it
                timeoutList.insertElement(entry);
                table.put(wantedDestination, entry);
                //Debug.print("addRREQ: added request entry 1", 1);
            }
        }
        notifyCleaner();
        return true;
    }

    /**
     * Helper function to search for an existing (active) entry
     * @param destAddr
     * @param origAddr
     * @param reqID  if non-zero then must match request ID
     * @param active if true then only match active requests
     * @return the request entry that is a match if found
     */
    public RequestEntry findRequest(long destAddr, long origAddr, int reqID, boolean active) {

        Long wantedDestination = new Long(destAddr);
        RequestEntry returnVal = null;

        synchronized(table) {
            if (!active) {
                cleanTable();
            }
            Object o = table.get(wantedDestination);
            if (o != null) {

                if (o instanceof Vector) {
                    Vector v = (Vector) o;

                    Enumeration e = v.elements();
                    while (e.hasMoreElements()) {
                        RequestEntry entry = (RequestEntry) e.nextElement();
                        if ((entry.originatorMACAddress.longValue() == origAddr) &&
                                (reqID == entry.requestID || reqID == 0) &&
                                (!active || entry.activityFlag)) {
                            returnVal = entry;
                            break;
                        }
                    }
                } else {
                    RequestEntry entry = (RequestEntry) o;
                    if ((entry.originatorMACAddress.longValue() == origAddr) &&
                            (reqID == entry.requestID || reqID == 0) &&
                            (!active || entry.activityFlag)) {
                        returnVal = entry;
                    }
                }
            }
        }

        return returnVal;
    }


    /**  
     * this method tells the caller if the request table already has an entry for
     * the destination of this route request and it is currently active
     *
     * @param message
     * @return hasActiveRequest
     */
    public boolean hasActiveRequest(RREQ message) {
        return findRequest(message.getDestAddress(), message.getOrigAddress(), message.getRequestID(), true) != null;
    }    
    
    /**  
     * this method tells the caller if the request table already has an entry for
     * the destination of this route request and it is currently active
     *
     * @param message
     * @return hasActiveRequest
     */
    public boolean hasActiveRequest(RREP message) {
        return findRequest(message.getDestAddress(), message.getOrigAddress(), 0, true) != null;
    }        

    /**
     * this method tells the caller if the request table already has an entry for
     * the destination of this route request
     *
     * @param message
     * @return hasRequest
     */
    public boolean hasRequest(RREQ message) {
        return findRequest(message.getDestAddress(), message.getOrigAddress(), message.getRequestID(), false) != null;
    }
    
    /**
     * this method tells the caller if the request table already has an entry for
     * the destination of this route reply
     *
     * @param message
     * @return hasRequest
     */
    public boolean hasRequest(RREP message) {
        return findRequest(message.getDestAddress(), message.getOrigAddress(), 0, false) != null;
    }
    
    /**
     * this method removes an outstanding request from the table
     *
     * @param destination
     * @param originator
     */
    // FIXME See if I can improve synchronization
    public void removeOutstandingRequest(long destination, long originator, int reqID) {
        Long wantedDestination = new Long(destination);
        synchronized (table) {
            Object o = table.get(wantedDestination);
            if (o != null) {                
                if (o instanceof Vector) {
                    Vector v = (Vector) o;
                    
                    Enumeration e = v.elements();
                    while (e.hasMoreElements()) {
                        RequestEntry entry = (RequestEntry) e.nextElement();
                        if (entry.originatorMACAddress.longValue() == originator &&
                                (reqID == entry.requestID || reqID == 0)) {
                            v.removeElement(entry);
                            timeoutList.removeElement(entry);
                            break;
                        }
                    // if vector is now empty or has only 1 entry should we remove it?
                    }
                } else {
                    RequestEntry entry = (RequestEntry) o;
                    if (entry.originatorMACAddress.longValue() == originator &&
                            (reqID == entry.requestID || reqID == 0)) {
                        table.remove(wantedDestination);
                        timeoutList.removeElement(entry);
                    }
                }
            }
        }
    }
    
    /**
     * This method removes all outstanding request that are expired
     */
    public void cleanTable() {
        synchronized (table) {
            long expiryCutoff =
                    System.currentTimeMillis() + Constants.EXPIRY_TIME_DELTA;
            RequestEntry entry = (RequestEntry) timeoutList.getFirstElement();
            
            while (entry != null) {
                if (entry.expiryTime < expiryCutoff) {             
                    timeoutList.removeFirstElement();
                    if (entry.activityFlag) {
                        entry.activityFlag = false;
                        entry.expiryTime = System.currentTimeMillis()+Constants.REQUEST_GRACE_PERIOD;
                        timeoutList.insertElement(entry);
                        // notify our callback when we deactivate, not when we expire
                        if ((entry.originatorMACAddress.longValue() - ourAddress) == 0) {
                            if (entry.client != null) {
                                RouteInfo info = new RouteInfo(entry.key.longValue(),
                                        Constants.INVALID_NEXT_HOP, 0);
                                entry.client.routeFound(info, entry.uniqueKey);
                            }
                        }                   
                    } else {    
                        // Debug.print("cleanTable: remove request entry ");
                        removeOutstandingRequest(entry.key.longValue(),
                                entry.originatorMACAddress.longValue(),
                                entry.requestID);
                        
                    }
                    entry = (RequestEntry) timeoutList.getFirstElement();
                } else {
                    break;
                }       
            }
        }
    }
    
    /**
     * This method provides access to the client that interested in the route
     * reply message that is passed as an argument
     * @param message
     * @return routeEventClient
     */
    public RouteEventClient getCallback(RREP message) {
        RequestEntry entry = findRequest(message.getDestAddress(), message.getOrigAddress(), 0, true);
        return entry != null ? entry.client : null;
    }
            
    private void notifyCleaner() {
        synchronized (cleanerMonitor) {
            cleanerMonitor.notifyAll();
        }
    }
    
    void waitUntilTableNotEmpty() {
        while (timeoutList.getFirstElement() == null) {
            synchronized (cleanerMonitor) {
                try {
                    cleanerMonitor.wait();
                } catch (InterruptedException e) {
                    // ignore & continue
                }
            }
            if (Thread.currentThread().getPriority() == Thread.MIN_PRIORITY) {
                break;
            }
        }
    }
    
    public void setOurAddress(long ourAddress) {
        this.ourAddress = ourAddress;
    }
    
}
