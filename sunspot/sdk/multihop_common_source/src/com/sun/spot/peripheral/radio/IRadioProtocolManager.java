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

import com.sun.spot.peripheral.ChannelBusyException;
import com.sun.spot.peripheral.NoAckException;
import com.sun.spot.peripheral.NoRouteException;

/**
 * A general purpose {@link IProtocolManager} designed to separate incoming radio packets into 
 * separate queues based on a unique identifying port number in the range 0-255.<br><br>
 * 
 * Currently underpins {@link com.sun.spot.io.j2me.radiogram.RadiogramConnection com.sun.squawk.io.j2me.radiogram.RadiogramConnection} and 
 * {@link 	com.sun.spot.io.j2me.radiostream.RadiostreamConnection com.sun.squawk.io.j2me.radio.RadioConnection}.
 */
public interface IRadioProtocolManager {
	
	/**
	 * The first port free for user use
	 */
	final byte FIRST_USER_PORT = 32;

	
	final int OUTPUT = 0;
	final int INPUT = 1;
	
	final int PORT_OFFSET = 0;
	
	/**
	 * Deregister a handler.
	 * 
	 * The ConnectionID supplied should be one that was created by a previous
	 * call to #addConnection. This call reverses the effect of the previous call.
	 * 
	 * @param cid - the ConnectionID to deregister
	 */
	void closeConnection(ConnectionID cid);

	/**
	 * Register a point-to-point connection on which packets can be sent
	 * 
	 * @param macAddress - address of the other device
	 * @param portNo - port number to communicate over
	 * @return resultant ConnectionID
	 */
	ConnectionID addOutputConnection(long macAddress, byte portNo);

	/**
	 * Register a point-to-point connection on which packets can be received
	 * 
	 * @param macAddress - address of the other device
	 * @param portNo - port number to communicate over
	 * @return resultant ConnectionID
	 */
	ConnectionID addInputConnection(long macAddress, byte portNo);

	/**
	 * Send a byte array using a ConnectionID.  The client code should leave 
	 * IPortBasedProtocolManager.DATA_OFFSET bytes free at the front of their
	 * payload as these will be overwritten.
	 * 
	 * @param cid the ConnectionID to send the packet over.
	 * @param payload the data
	 * @param length number of bytes to send, starting with index 0
	 * @return the time at which the data was sent
	 * @throws NoAckException
	 * @throws ChannelBusyException
	 * @throws NoRouteException
	 * @throws NoRouteException
	 * @throws NoMeshLayerAckException 
	 */
	long send(ConnectionID cid, long toAddress, byte[] payload, int length) throws NoAckException, ChannelBusyException, NoRouteException, NoMeshLayerAckException;

	/**
	 * Receive incoming data over a Connection ID. If the Connection ID is a server, this will 
	 * be any data received on the given protocol/port: if the Connection ID specifies a
	 * counterpart, it will be data received from that counterpart. It is an error to attempt
	 * to receive data over a broadcast Connection ID.
	 * 
	 * This method blocks until data is received over the given Connection ID.
	 *  
	 * @param cid the ConnectionID over which to receive data
	 * @return the received data
	 */
	IncomingData receivePacket(ConnectionID cid);

	/**
	 * Receive incoming data over a Connection ID. If the Connection ID is a server, this will 
	 * be any data received on the given protocol/port: if the Connection ID specifies a
	 * counterpart, it will be data received from that counterpart. It is an error to attempt
	 * to receive data over a broadcast Connection ID.
	 * 
	 * This method blocks until data is received over the given Connection ID, or until a 
	 * timouet expires.
	 * 
	 * @param cid the ConnectionID over which to receive data
	 * @param timeout the maximum time to block in milliseconds
	 * @return the received packet or null if a timeout occurs
	 */
	IncomingData receivePacket(ConnectionID cid, long timeout);

	/**
	 * Answer whether one or more radio packets have been received and are queued for the given
	 * ConnectionID. It is an error to call this method for a broadcast Connection ID.
	 * 
	 * @param connectionID
	 * @return whether packets are available.
	 */
	boolean packetsAvailable(ConnectionID connectionID);
	
}
