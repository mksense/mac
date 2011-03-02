/*
 * Copyright 2006-2009 Sun Microsystems, Inc. All Rights Reserved.
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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Timer;
import java.util.Vector;
import java.util.Random;

import com.sun.spot.peripheral.ChannelBusyException;
import com.sun.spot.peripheral.NoAckException;
import com.sun.spot.peripheral.NoRouteException;
import com.sun.spot.peripheral.SpotFatalException;
import com.sun.spot.peripheral.radio.routing.RouteInfo;
import com.sun.spot.peripheral.radio.mhrp.aodv.Constants;
import com.sun.spot.peripheral.radio.mhrp.interfaces.IMHEventListener;
import com.sun.spot.peripheral.radio.mhrp.lqrp.LQRPManager;
import com.sun.spot.peripheral.radio.routing.interfaces.IRoutingManager;
import com.sun.spot.peripheral.radio.routing.interfaces.RouteEventClient;
import com.sun.spot.service.IService;
import com.sun.spot.util.IEEEAddress;
import com.sun.squawk.util.IntHashtable;
import com.sun.spot.util.Debug;
import com.sun.spot.util.Queue;
import com.sun.spot.peripheral.radio.routing.RoutingPolicyManager;
import com.sun.spot.peripheral.radio.routing.interfaces.IRoutingPolicyManager;
import com.sun.spot.service.BasicService;
import com.sun.spot.service.ServiceRegistry;
import com.sun.spot.util.Utils;


/**
 * Packet processing layer based on the low pan draft. Provides link layer
 * fragmentation and mesh routing in cooperation with a routing manager.
 *
 * @author Allen Ajit George, Jochen Furthmueller, Pete St. Pierre
 * @version 1.0
 */
public class LowPan extends BasicService implements ILowPan, RouteEventClient {

    private final Object routeLock = new Integer(0);
    private static final int MAX_BROADCAST_DELTA = 30; // used for sequence checking
    private long ourAddress;
    private Vector dataListener;
    private Vector routeListener;
    private int datagramTag;
    private int broadcastSeqNo;
    private Random randomGen;
    private Queue bCastQueue;
    private BroadcastDispatcherThread bCastDispatcher;
    private static IntHashtable protocolTable;
    private static IntHashtable protocolFamilyTable;
    private static Hashtable availRoutes;
    private static ILowPan lowPan;
    private static IRoutingManager routingManager;
    private static IRadioPacketDispatcher packetDispatcher;
    private static Hashtable reassemblyBuffers;
    private static Hashtable bCastSeqNos;
    private static Timer reassemblyTimer;
    private static IService netmgr;
    private static IRoutingPolicyManager rpm;

    // For managment
    private static LowPanStats lpStats;


    private static final long REASSEMBLY_EXPIRATION_TIME = 15000;
    /**
     * Limit the number of packets on this queue to avoid out of memory errors when there's
     * lots of broadcast traffic.
     * <p/>
     * The value chosen is on the basis that we delay for an average time that is more than 50 millis
     * before forwarding broadcast packets, so any further packets added to the queue would
     * get broadcasted more than ten seconds late anyway and would be of doubtful value.
     */
    private static final int MAX_BROADCAST_QUEUE_LENGTH = 200;

    private static final int DEFAULT_PACKET_DELAY = 10;
    private static final int DEFAULT_FORWARDING_DELAY = 5;
    private static final int DEFAULT_PER_HOP_DELAY = 10;

    /**
     * Get the instance of this singleton.
     *
     * @return the LowPan packet dispatcher for this SPOT
     */
    public static synchronized ILowPan getInstance() {
        if (lowPan == null) {
            lowPan = (ILowPan) ServiceRegistry.getInstance().lookup(LowPan.class);
            if (lowPan == null) {
                lowPan = new LowPan(RadioFactory.getRadioPolicyManager().getIEEEAddress(), LQRPManager.getInstance(), RadioPacketDispatcher.getInstance());
                ServiceRegistry.getInstance().add((LowPan) lowPan);
            }
        }
        return lowPan;
    }

    public String getServiceName() {
        return "LowPan";
    }

    /**
     * protected constructor for the instantiation of the singleton
     *
     * @param ourAddress            MAC address we generate packets from/answer for
     * @param routingManager        the routing manager used for mesh routing
     * @param radioPacketDispatcher The underlying object that transmits and received 802.15.4 radio packets
     */
    protected LowPan(long ourAddress, IRoutingManager routingManager, IRadioPacketDispatcher radioPacketDispatcher) {
        // basic setup
        this.ourAddress = ourAddress;
        packetDispatcher = radioPacketDispatcher;
        lpStats = new LowPanStats();
        randomGen = new Random();

        // Protocol dispatching functionality
        protocolTable = new IntHashtable();
        protocolFamilyTable = new IntHashtable();

        // 6lowpan reassembly support
        datagramTag = 0;
        reassemblyBuffers = new Hashtable();
        reassemblyTimer = new Timer();

        // Broadcast support
        bCastSeqNos = new Hashtable();
        broadcastSeqNo = randomGen.nextInt(256); // Initialize with random start value
        bCastQueue = new Queue();
        bCastDispatcher = new BroadcastDispatcherThread(this);
        bCastDispatcher.start();

        // initialize routing policy manager
        rpm = RoutingPolicyManager.getInstance();

        // initialize routing manager
        routeListener = new Vector();
        dataListener = new Vector();
        availRoutes = new Hashtable();
        setRoutingManager(routingManager);

        // Must be the last thing we do
        packetDispatcher.initialize(this);
    }

    /**
     * Replace the routing manager with a different implemenation
     *
     * @param newRoutingManager a new routing manager
     * @return the old routing manager
     */

    public IRoutingManager setRoutingManager(IRoutingManager newRoutingManager) {

        IRoutingManager rm = this.routingManager;  // save this so we can return it
        if (rm != null) rm.stop();
        if (newRoutingManager != null) {
            newRoutingManager.initialize(ourAddress, this);
            newRoutingManager.start();
            this.routingManager = newRoutingManager;
        }
        return rm;
    }

