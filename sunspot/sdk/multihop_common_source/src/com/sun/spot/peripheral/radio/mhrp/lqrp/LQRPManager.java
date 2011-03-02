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

package com.sun.spot.peripheral.radio.mhrp.lqrp;

import com.sun.spot.peripheral.NoRouteException;
import java.util.Vector;

import com.sun.spot.peripheral.radio.IPacketQualityListener;
import com.sun.spot.peripheral.radio.RadioPacketDispatcher;
import com.sun.spot.peripheral.radio.mhrp.interfaces.ILQRPEventListener;
import com.sun.spot.peripheral.radio.mhrp.lqrp.linkParams.NodeLifeAndLinkMonitor;

import com.sun.spot.peripheral.radio.ILowPan;
import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.peripheral.radio.routing.RouteInfo;
import com.sun.spot.peripheral.radio.mhrp.lqrp.request.RequestTable;
import com.sun.spot.peripheral.radio.mhrp.lqrp.routing.RoutingEntry;
import com.sun.spot.peripheral.radio.mhrp.lqrp.routing.RoutingNeighbor;
import com.sun.spot.peripheral.radio.mhrp.lqrp.routing.RoutingTable;
import com.sun.spot.peripheral.radio.mhrp.interfaces.IMHEventListener;
import com.sun.spot.peripheral.radio.routing.RouteTable;
import com.sun.spot.peripheral.radio.routing.RoutingPolicyManager;
import com.sun.spot.peripheral.radio.routing.interfaces.IRoutingManager;
import com.sun.spot.peripheral.radio.routing.interfaces.RouteEventClient;
import com.sun.spot.service.IService;
import com.sun.spot.util.Debug;
import java.util.Enumeration;

// import com.sun.spot.util.Debug;
/**
 * @author Allen Ajit George, modification by Pradip De and Pete St. Pierre
 * @version 0.1
 */
public class LQRPManager implements IRoutingManager {

    private static String name = "LQRPManager";
    private Sender sender;
    private Receiver receiver;
    private RoutingTable routingTable;
    private RequestTable requestTable;
    private Vector mhRouteListeners;
    private Vector lqrpListeners;
    private long sequenceNumber = Constants.UNKNOWN_SEQUENCE_NUMBER;
    private final Object sequenceLock = new Integer(0);
    private long ourAddress;
    private static LQRPManager instance;
    private NodeLifeAndLinkMonitor linkMonitor;
    private ILowPan lp;
    private int state = IService.STOPPED;
    private RoutingNeighbor advertizer;
    private boolean advertise = true;

    /**
     * constructs a new LQRPManager
     */
    private LQRPManager() {
        mhRouteListeners = new Vector();
        lqrpListeners = new Vector();
        linkMonitor = NodeLifeAndLinkMonitor.getInstance();
        requestTable = RequestTable.getInstance();
        routingTable = RoutingTable.getInstance();
    }

    /**
     * @return lqrpManager instance of this singleton
     */
    public static synchronized LQRPManager getInstance() {
        if (instance == null) {
            instance = new LQRPManager();
        }

        return instance;
    }

    /**
     * initializes this routing manager by starting the sender and receiver
     * threads
     *
     * @param lowPan
     *            LowPan layer that is our route client
     */
    public synchronized void initialize(long ourAddress, ILowPan lowPan) {
        this.ourAddress = ourAddress;
        this.lp = lowPan;
        requestTable.setOurAddress(ourAddress);
        routingTable.setOurAddress(ourAddress);
        linkMonitor.initialize(ourAddress);
    }

    /**
     * This method returns a snapshot of the routing table
     */
    public RouteTable getRoutingTable() {
        RouteTable rt = new RouteTable();
        Vector v = routingTable.getAllEntries();
        Enumeration e = v.elements();
        while (e.hasMoreElements()) {
            RoutingEntry re = (RoutingEntry) e.nextElement();
            rt.addEntry(new RouteInfo(re.key.longValue(), re.nextHopMACAddress, re.hopCount));
        }
        return rt;
    }

    /**
     * This method is called to obtain a route info, and to refreshen a route
     * whenever it is used
     *
     * @param address
     *            destination for which this method returns a route info
     * @return routeInfo
     */
    public RouteInfo getRouteInfo(long address) {
        RouteInfo info = routingTable.getNextHopInfo(address);

        if ((info.nextHop != Constants.INVALID_NEXT_HOP) && (info.nextHop != address)) {
            // Is this valid? routingTable.freshenRoute(address);
            routingTable.freshenRoute(info.nextHop);
        }

        return info;
    }

