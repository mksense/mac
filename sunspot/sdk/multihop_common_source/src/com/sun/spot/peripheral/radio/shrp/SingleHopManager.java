/*
 * Copyright 2005-2008 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.spot.peripheral.radio.shrp;
import com.sun.spot.peripheral.radio.ILowPan;
import com.sun.spot.peripheral.radio.mhrp.aodv.routing.RoutingEntry;
import com.sun.spot.peripheral.radio.mhrp.interfaces.IMHEventListener;
import com.sun.spot.peripheral.radio.routing.RouteInfo;
import com.sun.spot.peripheral.radio.routing.RouteTable;
import com.sun.spot.peripheral.radio.routing.interfaces.IRoutingManager;
import com.sun.spot.peripheral.radio.routing.interfaces.RouteEventClient;
import com.sun.spot.service.IService;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;


/**
 *
 * The single hop routing modules makes all nodes look like they are 1 hop away.
 * If a route is invalidated 3 times, we throw a noRouteException, meaning a node
 * is probably not actually 1 hop away.
 *
 * Since A RouteInfo object is never freed we must create one the first time it is
 * requested.  We then store that reference in a Hashtable.  The next time it is requested,
 * we returned the caches RouteInfo object.  The intention is that this will minimize
 * garbage collection, as well as serve as a sample for writing simple routing managers.
 *
 * @author Pete St. Pierre
 */



public class SingleHopManager implements IRoutingManager {
    private int state;
    private static String name = "SingleHopManager";
    private static SingleHopManager instance;
    private static long ourAddress;
    private static ILowPan lowpan;
    private Hashtable routes;
    private Hashtable invalidRoutes;
    
    private static final int MAX_FAILURES = 3;
    /**
     * Creates a new instance of SingleHopManager
     * The SingleHopManager can be used by the LowPan layer to limit
     * communications to nodes within a single radio hop away.
     */
    public SingleHopManager() {
        routes = new Hashtable();
        invalidRoutes = new Hashtable();
        
    }
    /**
     * @return SingleHopManager instance of this singleton
     */
    public static synchronized SingleHopManager getInstance() {
        if (instance == null) {
            instance = new SingleHopManager();
        }
        
        return instance;
    }
    /**
     * This method returns a snapshot of the routing table
     * @return an object containing a snapshot of the routing table
     */
    public RouteTable getRoutingTable() {
        RouteTable rt = new RouteTable();
        Enumeration en = routes.elements();
        while (en.hasMoreElements()){
            rt.addEntry((RouteInfo)en.nextElement());
        }
        
        return rt;
    }
    
    /**
     * lookup a route to this address.  The answer will always be that the destination address
     * is exactly 1 hop away
     * @param address the destination address we are looking for
     * @param eventClient client to be called back with the routing information
     * @param uniqueKey a key that uniquely identifies this request/client
     * @return always returns success
     */
    public synchronized boolean findRoute(long address, RouteEventClient eventClient, Object uniqueKey) {
        Integer fails = (Integer)invalidRoutes.get(new Long(address));
        RouteInfo info = (RouteInfo)routes.get(new Long(address));
        
        if (fails != null) {
            if (fails.intValue() >= MAX_FAILURES) {
//                System.out.println("Return an invalid route: fails=" + fails.toString());
                info = new RouteInfo(address, -1, 0); // the answer is an invalid route
                invalidRoutes.remove(new Long(address)); // reset count
                eventClient.routeFound(info, uniqueKey);
                return true;
            }
        }
        
        if (info == null)  {
            info = new RouteInfo(address, address, 1); // the answer is directly 1 hop away
            routes.put(new Long(address), info);
        }
        eventClient.routeFound(info, uniqueKey);
        return true;
    }
    
    /**
     * registers a listener for routing messages.  Single hop has no messages so this
     * is a NO OP.
     * @param listener event listener for callbacks
     */
    public void registerEventListener(IMHEventListener listener) {
        // Routes don't change - so we ignore this
    }
    
    /**
     * deregisters a listener for routing messages.  Single hop has no messages so this
     * is a NO OP.
     * @param listener the listener to remove
     */
    public void deregisterEventListener(IMHEventListener listener) {
        // Routes don't change - so we ignore this
    }
    
    /**
     * Registers an event listener that is notified when this node
     * initiates/receives supported route events
     *
     * @param listener object that is notified when route events occur
     */
    public void addEventListener(IMHEventListener listener) {
        // Routes don't change - so we ignore this
    }

    /**
     * Remove the specified event listener that was registered for route events
     *
     * @param listener object that is notified when route events occur
     */
    public void removeEventListener(IMHEventListener listener) {
        // Routes don't change - so we ignore this
    }

    /**
     * Nodes are always 1 hop away.  Mark a node unreachable if it isn't
     * @param originator node that requested the route
     * @param destination route destination address
     * @return always returns true
     */
    public synchronized boolean invalidateRoute(long originator, long destination) {
        
//      System.out.println("Mark route invalid");
        Long key = new Long(destination);
        routes.remove(key);
        Integer val = (Integer)invalidRoutes.get(key);
        if (val == null) {
            invalidRoutes.put(key, new Integer(1));
        } else {
            invalidRoutes.put(key, new Integer(val.intValue()+1));
        }
        
        // routes.remove(new Long(destination));
        return true;
    }
    
    /**
     * setup this routing manager for use.  No state needs to be initialized for
     * the single hop routing mananger
     * @param ourAddress address used by this LowPan layer
     * @param lowPanLayer a reference to the lowpan layer
     */
    public synchronized void initialize(long ourAddress, ILowPan lowPanLayer) {
        this.ourAddress = ourAddress;
        this.lowpan = lowPanLayer;
    }
    
    /**
     * retrieve routing information for a destination address.  Answer is always that the
     * address is one hop away.
     * @param address destination address of route
     * @return returns a route info object where nexthop is the destination and is 1 hop away
     */
    public synchronized RouteInfo getRouteInfo(long address) {
        RouteInfo info = (RouteInfo)routes.get(new Long(address));
        if (info == null)  {
            info = new RouteInfo(address, address, 1); // the answer is directly 1 hop away
            routes.put(new Long(address), info);
        }
        return info;
    }
    
    public void setServiceName(String who) {
        name = who;
    }
    
    public void setEnabled(boolean enable) {
    }
    
    public boolean stop() {
        state = IService.STOPPED;
        return true;
    }
    
    public boolean start() {
        state = IService.RUNNING;
        return true;
    }
    
    public boolean resume() {
        return start();
    }
    
    public boolean pause() {
        return stop();
    }
    
    public boolean isRunning() {
        return (state == IService.RUNNING);
    }
    
    public int getStatus() {
        return state;
    }
    
    public String getServiceName() {
        return name;
    }
    
    public boolean getEnabled() {
        return false;
    }
    
}