    /**
     * Register a protocol manager to send and receive packets
     *
     * @param protocolFamily protocol family this protocol is in (ie IPv6, SunSPOT, etc)
     * @param protocolMan    the protocol manager to be registered
     */
    public void registerProtocolFamily(byte protocolFamily, IProtocolManager protocolMan) {
        if (protocolFamily == LowPanHeader.DISPATCH_SPOT) {
            throw new IllegalArgumentException("Cannot override SPOT protocol family ");

        } else { // It's a non-spot protocol
            if (getProtocolFamilyFor(protocolFamily) != null) {
                throw new IllegalArgumentException("Cannot add multiple protocol " +
                        "managers for family: " + protocolFamily);
            }

            synchronized (protocolFamilyTable) {
                protocolFamilyTable.put(protocolFamily, protocolMan);
                lpStats.protocolFamilyCount++;
            }
            //Debug.print("registerProtocolFamily: added protocol family " + protocolNum, 3);
        }


    }

    /**
     * Register a protocol manager to send and receive packets
     *
     * @param protocolNum number of protocol to be handled
     * @param protocolMan the protocol manager to be registered
     */
    public void registerProtocol(byte protocolNum, IProtocolManager protocolMan) {
        if (getProtocolFor(protocolNum) != null) {
            throw new IllegalArgumentException("Cannot add multiple protocol " +
                    "managers for " + protocolNum);
        }

        synchronized (protocolTable) {
            protocolTable.put(protocolNum, protocolMan);
            lpStats.protocolCount++;
        }
        //Debug.print("registerProtocol: added protocol " + protocolNum, 3);
    }

    /**
     * Deregister a protocol.
     *
     * @param protocolNum identifier for the protocol
     */
    public void deregisterProtocol(byte protocolNum) {
        synchronized (protocolTable) {
            Object protMgr = protocolTable.remove(protocolNum);
            if (protMgr == null) {
                throw new IllegalArgumentException("Cannot remove protocol manager " +
                        "for unknown protocol " + protocolNum);
            } else {
                lpStats.protocolCount--;
            }
        }
    }

    /**
     * Deregister a protocol family.
     *
     * @param protocolFamily identifier for the protocol
     */
    public void deregisterProtocolFamily(byte protocolFamily) {
        synchronized (protocolFamilyTable) {
            Object protMgr = protocolFamilyTable.remove(protocolFamily);
            if (protMgr == null) {
                throw new IllegalArgumentException("Cannot remove protocol manager " +
                        "for unknown protocol " + protocolFamily);
            } else {
                lpStats.protocolFamilyCount--;
            }
        }
    }

    /**
     * Register to be notified as soon as the LowPan module forwards data.
     *
     * @param listener the class that wants to be called back
     */
    public void registerDataEventListener(IDataEventListener listener) {
        addDataEventListener(listener);
    }

    /**
     * Undo a previous call of registerDataEventListener()
     *
     * @param listener the class that wants to be deregistered
     */
    public void deregisterDataEventListener(IDataEventListener listener) {
        removeDataEventListener(listener);
    }

    /**
     * Adds a new listener that is notified when this node is used to
     * forward a data packet
     *
     * @param listener object that is notified when data is forwarded
     */
    public void addDataEventListener(IDataEventListener listener) {
        this.dataListener.addElement(listener);
    }

    /**
     * Removes the specified listener that is called back when data is forwarded
     *
     * @param listener object that is notified when data is forwarded
     */
    public void removeDataEventListener(IDataEventListener listener) {
        this.dataListener.removeElement(listener);
    }

    /**
     * Register to be notified when certain routing events occur.
     *
     * @param listener the class that wants to be called back
     */
    public void registerRouteEventListener(IRouteEventListener listener) {
        addRouteEventListener(listener);
    }

    /**
     * Undo a previous call of registerRouteEventListener()
     *
     * @param listener the class that wants to be deregistered
     */
    public void deregisterRouteEventListener(IRouteEventListener listener) {
        removeRouteEventListener(listener);
    }

    /**
     * Adds a new listener that is notified when this node
     * initiates/receives supported route events
     *
     * @param listener object that is notified when route events occur
     */
    public void addRouteEventListener(IRouteEventListener listener) {
        this.routeListener.addElement(listener);
    }

    /**
     * Removes the specified listener that is notified when this node
     * initiates/receives supported route events
     *
     * @param listener object that is notified when route events occur
     */
    public void removeRouteEventListener(IRouteEventListener listener) {
        this.routeListener.removeElement(listener);
    }

    /**
     * Register to be notified when certain multihop routing events occur.
     *
     * @param mhListener the class that wants to be called back
     */
    public void registerMHEventListener(IMHEventListener mhListener) {
        addMHEventListener(mhListener);
    }

    /**
     * Adds a new listener that is notified when this node
     * initiates/receives supported route events
     *
     * @param listener object that is notified when route events occur
     */
    public void addMHEventListener(IMHEventListener listener) {
        routingManager.addEventListener(listener);
    }

    /**
     * Removes the specified listener that is notified when this node
     * initiates/receives supported route events
     *
     * @param listener object that is notified when route events occur
     */
    public void removeMHEventListener(IMHEventListener listener) {
        routingManager.removeEventListener(listener);
    }

    /**
     * This method is called by the routing manager as soon as a route is available
     * or if no route has been found within the defined period. Then the nextHop
     * field of the RouteInfo is invalid.
     *
     * @param info      object containg routing information
     * @param uniqueKey a key that uniquely identifies the route
     */
    public void routeFound(RouteInfo info, Object uniqueKey) {
        synchronized (routeLock) {
            availRoutes.put(uniqueKey, info);
            //the corresponding wait() is called in findNextHop()
            routeLock.notifyAll();
        }
    }