    /**
     * This method triggers a new route request.
     * Note: the radio must be on or no route will be found.
     *
     * @return true when finished
     *
     */
    public boolean findRoute(long address, RouteEventClient eventClient, Object uniqueKey)
            throws NoRouteException {
//        System.out.println("[LQRP] initate route lookup");
        if (sender != null) {
            return sender.sendNewRREQ(address, eventClient, uniqueKey);
        } else {
            throw new NoRouteException("LQRP Not running");
        }
    }

    /*
     * WARNING WARNING WARNING: TESTING ONLY!!!!
     *
     */
    public boolean initiateRouteDiscovery(long address) {
        return sender.sendNewRREQ(address, null, null);
    }

    /**
     * This method is called when it is recognized that the route is broken
     * somewhere. The route is marked deactivated and a RERR is being sent.
     *
     * @return true when finished
     */
    public boolean invalidateRoute(long originator, long destination) {
        routingTable.deactivateRoute(originator, destination);
        if (originator == ourAddress) {
            return true;
        }
        return sender.sendNewRERR(originator, destination);
    }

    /**
     * Registers an application etc. that is notified when this node processes
     * supported route events
     */
    public void addEventListener(IMHEventListener listener) {
        this.mhRouteListeners.addElement(listener);
    }

    /**
     * Deregisters an application etc. that was registered for route events
     */
    public void removeEventListener(IMHEventListener listener) {
        this.mhRouteListeners.removeElement(listener);
    }

    // These two routines handle the deprecated calls - for now
    /**
     * @deprecated
     * replaced by addEventListener
     */
    public void registerEventListener(IMHEventListener listener) {
        addEventListener(listener);
    }

    /**
     * @deprecated
     * replaced by removeEventListener
     */
    public void deregisterEventListener(IMHEventListener listener) {
        removeEventListener(listener);
    }

    /**
     * Registers an application etc. that is notified when this node processes
     * supported route events
     */
    public void addLQRPListener(ILQRPEventListener listener) {
        // System.out.println("[LQRP] registered listener");
        this.lqrpListeners.addElement(listener);
    }

    /**
     * Deregisters an application etc. that was registered for route events
     */
    public void removeLQRPListener(ILQRPEventListener listener) {
        this.lqrpListeners.removeElement(listener);
    }

    /**
     * @deprecated 
     * replaced by addLQRPListener
     */
    public void registerLQRPListener(ILQRPEventListener listener) {
        addLQRPListener(listener);
    }

    /**
     * @deprecated 
     * replaced by removeLQRPListener
     */
    public void deregisterLQRPListener(ILQRPEventListener listener) {
        removeLQRPListener(listener);
    }

    /**
     * This method creates new sequence numbers.
     *
     * @return seqNumber the next sequence number.
     */
    public int getNextSequenceNumber() {
        long returnedNumber;
        synchronized (sequenceLock) {
            if (sequenceNumber == Constants.MAX_SEQUENCE_NUMBER) {
                sequenceNumber = Constants.FIRST_VALID_SEQUENCE_NUMBER;
                returnedNumber = sequenceNumber;
            } else {
                sequenceNumber++;
                returnedNumber = sequenceNumber;
            }
        }

        return (int) returnedNumber;
    }

    /**
     * This method creates new sequence numbers.
     *
     * @param givenNumber
     * @return seqNumber the next sequence number that is greater or equal than
     *         the given number.
     */
    public int getNextSequenceNumber(int givenNumber) {
        long returnedNumber;
        synchronized (sequenceLock) {
            if (givenNumber++ > Constants.MAX_SEQUENCE_NUMBER) {
                sequenceNumber = Constants.FIRST_VALID_SEQUENCE_NUMBER;
                returnedNumber = sequenceNumber;
            } else {
                sequenceNumber = givenNumber;
                returnedNumber = sequenceNumber;
            }
        }

        return (int) returnedNumber;
    }

    /**
     * @return the current sequence number
     */
    public int getCurrentSequenceNumber() {
        return (int) sequenceNumber;
    }

    public NodeLifeAndLinkMonitor getLinkMonitor() {
        return linkMonitor;
    }

    /**
     * Assign a name to this service. For some fixed services this may not apply and
     * any new name will just be ignored.
     *
     * @param who the name for this service
     */
    public void setServiceName(String who) {
        name = who;
    }

