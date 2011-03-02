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
 * Implementors of this interface handle traffic relating to a particular protocol.
 * <br><br>
 * The separate class {@link com.sun.spot.peripheral.radio.LowPan} multiplexes 
 * traffic for up to 255 protocols over the radio connection between two Spots or
 * between a host application and a Spot. For more information about the overall
 * framework for implementing protocols, see that class.<br><br>
 *
 * To handle incoming traffic for a protocol you need an object that implements
 * {@link com.sun.spot.peripheral.radio.IProtocolManager}. This object can 
 * be registered to receive traffic:<br><br>
 * <code>
 * ...<br>
 * LowPan.getInstance().registerProtocol(MY_PROTOCOL_NUMBER, pm, ..., ,...);<br>
 * ...<br>
 * </code><br>
 * and similarly deregistered<br><br>
 * <code>
 * ...<br>
 * LowPan.getInstance().deregisterProtocol(MY_PROTOCOL_NUMBER);<br>
 * ...<br>
 * </code><br>
 * There should only be one registered protocol manager for each unique protocol
 * number.<br><br>
 *
 * {@link com.sun.spot.peripheral.radio.RadiogramProtocolManager} is an example 
 * implementation. Note that although these protocols are implemented 
 * as Protocols in the sense implied by the GCF framework, this is not required.
 * The word protocol in "IProtocolManager" has a different meaning to that
 * in "GCF Protocol".<br>
 *
 * @author Allen Ajit George, Jochen Furthmueller, Pete St. Pierre
 * @version 2.0
 *
 */
public interface IProtocolManager {
    
    /**
     * Called whenever data is received that is addressed to this 
     * {@link com.sun.spot.peripheral.radio.IProtocolManager}.
     *
     * IProtocolManagers should do as little processing as possible inside 
     * this method, to avoid blocking other INewProtocolManagers from receiving 
     * their packets. Typically an IProtocolManager will queue received 
     * packets for later dispatch to applications.
     *
     */
    void processIncomingData( byte[] payload, 
            LowPanHeaderInfo headerInfo);
}