    /**
     * This method is called by the packet dispatcher this low pan layer is
     * registered with. It decides which instance this packet should be passed
     * to: forwardMeshPacket() for packets that are addressed to another node,
     * reassembly() for fragmented packets or readPacket() for non fragmented packets.
     *
     * @param packet packet that was received by the underlying radio
     * @throws com.sun.spot.peripheral.ChannelBusyException
     *          the radio was busy/in-use
     * @throws com.sun.spot.peripheral.NoRouteException
     *          a mesh route could not be found to the next hop for the received packet
     */
    public synchronized void receive(RadioPacket packet) throws ChannelBusyException, NoRouteException {
        LowPanPacket lpp = new LowPanPacket(packet);
//        System.out.println(packet.toString());
        if (lpp.isMeshed()) {
            lpStats.meshPacketsReceived++;
            // WARNING::: Do not change the following line!
            if ((lpp.getFDestinationAddress() - ourAddress) == 0) {
                if (lpp.isFragged()) {
                    //System.out.println("[lowpan] Handle Fragged packet");
                    //Debug.print("receive: " +
                    //"multihop packet is fragmented - do reassembly", 1);
                    reassembly(lpp);
                } else { // unfragged packet for us
                    int dataStart = lpp.getLppPayloadOffset();
                    int dataLength = lpp.getPayloadSize();
                    readPacket(lpp, dataStart, dataLength);
                }
            } else {
                // Didn't have our Final Destination check broadcast

                if (!lpp.isBCast() && !rpm.isEndNode()) {

                    // Not a broadcast, send it along
                    forwardMeshPacket(lpp);
                } else { // is a meshed broadcast
                    if ((lpp.getOriginatorAddress() != ourAddress) && (validateBroadcastForForwarding(lpp))) {  // if we won't forward it, we don't process either
                        //                       System.out.println("[lowpan] Recived Meshed Broadcast from " +
                        //                               IEEEAddress.toDottedHex(lpp.getRPSourceAddress()));
                        // Now process the packet locally
                        lpStats.meshBroadcastsReceived++;
                        if (lpp.isFragged()) {
                            reassembly(lpp);
                        } else {
                            int dataStart = lpp.getLppPayloadOffset();
                            int dataLength = lpp.getPayloadSize();

                            if (dataLength < 0) {
                                System.out.println("[LowPan] Received packet with apparently negative data length");
                                System.out.println("RadioPacket: " + packet);
                            } else {
                                // read ourselves a meshed broadcst packet
                                readPacket(lpp, dataStart, dataLength);
                            } // end if (dataLength < 0)
                        } // end unfragged meshed broadcast
                        // Have not seen this one, so forward
                        if (!rpm.isEndNode()) {
                            if (lpp.getHopsLeft() > 1)
                                queueBroadcastPacket(lpp); // used to forward, now we Queue it -- pete
                        }

                    } else { // end valid meshed broadcast
                        lpStats.droppedBroadcasts++; // it was either ours, or an invalid seqNo
                    }
                } // End meshed broadcast
            } // end packet not specifically to us
        } else { // Time to process a single hop packet
            //Debug.print("receive: single hop delivery", 1);
            // Single hop
            lpStats.nonMeshPacketsReceived++;
            if (lpp.isFragged()) {
                reassembly(lpp);
            } else {
                int dataStart = lpp.getLppPayloadOffset();
                int dataLength = lpp.getPayloadSize();

                if (dataLength < 0) {
                    System.out.println("[LowPan] Received packet with apparently negative data length");
                    System.out.println("RadioPacket: " + packet);
                } else {
                    readPacket(lpp, dataStart, dataLength);
                }
            }
        }
    }

    private boolean validateBroadcastForForwarding(LowPanPacket lpp) throws ChannelBusyException, NoRouteException {
        Integer lastSeqNo = (Integer) bCastSeqNos.get(new Long(lpp.getOriginatorAddress()));
        if (lastSeqNo == null) { // never seen a Bcast from this address
            bCastSeqNos.put(new Long(lpp.getOriginatorAddress()), new Integer((int) (lpp.getBCastSeqNo() & 0xff)));
            return true;
        }
        int limit = lastSeqNo.intValue();
        int current = lpp.getBCastSeqNo();
        // We either increased or decreased
        if (current <= limit) { // new packet has lower value
            if (current > (limit - MAX_BROADCAST_DELTA)) { // within window, drop it
//                Debug.print("[lowpan] Squelch broadcast, Origin: " + IEEEAddress.toDottedHex(lpp.getOriginatorAddress()) +
//                        ", Sequence: " + current + " Previous: " + limit + ", delta: " + (limit-current));
                return false;
            }
        } else { // New value is larger, a long as it didn't wrap, we're fine
            if ((limit - MAX_BROADCAST_DELTA + 255) < current) {
//                Debug.print("[lowpan] Squelch broadcast, Origin: " + IEEEAddress.toDottedHex(lpp.getOriginatorAddress()) +
//                        ", Sequence: " + current + " Previous: " + limit + ", delta: " + (limit-current));
                return false;
            }
        }
        // far enough out of window it may be a new broadcast
        bCastSeqNos.put(new Long(lpp.getOriginatorAddress()), new Integer((int) (lpp.getBCastSeqNo() & 0xff)));
        return true;
    }

    /**
     * returns a handle to the routing manager object
     *
     * @return An object that implements the IRoutingManager interface
     */
    public IRoutingManager getRoutingManager() {
        return routingManager;
    }

    /**
     * returns a handle to the netmanagement  object
     *
     * @return An object that implements the IService interface
     */
    public IService getNetManager() {
        return netmgr;
    }

    /**
     * returns a handle to the RoutingPolicyManager  object
     *
     * @return An object that implements the IRoutingManager interface
     */
    public IRoutingPolicyManager getRoutingPolicyManager() {
        return rpm;
    }

    public long send(byte protocolFamily, byte protocolNum, long toAddress, byte[] buffer,
                     int startOffset, int endOffset)
            throws ChannelBusyException, NoRouteException {
        LowPanPacket lpp = new LowPanPacket(LowPanPacket.DATA_PACKET);
        sendPrim(protocolFamily, protocolNum, toAddress, buffer, startOffset, endOffset, false,
                lpp);
        return lpp.getRadioPacket().getTimestamp();
    }

    public boolean send(byte protocolFamily, byte protocolNum, long toAddress, byte[] buffer,
                        int startOffset, int endOffset, boolean failIfNotSingleHop)
            throws ChannelBusyException, NoRouteException {
        return sendPrim(protocolFamily, protocolNum, toAddress, buffer, startOffset, endOffset,
                failIfNotSingleHop, new LowPanPacket(LowPanPacket.DATA_PACKET));
    }