    /**
     * Enable/disable whether service is started automatically.
     * This may not apply to some services and calls to setEnabled() may be ignored.
     *
     * @param enable true if the service should be started automatically on reboot
     */
    public void setEnabled(boolean enable) {
    }

    /**
     * Control if an advertising thread will be run.
     * <p>
     * The AODV Routing Manager normally starts up a special thread to periodically
     * send out a route reply message to advertise this nodes presence to its neighbors.
     * If a SPOT application will be deep sleeping it may want to disable this
     * advertising thread, so the SPOT will not wake up every 30 seconds.
     *
     * @param enable true if advertisements should be sent periodically.
     */
    public void enableAdvertising(boolean enable) {
        if (advertise != enable) {
            advertise = enable;
            if (enable) {
                if (state == IService.RUNNING && !RoutingPolicyManager.getInstance().isEndNode()) {
                    advertizer = new RoutingNeighbor(lp, ourAddress);
                    advertizer.start();
                }
            } else {
                if (advertizer != null) {
                    advertizer.stopThread();
                    advertizer = null;
                }
            }
        }
    }

    /**
     * Stop the service, and return whether successful.
     * Stops all running threads. Closes any open IO connections.
     *
     * @return true if the service was successfully stopped
     */
    public boolean stop() {
        if (state != IService.STOPPED) {
            linkMonitor.setSender(null);
            if (sender != null) {
                sender.stopThread();
                sender = null;
            }
            if (receiver != null) {
                receiver.stopThread();
                receiver = null;
            }
            if (advertizer != null) {
                advertizer.stopThread();
                advertizer = null;
            }

            requestTable.stop();
            routingTable.stop();
            lp.deregisterProtocol(Constants.LQRP_PROTOCOL_NUMBER);
            RadioPacketDispatcher.getInstance().removePacketQualityListener((IPacketQualityListener) linkMonitor);

            state = IService.STOPPED;
        }
        return true;

    }

    /**
     * Start the service, and return whether successful.
     *
     * @return true if the service was successfully started
     */
    public boolean start() {
        Debug.print("[" + name + "]" + "Starting");
        if (state != IService.RUNNING) {
            state = IService.STARTING;

            if (!RoutingPolicyManager.getInstance().isEndNode() && advertise) {
                advertizer = new RoutingNeighbor(lp, ourAddress);
                advertizer.start();
            } else {
                advertizer = null;
            }

            requestTable.start();
            routingTable.start();
            sender = new Sender(ourAddress, lp, mhRouteListeners, lqrpListeners);
            receiver = new Receiver(ourAddress, sender, lp, mhRouteListeners, lqrpListeners);
            RadioFactory.setAsDaemonThread(sender);
            RadioFactory.setAsDaemonThread(receiver);
            sender.start();
            receiver.start();
            RadioPacketDispatcher.getInstance().addPacketQualityListener((IPacketQualityListener) linkMonitor);
            linkMonitor.setSender(sender);

            state = IService.RUNNING;
        }
        return true;
    }

    /**
     * Resume the service, and return whether successful.
     * Picks up from state when service was paused.
     *
     * If there was no particular state associated with this service
     * then resume() can be implemented by calling start().
     *
     * @return true if the service was successfully resumed
     */
    public boolean resume() {
        return start();
    }

    /**
     * Pause the service, and return whether successful.
     * Preserve any current state, but do not handle new requests.
     * Any running threads should block or sleep.
     * Any open IO connections may be kept open.
     *
     * If there is no particular state associated with this service
     * then pause() can be implemented by calling stop().
     *
     * @return true if the service was successfully paused
     */
    public boolean pause() {
        return stop();
    }

    /**
     * Return whether the service is currently running.
     *
     * @return true if the service is currently running
     */
    public boolean isRunning() {
        return (state == IService.RUNNING);
    }

    /**
     * Return the current status of this service.
     *
     * @return the current status of this service, e.g. STOPPED, STARTING, RUNNING, PAUSED, STOPPING, etc.
     */
    public int getStatus() {
        return state;
    }

    /**
     * Return the name of this service.
     *
     * @return the name of this service
     */
    public String getServiceName() {
        return name;
    }

    /**
     * Return whether service is started automatically on reboot.
     * This may not apply to some services and for those services it will always return false.
     *
     * @return true if the service is started automatically on reboot
     */
    public boolean getEnabled() {
        return false;
    }
}
