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
 * Applications that implement this interface are notified when their physical 
 * node is used to forward mesh packets
 *
 * @author Allen Ajit George
 * @version 0.1
 */
public interface IDataEventListener {
    /**
     * Method that is called when data is forwarded through this physical node
     *
     * @param lastHop previous node on the multi hop path from which the packet 
     * was received
     * @param nextHop next node on the multi hop path to which the packet is sent
     * @param originator original sender of the packet
     * @param destination final destination of the packet
     */
    public void notifyForward(long lastHop, long nextHop, long originator, 
            long destination);
}
