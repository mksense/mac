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

package com.sun.spot.peripheral.radio.routing;

/**
 * An object that describes the basic routing policy
 */
public class RoutingPolicy {
    
    /**
     * The policy is a routing consumer only
     */
    public static final int UNDEFINED = 0x00;
    /**
     * The policy is a routing consumer only
     */
    public static final int ENDNODE = 0x01;
    /**
     * The node routes all packets and disables deepSleep
     */
    public static final int ALWAYS = 0x02;
    /**
     * we route all packets, but only if we are awake
     */
    public static final int IFAWAKE = 0x03;
    /**
     * we are a shared basestation - route always, but also pass broadcasts without decrementing hopcount
     */
    public static final int SHAREDBASESTATION = 0x04; 
    
    private int policy;
    
    /**
     * create a routing policy object
     * @param state the type of routing this policy object represents
     */
    public RoutingPolicy(int state) {
        this.policy = state;
    }
    
    /**
     * create a string representation of this routing policy
     * @return a string description of the routing policy
     */
    public String toString() {
        switch (policy) {
            case ENDNODE:
                return "ENDNODE";
            case ALWAYS:
                return "ALWAYS";
            case IFAWAKE:
                return "IFAWAKE";
            case SHAREDBASESTATION:
                return "SHAREDBASESTATION";
                
            default:
                return "undefined";                
        }       
    }
      
    
    /**
     * compare this policy to a numerical equivilent
     * @param val a numerical equivilent for a policy
     * @return true if the policy matches
     */
    public boolean equals(int val) {
        return (this.policy == val);
    }
    
    /**
     * compare two policy objects
     * @param rp the policy to compare this with
     * @return true if the two policies match
     */
    public boolean equals(RoutingPolicy rp) {
        return (this.policy == rp.policy);
    }
}
