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

package com.sun.spot.peripheral.radio.mhrp.lqrp.routing;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import com.sun.spot.peripheral.radio.mhrp.lqrp.linkParams.ConfigLinkParams;
import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.peripheral.radio.routing.RouteInfo;
import com.sun.spot.peripheral.radio.routing.SortedList;
import com.sun.spot.peripheral.radio.mhrp.lqrp.Constants;
import com.sun.spot.peripheral.radio.mhrp.lqrp.messages.RREP;
import com.sun.spot.peripheral.radio.mhrp.lqrp.messages.RREQ;
import com.sun.spot.util.Debug;
import com.sun.spot.util.IEEEAddress;
import com.sun.squawk.util.MathUtils;

/**
 * An object that represents a routing table for the mesh
 * @author Allen Ajit George, Jochen Furtmueller, modifications by Pradip De, Pete St. Pierre & Ron Goldman
 * @version 0.1
 */
public class RoutingTable {
    
    private final Hashtable table;
    private long ourAddress;
    private static RoutingTable instance;
    
    /**
     * construct a new routing table
     */
    private RoutingTable() {
        table = new Hashtable();
    }
    
    public void start() {
    }
    
    public void stop() {
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
     * Returns the route entry for a given destination.
     * Note: now checks if route has expired.
     *
     * @param address for which a route is wanted
     * @return routeEntry in Routing table, if present, or null
     */
    public RoutingEntry getEntry(long address) {
        synchronized (table) {
            RoutingEntry entry = (RoutingEntry) table.get(new Long(address));
            if (entry != null) {
                // check if entry has expired
                long now = System.currentTimeMillis();
                if (entry.expiryTime <= now) {
                    if ((entry.expiryTime + Constants.DELETE_PERIOD) <= now) {
                        entry.activityFlag = false;
                        Debug.print("[LQRP] clear flag for route to " +
                                IEEEAddress.toDottedHex(address) +
                                " through " + IEEEAddress.toDottedHex(entry.nextHopMACAddress) +
                                " at " + now);
                    } else {
                        Debug.print("[LQRP] remove entry route to   " +
                                IEEEAddress.toDottedHex(address) +
                                " through " + IEEEAddress.toDottedHex(entry.nextHopMACAddress) +
                                " at " + now);
                        table.remove(entry);
                        entry = null;
                    }
                }
            }
            return entry;
        }
    }

    /**
     * returns an object that describes the route for a given destination
     *
     * @param address for which a route info is wanted
     * @return routeInfo that has a valid next hop field if there was a route in
     * the table, or a invalid next hop field if there was not
     */
    public RouteInfo getNextHopInfo(long address) {
        RoutingEntry returnedEntry = getEntry(address);
        
        // FIXME - Is this necessarily correct?
        if (returnedEntry != null && returnedEntry.activityFlag) {
            freshenRoute(returnedEntry);
            return new RouteInfo(address, returnedEntry.nextHopMACAddress, returnedEntry.hopCount);
        } else {
            if (returnedEntry == null) {
                Debug.print("[LQRP] no route found for: " + IEEEAddress.toDottedHex(address) +
                        " at " + System.currentTimeMillis());
                return new RouteInfo(address, Constants.INVALID_NEXT_HOP, 0);
            } else {
                Debug.print("[LQRP] reactivate inactive route found for: " + IEEEAddress.toDottedHex(address) +
                        " through " + IEEEAddress.toDottedHex(returnedEntry.nextHopMACAddress) +
                        " hops " + returnedEntry.hopCount +
                        " at " + System.currentTimeMillis());
                freshenRoute(returnedEntry);
                return new RouteInfo(address, returnedEntry.nextHopMACAddress, returnedEntry.hopCount);
            }
        }
    }
    
    /**
     * Create a new route entry using the parameters, then call doTableAddition()
     * @param address destination address of route entry
     * @param nextHopMACAddress address of next hop to this address
     * @param hopCount number of hops to destination
     * @param routeCost LQRP route cost mechanism
     * @param destSeqNumber LQRP sequence number
     */
    public void addRoute(long address, long nextHopMACAddress, int hopCount,
            double routeCost, int lowLQlinkCount, int destSeqNumber) {
        Debug.print("addRoute: adding route for "
                + IEEEAddress.toDottedHex(address)
                + " next hop " + IEEEAddress.toDottedHex(nextHopMACAddress)
                + " hops " + hopCount
                + " at " + System.currentTimeMillis());
        
        RoutingEntry entry = new RoutingEntry();
        
        entry.key = new Long(address);
        entry.nextHopMACAddress = nextHopMACAddress;
        entry.hopCount = hopCount;
        entry.routeCost = routeCost;
        entry.lowLQlinkCount = lowLQlinkCount;
        entry.sequenceNumber = destSeqNumber;
        entry.activityFlag = true;
        doTableAddition(entry);
    }
    
    /**
     * Create a new route entry based on a received route request, then call doTableAddition()
     * @param senderMACAddress mac address of the requestor
     * @param message route request message received
     */
    public void addRoute(long senderMACAddress, RREQ message) {
        long wantedOriginator = message.getOrigAddress();
        Debug.print("addRoute (RREQ): adding back route for " + IEEEAddress.toDottedHex(message.getOrigAddress())
        + " asked about " + IEEEAddress.toDottedHex(message.getDestAddress())
        + " heard from " + IEEEAddress.toDottedHex(senderMACAddress)
        + " hops " + message.getHopCount()
        + " at " + System.currentTimeMillis());
        if (wantedOriginator != ourAddress) {
            // Create an entry for the originator of the RREQ message
            RoutingEntry entry = new RoutingEntry();
            
            entry.key = new Long(wantedOriginator);
            entry.nextHopMACAddress = senderMACAddress;
            entry.hopCount = message.getHopCount();
            //Assume symmetric link for setting the route cost to the originator
            entry.routeCost = message.getRouteCost();//message.getFwdRouteCost();
            entry.lowLQlinkCount = message.getLowLQlinkCount();
            
            entry.sequenceNumber = message.getOrigSeqNum();
            entry.activityFlag = true;

            doTableAddition(entry);
        }
    }
    /**
     * Create a new route entry based on a received route reply, then call doTableAddition()
     * @param senderMACAddress sender of the route response
     * @param message route response message received
     */
    public void addRoute(long senderMACAddress, RREP message) {
        long wantedDestination = message.getDestAddress();
        Debug.print("addRoute (RREP): adding route for "
                + IEEEAddress.toDottedHex(wantedDestination)
                + " through " + IEEEAddress.toDottedHex(senderMACAddress)
                + " hops " + message.getHopCount()
                + " at " + System.currentTimeMillis());
        
        // Create an entry for the originator of the RREP message
        RoutingEntry entry = new RoutingEntry();
        
        entry.key = new Long(wantedDestination);
        entry.nextHopMACAddress = senderMACAddress;
        entry.hopCount = message.getHopCount();
        //setup the route to the destination from the updated route cost as recvd in the RREP
        entry.routeCost = message.getRouteCostToDest(); // used to divide it by next line
//                MathUtils.pow((double)ConfigLinkParams.LOW_LQI_PENALTY_FACTOR, message.getLowLQlinkCount());
        entry.lowLQlinkCount = message.getLowLQlinkCount();
        
        entry.sequenceNumber = message.getDestSeqNum();
        entry.activityFlag = true;
        entry.routeUsers.addElement(new Long(message.getOrigAddress()));
        
        doTableAddition(entry);
    }
    
    /**
     * get the destination sequence number for a certain entry
     * @param address address of the entry that we are interested in
     * @return destinationSequenceNumber
     */
    public int getDestinationSequenceNumber(long address) {        
        RoutingEntry entry = getEntry(address);
        if (entry != null) {
            return entry.sequenceNumber;
        }
        return Constants.UNKNOWN_SEQUENCE_NUMBER;
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
    private void doTableAddition(RoutingEntry newEntry) {
        synchronized (table) {
            newEntry.expiryTime = System.currentTimeMillis() + Constants.ACTIVE_ROUTE_TIMEOUT;
            RoutingEntry existingEntry = getEntry(newEntry.key.longValue());
            if (existingEntry != null) {
                if (existingEntry.activityFlag) {
//                    Debug.print("doTableAddition: existing hop count: "
//                            + existingEntry.hopCount + " new hop count: "
//                            + newEntry.hopCount, 2);
                    if (existingEntry.lowLQlinkCount > newEntry.lowLQlinkCount ||
                            (existingEntry.lowLQlinkCount == newEntry.lowLQlinkCount &&
                             existingEntry.routeCost > newEntry.routeCost))  {
//                        Debug.print("doTableAddition: replacing old entry", 2);
                        if (!existingEntry.routeUsers.isEmpty()) {
                            // Copy the users from the old list to the new list
                            copyUserList(existingEntry.routeUsers, newEntry.routeUsers);
                        }
                        table.put(newEntry.key, newEntry);
//                        Debug.print("doTableAddition: added route for "
//                                + IEEEAddress.toDottedHex(key.longValue())
//                                + " through "
//                                + IEEEAddress.toDottedHex(newEntry.nextHopMACAddress), 2);
                    } else {
//                        Debug.print("doTableAddition: existing route for "
//                                + IEEEAddress.toDottedHex(key.longValue()) + " found", 2);
                        existingEntry.expiryTime = newEntry.expiryTime;
                        if (!newEntry.routeUsers.isEmpty()) {
                            // There's only one user in the new entry's list, so add that
                            Long user = (Long) newEntry.routeUsers.firstElement();
                            existingEntry.routeUsers.addElement(user);
//                            Debug.print("doTableAddition: added "
//                                    + IEEEAddress.toDottedHex(user.longValue())
//                                    + " to the users list", 2);
                        }
                    }
                } else {    // just replace deactivated existing entry
                    if (!existingEntry.routeUsers.isEmpty()) {
                        // Copy the users from the old list to the new list
                        copyUserList(existingEntry.routeUsers, newEntry.routeUsers);
                    }
                    table.put(newEntry.key, newEntry);
                }
            } else {
                table.put(newEntry.key, newEntry);
//                Debug.print("doTableAddition: new entry added route for "
//                        + IEEEAddress.toDottedHex(key.longValue()) + " through "
//                        + IEEEAddress.toDottedHex(newEntry.nextHopMACAddress)
//                        + " expires at " + newEntry.expiryTime, 2);
            }
        }
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
        RoutingEntry entry = getEntry(address);
        if (entry == null) {
            Debug.print("[LQRP] Attempt to freshen non-existant route to " + IEEEAddress.toDottedHex(address));
            return false;
        } else {
            freshenRoute(entry);
            return true;
        }
    }

    private void freshenRoute(RoutingEntry entry) {
        entry.activityFlag = true;
        entry.expiryTime = System.currentTimeMillis() + Constants.ACTIVE_ROUTE_TIMEOUT;
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
                Debug.print("[LQRP] deactivatingRoute (remove entry): " + IEEEAddress.toDottedHex(entry.key.longValue()) +
                        " through " + IEEEAddress.toDottedHex(entry.nextHopMACAddress) +
                        " user " + IEEEAddress.toDottedHex(originator) +
                        " at " + System.currentTimeMillis());
                table.remove(entry.key);
                // Remove the originator from the route user's list
//                boolean entryRemoved =
//                        entry.routeUsers.removeElement(new Long(originator));
//                if (entryRemoved) {
//                    Debug.print("deactivateRoute: removed " + IEEEAddress.toDottedHex(originator)
//                    + " from the users list", 1);
//                }
            } else {
                Debug.print("[LQRP] attempt to deactivate missing route to " + destination + " at " + System.currentTimeMillis(), 1);
            }
            
        }
    }

