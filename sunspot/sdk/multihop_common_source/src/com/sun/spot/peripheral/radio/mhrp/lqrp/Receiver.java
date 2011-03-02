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

import java.util.Enumeration;
import java.util.Vector;

import com.sun.spot.peripheral.radio.ILowPan;
import com.sun.spot.peripheral.radio.IProtocolManager;
import com.sun.spot.peripheral.radio.LowPan;
import com.sun.spot.peripheral.radio.LowPanHeaderInfo;
import com.sun.spot.peripheral.radio.routing.RouteInfo;
import com.sun.spot.peripheral.radio.mhrp.lqrp.messages.LQRPMessage;
import com.sun.spot.peripheral.radio.mhrp.lqrp.messages.RERR;
import com.sun.spot.peripheral.radio.mhrp.lqrp.messages.RREP;
import com.sun.spot.peripheral.radio.mhrp.lqrp.messages.RREQ;
import com.sun.spot.peripheral.radio.mhrp.lqrp.request.RequestTable;
import com.sun.spot.peripheral.radio.mhrp.lqrp.routing.RoutingTable;
import com.sun.spot.peripheral.radio.routing.interfaces.IRoutingPolicyManager;
import com.sun.spot.peripheral.radio.routing.interfaces.RouteEventClient;
import com.sun.spot.peripheral.radio.mhrp.interfaces.ILQRPEventListener;
import com.sun.spot.peripheral.radio.mhrp.interfaces.IMHEventListener;
import com.sun.spot.peripheral.radio.mhrp.lqrp.linkParams.ConfigLinkParams;
import com.sun.spot.peripheral.radio.mhrp.lqrp.linkParams.NbrLinkInfo;
import com.sun.spot.peripheral.radio.mhrp.lqrp.linkParams.NodeLifeAndLinkMonitor;
import com.sun.spot.peripheral.radio.mhrp.lqrp.messages.LQREP;
import com.sun.spot.peripheral.radio.mhrp.lqrp.messages.LQREQ;
import com.sun.spot.peripheral.radio.mhrp.lqrp.request.RequestEntry;
import com.sun.spot.peripheral.radio.mhrp.lqrp.routing.RoutingEntry;
import com.sun.spot.util.Debug;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Queue;
/**
 * @author Allen Ajit George, modification by Pradip De and Pete St. Pierre
 * @version 0.1
 */
public class Receiver extends Thread implements IProtocolManager {
    
    private RoutingTable routingTable = RoutingTable.getInstance();
    private RequestTable requestTable = RequestTable.getInstance();
    
    private static final int MAX_REQUESTS_OUTSTANDING = 500;
    private Sender sender;
    private boolean keepRunning = true;
    private Queue messageQueue;
    private Vector MHListeners;
    private Vector lqrpListeners;
    private IRoutingPolicyManager rpm;
    private final long ourAddress;
    
    private NodeLifeAndLinkMonitor linkMonitor = NodeLifeAndLinkMonitor.getInstance();
    
    /**
     * constructs a new receiver thread.
     * @param ourAddr
     * @param sndr
     * @param lowPan
     * @param listeners
     * @param LQListeners 
     */
    public Receiver(long ourAddr, Sender sndr, ILowPan lowPan, Vector listeners, Vector LQListeners) {
        super("LQRPReceiver");
        sender = sndr;
        ourAddress = ourAddr;
        messageQueue = new Queue();
        lowPan.registerProtocol(Constants.LQRP_PROTOCOL_NUMBER, this);
        MHListeners = listeners;
        lqrpListeners = LQListeners;
        rpm = ((LowPan)lowPan).getRoutingPolicyManager();
        
        //Debug.print("receiver: started", 2);
    }
    