    /**
     * Send a byte buffer on a given protocol. The caller simply supplied a byte
     * payload
     */
    private boolean sendPrim(byte protocolFamily, byte protocolNum, long toAddress, byte[] buffer,
                             int startOffset, int endOffset, boolean failIfNotSingleHop, LowPanPacket lpp)
            throws ChannelBusyException, NoRouteException {
//        System.out.println("[sendPrim] Start: " + startOffset + " end:" + endOffset);
        IProtocolManager protocolManager;
        LowPanHeader lph = new LowPanHeader();
        lph.setProtocolInfo(protocolFamily, protocolNum);

//        System.out.println("[sendPrim] Sending to " + IEEEAddress.toDottedHex(toAddress));
//        System.out.println("[sendPrim] failIfNotSingleHop: " + failIfNotSingleHop);
        if (protocolFamily == LowPanHeader.DISPATCH_SPOT) {
            protocolManager = getProtocolFor(protocolNum);
        } else {
            protocolManager = getProtocolFamilyFor(protocolFamily);
        }
        if (protocolManager == null) {
            throw new IllegalArgumentException("Unknown protocol " + protocolNum);
        }
        boolean result = true;

        RouteInfo info = routingManager.getRouteInfo(toAddress);
//		System.out.println("doFSend: meshing enabled");
//		System.out.println("doFSend: next hop is " +
//                        IEEEAddress.toDottedHex(info.nextHop));

        if (info.nextHop == Constants.INVALID_NEXT_HOP) {
            info = findNextHop(toAddress);
        }

        lpp.getRadioPacket().setDestinationAddress(info.nextHop);

        //how much payload data can be filled into one radio packet
        byte freeSpace = RadioPacket.MIN_PAYLOAD_LENGTH - ILowPan.MAC_PAYLOAD_OFFSET;
        freeSpace -= LowPanHeader.MAX_PROTOCOL_HEADER_LENGTH;
        if (info.hopCount > 1) {
            freeSpace -= LowPanHeader.MAX_MESH_HEADER_LENGTH;
        }
        for (int i = 0; i < 3; i++) {
            try {
                if (freeSpace >= (endOffset - startOffset)) {
                    if (info.hopCount > 1 && failIfNotSingleHop) {
                        result = false;
                        return result;
                    } else {
                        sendInOnePacket(info, protocolNum, buffer,
                                startOffset, endOffset, lpp, lph);
                        lpStats.unicastsSent++;
                        return result;
                    }
                } else {
                    if (failIfNotSingleHop) {
                        throw new RuntimeException("The failIfNotSingleHop facility is not compatible with payloads that need fragmentation");
                    }
                    freeSpace -= LowPanHeader.MAX_FRAGMENTATION_HEADER_LENGTH;
                    sendInFragments(info, protocolNum, buffer, startOffset,
                            endOffset, lpp, lph, freeSpace);
                    lpStats.unicastsSent++;
                    lpStats.unicastsFragmented++;
                    return result;
                }
            } catch (NoAckException e) {
                // originally deactivated next hop -- we really need to invalidate the whole route
                // routingManager.invalidateRoute(ourAddress, info.nextHop);
            }
        }
        // we did not successfully send - so invalidate the route and retry once, if we locate a new route
        routingManager.invalidateRoute(ourAddress, info.destination);
        try {
            info = findNextHop(toAddress);
        } catch (NoRouteException nre) {
            Debug.print("[LowPan] received a NoAckException on route to " +
                    IEEEAddress.toDottedHex(toAddress) + " through " + IEEEAddress.toDottedHex(info.nextHop));
            throw new NoRouteException("[LowPan] received a NoAckException on route to " +
                    IEEEAddress.toDottedHex(toAddress) + " through " + IEEEAddress.toDottedHex(info.nextHop));
        }
        send(protocolFamily, protocolNum, toAddress, buffer, startOffset, endOffset,
                failIfNotSingleHop);
        return result;
    }

    /**
     * Sends a packet over a single hop.  The buffer must fit within a single 802.15.4 packet.
     * No 6lowpan headers are used.
     *
     * @param protocolNum SPOT protocol number
     * @param toAddress   destination address for this packet
     * @param buffer      the data buffer being sent
     * @param startOffset start index for data to be sent
     * @param endOffset   end index of data from buffer to be sent
     * @throws com.sun.spot.peripheral.NoAckException
     *          receiving end did not generate the 802.15.4 layer ACK for this packet
     * @throws com.sun.spot.peripheral.ChannelBusyException
     *          radio was busy/in-use
     */
    public void sendWithoutMeshingOrFragmentation(byte protocolNum, long toAddress,
                                                  byte[] buffer, int startOffset, int endOffset) throws NoAckException, ChannelBusyException {
        LowPanPacket lpp = new LowPanPacket(LowPanPacket.DATA_PACKET);
        LowPanHeader lph = new LowPanHeader();
        lph.setOutgoingDestinationAddress(toAddress);
        lph.setProtocolInfo(LowPanHeader.DISPATCH_SPOT, protocolNum);
        lph.setMeshed(false);
        lph.setBCast(false);
        lph.setFragged(false);
        lph.setOutgoingFragTag(LowPanHeader.UNFRAGMENTED);

        lpp.writeHeaderAndPayload(lph, buffer, startOffset, endOffset);
        lpp.getRadioPacket().setDestinationAddress(toAddress);
        if (toAddress == 0xffff) lpStats.broadcastsSent++;
        else lpStats.unicastsSent++;
        lpStats.nonMeshPacketsSent++;
        lpStats.packetsSent++;
        packetDispatcher.sendPacket(lpp.getRadioPacket());
        Utils.sleep(DEFAULT_PACKET_DELAY);  // Delay between packets to allow receivers to keep up
        // & to prevent collision with forwarding of fragment
    }

    /**
     * send a LowPan packet using 802.15.4 broadcast packets
     *
     * @param protocolNum The SPOT prootocol number of the packet
     * @param buffer      data buffer to send
     * @param startOffset start of data buffer to send
     * @param endOffset   end index of data buffer to send
     * @param hops        maximum number of hops this LowPan packet should be passed within a mesh routed environment
     * @return timestamp packet was sent
     * @throws com.sun.spot.peripheral.ChannelBusyException
     *          radio channel was busy/in-use
     */
    public long sendBroadcast(byte protocolNum, byte[] buffer, int startOffset,
                              int endOffset, int hops) throws ChannelBusyException {

        return sendBroadcast(LowPanHeader.DISPATCH_SPOT, protocolNum, buffer,
                startOffset, endOffset, hops);
    }

