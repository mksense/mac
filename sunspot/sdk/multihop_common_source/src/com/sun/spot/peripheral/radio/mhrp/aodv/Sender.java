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

package com.sun.spot.peripheral.radio.mhrp.aodv;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import com.sun.spot.peripheral.ChannelBusyException;
import com.sun.spot.peripheral.NoAckException;
import com.sun.spot.peripheral.radio.ILowPan;
import com.sun.spot.peripheral.radio.mhrp.aodv.messages.AODVMessage;
import com.sun.spot.peripheral.radio.mhrp.aodv.messages.RERR;
import com.sun.spot.peripheral.radio.mhrp.aodv.messages.RREP;
import com.sun.spot.peripheral.radio.mhrp.aodv.messages.RREQ;
import com.sun.spot.peripheral.radio.mhrp.aodv.request.RequestTable;
import com.sun.spot.peripheral.radio.mhrp.aodv.routing.RoutingTable;
import com.sun.spot.peripheral.radio.mhrp.interfaces.IMHEventListener;
import com.sun.spot.peripheral.radio.routing.interfaces.RouteEventClient;
import java.util.Random;
//import com.sun.spot.util.Debug;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Queue;
import com.sun.spot.util.Utils;

/**
 * @author Allen Ajit George
 * @version 0.1
 *
 */
public class Sender extends Thread {
    
    private boolean keepRunning = true;
    private Queue outgoingAODVMessageQueue;
    private Queue routeWantedQueue;
    private Queue routeErrorQueue;
    private ILowPan lowPan;
    private RoutingTable routingTable;
    private RequestTable requestTable;
    private Vector mhRouteListeners;
    private Random randomGen;
    private final Object queueLock = new Integer(0);
    private static final int MAX_RETRIES = 3;
    private static final int MAX_RETRY_DELAY = 50;
    private final long ourAddress;
    
    public Sender(long ourAddress, ILowPan lowPan, Vector listeners) {
        super("AODVSender");
        this.ourAddress = ourAddress;
        outgoingAODVMessageQueue = new Queue();
        routeWantedQueue = new Queue();
        routeErrorQueue = new Queue();
        this.lowPan = lowPan;
        this.mhRouteListeners = listeners;
        routingTable = RoutingTable.getInstance();
        requestTable = RequestTable.getInstance();
        randomGen = new Random();
        //Debug.print("Sender: started", 2);
        
    }
    