    /**
     * incoming packets are evaluated and the appropriate message handler is called
     */
    public void run() {
        ReceivedPacket receivedPacket;
        
        while (keepRunning) {
            receivedPacket = (ReceivedPacket) messageQueue.get();
            if (receivedPacket != null) {
                switch (receivedPacket.message.getType()) {
                    case Constants.RREQ_TYPE:
                        handleRREQMessage((RREQ) receivedPacket.message, receivedPacket.messageSender);
                        break;
                    case Constants.RREP_TYPE:
                        handleRREPMessage((RREP) receivedPacket.message, receivedPacket.messageSender);
                        break;
                    case Constants.RERR_TYPE:
                        handleRERRMessage((RERR) receivedPacket.message, receivedPacket.messageSender);
                        break;
                        
                    case Constants.LQREQ_TYPE:
                        handleLQREQMessage((LQREQ) receivedPacket.message, receivedPacket.messageSender);
                        break;
                    case Constants.LQREP_TYPE:
                        handleLQREPMessage((LQREP) receivedPacket.message, receivedPacket.messageSender);
                        break;

                    default:
                        break;
                }
            }
        }
    }
    
    public void stopThread() {
        keepRunning = false;
        messageQueue.put(null);
    }
    
    /**
     * Handler for route error messages, deactivates the broken route.
     * If necessary the message is forwarded.
     *
     * @param message
     * @param lastHop the node that this message was sent from
     */
    private void handleRERRMessage(RERR message, long lastHop) {
//        Debug.print("[LQRP] handle RERRMessage from " + new IEEEAddress(lastHop) + " for route to "
//                + new IEEEAddress(message.getDestAddress()) + " as requested by "
//                + new IEEEAddress(message.getOrigAddress()) + " at " + System.currentTimeMillis());
        
        
        routingTable.deactivateRoute(ourAddress, message.getDestAddress());
        if (message.getOrigAddress() == ourAddress) {
            // Debug.print("handleRERRMessage: RERR for this node", 1);
        } else {
            routingTable.deactivateRoute(message.getOrigAddress(), message.getDestAddress());
            sender.forwardLQRPMessage(message);
        }
        if (!lqrpListeners.isEmpty()) {
            Enumeration en = lqrpListeners.elements();
            while (en.hasMoreElements()) {
                ((ILQRPEventListener) en.nextElement()).RERRReceived(message.getOrigAddress(), message.getDestAddress());
            }
        }
        if (!MHListeners.isEmpty()) {
            Enumeration en = MHListeners.elements();
            while (en.hasMoreElements()) {
                ((IMHEventListener) en.nextElement()).RERRReceived(message.getOrigAddress(), message.getDestAddress());
            }
        }
    }
    
    /**
     * Handler for route reply messages. A new route is added, if necessary the
     * message is forwarded.
     *
     * @param message
     * @param lastHop  the node we got this message from
     */
    private void handleRREPMessage(RREP message, long lastHop) {
        
//        Debug.print("[LQRP] handle RREPMessage from " + new IEEEAddress(lastHop)
//        + " for route to " + new IEEEAddress(message.getDestAddress())
//        + " as requested by " + new IEEEAddress(message.getOrigAddress())
//        + " at " + System.currentTimeMillis());

        message.incrementHopCount();
        
        if (message.getOrigAddress() == (long)0xffff) {
//            Debug.print("[AODV] Processing Neighbor Advertisement");
            // getNextHopInfo will freshen the route if it exists
            RouteInfo ri = routingTable.getNextHopInfo(message.getDestAddress());
            if (ri.nextHop == Constants.INVALID_NEXT_HOP) {
                routingTable.addRoute(lastHop, message);
            }
            
            return;
        }
        //Debug.print("handleRREPMessage: RREP from " + IEEEAddress.toDottedHex(lastHop), 1);

        double lq = message.getRevLastHopCost();
        routingTable.addRoute(lastHop, lastHop, 1, lq, (lq < ConfigLinkParams.THRESH_LOW_LQI ? 1 : 0), Constants.UNKNOWN_SEQUENCE_NUMBER);
        routingTable.addRoute(lastHop, message);
        
        if (message.getOrigAddress() == ourAddress) {
            RequestEntry entry = null;
            while ((entry = requestTable.findRequest(message.getDestAddress(), message.getOrigAddress(), 0, true)) != null) {
                // only notify client about first returned route
                requestTable.removeOutstandingRequest(message.getDestAddress(), message.getOrigAddress(), entry.requestID);
                RouteEventClient client = entry.client;
                if (client != null) {
                    RouteInfo info = new RouteInfo(message.getDestAddress(), lastHop, message.getHopCount());
                    client.routeFound(info, entry.uniqueKey);
                } else {
                    Debug.print("handleRREPMessage: null client asssociated"
                            + " Destination " + IEEEAddress.toDottedHex(message.getDestAddress())
                            + " Originator " + IEEEAddress.toDottedHex(message.getOrigAddress()), 1);
                }
            }
        } else if (!rpm.isEndNode()) { // Only forward RREP if not an end node
            sender.forwardLQRPMessage(message);
        }
        if (!lqrpListeners.isEmpty()) {
            Enumeration en = lqrpListeners.elements();
            while (en.hasMoreElements()) {
                ((ILQRPEventListener)
                en.nextElement()).RREPReceived(message.getOrigAddress(),
                        message.getDestAddress(),
                        lastHop,
                        message.getHopCount(),
                        message.getRouteCostToDest());//This is the updated routeCost to Dest done by prev sender
                
                
            }
        }
        if (!MHListeners.isEmpty()) {
            Enumeration en = MHListeners.elements();
            while (en.hasMoreElements()) {
                ((IMHEventListener)
                en.nextElement()).RREPReceived(message.getOrigAddress(),
                        message.getDestAddress(),
                        message.getHopCount());
            }
        }
    }
    
