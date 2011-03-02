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

package com.sun.spot.peripheral.radio;

import com.sun.spot.peripheral.ChannelBusyException;
import com.sun.spot.peripheral.NoAckException;
import com.sun.spot.peripheral.NoRouteException;
import com.sun.spot.peripheral.radio.mhrp.interfaces.IMHEventListener;
import com.sun.spot.peripheral.radio.routing.interfaces.IRoutingManager;

/**
 * Interface that specifies the minimum functionality offered by a layer that
 * implements the LowPan specification
 *
 * @author Allen Ajit George
 * @version 0.1
 */
public interface ILowPan {
    /**
     * Minimum length of the LowPan header
     */
    public static final byte MAC_PAYLOAD_OFFSET = 2;
    /*
     ** default number of hops a packet will take
     * used for broadcasts.  AODV route discovered packets have actual AODV hop count set in
     * packet header
     */
    /**
     * default number of hops a routed packet should make
     */
    public static final byte DEFAULT_HOPS = 0x1;
    
    
    /**
     * normal length of a MESHED LowPan Packet, not counting broadcast or fragmentation
     */
    public static final byte MAX_MAC_PAYLOAD_OFFSET = 18;
    
    /**
     * Register protocol managers that send/receive data encoded for all
     * protocols in a given family.  Protocol Family numbers map to 6lowpan
     * protocol dispatch numbers
     *
     * @param protocolFamily mily to which this protocol belongs.  This should map to
     * a 6lowpan protocol dispatch such as IPv6, etc.
     * @param protocolMan protocol manager that will handle messages encoded
     * using this protocol number
     */
    public void registerProtocolFamily(byte protocolFamily, IProtocolManager protocolMan);
    
    /**
     * Register protocol managers that send/receive data encoded using the
     * specified protocol number, within the specified family.
     *
     * @param protocolNum unique number to identify the protocol (0...255).
     * @param protocolMan protocol manager that will handle messages encoded
     * using this protocol number
     */
    public void registerProtocol(byte protocolNum, IProtocolManager protocolMan);
    /**
     * Deregisters the protocol manager that handles the specifies protocol manager
     *
     * @param protocolFamily family to which this protocol belongs.  This should map to
     * a 6lowpan protocol dispatch such as IPv6, etc.
     */
    
    public void deregisterProtocolFamily(byte protocolFamily);
    
    /**
     * Deregisters the protocol manager that handles the specifies protocol manager
     *
     * @param protocolNum unique number to identify the protocol (0...255).
     */
    
    public void deregisterProtocol(byte protocolNum);
    /**
     * Registers an application etc. that is notified when this node is used to
     * forward a data packet
     *
     * @param listener object that is notified when data is forwarded
     * @deprecated use addDataEventListener()
     */
    public void registerDataEventListener(IDataEventListener listener);
    
    /**
     * Deregisters an application etc. that is called back when data is forwarded
     *
     * @param listener object that is notified when data is forwarded
     * @deprecated use removeDataEventListener()
     */
    public void deregisterDataEventListener(IDataEventListener listener);

    /**
     * Adds a new listener that is notified when this node is used to
     * forward a data packet
     *
     * @param listener object that is notified when data is forwarded
     */
    public void addDataEventListener(IDataEventListener listener);

    /**
     * Removes the specified listener that is called back when data is forwarded
     *
     * @param listener object that is notified when data is forwarded
     */
    public void removeDataEventListener(IDataEventListener listener);

    /**
     * Registers an application etc. that is notified when this node
     * initiates/receives supported route events
     *
     * @param listener object that is notified when route events occur
     * @deprecated use addRouteEventListener()
     */
    public void registerRouteEventListener(IRouteEventListener listener);
    
    /**
     * Undo a previous call of registerRouteEventListener()
     * @param listener the class that wants to be deregistered
     * @deprecated use removeRouteEventListener()
     */
    public void deregisterRouteEventListener(IRouteEventListener listener);

    /**
     * Adds a new listener that is notified when this node
     * initiates/receives supported route events
     *
     * @param listener object that is notified when route events occur
     */
    public void addRouteEventListener(IRouteEventListener listener);

    /**
     * Removes the specified listener that is notified when this node
     * initiates/receives supported route events
     *
     * @param listener object that is notified when route events occur
     */
    public void removeRouteEventListener(IRouteEventListener listener);

    /**
     * Registers an application etc. that is notified when this node
     * initiates/receives supported route events
     *
     * @param listener object that is notified when route events occur
     * @deprecated use addMHEventListener()
     */
    public void registerMHEventListener(IMHEventListener listener);

    /**
     * Adds a new listener that is notified when this node
     * initiates/receives supported route events
     *
     * @param listener object that is notified when route events occur
     */
    public void addMHEventListener(IMHEventListener listener);