    /**
     * send a LowPan packet using 802.15.4 broadcast packets
     *
     * @param protocolFamily The DISPATCH value for this protocol family
     * @param protocolNum    The SPOT prootocol number of the packet
     * @param buffer         data buffer to send
     * @param startOffset    start of data buffer to send
     * @param endOffset      end index of data buffer to send
     * @param hops           maximum number of hops this LowPan packet should be passed within a mesh routed environment
     * @return timestamp packet was sent
     * @throws com.sun.spot.peripheral.ChannelBusyException
     *          radio channel was busy/in-use
     */
    public long sendBroadcast(byte protocolFamily, byte protocolNum, byte[] buffer, int startOffset,
                              int endOffset, int hops) throws ChannelBusyException {

        LowPanPacket lpp = new LowPanPacket(LowPanPacket.BROADCAST_PACKET);
        LowPanHeader lph = new LowPanHeader();
        lph.setProtocolInfo(protocolFamily, protocolNum);
        //how much payload data can be filled into one radio packet
        byte freeSpace = RadioPacket.MIN_PAYLOAD_LENGTH - ILowPan.MAC_PAYLOAD_OFFSET -
                LowPanHeader.MAX_PROTOCOL_HEADER_LENGTH;
        long dest = 0xFFFF;
        if (hops > 1) {
            lph.setMeshed(true);
            lph.setOutgoingHops(hops);
            // set other mesh params
            lph.setOutgoingOriginatorAddress(ourAddress);
            lph.setOutgoingDestinationAddress(dest);
            lph.setBCast(true);

            // Free space gets smaller
            freeSpace -= (LowPanHeader.MAX_MESH_HEADER_LENGTH + LowPanHeader.BROADCAST_HEADER_LENGTH);
        } else {
            lph.setMeshed(false);
        }
        // No mesh header is needed
        //Debug.print("doFSend: broadcasting packet", 5);
        //System.out.println("isMeshed?? :" + lph.isMeshed());
        try {
            //if we have enough space in a single packet as we need no
            //meshheader, we can just send it
            if (!lph.isMeshed() && (endOffset - startOffset) < LowPanHeader.MAX_UN_FRANG_BDC_MSG) {
                sendInOnePacket(null, protocolNum, buffer, startOffset,
                        endOffset, lpp, lph);
                lpStats.broadcastsSent++;
            } else if (freeSpace >= (endOffset - startOffset)) {
                sendInOnePacket(null, protocolNum, buffer, startOffset,
                        endOffset, lpp, lph);
                lpStats.broadcastsSent++;
            } else {
                freeSpace -= LowPanHeader.MAX_FRAGMENTATION_HEADER_LENGTH;
                sendInFragments(null, protocolNum, buffer, startOffset,
                        endOffset, lpp, lph, freeSpace);
                lpStats.broadcastsSent++;
                lpStats.broadcastsFragmented++;
            }
        } catch (NoAckException e) {
            throw new SpotFatalException("Should never get a NoAck when broadcasting");
        } catch (NoRouteException e) {
            throw new SpotFatalException("Should never get a NoRoute when broadcasting");
        }
        return lpp.getRadioPacket().getTimestamp();
    }

    private void setSequenceNumber(LowPanHeader lph) {
        if (lph.isBCast()) {
            lph.setOutgoingBCastSeqNo(broadcastSeqNo++);
            broadcastSeqNo %= 256;
            // update table with our info so we don't repeat our packet when we see it
            bCastSeqNos.put(new Long(ourAddress), new Integer(broadcastSeqNo - 1));
        }
    }

    private void sendInOnePacket(RouteInfo routeInfoOrNull,
                                 byte protocolNum,
                                 byte[] buffer,
                                 int startOffset,
                                 int endOffset,
                                 LowPanPacket lpp,
                                 LowPanHeader lph)
            throws ChannelBusyException, NoRouteException, NoAckException {
        //  System.out.println("[lowpan] Sending as one packet, proto: " +protocolNum);

        int delay = DEFAULT_PACKET_DELAY;
        boolean isMeshing = (routeInfoOrNull != null) && (routeInfoOrNull.hopCount > 1);

        lph.setOutgoingFragType(LowPanHeader.UNFRAGMENTED);

        // mesh header may already be set if this is a broadcast > 1 hop
        if (isMeshing) {
            lph.setMeshed(true);
            lph.setOutgoingHops(routeInfoOrNull.hopCount);
            lph.setOutgoingOriginatorAddress(ourAddress);
            lph.setOutgoingDestinationAddress(routeInfoOrNull.destination);
            delay += DEFAULT_FORWARDING_DELAY + (Math.min(3, routeInfoOrNull.hopCount) - 1) * DEFAULT_PER_HOP_DELAY;
        }

        if (lph.isBCast()) {
            lpStats.meshBroadcastsSent++;
            setSequenceNumber(lph);
            delay += DEFAULT_FORWARDING_DELAY;
        } else if (lpp.getRadioPacket().getDestinationAddress() == 0xffff) {
            delay += DEFAULT_FORWARDING_DELAY;
            ;
        }


        lpp.writeHeaderAndPayload(lph, buffer, startOffset, endOffset);
        if (isMeshing || lph.isBCast()) lpStats.meshPacketsSent++;
        else lpStats.nonMeshPacketsSent++;
        lpStats.packetsSent++;
        packetDispatcher.sendPacket(lpp.getRadioPacket());
        Utils.sleep(delay); // Delay between packets to allow receivers to keep up
        // & to prevent collision with forwarding of fragment
    }

    // Note below:  For broadcast packets we must increment the sequence number in each packet
    //             Otherwise the source/SeqNo will match a previous packet and it will be
    //             discarded

