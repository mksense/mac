/*
 * Copyright 2008-2009 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.spot.service;

import com.sun.spot.peripheral.radio.routing.RouteInfo;
import javax.microedition.io.Datagram;

/**
 * Interface for a generic SPOT Radio Manager service.
 *
 * @author Ron Goldman
 */
public interface ISpotRadioHelper extends IService {

    /**
     * Tell others the Datagram protocol name for use with GCF.
     *
     * @return Datagram protocol name for use with GCF
     */
    public String getDatagramConnectionProtocol();

    /**
     * Tell others the Stream protocol name for use with GCF.
     *
     * @return Stream protocol name for use with GCF
     */
    public String getStreamConnectionProtocol();

    /**
     * Returns the link quality for the received datagram. 
     * Values should range from 0 (unreadable) to 100 (perfect).
     * If transport layer does not have a link quality metric then
     * return -1.
     * 
     * @param dg Datagram
     * @return link quality or -1
     */
    int getLinkQuality(Datagram dg);

    /**
     * Returns the link strength for the received datagram.
     * Values should range from 0 (weak) to 100 (strong).
     * If transport layer does not have a link strength metric then
     * return -1.
     *
     * @param dg Datagram
     * @return link strength or -1
     */
    int getLinkStrength(Datagram dg);

    /**
     * Return the time that a Datagram packet was actually received/sent.
     *
     * @param dg the Datagram to check
     * @return time that the Datagram packet was actually received/sent

     */
    long getTimestamp(Datagram dg);

    /**
     * Tell if datagram was sent as a broadcast message.
     *
     * @param dg the Datagram to check
     * @return true if the Datagram had been broadcast
     */
    boolean isBroadcast(Datagram dg);

    /**
     * Return the name of the current Routing Manager.
     *
     * @return the name of the current Routing Manager
     */
    String getRoutingManagerName();

    /**
     * Return information on the current route(s) to the specified destination address.
     *
     * @param address the specified destination address
     * @return the current route(s) to the specified destination address
     */
    RouteInfo[] getRouteInfo(long address);

    /**
     * Return an array of the current route info to all known addresses.
     *
     * @return the current route info to all known addresses
     */
    RouteInfo[] getRouteInfo();

    /**
     * Check whether this Routing Manager allows writing to the Routing Table.
     *
     * @return true if Routing Table is mutable
     */
    boolean isMutableRoutingManager();

    // to be determined later:
    //    methods to set the route table

    /*

    void setRoute(long destination, long nextHop, int hopCount);

    void setRoutingTable(String table);

    void clearRoutingTable();

    void saveRoutingTable();

     */

}
