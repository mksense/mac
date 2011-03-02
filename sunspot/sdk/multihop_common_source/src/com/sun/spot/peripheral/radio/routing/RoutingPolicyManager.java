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

package com.sun.spot.peripheral.radio.routing;

import com.sun.spot.peripheral.Spot;
import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.peripheral.radio.routing.interfaces.IRoutingPolicyManager;
import com.sun.spot.service.BasicService;
import com.sun.spot.service.ServiceRegistry;
import com.sun.spot.util.Debug;

/**
 * The onject that oversees the routing policy for this node
 * @author pete
 */
public class RoutingPolicyManager extends BasicService implements IRoutingPolicyManager {
    private static RoutingPolicyManager rpm;
    
    private static final String ROUTING_POLICY_PROPERTY = "spot.mesh.routing.enable";
    private static RoutingPolicy defaultPolicy = new RoutingPolicy(RoutingPolicy.IFAWAKE);
    // Keep old sleep state.  If we exit "route always" mode, we restore the old state
    private static boolean oldSleepMode;
    
    private RoutingPolicy active;
    
    /** Creates a new instance of RoutingPolicyManager */
    private RoutingPolicyManager() {
        active = new RoutingPolicy(RoutingPolicy.UNDEFINED);
        if (!RadioFactory.isRunningOnHost())
           oldSleepMode = RadioFactory.getSleepManager().isDeepSleepEnabled();
        RoutingPolicy startup = defaultPolicy;
        Debug.print("[RoutingPolicyManager] Starting Routing Policy Manager");
        String mode = System.getProperty(ROUTING_POLICY_PROPERTY);
        if (mode == null || "true".equalsIgnoreCase(mode))
            startup = defaultPolicy;
        if ("endnode".equalsIgnoreCase(mode) || "false".equalsIgnoreCase(mode))
            startup = new RoutingPolicy(RoutingPolicy.ENDNODE);            
        if ("always".equalsIgnoreCase(mode))
            startup = new RoutingPolicy(RoutingPolicy.ALWAYS);
        if ("ifawake".equalsIgnoreCase(mode))
            startup = new RoutingPolicy(RoutingPolicy.IFAWAKE);
 
        policyHasChanged(startup);
    }

    /**
     * Return a singleton for the RoutingPolicyManager
     * @return the singleton that manages routing policy
     */
    public synchronized static IRoutingPolicyManager getInstance() {
        if (rpm == null) {
            rpm = (RoutingPolicyManager) ServiceRegistry.getInstance().lookup(RoutingPolicyManager.class);
            if (rpm == null) {
                rpm = new RoutingPolicyManager();
                ServiceRegistry.getInstance().add(rpm);
            }
        }
        return rpm;
    }
    
    public String getServiceName() {
        return "RoutingPolicyManager";
    }

    /**
     * Notify the routing subsystem of a new routing policy
     * @param newPolicy policy that should now be in effect
     */
    public void policyHasChanged(RoutingPolicy newPolicy) {
        if (active.equals(newPolicy)) return;  // Policy did not change
        active = newPolicy;
        Debug.print("[rpm] Routing policy now: " + toString());               
        if (!RadioFactory.isRunningOnHost()) {
            if (routeAlways()) {
                // We need to turn off the sleep manager
                oldSleepMode = RadioFactory.getSleepManager().isDeepSleepEnabled();
                Spot.getInstance().getSleepManager().disableDeepSleep();
            } else {
                Spot.getInstance().getSleepManager().enableDeepSleep(oldSleepMode);
            }
            Debug.print("[rpm] Old Sleep policy: " + oldSleepMode);
            Debug.print("[rpm] New Sleep policy: " + Spot.getInstance().getSleepManager().isDeepSleepEnabled());
        }
    }

    /**
     * create a string representation of this object
     * @return the routing policy as a string description
     */
    public String toString() {
        
        return active.toString();
    }
    
    /**
     * determine whether this routing policy allows deepSleep
     * @return true if the deepSleep is not disabled
     */
    public boolean maySleep() {
        return (! (active.equals(RoutingPolicy.ALWAYS) || active.equals(RoutingPolicy.SHAREDBASESTATION)));
    }

    /**
     * determine if we always pass packets
     * @return true if we pass packets for other nodes
     */
    public boolean routeAlways() {
        return (active.equals(RoutingPolicy.ALWAYS) || active.equals(RoutingPolicy.SHAREDBASESTATION));
    }

    /**
     * determine if we are only a routing consumer
     * @return true if we only generate routing requests for ourself
     */
    public boolean isEndNode() {
        return (active.equals(RoutingPolicy.ENDNODE));
    }
       
    public boolean bridgeBroadcasts() {
        return (active.equals(RoutingPolicy.SHAREDBASESTATION));
    }
}
