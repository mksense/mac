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

import com.sun.spot.peripheral.ChannelBusyException;
import com.sun.spot.peripheral.NoAckException;
import com.sun.spot.peripheral.NoRouteException;
import com.sun.spot.peripheral.SpotFatalException;
import com.sun.spot.util.Queue;
import com.sun.squawk.util.IntHashtable;
import java.util.Enumeration;


/**
 * A helper class for {@link RadiogramProtocolManager}.
 */
class ConnectionState {
	
	static final int INTACT = 0;
	static final int NO_MESHLAYER_ACK = 1;
	static final int NO_ACK = 2;
	static final int NO_ROUTE = 3;
	static final int CHANNEL_BUSY = 4;
	static final int CLOSED = 5;
	
	/**
	 * The status of this connection
	 */
	int status = INTACT;
	
    /**
	 * The queue of incoming radio packets. The queue is null for server and broadcast
	 * connections.
	 */
	private Queue queue;

	/**
	 * The public ID of this connection state.  
	 */
	ConnectionID id;

	/**
	 * helper field for PortBasedProtocolManager - not for public use.
	 */
	int lastIncomingSeq = -1;

	/**
	 * helper field for PortBasedProtocolManager - not for public use.
	 */
	int lastOutgoingSeq = -1;

	/**
	 * helper field for PortBasedProtocolManager - not for public use.
	 */
	int nextACKSeq = -1;

    /**
     * Table of buffers that might need to be retransmitted if the connection is 
     * reliable
     */
    private IntHashtable retransBuffers = new IntHashtable();
    
    /**
     * Table of incoming data buffers that might need to be reorderd before 
     * putting them in the queue in case of a reliable connection
     */
    IntHashtable reorderTable = new IntHashtable();

	static ConnectionState newInstance(boolean canReceive, ConnectionID cid) {
		if (cid.isBroadcast()) {
			return new BroadcastConnectionState(cid);
		} else {
			return new ConnectionState(canReceive, cid);
		}
	}
	
	ConnectionState(boolean canReceive, ConnectionID cid) {
		setCanReceive(canReceive);
		this.id = cid;
	}
	
	public String toString() {
		return "Connection state for "+id.toString();
	}

	public int hashCode() {
		return id.hashCode();
	}

	public boolean equals(Object c) {
		if (c == null) return false;
		//TODO check class
		ConnectionState cs = (ConnectionState)c;
		return id.equals(cs.id);
	}

	IncomingData getQueuedPacket() {
		if (! canReceive())
			throw new IllegalArgumentException(id.toString()+" cannot be used for receiving");
		if (status == CLOSED) return null;
		return (IncomingData) queue.get();
	}

	IncomingData getQueuedPacket(long timeout) {
		if (! canReceive())
			throw new IllegalArgumentException(id.toString()+" cannot be used for receiving");
		if (status == CLOSED) return null;
		return (IncomingData) queue.get(timeout);
	}

	boolean packetsAvailable() {
		if (! canReceive())
			throw new IllegalArgumentException(id.toString()+" does not have a received packet queue");
		return ! queue.isEmpty();
	}

	private boolean canReceive() {
		return queue != null;
	}

	private void setCanReceive(boolean b) {
		if (b) 
			queue = new Queue();
		else 
			queue = null;
	}

	void waitUntilNoRetransBuffers() throws NoAckException, ChannelBusyException, NoMeshLayerAckException, NoRouteException {
		synchronized (retransBuffers) {
			while (!retransBuffers.isEmpty()) {
				try {
					retransBuffers.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			checkStatusAndReport();
		}
	}
	
    void checkStatusAndReport() throws NoAckException, ChannelBusyException, NoMeshLayerAckException, NoRouteException {
		if (status != INTACT) {
			int oldStatus = status;
			status = INTACT;
			lastOutgoingSeq = -1;
			switch (oldStatus) {
			case NO_ACK:
				throw new NoAckException("NoAckException on " + this);
			case CHANNEL_BUSY:
				throw new ChannelBusyException("ChannelBusyException on " + this);
			case NO_MESHLAYER_ACK:
				throw new NoMeshLayerAckException("NoMeshLayerAckException on " + this);
			case NO_ROUTE:
				throw new NoRouteException("NoRouteException on " + this);
			default:
				throw new SpotFatalException("Error - shouldn't throw exception for status " + oldStatus);
			}
		}
	}

	void addRetransBuffer(byte seqNum, RetransmitBuffer rb) {
        synchronized (retransBuffers) {
        	retransBuffers.put(seqNum, rb);
        }
	}

	void removeAllRetransBuffers() {
        synchronized (retransBuffers) {
            Enumeration keys = retransBuffers.keys();
            while (keys.hasMoreElements()) {
                RetransmitBuffer rb = (RetransmitBuffer) retransBuffers.remove(((Integer)(keys.nextElement())).intValue());
                if (rb != null && rb.retransmitTimer != null) {
                    rb.retransmitTimer.cancel();
                }
            }
        	retransBuffers.notifyAll();
        }
    }

	void removeRetransBuffer(byte seqNum) {
        synchronized (retransBuffers) {
        	RetransmitBuffer rb = (RetransmitBuffer)retransBuffers.remove(seqNum);
        	if (rb != null && rb.retransmitTimer != null) {
        		rb.retransmitTimer.cancel();
        	}
        	retransBuffers.notifyAll();
        }
	}

	RetransmitBuffer getRetransBuffer(byte seqNum) {
        synchronized (retransBuffers) {
        	return (RetransmitBuffer)retransBuffers.get(seqNum);
        }
	}

	public void emptyReorderTable() {
		reorderTable = new IntHashtable();
	}

	public boolean close() {
		status = ConnectionState.CLOSED;
		if (queue != null) {
			queue.stop();
		}
		return true;
	}

	void addToQueue(IncomingData data) {
		queue.put(data);
	}

	int queueSize() {
		return queue.size();
	}
}
