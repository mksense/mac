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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.peripheral.radio.routing.RouteInfo;
import com.sun.spot.peripheral.radio.routing.SortedList;
import com.sun.spot.peripheral.radio.mhrp.aodv.Constants;
import com.sun.spot.peripheral.radio.mhrp.aodv.messages.RREP;
import com.sun.spot.peripheral.radio.mhrp.aodv.messages.RREQ;
import com.sun.spot.util.Debug;
import com.sun.spot.util.IEEEAddress;

/**
 * An object that represents a routing table for the mesh
 * @author Allen Ajit George, Jochen Furtmueller
 * @version 0.1
 */
public class RoutingTable {
    
    private final Hashtable table;
    private final SortedList timeoutList;
    private Long ourAddress;
    private static RoutingTable instance;
    private RoutingTableCleaner cleaner;
    private Object cleanerMonitor;
    
    /**
     * construct a new routing table
     */
    private RoutingTable() {
        timeoutList = new SortedList();
        table = new Hashtable();

        cleanerMonitor = new Object();
    }
    
    public void start() {
        cleaner = new RoutingTableCleaner(this);
        RadioFactory.setAsDaemonThread(cleaner);
        cleaner.start();
    }
    
    public void stop() {
        cleaner.stopThread();
    }
    
    /**
     * retrieve a handle to this routing table
     * @return instance of this singleton
     */
    public synchronized static RoutingTable getInstance() {
        if (instance == null) {
            instance = new RoutingTable();
        }
        return instance;
    }
    
    /**
     * returns an object that describes the route for a given destination
     * @param address for which a route info is wanted
     * @return routeInfo that has a valid next hop field if there was a route in
     * the table, or a invalid next hop field if there was not
     */
    public RouteInfo getNextHopInfo(long address) {
        RoutingEntry returnedEntry;
        synchronized (table) {
            returnedEntry = (RoutingEntry) table.get(new Long(address));
        }
        
        // FIXME - Is this necessarily correct?
        if (returnedEntry != null && returnedEntry.activityFlag) {
            freshenRoute(address);
            return new RouteInfo(address,
                    (returnedEntry.nextHopMACAddress).longValue(),
                    returnedEntry.hopCount);
        } else {
            if (returnedEntry == null) {
                Debug.print("[AODV] no route found for: " + IEEEAddress.toDottedHex(address) +
                        " at " + System.currentTimeMillis());
                return new RouteInfo(address, Constants.INVALID_NEXT_HOP, 0);
            } else {
                Debug.print("[AODV] reactivate inactive route found for: " + IEEEAddress.toDottedHex(address) +
                        " through " + IEEEAddress.toDottedHex(returnedEntry.nextHopMACAddress.longValue()) +
                        " hops " + returnedEntry.hopCount +
                        " at " + System.currentTimeMillis());
                freshenRoute(address);
                return new RouteInfo(address,
                        (returnedEntry.nextHopMACAddress).longValue(),
                        returnedEntry.hopCount);
            }
        }
    }
    
    /**
     * Create a new route entry using the parameters, then call doTableAddition()
     * @param address destination address of route entry
     * @param nextHopMACAddress address of next hop to this address
     * @param hopCount number of hops to destination
     * @param routeCost AODV route cost mechanism
     * @param destSeqNumber AODV sequence number
     */
    public void addRoute(long address, long nextHopMACAddress, int hopCount,
            int routeCost, int destSeqNumber) {
        Debug.print("addRoute: adding route for "
                + IEEEAddress.toDottedHex(address)
                + " next hop " + IEEEAddress.toDottedHex(nextHopMACAddress)
                + " hops " + hopCount
                + " at " + System.currentTimeMillis());
        Long wantedAddress = new Long(address);
        
        RoutingEntry entry = new RoutingEntry();
        
        entry.key = wantedAddress;
        entry.nextHopMACAddress = new Long(nextHopMACAddress);
        entry.hopCount = hopCount;
        entry.routeCost = routeCost;
        entry.sequenceNumber = destSeqNumber;
        entry.activityFlag = true;
        doTableAddition(wantedAddress, entry);
    }
    
