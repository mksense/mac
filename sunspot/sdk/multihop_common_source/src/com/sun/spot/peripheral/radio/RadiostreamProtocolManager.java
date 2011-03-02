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


import java.util.Timer;

import com.sun.spot.interisolate.InterIsolateServer;
import com.sun.spot.peripheral.ChannelBusyException;
import com.sun.spot.peripheral.NoAckException;
import com.sun.spot.peripheral.NoRouteException;
import com.sun.spot.peripheral.radio.LowPanHeader;
import com.sun.spot.peripheral.radio.proxy.IRadioServerContext;
import com.sun.spot.peripheral.radio.proxy.ProxyRadiostreamProtocolManager;
import com.sun.spot.peripheral.radio.routing.RouteInfo;
import com.sun.spot.peripheral.radio.routing.interfaces.IRoutingManager;
import com.sun.spot.util.Debug;
import com.sun.spot.util.Queue;
import com.sun.spot.util.Utils;


/* (non-Javadoc)
 * @see com.sun.squawk.peripheral.radio.IPortBasedProtocolManager#addConnection(long, byte, boolean)
 */
public class RadiostreamProtocolManager extends RadioProtocolManager implements IProtocolManager, IRadiostreamProtocolManager {
	/*
	 * Note on synchronization:
	 * All methods that rely on there being no external changes to the connections hashtable
	 * during their execution are synchronized.
	 */

	public static final byte PROTOCOL_NUMBER = 104;
	public static final String PROTOCOL_NAME = "radiostream";
	private static IRadiostreamProtocolManager theInstance;
	
	static final int NUMBER_OF_RETRIES = 5;  // was 3;
	static final byte CTRL_ACK = 2;
	static final byte CTRL_ACK_REQUIRED = 4;
	static int RETRANSMIT_BASE_TIMEOUT = 500;  // was 15000; // not final to aid testing
	static int RETRANSMIT_PER_HOP_TIMEOUT = 250;
    private static final int WINDOW_SIZE = 50;

	private Timer retransScheduler;
    private Queue inputQueue;
	private InputHandler inputHandler;
    private static IRoutingManager routingManager;
	
	public static void main(String[] args) {
		InterIsolateServer.run(ProxyRadiostreamProtocolManager.CHANNEL_IDENTIFIER,
			new IRadioServerContext() {
				public IRadioProtocolManager getRadioProtocolManager() {
					return RadiostreamProtocolManager.getInstance();
				}
		});
	}

	public synchronized static IRadiostreamProtocolManager getInstance() {
		if (theInstance == null) {
			if (RadioFactory.isMasterIsolate()) {
				theInstance = new RadiostreamProtocolManager();
			} else {
				theInstance = new ProxyRadiostreamProtocolManager(PROTOCOL_NUMBER, PROTOCOL_NAME);
			}
		}
		return theInstance;
	}
	
	RadiostreamProtocolManager(ILowPan lowpan, IRadioPolicyManager radioPolicyManager) {
		super(lowpan, radioPolicyManager);
        routingManager = lowpan.getRoutingManager();
		retransScheduler = new Timer();
		inputQueue = new Queue();
		inputHandler = new InputHandler();
        RadioFactory.setAsDaemonThread(inputHandler);
		inputHandler.start();
	}

	/**
	 * Construct an instance to manage the given protocol number.
	 */
	public RadiostreamProtocolManager() {
		this(LowPan.getInstance(), RadioFactory.getRadioPolicyManager());
		lowpan.registerProtocol(PROTOCOL_NUMBER, this);
	}

	public long send(ConnectionID cid, long toAddress, byte[] payload, int length) throws NoAckException, ChannelBusyException, NoRouteException, NoMeshLayerAckException {
		/* Note: no need to sync this method because the connections hashtable is itself
		 * thread-safe, and it is accessed only once here.
		 */
		if (! cid.canSend())
			throw new IllegalArgumentException(cid.toString()+" cannot be used for sending");
		
		ConnectionState cs = (ConnectionState)connectionIDTable.get(cid);
        if (cs.status != ConnectionState.INTACT) {
            System.out.println("[Radiostream] Discovered broken connection - reporting");
            cs.removeAllRetransBuffers();
            cs.checkStatusAndReport();
            return 0; // should never be executed because an exception is expected
        }
		synchronized (cs) {
			payload[PORT_OFFSET] = cid.getPortNo();
			int newSeq = (cs.lastOutgoingSeq + 1) % 256;
			if (cs.lastOutgoingSeq==-1) {
				// send a control packet
				payload[CTRL_OFFSET] = CTRL_NEW_CONN;
				payload[SEQ_OFFSET] = (byte)newSeq;
                cs.lastOutgoingSeq = newSeq;
                cs.nextACKSeq = newSeq;
                newSeq = (newSeq + 1) % 256;
				sendData(cs, payload, DATA_OFFSET);
				cs.waitUntilNoRetransBuffers();
			}
            while (!inWindow(cs.nextACKSeq, newSeq)) {
                try {
                    cs.wait();
                } catch (InterruptedException ex) {
                    // ignore
                }
            }
			payload[CTRL_OFFSET] = 0;
			payload[SEQ_OFFSET] = (byte)newSeq;
			sendData(cs, payload, length);
			cs.lastOutgoingSeq  = newSeq;
		}
		return 0;
	}