    /**
     * Handler for route request messages. Eventually new routes are added. If necessary
     * message is forwarded, or a route reply is sent.
     * @param message
     * @param lastHop
     */
    private void handleRREQMessage(RREQ message, long lastHop) {
//        Debug.print("[LQRP] handle RREQMessage from " + new IEEEAddress(lastHop) + " for route to "
//                + new IEEEAddress(message.getDestAddress()) + " as requested by "
//                + new IEEEAddress(message.getOrigAddress()) + " at " + System.currentTimeMillis());
        message.incrementHopCount();
        if (message.getHopCount() > Constants.NET_DIAMETER) {
            return;  // Don't forward request
        }

        double lastHopLinkCost = message.updateRouteCost(lastHop);//Assume symmetric link to set up reverse route
        //double lastHopLinkCost = 1.0 / (linkMonitor.getCurrNormLQForAddress(lastHop));

        if (requestTable.hasActiveRequest(message)) {
            RoutingEntry entry = routingTable.getEntry(message.getOrigAddress());
            if (entry != null) {
//                System.out.println("Got 2nd RREQ from " + IEEEAddress.toDottedHex(lastHop) +
//                    "     reqID = " + message.getRequestID() +
//                    "\n   new cost = " + message.getRouteCost() + " / " + message.getLowLQlinkCount() +
//                    "\n   old cost = " + entry.routeCost + " / " + entry.lowLQlinkCount);
            }
            if (entry != null && entry.activityFlag && 
                    (entry.lowLQlinkCount < message.getLowLQlinkCount() ||
                    (entry.lowLQlinkCount == message.getLowLQlinkCount() &&
                     entry.routeCost <= message.getRouteCost()))) {
                return;     // already have forwarded a better route request
            }
            // remove old request & replace with new one so request timeout is set properly
            requestTable.removeOutstandingRequest(message.getDestAddress(),
                                                  message.getOrigAddress(),
                                                  message.getRequestID());
        } else if (requestTable.hasRequest(message)) {
            return;     // ignore repeat requests during grace period after active period has expired
        }

        requestTable.addRREQ(message, null, null);
        routingTable.addRoute(lastHop, lastHop, 1, lastHopLinkCost, (lastHopLinkCost < ConfigLinkParams.THRESH_LOW_LQI ? 1 : 0), Constants.UNKNOWN_SEQUENCE_NUMBER);
        routingTable.addRoute(lastHop, message);

        int currentSequenceNumber = routingTable.getDestinationSequenceNumber(message.getDestAddress());
        int receivedSequenceNumber = message.getDestSeqNum();

        if (currentSequenceNumber > receivedSequenceNumber) {
            message.setDestinationSequenceNumber(currentSequenceNumber);
        }

        if (message.getDestAddress() == ourAddress) {
            // Debug.print("handleRREQMessage: RREQ for this node", 1);
            RREP routeFoundMessage = new RREP(message);
            sender.sendNewRREP(routeFoundMessage);
        } else if (!rpm.isEndNode()) {// Only forward RREQ if not an end node
            sender.forwardLQRPMessage(message);
        }

        if (!lqrpListeners.isEmpty()) {
            Enumeration en = lqrpListeners.elements();
            while (en.hasMoreElements()) {
                ((ILQRPEventListener) en.nextElement()).RREQReceived(message.getOrigAddress(),
                        message.getDestAddress(),
                        lastHop,
                        message.getHopCount(),
                        message.getRouteCost());
            }
        }
        if (!MHListeners.isEmpty()) {
            Enumeration en = MHListeners.elements();
            while (en.hasMoreElements()) {
                ((IMHEventListener) en.nextElement()).RREQReceived(message.getOrigAddress(),
                        message.getDestAddress(),
                        message.getHopCount());
            }
        }
    }
    