    /**
     * Create a new route entry based on a received route request, then call doTableAddition()
     * @param senderMACAddress mac address of the requestor
     * @param message route request message received
     */
    public void addRoute(long senderMACAddress, RREQ message) {
        Long wantedOriginator = new Long(message.getOrigAddress());
        Debug.print("addRoute (RREQ): adding back route for " + IEEEAddress.toDottedHex(message.getOrigAddress())
        + " asked about " + IEEEAddress.toDottedHex(message.getDestAddress())
        + " heard from " + IEEEAddress.toDottedHex(senderMACAddress)
        + " hops " + message.getHopCount()
        + " at " + System.currentTimeMillis());
        if (!wantedOriginator.equals(ourAddress)) {
            // Create an entry for the originator of the RREQ message
            RoutingEntry entry = new RoutingEntry();
            
            entry.key = new Long(message.getOrigAddress());
            entry.nextHopMACAddress = new Long(senderMACAddress);
            entry.hopCount = message.getHopCount();
            entry.sequenceNumber = message.getOrigSeqNum();
            entry.activityFlag = true;
            doTableAddition(wantedOriginator, entry);
            
        }
    }
    /**
     * Create a new route entry based on a received route reply, then call doTableAddition()
     * @param senderMACAddress sender of the route response
     * @param message route response message received
     */
    public void addRoute(long senderMACAddress, RREP message) {
        Long wantedDestination = new Long(message.getDestAddress());
        Debug.print("addRoute (RREP): adding route for "
                + IEEEAddress.toDottedHex(wantedDestination.longValue())
                + " through " + IEEEAddress.toDottedHex(senderMACAddress)
                + " hops " + message.getHopCount()
                + " at " + System.currentTimeMillis());
        
        // Create an entry for the originator of the RREP message
        RoutingEntry entry = new RoutingEntry();
        
        entry.key = wantedDestination;
        entry.nextHopMACAddress = new Long(senderMACAddress);
        entry.hopCount = message.getHopCount();
        entry.sequenceNumber = message.getDestSeqNum();
        entry.activityFlag = true;
        entry.routeUsers.addElement(new Long(message.getOrigAddress()));
        
        doTableAddition(wantedDestination, entry);
    }
    
    /**
     * get the destination sequence number for a certain entry
     * @param address address of the entry that we are interested in
     * @return destinationSequenceNumber
     */
    public int getDestinationSequenceNumber(long address) {
        Long wantedDestination = new Long(address);
        
        RoutingEntry entry = (RoutingEntry) table.get(wantedDestination);
        if (entry != null) {
            return entry.sequenceNumber;
        }
        return Constants.UNKNOWN_SEQUENCE_NUMBER;
    }
    
    /**
     * delete all table entries that are expired
     * 
     * @return time until next route expires or 0 if table is empty
     */
    public long cleanTable() {
        long result = 0;
        synchronized (table) {
            long expiryCutoff = System.currentTimeMillis() + Constants.EXPIRY_TIME_DELTA;
            RoutingEntry entry = (RoutingEntry) timeoutList.getFirstElement();
            
            while ((entry != null)) {
                if (entry.expiryTime < expiryCutoff) {
                    timeoutList.removeFirstElement();
                    if (entry.activityFlag) {
                        Debug.print("[AODV] cleaner clear flag for route to " +
                                IEEEAddress.toDottedHex(entry.key.longValue()) +
                                " through " + IEEEAddress.toDottedHex(entry.nextHopMACAddress.longValue()) +
                                " at " + System.currentTimeMillis());
                        entry.activityFlag = false;
                        entry.expiryTime = System.currentTimeMillis()+Constants.DELETE_PERIOD;
                        timeoutList.insertElement(entry);
                    } else {
                        Debug.print("[AODV] cleaner remove entry route to   " +
                                IEEEAddress.toDottedHex(entry.key.longValue()) +
                                " through " + IEEEAddress.toDottedHex(entry.nextHopMACAddress.longValue()) +
                                " at " + System.currentTimeMillis());
                        table.remove(entry.key);
                    }
                } else {
                    break;
                }
                
                entry = (RoutingEntry) timeoutList.getFirstElement();
            }
            if (entry != null) {
                result = entry.expiryTime - System.currentTimeMillis();
            }
        }
        return result;
    }
    