	public void waitForAllAcks(ConnectionID outConnectionId) throws NoAckException, ChannelBusyException, NoMeshLayerAckException, NoRouteException {
		((ConnectionState)connectionIDTable.get(outConnectionId)).waitUntilNoRetransBuffers();
	}

	/* 
	 * This is the method called by the lowpan when it receives a packet for us.
	 * It just puts the message on the input queue to be pulled off and processed
	 * by the input handler thread.
	 */
	public void processIncomingData(byte[] payload, LowPanHeaderInfo headerInfo) {
		inputQueue.put(new IncomingData(payload, headerInfo));
	}
	
	/*
	 * This is the method called by the input handler thread to process packets that arrive for us.
	 */
	private void processIncomingData(IncomingData incoming) {
//		System.out.println("Processing incoming data from " + sourceAddress + " " + Utils.stringify(payload));
		byte portNumber = incoming.payload[PORT_OFFSET];
		if (isAck(incoming.payload)) {
			ConnectionState connectionState = getConnectionState(incoming.headerInfo.originator, OUTPUT, portNumber); 
			if (connectionState != null) {
                connectionState.removeRetransBuffer(incoming.payload[SEQ_OFFSET]);
                if (connectionState.nextACKSeq == (incoming.payload[SEQ_OFFSET] & 0xff)) {
                    synchronized (connectionState) {
                        connectionState.nextACKSeq = (connectionState.nextACKSeq + 1) % 256;
                        int lastOutgoingSeq = (connectionState.lastOutgoingSeq + 1) % 256;
                        while (connectionState.nextACKSeq != lastOutgoingSeq) {
                            if (connectionState.getRetransBuffer((byte)connectionState.nextACKSeq) != null) {
                                break;
                            } else {
                                connectionState.nextACKSeq = (connectionState.nextACKSeq + 1) % 256;
                            }
                        }
                        connectionState.notifyAll();
                    }
                }
            }
		} else {
            ConnectionState connectionState = getConnectionState(incoming.headerInfo.originator, INPUT, portNumber);
			if (connectionState != null) {
				if (isAckRequested(incoming.payload)) {
					sendAck(connectionState, incoming.payload[SEQ_OFFSET]);
				}
                synchronized (connectionState) {
                    checkSequenceNumberAndEnqueue(connectionState, incoming);
                }
			}
		}
	}

	private boolean isAck(byte[] payload) {
		return payload[CTRL_OFFSET] == CTRL_ACK;
	}

	private boolean isAckRequested(byte[] payload) {
		return (payload[CTRL_OFFSET] & CTRL_ACK_REQUIRED) != 0;
	}

	private boolean isNewConnection(byte[] payload) {
		return (payload[CTRL_OFFSET] & CTRL_NEW_CONN) != 0;
	}

	/**
	 * Send an ack for packet whose seq num was seqNum
	 */
	private void sendAck(ConnectionState connectionState, byte seqNum) {
        byte[] controlBuffer = new byte[] {connectionState.id.getPortNo(), seqNum, CTRL_ACK};
        for (int i = 0; i <= 5; i++) {
            int delay = 5;
            try {
                lowpan.send(LowPanHeader.DISPATCH_SPOT, PROTOCOL_NUMBER, connectionState.id.getMacAddress(), controlBuffer, 0, controlBuffer.length);
                break;
            } catch (NoRouteException ex) {
                delay = 200;
                Debug.print("[Radiostream] unable to send meshlayer ack " + (seqNum & 0xff) +
                        " due to no route exception.");
            // ex.printStackTrace();
            } catch (ChannelBusyException ex) {
                Debug.print("[Radiostream] unable to send meshlayer ack " + (seqNum & 0xff) +
                        " due to channel busy.");
            // ex.printStackTrace();
            }
            Utils.sleep(delay);
        }
	}

	public String getName() {
		return PROTOCOL_NAME;
	}

	void checkSequenceNumberAndEnqueue(ConnectionState connectionState, IncomingData incomingData) {
		int expectedSeq;
		int receivedSeq = incomingData.payload[SEQ_OFFSET] & 0xFF;
		boolean newConnectionRequested = isNewConnection(incomingData.payload);
		int previousSequenceNumber = connectionState.lastIncomingSeq;
		
		if (newConnectionRequested || previousSequenceNumber == -1) {
			expectedSeq = receivedSeq;
			connectionState.lastIncomingSeq = receivedSeq;
			connectionState.emptyReorderTable();
		} else {
			expectedSeq = (previousSequenceNumber + 1) % 256;
		}

		if (receivedSeq == previousSequenceNumber || newConnectionRequested) {
			// throwing away a duplicate || this was a control packet
		} else if (expectedSeq == receivedSeq) {
            do {
                connectionState.addToQueue(incomingData);
                expectedSeq = (expectedSeq + 1) % 256;
                incomingData = (IncomingData) connectionState.reorderTable.remove(expectedSeq);
            } while (incomingData != null);
            connectionState.lastIncomingSeq = (expectedSeq == 0) ? 255 : (expectedSeq - 1);
        } else {
            if (inWindow(expectedSeq, receivedSeq)) {
                connectionState.reorderTable.put(receivedSeq, incomingData);
            }
        }
	}

