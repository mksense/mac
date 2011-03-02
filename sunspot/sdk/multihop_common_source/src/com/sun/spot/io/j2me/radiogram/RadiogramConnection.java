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

package com.sun.spot.io.j2me.radiogram;

import javax.microedition.io.DatagramConnection;

import com.sun.spot.peripheral.IRadioControl;
import com.sun.spot.peripheral.ITimeoutableConnection;
import com.sun.spot.peripheral.IMultipleHopConnection;

/**
 * This interface defines the "radiogram" protocol - the radiogram protocol is a datagram-based
 * protocol that allows the exchange of packets between two devices.<br><br>
 * 
 * IMPORTANT This protocol is provided for test purposes and to allow creation of simple
 * demonstrations. It is NOT designed to be used as the base for higher level or more complex
 * protocols. If you want to create something more sophisticated write a new protocol that
 * calls the {@link com.sun.spot.peripheral.radio.RadioPacketDispatcher} or {@link com.sun.spot.peripheral.radio.LowPan}
 * directly. See {@link com.sun.spot.peripheral.radio.IProtocolManager} for more information.<br><br>
 * 
 * To establish a point-to-point connection both ends must open connections specifying the same portNo
 * and corresponding IEEE addresses.
 * <br>
 * Port numbers between 0 and 31 are reserved for system services.  Use by applications may result in conflicts.
 * <br>
 * 
 * Once the connection has been opened, each end can send and receive data using a datagram created on that connection, eg:<br>
 * <br>
 * <code>
 * ...<br>
 * DatagramConnection conn = (DatagramConnection) Connector.open("radiogram://" + targetIEEEAddress + ":10");<br>
 * Datagram dg = conn.newDatagram(conn.getMaximumLength());<br>
 * dg.writeUTF("My message");<br>
 * conn.send(dg);<br>
 * ...<br>
 * conn.receive(dg);<br>
 * String answer = dg.readUTF();<br>
 * ...<br>
 * </code><br>
 * <br>
 * The radiogram protocol also supports broadcast mode, where radiograms are delivered
 * to all listeners on the given port. Because broadcast mode does not use I802.15.4
 * ACKing, there are no delivery guarantees.<br>
 * <br>
 * <code><br>
 * ...<br>
 * DatagramConnection sendConn = (DatagramConnection) Connector.open("radiogram://broadcast:10");<br>
 * dg.writeUTF("My message");<br>
 * sendConn.send(dg);<br>
 * ...<br>
 * DatagramConnection recvConn = (DatagramConnection) Connector.open("radiogram://:10");<br>
 * recvConn.receive(dg);<br>
 * String answer = dg.readUTF();<br>
 * </code><br>
 * <br>
 */
public interface RadiogramConnection extends ITimeoutableConnection, DatagramConnection, IRadioControl, IMultipleHopConnection {
    /**
     * determines whether there are radiograms that can be read from this connection
     * @return true if there are packets that can be read from the connection
     */
	public boolean packetsAvailable();
    
}