    /**
     * this method checks if a new route is better than the one that already
     * might be existing in the routing table. Only if this is the case the
     * old one is replaced.
     * @param key the destination address for which we have a new route
     * @param routingEntry
     *
     */
    // FIXME See if I can improve the synchronization here
    private void doTableAddition(Long key, RoutingEntry newEntry) {
        synchronized (table) {
            newEntry.expiryTime =
                    System.currentTimeMillis() + Constants.ACTIVE_ROUTE_TIMEOUT;
            RoutingEntry existingEntry = (RoutingEntry) table.get(key);
            if (existingEntry != null) {
                if (existingEntry.activityFlag) {
//                    Debug.print("doTableAddition: existing hop count: "
//                            + existingEntry.hopCount + " new hop count: "
//                            + newEntry.hopCount, 2);
                    if (existingEntry.hopCount > newEntry.hopCount) {
//                        Debug.print("doTableAddition: replacing old entry", 2);
                        timeoutList.insertElement(newEntry);
                        if (!existingEntry.routeUsers.isEmpty()) {
                            // Copy the users from the old list to the new list
                            copyUserList(existingEntry.routeUsers, newEntry.routeUsers);
                        }
                        table.put(key, newEntry);
                        timeoutList.removeElement(existingEntry);
//                        Debug.print("doTableAddition: added route for "
//                                + IEEEAddress.toDottedHex(key.longValue())
//                                + " through "
//                                + IEEEAddress.toDottedHex(newEntry.nextHopMACAddress
//                                .longValue()), 2);
                    } else {
//                        Debug.print("doTableAddition: existing route for "
//                                + IEEEAddress.toDottedHex(key.longValue())
//                                + " found", 2);
                        freshenRoute(key.longValue());
                        if (!newEntry.routeUsers.isEmpty()) {
                            // There's only one user in the new entry's list, so add that
                            Long user = (Long) newEntry.routeUsers.firstElement();
                            existingEntry.routeUsers.addElement(user);
//                            Debug.print("doTableAddition: added "
//                                    + IEEEAddress.toDottedHex(user.longValue())
//                                    + " to the users list", 2);
                        }
                    }
                } else {
                    if (newEntry.nextHopMACAddress.equals(existingEntry.nextHopMACAddress)) {
                        timeoutList.removeElement(existingEntry);
                        existingEntry.activityFlag = true;
                        existingEntry.expiryTime =
                                System.currentTimeMillis() + Constants.ACTIVE_ROUTE_TIMEOUT;
                        if (!newEntry.routeUsers.isEmpty()) {
                            // There's only one user in the new entry's list, so add that
                            Long user = (Long) newEntry.routeUsers.firstElement();
                            existingEntry.routeUsers.addElement(user);
//                            Debug.print("doTableAddition: added "
//                                    + IEEEAddress.toDottedHex(user.longValue())
//                                    + " to the users list", 2);
                        }
                        timeoutList.insertElement(existingEntry);
                    } else {
                        timeoutList.removeElement(existingEntry);
                        if (!existingEntry.routeUsers.isEmpty()) {
                            // Copy the users from the old list to the new list
                            copyUserList(existingEntry.routeUsers, newEntry.routeUsers);
                        }
                        timeoutList.insertElement(newEntry);
                        table.put(key, newEntry);
                    }
                }
            } else {
                timeoutList.insertElement(newEntry);
                table.put(key, newEntry);
//                Debug.print("doTableAddition: new entry added route for "
//                        + IEEEAddress.toDottedHex(key.longValue())
//                        + " through "
//                        + IEEEAddress.toDottedHex(newEntry.nextHopMACAddress.longValue())
//                        + " expires at " + newEntry.expiryTime, 2);
            }
        }
        notifyCleaner();
    }
    
