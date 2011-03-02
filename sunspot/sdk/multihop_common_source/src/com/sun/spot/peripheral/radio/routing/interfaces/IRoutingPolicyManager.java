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

package com.sun.spot.peripheral.radio.routing.interfaces;

import com.sun.spot.peripheral.radio.routing.RoutingPolicy;

/**
 *
 * @author Pete St. Pierre
 * based on the RoutingPolicy and RoutingPolicyManager classes developed by Syntropy
 */
public interface IRoutingPolicyManager {
    
   
    
    /**
     * Notify the policy manager that a policy has changed
     * @param newPolicy the policy to start enforcing
     */
    void policyHasChanged(RoutingPolicy newPolicy);
    
    /**
     * return true if this routing policy requires us not to go to sleep
     * @return true if we always route packets
     */
    boolean routeAlways();
    
    /**
     * return true if we are only a route consumer
     * @return true if we are only a routing consumer
     */
    boolean isEndNode();
    
    /**
     * 
     * return true if we allow deepSleep
     * @return true if we do not disable deep sleep
     */
    public boolean maySleep();
    
    /**
     * 
     * return true if we pass broadcasts without decrementing hop count
     * @return true if we pass broadcasts without decrementing hop count
     */
    public boolean bridgeBroadcasts();    
}