    private boolean inWindow(int windowStart, int seqNo) {
        windowStart = (windowStart & 0xff);
        int windowEnd = (windowStart + WINDOW_SIZE) % 256;
        seqNo = (seqNo & 0xff);
        return windowStart < windowEnd ? (windowStart <= seqNo && seqNo < windowEnd) : (windowStart <= seqNo || seqNo < windowEnd);
    }
	
	private void sendData(ConnectionState cs, byte[] payload, int length) {
		// assume that this will be a single hop, so don't adjust CTRL byte
        RetransmitBuffer rb = new RetransmitBuffer(payload, length, NUMBER_OF_RETRIES);
		transmitWithRetries(rb, cs);
	}

	/* package */ void setRadioPolicyManager(IRadioPolicyManager manager) {
		radioPolicyManager = manager;
	}

	void retransmit(RetransmitBuffer rb, ConnectionState cs, int conStat) {
		byte seqNum = rb.buffer[SEQ_OFFSET];
        if (rb.retransCounter == 0) {
            //we already tried to retransmit several times. We tell the
            //application that the stream is broken
            cs.status = conStat;
			cs.removeRetransBuffer(seqNum);
        } else {
            rb.retransCounter--;
            transmitWithRetries(rb, cs);
        }
	}

	private void transmitWithRetries(RetransmitBuffer rb, ConnectionState cs) {
		byte seqNum = rb.buffer[SEQ_OFFSET];
		try {
			boolean wasSent = lowpan.send(LowPanHeader.DISPATCH_SPOT, PROTOCOL_NUMBER, cs.id.getMacAddress(), rb.buffer, 0, rb.buffer.length, !isAckRequested(rb.buffer));
			if (!wasSent) {
				// ok, so it wasn't a single hop, now ask for an ack
				rb.buffer[CTRL_OFFSET] = (byte)(rb.buffer[CTRL_OFFSET] | CTRL_ACK_REQUIRED);
				cs.addRetransBuffer(seqNum, rb);
				//Debug.print("[Radiostream] transmit: no single hop route available. Trying again with ACK request");
				transmitWithRetries(rb, cs);
			} else if (isAckRequested(rb.buffer)) {
				if (!radioPolicyManager.isRadioReceiverOn()) {
					throw new RadioOffException("Attempt to perform multihop send with radio receiver off");
				}
				rb.retransmitTimer = new RetransmitTimer(seqNum, cs, this);
                int timeout = RETRANSMIT_BASE_TIMEOUT;
                RouteInfo info = routingManager.getRouteInfo(cs.id.getMacAddress());
                if (info.nextHop != com.sun.spot.peripheral.radio.mhrp.aodv.Constants.INVALID_NEXT_HOP) {
                    timeout += (info.hopCount - 1) * RETRANSMIT_PER_HOP_TIMEOUT;
                } else {
                    timeout += 4 * RETRANSMIT_PER_HOP_TIMEOUT;  // guess it might be 4 hops
                }
                // make sure to schedule retransmit after sending as send takes time
				retransScheduler.schedule(rb.retransmitTimer, timeout);
			} else {    // single hop
                cs.nextACKSeq = ((rb.buffer[SEQ_OFFSET] & 0xff) + 1) % 256;
            }

		// For each of the exceptions below, we could reset the CTRL_ACK_REQUIRED bit.
		// We choose not to as - most likely - the route hop count won't have changed.
		} catch (NoRouteException ex) {
			if (rb.retransmitTimer != null) rb.retransmitTimer.cancel();
			Debug.print("[Radiostream] transmit: seq=" + (seqNum & 0xff) + " NoRouteException caught. Trying to retransmit");
            Utils.sleep(500);
		    retransmit(rb, cs, ConnectionState.NO_ROUTE);
		} catch (ChannelBusyException ex) {
			if (rb.retransmitTimer != null) rb.retransmitTimer.cancel();
			Debug.print("[Radiostream] transmit: seq=" + (seqNum & 0xff) + " ChannelBusyException caught. Trying to retransmit");
		    retransmit(rb, cs, ConnectionState.CHANNEL_BUSY);
		}
	}

	private class InputHandler extends Thread {
		public InputHandler() {
			super("RadiostreamInputHandler");
		}
		public void run() {
           while (true) {
        	   IncomingData incoming = (IncomingData)inputQueue.get();
               try {
                   processIncomingData(incoming);
               } catch (Throwable e) {
                   e.printStackTrace();
               }
           }
       }
   }
}