    /**
     * When a routing entry in the table is replaced by a better one, then all
     * the users of the old route should use the new one from now on. Therefor
     * this method copies the user list of one entry to the the other entry
     * @param oldList
     * @param newList
     */
    private void copyUserList(Vector oldList, Vector newList) {
        Enumeration oldRouteUsers = oldList.elements();
        
        while (oldRouteUsers.hasMoreElements()) {
            Long routeUser = (Long) oldRouteUsers.nextElement();
            //Debug.print("copyUserList: added " + routeUser + " to the users list", 1);
            newList.addElement(routeUser);
        }
    }
    
    /**
     * increases the expiry time for a route that is specified by the destination
     * address
     * @param address destination address to be refreshed
     * @return true when finished
     */
    public boolean freshenRoute(long address) {
        Long wantedAddress = new Long(address);
        
        RoutingEntry entry;
        synchronized (table) {
            entry = (RoutingEntry) table.get(wantedAddress);
            
            if (entry == null) {
                Debug.print("[AODV] Attempt to freshen non-existant route to " + IEEEAddress.toDottedHex(address));
                return false;
            } else {
                timeoutList.removeElement(entry);                     
//                Do not turn this on for production -- performance hit on every packet sent
//                Debug.print("[AODV] freshen route route to " +
//                        IEEEAddress.toDottedHex(address) + " at " + System.currentTimeMillis(), 1);
                entry.activityFlag = true;
                entry.expiryTime =
                        System.currentTimeMillis() + Constants.ACTIVE_ROUTE_TIMEOUT;
                timeoutList.insertElement(entry);
            }
        }
        return true;
    }
    
    /**
     * removes a node from the users list of a route and sets the activity flag
     * to false
     * @param originator originator of the route request
     * @param destination destination address of route entry
     */
    public void deactivateRoute(long originator, long destination) {
        Long wantedAddress = new Long(destination);
        
        RoutingEntry entry;
        synchronized (table) {
            entry = (RoutingEntry) table.get(wantedAddress);
            
            if (entry != null) {
                Debug.print("[AODV] deactivatingRoute (remove entry): " + IEEEAddress.toDottedHex(entry.key.longValue()) +
                        " through " + IEEEAddress.toDottedHex(entry.nextHopMACAddress.longValue()) +
                        " user " + IEEEAddress.toDottedHex(originator) +
                        " at " + System.currentTimeMillis());
                timeoutList.removeElement(entry);
                table.remove(entry.key);
                // Set the destination's valid flag to false
//                entry.activityFlag = false;
//                entry.expiryTime = System.currentTimeMillis();
//                timeoutList.insertElement(entry);
                // Remove the originator from the route user's list
//                boolean entryRemoved =
//                        entry.routeUsers.removeElement(new Long(originator));
//                if (entryRemoved) {
//                    Debug.print("deactivateRoute: removed " + IEEEAddress.toDottedHex(originator)
//                    + " from the users list", 1);
//                }
            } else {
                Debug.print("[AODV] attempt to deactivate missing route to " + destination + " at " + System.currentTimeMillis(), 1);
            }
            
        }
    }
    
    /**
     * This method provides access to the entire routing table. It can be used to
     * monitor the state of the routing table by an application. CAUTION: As this
     * method accesses a syncronized object, it should not be called to often.
     * @return vector of all routing entries
     */
    public Vector getAllEntries(){
        Vector v = new Vector();
        Enumeration en = table.elements();
        while (en.hasMoreElements()){
            v.addElement(en.nextElement());
        }
        return v;
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
    
    public void dumpTable() {
        Vector v = getAllEntries();
        
        System.out.println("Dest\t\tNext Hop\t\tDist\tActive\tTimeout");
        for (int i = 0; i < v.size(); i++) {
            
            
            RoutingEntry re = (RoutingEntry)v.elementAt(i);
            System.out.println(IEEEAddress.toDottedHex(re.key.longValue()) + "\t"
                    + IEEEAddress.toDottedHex(re.nextHopMACAddress.longValue()) + "\t"
                    + re.hopCount + "\t"
                    + re.activityFlag +"\t"
                    + re.expiryTime);
        }
        
    }
    /**
     * sets our IEEE Address
     * @param ourAddress our IEEE Address as a long
     */
    public void setOurAddress(long ourAddress) {
        this.ourAddress = new Long(ourAddress);
    }
}
