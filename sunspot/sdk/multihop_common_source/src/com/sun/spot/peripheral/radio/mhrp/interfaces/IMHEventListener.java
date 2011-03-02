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

package com.sun.spot.peripheral.radio.mhrp.interfaces;

/**
 * Interface implemented by classes that want to receive notifications when the
 * node initiates a routing event
 *
 * @author Pete St. Pierre
 * @version 0.1
 */
public interface IMHEventListener {

    /**
     * Method called when a RREQ is sent
     *
     * @param originator Route Request originator
     * @param destination Route Request destination/target
     * @param hopCount  number of hops to the destination
     */

    public void RREQSent(long originator, long destination, int hopCount);

    /**
     * Method called when a RREP is sent
     *
     * @param originator Route Request originator
     * @param destination Route Request destination/target
     * @param hopCount  number of hops to the destination
     */
    public void RREPSent(long originator, long destination, int hopCount);
					      
    /**
     * Method called when a RERR is sent
     *
     * @param originator Route Request originator
     * @param destination Route Request destination/target
     */
    public void RERRSent(long originator, long destination);

    /**
     * Method called when a RREQ is received
     *
     * @param originator Route Request originator
     * @param destination Route Request destination/target
     * @param hopCount  number of hops to the destination
     */

    public void RREQReceived(long originator, long destination, int hopCount);

    /**
     * Method called when a RREP is received
     *
     * @param originator Route Request originator
     * @param destination Route Request destination/target
     * @param hopCount  number of hops to the destination
     */
    public void RREPReceived(long originator, long destination, int hopCount);
					      
    /**
     * Method called when a RERR is received
     *
     * @param originator Route Request originator
     * @param destination Route Request destination/target
     */
    public void RERRReceived(long originator, long destination);
    
}
