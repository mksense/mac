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

package com.sun.spot.peripheral.radio;

/**
 * Interface implemented by classes that want to receive notifications when the
 * node initiates a routing event
 *
 * @author Allen Ajit George, Jochen Furthmueller
 * @version 0.1
 */
public interface IRouteEventListener {
    /**
     * Method that is called whenever the node initiates a route request
     *
     * @param destination final destination for which a route needs to be found
     */
    public void routeRequestMade(long destination);
    
    /**
     * Method that is called whenever the node receives a response to a route
     * request sent out
     *
     * @param destination final destination for which a route needs to be found
     * @param hopCount  number of hops to the destination
     * @param success indicates whether a path was found (true indicates success,
     * false indicates failure)
     *
     */
    public void routeResponseReceived(long destination, int hopCount,
            boolean success);
}