    /**
     * removes all routes starting with given node
     *
     * @param nextHop starting point for routes to remove
     */
    public void deactivateRoutesUsing(long nextHop) {
        synchronized (table) {
            Enumeration en = table.elements();
            while (en.hasMoreElements()) {
                RoutingEntry entry = (RoutingEntry)en.nextElement();
                if (entry.nextHopMACAddress == nextHop) {
                    Debug.print("[LQRP] deactivatingRoute: " + IEEEAddress.toDottedHex(entry.key.longValue()) +
                            " through " + IEEEAddress.toDottedHex(entry.nextHopMACAddress));
                    table.remove(entry.key);
                }
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
            
    public void dumpTable() {
        Vector v = getAllEntries();
        System.out.println("Dest\t\tNext Hop\t\tDist\tCost\tActive\tTimeout");
        for (int i = 0; i < v.size(); i++) {
            RoutingEntry re = (RoutingEntry)v.elementAt(i);
            Debug.print(IEEEAddress.toDottedHex(re.key.longValue()) + "\t"
                    + IEEEAddress.toDottedHex(re.nextHopMACAddress) + "\t"
                    + re.hopCount + "\t"
                    + re.routeCost + "\t"
                    + re.activityFlag +"\t"
                    + re.expiryTime);
        }
        
    }
    /**
     * sets our IEEE Address
     * @param ourAddress our IEEE Address as a long
     */
    public void setOurAddress(long ourAddress) {
        this.ourAddress = ourAddress;
    }
}