    private void sendInFragments(RouteInfo routeInfoOrNull, byte protocolNum, byte[] buffer,
                                 int startOffset, int endOffset,
                                 LowPanPacket lpp, LowPanHeader lph, byte freeSpace) throws NoAckException, ChannelBusyException {

        int delay = DEFAULT_PACKET_DELAY;
        datagramTag++;
        boolean isMeshing = (routeInfoOrNull != null) && (routeInfoOrNull.hopCount > 1);
        if (isMeshing) {
            lph.setMeshed(true);
            lph.setOutgoingHops(routeInfoOrNull.hopCount);
            lph.setOutgoingOriginatorAddress(ourAddress);
            lph.setOutgoingDestinationAddress(routeInfoOrNull.destination);
            delay += DEFAULT_FORWARDING_DELAY + (Math.min(3, routeInfoOrNull.hopCount) - 1) * DEFAULT_PER_HOP_DELAY;
        } else if (lpp.getRadioPacket().getDestinationAddress() == 0xffff) {
            delay += DEFAULT_FORWARDING_DELAY;
        }

        lph.setFragged(true);
        // according to 6lowpan draft, fragment in increments in steps of 8 octets

        freeSpace = (byte) (freeSpace - (freeSpace % 8));

        lph.setOutgoingFragTag(datagramTag);
        lph.setOutgoingFragSize(endOffset - startOffset);
        //Debug.print("doFSend: freeSpace = "+freeSpace, 5);
        int numOfFragments = (endOffset - startOffset + freeSpace - 1) / freeSpace;
        for (int i = 0; i < numOfFragments - 1; i++) {
            //System.out.println("doFSend: preparing fragment "+i);
            if (i == 0) {
                lph.setOutgoingFragType(LowPanHeader.FIRST_FRAGMENT);

            } else {
                lph.setOutgoingFragType(LowPanHeader.INTERIOR_FRAGMENT);
            }
            int datagramOffset = i * freeSpace / 8;
            lph.setOutgoingFragOffset(datagramOffset);
            if (lph.isBCast()) {
                lpStats.meshBroadcastsSent++;
                setSequenceNumber(lph);
            }
            lpp.writeHeaderAndPayload(lph, buffer, datagramOffset * 8,
                    datagramOffset * 8 + freeSpace);
            // System.out.println("doFSend: sending fragment "+i);
            if (isMeshing || lph.isBCast()) lpStats.meshPacketsSent++;
            else lpStats.nonMeshPacketsSent++;
            lpStats.packetsSent++;
            packetDispatcher.sendPacket(lpp.getRadioPacket());
            Utils.sleep(delay); // Delay between packets to allow receivers to keep up
            // & to prevent collision with forwarding of fragment
        }

        //sending the last fragment
        //Debug.print("doFSend: preparing last fragment", 5);
        //packet.setMACPayloadLength(RadioPacket.MIN_PAYLOAD_LENGTH);
        lph.setOutgoingFragType(LowPanHeader.LAST_FRAGMENT);
        byte datagramOffset = (byte) ((numOfFragments - 1) * freeSpace / 8);
        lph.setOutgoingFragOffset(datagramOffset);
        if (lph.isBCast()) {
            lpStats.meshBroadcastsSent++;
            setSequenceNumber(lph);
        }
        lpp.writeHeaderAndPayload(lph, buffer, startOffset + ((numOfFragments - 1) * freeSpace),
                endOffset);

        // System.out.println("doFSend: sending last fragment");
        if (isMeshing || lph.isBCast()) lpStats.meshPacketsSent++;
        else lpStats.nonMeshPacketsSent++;
        lpStats.packetsSent++;
        packetDispatcher.sendPacket(lpp.getRadioPacket());
        Utils.sleep(delay); // still need to prevent collision with forwarding of fragment
    }