    /**
     * Removes the specified listener that is notified when this node
     * initiates/receives supported route events
     *
     * @param listener object that is notified when route events occur
     */
    public void removeMHEventListener(IMHEventListener listener);

    /**
     * Send a byte buffer on a given protocol. The caller simply supplied a byte
     * payload
     *
     * @param protocolFamily the protocol family associated with the outgoing packet
     * @param protocolNum the protocol number associated with the outgoing packet
     * @param toAddress the destination
     * @param payload byte array that holds the data to be sent
     * @param startOffset offset within the byte array at which data should be read
     * @param endOffset offset within the byte array after which data
     * <em>should not</em> be read
     * @param failIfNotSingleHop if true this method should not send if not single hop.
     * @return true if the buffer was sent
     * @throws ChannelBusyException the radio channel could not be accessed
     * @throws NoRouteException a route to the destination could not be found
     */
    public boolean send(byte protocolFamily, byte protocolNum, long toAddress, byte[] payload,
            int startOffset, int endOffset, boolean failIfNotSingleHop)
            throws ChannelBusyException, NoRouteException;
    
    /**
     * Send a byte buffer on a given protocol. The caller simply supplied a byte
     * payload
     *
     * @param protocolFamily the protocol family associated with the outgoing packet
     * @param protocolNum the protocol number associated with the outgoing packet
     * @param toAddress the destination
     * @param payload byte array that holds the data to be sent
     * @param startOffset offset within the byte array at which data should be read
     * @param endOffset offset within the byte array after which data
     * <em>should not</em> be read
     * @return the time at which the data was sent
     * @throws ChannelBusyException the radio channel could not be accessed
     * @throws NoRouteException a route to the destination could not be found
     */
    public long send(byte protocolFamily, byte protocolNum, long toAddress, byte[] payload,
            int startOffset, int endOffset) throws ChannelBusyException, NoRouteException;
    
    /**
     * Send a byte buffer on a given protocol, without any attempt at meshing or fragmentation
     * @param protocolNum higher level protocol number (for port based protocol manager)
     * @param toAddress address of the remote device for radio packet
     * @param buffer data buffer to be sent
     * @param startOffset index of first byte of data to be sent from the buffer
     * @param endOffset index of the last byte of data to be sent
     * @throws NoAckException Ack was expected but not received
     * @throws ChannelBusyException radio channel was busy when send was attempted
     */
    public void sendWithoutMeshingOrFragmentation(byte protocolNum, long toAddress, byte[] buffer, int startOffset, int endOffset) throws NoAckException, ChannelBusyException;
    
    /**
     * Broadcast a byte buffer on a given protocol, without any attempt at meshing (can be fragmented)
     * @return the time at which the data was sent
     * @param protocolFamily The dispatch value for the family this protocol is part
     * @param protocolNum higher level protocol number (for port based protocol manager)
     * @param buffer data buffer to be sent
     * @param startOffset index of first byte of data to be sent from the buffer
     * @param endOffset index of the last byte of data to be sent
     * @param hops number of mesh hops this broadcast should take
     * @throws ChannelBusyException radio channel was busy when send attempted
     */
    public long sendBroadcast(byte protocolFamily, byte protocolNum, byte[] buffer, int startOffset,
            int endOffset, int hops) throws ChannelBusyException;
    
        /**
     * Broadcast a byte buffer on a given protocol, without any attempt at meshing (can be fragmented)
     * @return the time at which the data was sent
     * @param protocolNum higher level protocol number (for port based protocol manager)
     * @param buffer data buffer to be sent
     * @param startOffset index of first byte of data to be sent from the buffer
     * @param endOffset index of the last byte of data to be sent
     * @param hops number of mesh hops this broadcast should take
     * @throws ChannelBusyException radio channel was busy when send attempted
     */
    public long sendBroadcast(byte protocolNum, byte[] buffer, int startOffset,
            int endOffset, int hops) throws ChannelBusyException;
    
    /**
     * Method called by INewpacketDispatcher when a packet is received
     * @param packet packet received over the radio
     * @throws ChannelBusyException channel was busy when access was attempted
     * @throws NoRouteException No route could be found to the destination
     */
    public void receive(RadioPacket packet)
    throws ChannelBusyException, NoRouteException;
    
    /**
     * Method called by RadioPacketDispatcher on a host. The low pan needs
     * to use the basestations ieee address.
     * @param addr 64-bit value representing our IEEE address
     */
    public void setOurAddress(long addr);
    
    /**
     * Get the routing manager
     * @return the routing manager
     */
    public IRoutingManager getRoutingManager();
    
    /**
     * Replace the routing manager with a different implemenation
     * @param newRoutingManager a new routing manager
     * @return the old routing manager
     */
    public IRoutingManager setRoutingManager(IRoutingManager newRoutingManager);
    
}