    /**
     * Spins on the outgoing message queue and calls the appropriate send methods
     */
    public void run() {
        AODVMessage message;
        RouteWantedEntry routeWanted;
        RouteErrorEntry routeError;
        
        try {
            // FIXME Reenable on production code
            // Thread.sleep(Constants.ACTIVE_ROUTE_TIMEOUT);
            
            while (keepRunning) {
                synchronized (queueLock) {
                    if (outgoingAODVMessageQueue.isEmpty()
                    && routeWantedQueue.isEmpty()
                    && routeErrorQueue.isEmpty()) {
                        queueLock.wait();
                    }
                    
                    // Forward any messages to be forwarded and RREPs first
                    while (!outgoingAODVMessageQueue.isEmpty()) {
                        message = (AODVMessage) outgoingAODVMessageQueue.get();
                        
                        try {
                            if (message != null) {
                                switch (message.getType()) {
                                    case Constants.RREQ_TYPE:
                                        // experimental to avoid collisions of RREQ-broadcasts
                                        try {
                                            int r = randomGen.nextInt(50);
                                            Thread.sleep(r * 5);
                                        } catch (InterruptedException ie) {
                                        }
                                        sendRREQ((RREQ) message, null, null);
                                        break;
                                    case Constants.RREP_TYPE:
                                        sendRREP((RREP) message);
                                        break;
                                    case Constants.RERR_TYPE:
                                        sendRERR((RERR) message);
                                        break;
                                    default:
                                        System.err.println(Sender.class.getName()
                                        + ": unknown AODV message type");
                                        break;
                                }
                            }
                        } catch (NoAckException e) {
                            //Debug.print("sender: ignoring missing RERR ack", 0);
                        } catch (ChannelBusyException e) {
                            //Debug.print("sender: ignoring channel busy exception", 0);
                        }
                    }
                    
                    // Now handle new RERR messages
                    while (!routeErrorQueue.isEmpty()) {
                        routeError = (RouteErrorEntry) routeErrorQueue.get();
                        sendRERR(new RERR(routeError.originator,
                                routeError.destination));
                    }
                    
                    // Now handle new RREQ messages
                    while (!routeWantedQueue.isEmpty()) {
                        routeWanted = (RouteWantedEntry) routeWantedQueue.get();
                        sendRREQ(new RREQ(ourAddress, routeWanted.address),
                                routeWanted.eventClient, routeWanted.uniqueKey);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    public void stopThread() {
        keepRunning = false;
        synchronized (queueLock) {
            queueLock.notifyAll();
        }
    }
    

    /**
     * Create an entry for the queue and put it in to the designated queue
     * @param address destination for which a route is wanted
     * @param eventClient the instance that is waiting for this route
     * @param uniqueKey identifier for this rreq
     * @return succes
     */
    public boolean sendNewRREQ(long address, RouteEventClient eventClient,
            Object uniqueKey) {
        RouteWantedEntry entry = new RouteWantedEntry();
        entry.address = address;
        entry.eventClient = eventClient;
        entry.uniqueKey = uniqueKey;
        
        routeWantedQueue.put(entry);
        
        synchronized (queueLock) {
            queueLock.notifyAll();
        }
        
        return true;
    }
    
    /**
     * Create an entry for the queue and put it in to the designated queue
     * @param originator
     * @param destination
     * @return succes
     */
    
    public boolean sendNewRERR(long originator, long destination) {
        RouteErrorEntry entry = new RouteErrorEntry();
        entry.originator = originator;
        entry.destination = destination;
        
        routeErrorQueue.put(entry);
        
        synchronized (queueLock) {
            queueLock.notifyAll();
        }
        
        return true;
    }
    
    /**
     * Create an entry for the queue and put it in to the designated queue
     * @param message
     * @return succes
     */
    
    public boolean sendNewRREP(RREP message) {
        outgoingAODVMessageQueue.put(message);
        
        synchronized (queueLock) {
            queueLock.notifyAll();
        }
        
        return true;
    }
    
    /**
     * put a message that must be forwarded into the queue
     * @return succes
     */
    
    public boolean forwardAODVMessage(AODVMessage message) {
        outgoingAODVMessageQueue.put(message);
        
        synchronized (queueLock) {
            queueLock.notifyAll();
        }
        
        return true;
    }
    
//  public void stop() {
//    keepRunning = false;
//
//    synchronized (queueLock) {
//      queueLock.notifyAll();
//    }
//  }
    /**
     * send out a route request
     * @param message
     * @param eventClient the instance that caused this request
     * @param uniqueKey indentifier for this request
     */
    private void sendRREQ(RREQ message, RouteEventClient eventClient,
            Object uniqueKey) throws ChannelBusyException, NoAckException {
//        Debug.print("[AODV] sendRREQMessage for "
//                + new IEEEAddress(message.getDestAddress()).asDottedHex()
//                + " at " + System.currentTimeMillis());


        byte[] buffer = message.writeMessage();
        // Debug.print("sendRREQ: about to send RREQ", 1);
        if (!requestTable.hasActiveRequest(message)) {
            requestTable.addRREQ(message, eventClient, uniqueKey);
        }
        lowPan.sendBroadcast(Constants.AODV_PROTOCOL_NUMBER, buffer, 0,
                buffer.length, 0);
        if (!mhRouteListeners.isEmpty()) {
            Enumeration en = mhRouteListeners.elements();
            while (en.hasMoreElements()) {
                ((IMHEventListener) en.nextElement()).RREQSent(message
                        .getOrigAddress(), message.getDestAddress(), message
                        .getHopCount());
                
            }
        }
        // Debug.print("sendRREQ: sent RREQ #" + message.getRequestID() + " from "
        //+ new IEEEAddress(message.getOrigAddress()).asDottedHex(), 1);
    }
    
    /**
     * send out a route reply
     * @param message
     */
    
    private void sendRREP(RREP message) throws ChannelBusyException {
        byte[] buffer = message.writeMessage();
        long destinationAddress =
                (routingTable.getNextHopInfo(message.getOrigAddress())).nextHop;
//        Debug.print ("[AODV] sendRREPMessage to " + new IEEEAddress(message.getOrigAddress()).asDottedHex()
//                + " through " + new IEEEAddress(destinationAddress).asDottedHex()
//                + " at " + System.currentTimeMillis());
        if (destinationAddress != Constants.INVALID_NEXT_HOP) {
            for (int i = 0; i < MAX_RETRIES; i++) {
                try {
                    lowPan.sendWithoutMeshingOrFragmentation(Constants.AODV_PROTOCOL_NUMBER,
                            destinationAddress, buffer, 0, buffer.length);
                    break;
                } catch (NoAckException e) {
//                    Debug.print("sendRREP: can't send RREP to "
//                            + new IEEEAddress(message.getOrigAddress()).asDottedHex()
//                            + " through "
//                            + new IEEEAddress(destinationAddress).asDottedHex()
//                            + " attempt " + i + "/" + MAX_RETRIES);
                    if (i < (MAX_RETRIES - 1)) {
                        Utils.sleep(randomGen.nextInt(MAX_RETRY_DELAY));
                    } else {
                        routingTable.deactivateRoute(message.getDestAddress(),
                                message.getOrigAddress());
                        // FIXME Remove our address from the routing entry
                        //Debug.print("sendRREP: adding a new RERR (shouldn't deadlock)", 1);
                        sendNewRERR(message.getDestAddress(), message.getOrigAddress());
                        //Debug.print("sendRREP: added a new RERR (didn't deadlock", 1);
                    }
                }
            }
        } else {
            //Debug.print("sendRREP: can't find a next hop for RREP", 1);
        }
        if (!mhRouteListeners.isEmpty()) {
            Enumeration en = mhRouteListeners.elements();
            while (en.hasMoreElements()) {
                ((IMHEventListener)
                en.nextElement()).RREPSent(message.getOrigAddress(),
                        message.getDestAddress(),
                        message.getHopCount());
                
            }
        }
        //Debug.print("sendRREP: sent RREP from "
        //+ new IEEEAddress(message.getDestAddress()).asDottedHex() +" to "
        //+ new IEEEAddress(message.getOrigAddress()).asDottedHex() +" through "
        //+ new IEEEAddress(cs.destinationAddress).asDottedHex(), 1);
    }
    
    /**
     * send out a route error
     * @param message
     */
    
    private void sendRERR(RERR message) throws ChannelBusyException {
        byte[] buffer = message.writeMessage();
        long destinationAddress =
                (routingTable.getNextHopInfo(message.getOrigAddress())).nextHop;
//        Debug.print(
//                "[AODV] sendRERRMessage to " + new IEEEAddress(message.getOrigAddress()).asDottedHex()
//                + " through " + new IEEEAddress(destinationAddress).asDottedHex()
//                + " at " + System.currentTimeMillis());
        if (destinationAddress != Constants.INVALID_NEXT_HOP) {
	    // Try only once, since we may be reporting *THIS* is broken
            try {
                lowPan.sendWithoutMeshingOrFragmentation(Constants.AODV_PROTOCOL_NUMBER,
                        destinationAddress, buffer, 0, buffer.length);
                
            } catch (NoAckException e) {
//                    Debug.print("[AODV] sendRERR: can't send RERR to "
//                            + new IEEEAddress(message.getOrigAddress()).asDottedHex()
//                            + " through "
//                            + new IEEEAddress(destinationAddress).asDottedHex()
//                            + " attempt " + i + "/" + MAX_RETRIES);
                routingTable.deactivateRoute(message.getDestAddress(),
                        message.getOrigAddress());
                // FIXME Remove our address from the routing entry
            }

        } else {
            //Debug.print("sendRERR: can't find a next hop for RERR", 1);
        }
        if (!mhRouteListeners.isEmpty()) {
            Enumeration en = mhRouteListeners.elements();
            while (en.hasMoreElements()) {
                ((IMHEventListener)
                en.nextElement()).RERRSent(message.getOrigAddress(),
                        message.getDestAddress());
                
            }
        }
        //Debug.print("sendRERR: sent RERR to " + new IEEEAddress(message.getOrigAddress()).asDottedHex()
        //+ " through " + new IEEEAddress(cs.destinationAddress).asDottedHex(), 1);
    }
    
    private class RouteWantedEntry {
        public long address;
        public RouteEventClient eventClient;
        public Object uniqueKey;
    }
    
    private class RouteErrorEntry {
        public long originator;
        public long destination;
    }
}