    /**
     * This method is called when the routing manager cannot provide a routing
     * info with a valid next hop for a certain destination address. It makes
     * the routing manager start a new route discovery and waits for a result
     *
     * @param destinationAddress address for which we want a next hop
     * @return RouteInfo route info that we need to send a packet to the final
     *         destination
     */
    private RouteInfo findNextHop(long destinationAddress)
            throws NoRouteException {

        RouteInfo info = null;
        Object uniqueKey = new Object();

        //notify all registered listeners, that a route request is made
        if (!routeListener.isEmpty()) {
            Enumeration en = routeListener.elements();
            while (en.hasMoreElements()) {
                ((IRouteEventListener) en.nextElement()).routeRequestMade(destinationAddress);
            }
        }

        try {
            synchronized (routeLock) {
                routingManager.findRoute(destinationAddress, this, uniqueKey);
                while (!availRoutes.containsKey(uniqueKey)) {
                    //corresponding notify is called in routeFound()
                    routeLock.wait();
                }

                info = (RouteInfo) availRoutes.remove(uniqueKey);
                if ((info.nextHop - Constants.INVALID_NEXT_HOP) == 0) {
                    //Debug.print("findNextHop: no next hop", 4);

                    if (!routeListener.isEmpty()) {
                        Enumeration en = routeListener.elements();
                        while (en.hasMoreElements()) {
                            ((IRouteEventListener) en.nextElement())
                                    .routeResponseReceived(info.destination,
                                            -1, false);
                        }
                    }
                    throw new NoRouteException("No route found");
                } else {
                    //Debug.print("findNextHop: found next hop",4);
                    if (!routeListener.isEmpty()) {
                        Enumeration en = routeListener.elements();
                        while (en.hasMoreElements()) {
                            ((IRouteEventListener) en.nextElement())
                                    .routeResponseReceived(
                                            info.getDestination(), info.hopCount, true);
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            System.err.println("findNextHop: interrupted during find route");
        }
        return info;
    }

    /**
     * This method is called whenever this low pan layer receives an incoming
     * packet which is addressed to ourselves and is not fragmented. This method
     * creates a header information to save the data that would be lost after
     * stripping away the radio packet structure. Then it will call the receiveData()
     * method of the appropriate protocol manager
     *
     * @param packet   received packet
     * @param startPos index of the first byte that is to be passed to the
     *                 protocol manager
     * @param length   length of the payload of this packet
     */
    private void readPacket(LowPanPacket lpp, int startPos, int length) {
        IProtocolManager protocolManager = null;
        byte protocolNum = lpp.getProtocol();
        byte protocolFamily = lpp.getProtocolFamily();
        if (protocolFamily == LowPanHeader.DISPATCH_SPOT)
            protocolManager = getProtocolFor(protocolNum);
        else
            protocolManager = getProtocolFamilyFor(protocolFamily);

        if (protocolManager == null) {
            lpStats.protocolHandlerMissing++;
//            System.out.println("No protocol manager registered, dropping packet with" +
//                    " protocol family: " + protocolFamily +" protocol number: "+protocolNum);
        } else {

            byte[] rpPayload = new byte[length];
            System.arraycopy(lpp.getRadioPacket().buffer, startPos, rpPayload, 0, length);

            // create a headerInfo to provide applications with fields of
            // the radio header and if given the mesh header note that hopcount
            // does not provide the number of taken hops but should be zero all
            // the time
            LowPanHeaderInfo lpHeaderInfo = null;
            RadioPacket packet = lpp.getRadioPacket();
            if (lpp.isMeshed()) {
                lpHeaderInfo = new LowPanHeaderInfo(packet.getDestinationAddress(),
                        packet.getSourceAddress(), packet.getRssi(),
                        packet.getCorr(), packet.getLinkQuality(),
                        packet.getDestinationPanID(), packet.getSourcePanID(),
                        false,
                        lpp.getOriginatorAddress(),
                        lpp.getFDestinationAddress(),
                        lpp.getHopsLeft(),
                        packet.getTimestamp());
            } else {
                // the packet was not meshDelivered. So source = originator and
                // destination = finalDestination
                lpHeaderInfo = new LowPanHeaderInfo(packet.getDestinationAddress(),
                        packet.getSourceAddress(), packet.getRssi(),
                        packet.getCorr(), packet.getLinkQuality(),
                        packet.getDestinationPanID(), packet.getSourcePanID(),
                        false, packet.getSourceAddress(),
                        packet.getDestinationAddress(), (byte) 0,
                        packet.getTimestamp());
            }
            if (packet.getDestinationAddress() == 0xffff) lpStats.broadcastsReceived++;
            else lpStats.unicastsReceived++;
            protocolManager.processIncomingData(rpPayload, lpHeaderInfo);
        }
    }

    private void queueBroadcastPacket(LowPanPacket lpp) {
        if (bCastQueue.size() < MAX_BROADCAST_QUEUE_LENGTH) {
            bCastQueue.put(lpp);
        } else {
            lpStats.broadcastsQueueFull++;
            // Drop packet
        }
    }

    /**
     * This method is called whenever low pan has to process a packet that
     * is addressed to another node. It writes the new values in into the
     * source and destination fields of the radio packet and forwards it
     * afterwards.
     *
     * @param packet           packet to be forwarded
     * @param finalDestination node that this packet should reach in the end
     */
    private void forwardMeshPacket(LowPanPacket lpp)
            throws ChannelBusyException {
        long lastHop = 0;
        long nextHop = 0;
        int delay = DEFAULT_PACKET_DELAY;

        lpp.setHopsLeft(lpp.getHopsLeft() - 1);
        if (lpp.getHopsLeft() <= 0) {
            lpStats.ttlExpired++;
            return; // time for this packet to vaporize itself
        }

        if (lpp.isBCast()) {
            lpStats.meshBroadcastsForwarded++;
            lastHop = 0xffff; // tell forwarder it was a broadcast
            nextHop = 0xffff;
            delay += DEFAULT_FORWARDING_DELAY;
        } else {  // go lookup address
            RouteInfo info = routingManager.getRouteInfo(lpp.getFDestinationAddress());
            //Debug.print("forwardMeshPacket: " +
            //"next hop is " + IEEEAddress.toDottedHex(info.nextHop), 1);
            // Debug.print("" + System.currentTimeMillis() + " forwardMeshPacket: " + lpp, 0);
            // FIXME Needed because otherwise the sender route times out...
            routingManager.getRouteInfo(lpp.getOriginatorAddress());

            if (info.nextHop != Constants.INVALID_NEXT_HOP) {

                lpp.getRadioPacket().setDestinationAddress(info.nextHop);

                //save data for notifyForward
                lastHop = lpp.getRPSourceAddress();
                nextHop = info.nextHop;
                delay += DEFAULT_FORWARDING_DELAY + (Math.min(3, info.hopCount) - 1) * DEFAULT_PER_HOP_DELAY;
            } else { // no valid route -- don't send or notify, invalidate route and return
                //      routingManager.invalidateRoute(lpp.getOriginatorAddress(),
                //             lpp.getFDestinationAddress());
                return;
            }
        }
        // Try to send it now
        try {
            for (int i = 0; i < 3; i++) {
                try {
                    lpStats.packetsForwarded++;
                    packetDispatcher.sendPacket(lpp.getRadioPacket());
                    break;
                } catch (NoAckException e) {
                    if (i >= 2) {
                        throw e;
                    }
                }
            }
            Utils.sleep(delay); // Delay between packets to allow receivers to keep up
            // & to prevent collision with forwarding of fragment
        } catch (NoAckException e) {
            Debug.print("forwardMeshPacket: can't forward packet from " +
                    IEEEAddress.toDottedHex(lpp.getOriginatorAddress()) + " to " +
                    IEEEAddress.toDottedHex(lpp.getFDestinationAddress()));
            if (!lpp.isBCast()) {
                routingManager.invalidateRoute(lpp.getOriginatorAddress(),
                        lpp.getFDestinationAddress());
                return;
            }
        }

        if (!dataListener.isEmpty()) {
            Enumeration en = dataListener.elements();
            while (en.hasMoreElements()) {
                ((IDataEventListener) en.nextElement()).notifyForward(lastHop,
                        nextHop, lpp.getOriginatorAddress(), lpp.getFDestinationAddress());
            }
        }
    }

    private IProtocolManager getProtocolFamilyFor(byte protocolFam) {
        synchronized (protocolFamilyTable) {
            return (IProtocolManager) protocolFamilyTable.get(protocolFam);
        }
    }


    private IProtocolManager getProtocolFor(byte protocolNum) {
        synchronized (protocolTable) {
            return (IProtocolManager) protocolTable.get(protocolNum);
        }
    }

    /**
     * This method is called by receive() everytime a fragmented packet for this
     * node arrives. This method calls the appropriate reassemblyBuffer.
     *
     * @param packet fragment to be reassembled
     */
    private void reassembly(LowPanPacket lpp) {
        long originator;
        long destination;
        short datagramTag;
        short datagramSize;
        int datagramOffset;

        lpStats.fragmentsReceived++;
        //get relevant data
        if (lpp.isMeshed()) {
            originator = lpp.getOriginatorAddress();
        } else {
            originator = lpp.getRPSourceAddress();
        }

        // ATTENTION: according to the low pan spec the following line should
        // actually be destination = packet.getDestinationAddress();
        // here we are assuming that every packet that we are processing in the
        // low pan layer has our IEEE address as destination address. So we are
        // neglecting broadcast packets. We do this to deal with the "base
        // station hack": when the basestation is forwarding a packet to the
        // hostApplication  it replaces the destination address with a bitcode
        // representing the rssi and corr.
        destination = ourAddress;
        //destination = packet.getDestinationAddress();

        datagramTag = lpp.getFragTag();
        datagramSize = lpp.getFragSize();
        if (lpp.isFirstFrag()) {
            datagramOffset = 0;
        } else {
            datagramOffset = (int) (lpp.getFragOff() & 0xff);
        }
//        System.out.println("reassembly: orig= "+ IEEEAddress.toDottedHex(originator)
//        +" dest="+ IEEEAddress.toDottedHex(destination)
//        +" datagramSize="+datagramSize
//                +" datagramOffset="+datagramOffset
//                +" datagramTag="+datagramTag);
        //is there already a reassembly buffer?
        String key = Long.toString(originator) + Long.toString(destination)
                + Integer.toString(datagramTag) + Integer.toString(datagramSize);
        ReassemblyBuffer rb = (ReassemblyBuffer) reassemblyBuffers.get(key);
        //if not, create one
        if (rb == null) {
            //Debug.print("reassembly: no reassemblyBuffer - creating new one", 3);
            rb = new ReassemblyBuffer(datagramSize);
            reassemblyBuffers.put(key, rb);
            //if the buffer is not completed after 15 seconds, discard buffer
            ReassemblyExpiration ex = new ReassemblyExpiration(key, reassemblyBuffers, lpStats);
            reassemblyTimer.schedule(ex, REASSEMBLY_EXPIRATION_TIME);
        }

        if (lpp.isFirstFrag()) {
            //Debug.print("reassembly: first fragment received, setting protNum " +
            //"of rb to "+getProtocolNumber(packet), 3);
            rb.protocolNumber = lpp.getProtocol();
            rb.protocolFamily = lpp.getProtocolFamily();
        }

        //The firstByte of our fragment
        int firstByte = lpp.getLppPayloadOffset();
        int fragmentLength = lpp.getPayloadSize();

        boolean success = rb.write(datagramOffset, lpp.getRadioPacket(),
                firstByte, fragmentLength);
        if (!success) {
            reassemblyBuffers.remove(key);
            return;
        }
        if (rb.isComplete()) {
            // create a headerInfo to provide applications with fields of
            // radioHeader and if given meshHeader note that hopcount does not
            // provide the number of taken hops but should be zero all the time
            LowPanHeaderInfo lpHeaderInfo;
            if (lpp.isMeshed()) {
                RadioPacket packet = lpp.getRadioPacket();
                lpHeaderInfo = new LowPanHeaderInfo(packet.getDestinationAddress(),
                        packet.getSourceAddress(), packet.getRssi(),
                        packet.getCorr(), packet.getLinkQuality(),
                        packet.getDestinationPanID(), packet.getSourcePanID(),
                        true,
                        lpp.getOriginatorAddress(),
                        lpp.getFDestinationAddress(),
                        lpp.getHopsLeft(),
                        packet.getTimestamp());
                if (lpp.getFDestinationAddress() == 0xffff) lpStats.broadcastsReceived++;
                else lpStats.unicastsReceived++;
            } else {
                RadioPacket packet = lpp.getRadioPacket();
                // the packet was not meshDelivered. So source = originator and
                // destination = finalDestination
                // NOTE: only the values of the last radioPacket are stored in the
                // lpHeaderInfo
                lpHeaderInfo = new LowPanHeaderInfo(packet.getDestinationAddress(),
                        packet.getSourceAddress(), packet.getRssi(), packet.getCorr(),
                        packet.getLinkQuality(), packet.getDestinationPanID(),
                        packet.getSourcePanID(), true,
                        packet.getSourceAddress(), packet.getDestinationAddress(),
                        (byte) 0, packet.getTimestamp());
                if (packet.getDestinationAddress() == 0xffff) lpStats.broadcastsReceived++;
                else lpStats.unicastsReceived++;
            }
            // clean up the reassembly buffer
            lpStats.datagramsReassembled++;
            reassemblyBuffers.remove(key);
            IProtocolManager pm;

            if (rb.protocolFamily == LowPanHeader.DISPATCH_SPOT) {
                pm = getProtocolFor(rb.protocolNumber);
            } else {
                pm = getProtocolFamilyFor(rb.protocolFamily);
            }
            if (pm != null) {
                pm.processIncomingData(rb.buffer, lpHeaderInfo);
            } else lpStats.protocolHandlerMissing++;
        }
    }

    /**
     * set the IEEE Address for which we process packets
     *
     * @param addr our IEEE Address as a long
     */
    public void setOurAddress(long addr) {
        ourAddress = addr;
    }

    /**
     * return a copy of the lowpan statistics object
     */
    public LowPanStats getStatistics() {
        return lpStats.clone();
    }

    /**
     * This private class implements a thread to forward broadcast packets at a
     * different rate than unicast packets.  The idea is to limit the likelyhood
     * of collisions when multiple nodes repeat a broadcast.  It also mitigates the local
     * impact of forwarded broadcasts on unicast packets passing through the host.
     */
    private class BroadcastDispatcherThread extends Thread {
        LowPan lowpan;

        BroadcastDispatcherThread(LowPan lp) {
            super("BroadcastDispatcher");
            //      System.out.println("Created Broadcast forwarding thread");
            lowpan = lp;
        }

        private void monitorQueue() {
            while (true) {
                LowPanPacket lpp = null;
                while (lpp == null) {
                    lpp = (LowPanPacket) bCastQueue.get();
                }
                // Insert random delay if desired
                // We assume stack latency is roughly 13 milliseconds and we plan on
                // 10 windows for a max delay of 130ms on a broadcast packet
                int delay = randomGen.nextInt(10);
                //   System.out.println("Bcast delay: " + delay*13);
                Utils.sleep(delay * 13);

                // send the packet
                try {
                    lowpan.forwardMeshPacket(lpp);
                } catch (ChannelBusyException e) {
                    lpStats.droppedBroadcasts++;
                    Debug.print("[lowpan] Channel Busy: Broadcast discarded", 2);
                }
            }
        }

        public void run() {
            monitorQueue();
        }
    }

}


