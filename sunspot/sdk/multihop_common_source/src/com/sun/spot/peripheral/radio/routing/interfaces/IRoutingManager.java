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

package com.sun.spot.peripheral.radio.routing.interfaces;

import com.sun.spot.peripheral.NoRouteException;
import com.sun.spot.peripheral.radio.ILowPan;
import com.sun.spot.peripheral.radio.routing.RouteInfo;
import com.sun.spot.peripheral.radio.mhrp.interfaces.IMHEventListener;
import com.sun.spot.peripheral.radio.routing.RouteTable;
import com.sun.spot.service.IService;

/**
 * This interface defines a routing manager
 * @author Allen Ajit George
 * @version 0.1
 */
public interface IRoutingManager extends IService {
    /**
     * for the initialization the routing manager must know a low pan instance
     * to interoperate with
     * @param ourAddress
     * @param lowPanLayer
     */
    public void initialize(long ourAddress, ILowPan lowPanLayer);
    /**
     * this method can be called to obtain a route info for an address
     * @param address
     */
    public RouteInfo getRouteInfo(long address);
    /**
     * This method triggers a new route request. 
     * Note: the radio must be on or no route will be found.
     */
    public boolean findRoute(long address, RouteEventClient eventClient, Object uniqueKey) 
    throws NoRouteException ;
    /**
     * returns a copy of the routing table
     */
    public RouteTable getRoutingTable();
    /**
     * invalidate a route that is reported to be broken
     */
    public boolean invalidateRoute(long originator, long destination);
 
    /**
     * Registers an application etc. that is notified when this node
     * initiates/receives supported route events
     *
     * @param listener object that is notified when route events occur
     * @deprecated use addEventListener()
     */
    public void registerEventListener(IMHEventListener listener);

    /**
     * Deregisters an application etc. that was registered for route events
     *
     * @param listener object that is notified when route events occur
     * @deprecated use removeEventListener()
     */
    public void deregisterEventListener(IMHEventListener listener);

    /**
     * Registers an event listener that is notified when this node
     * initiates/receives supported route events
     *
     * @param listener object that is notified when route events occur
     */
    public void addEventListener(IMHEventListener listener);

    /**
     * Remove the specified event listener that was registered for route events
     *
     * @param listener object that is notified when route events occur
     */
    public void removeEventListener(IMHEventListener listener);

}