    /**
     * Handler for link quality request messages.
     * @param message
     * @param lastHop
     */
    private void handleLQREQMessage(LQREQ message, long requestor) {
//        System.out.println("Received LQREQ from " + IEEEAddress.toDottedHex(requestor));
        NbrLinkInfo info = linkMonitor.getNbrLinkInfoWithAddress(requestor);
        if (info == null) {
            info = linkMonitor.addLinkWithAddress(requestor, message.getReceivedCost());
        }
        info.setNbrLQ(message.getRouteCost());
        info.setOurNbrLQ(info.getCurrNormalizedLQ());
        message.updateRequestorAddress(requestor);
        sender.forwardLQRPMessage(new LQREP(message, info.getCurrNormalizedLQ()));
    }

    /**
     * Handler for link quality reply messages.
     * @param message
     * @param lastHop
     */
    private void handleLQREPMessage(LQREP message, long lastHop) {
        linkMonitor.setNbrLQ(lastHop, message.getRouteCost());
//        System.out.println("Received LQREP from " + IEEEAddress.toDottedHex(lastHop));
    }

    /**
     * This method is called whenever the low pan layer receives a packet that
     * carries this routing manager's protocol number. It implements the
     * protocol managers interface.
     *
     * @param payload
     * @param headerInfo
     */
    public void processIncomingData(byte[] payload, LowPanHeaderInfo headerInfo) {
        byte LQRPMessageType = payload[0];
        switch (LQRPMessageType) {
            case Constants.RREQ_TYPE:
//                Debug.print("receiveData: RREQ from " + IEEEAddress.toDottedHex(headerInfo.sourceAddress), 1);
                RREQ rreqMessage = new RREQ(payload);
                if (rreqMessage.getOrigAddress() != ourAddress &&
                        messageQueue.size() < MAX_REQUESTS_OUTSTANDING) {
                        messageQueue.put(new ReceivedPacket(rreqMessage, headerInfo.sourceAddress));
                    }
                break;
            case Constants.RREP_TYPE:
//                Debug.print("receiveData: RREP from " + IEEEAddress.toDottedHex(headerInfo.sourceAddress), 1);
                messageQueue.put(new ReceivedPacket(new RREP(payload), headerInfo.sourceAddress));
                break;
            case Constants.RERR_TYPE:
//                Debug.print("receiveData: RERR from " + IEEEAddress.toDottedHex(headerInfo.sourceAddress), 1);
                messageQueue.put(new ReceivedPacket(new RERR(payload), headerInfo.sourceAddress));
                break;

            case Constants.LQREQ_TYPE:
//                Debug.print("receiveData: LQREQ from " + IEEEAddress.toDottedHex(headerInfo.sourceAddress), 1);
                messageQueue.put(new ReceivedPacket(new LQREQ(payload, headerInfo.linkQuality), headerInfo.sourceAddress));
                break;
            case Constants.LQREP_TYPE:
//                Debug.print("receiveData: LQREP from " + IEEEAddress.toDottedHex(headerInfo.sourceAddress), 1);
                messageQueue.put(new ReceivedPacket(new LQREP(payload), headerInfo.sourceAddress));
                break;

            default:
                System.err.println("receiveData: bad packet from"
                        + IEEEAddress.toDottedHex(headerInfo.sourceAddress));
                break;
        }
    }
    
    private class ReceivedPacket {
        public ReceivedPacket(LQRPMessage message, long messageSender) {
            this.message = message;
            this.messageSender = messageSender;
        }
        
        public LQRPMessage message;
        public long messageSender;
    }
}
