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

package com.sun.spot.peripheral.radio.proxy;

import com.sun.spot.interisolate.BooleanReplyEnvelope;
import com.sun.spot.interisolate.NumberReplyEnvelope;
import com.sun.spot.interisolate.ReplyEnvelope;
import com.sun.spot.interisolate.RequestSender;
import com.sun.spot.peripheral.ChannelBusyException;
import com.sun.spot.peripheral.NoAckException;
import com.sun.spot.peripheral.NoRouteException;
import com.sun.spot.peripheral.SpotFatalException;
import com.sun.spot.peripheral.radio.ConnectionID;
import com.sun.spot.peripheral.radio.IRadioProtocolManager;
import com.sun.spot.peripheral.radio.IncomingData;
import com.sun.squawk.io.mailboxes.NoSuchMailboxException;

public class ProxyRadioProtocolManager implements IRadioProtocolManager {
	protected RequestSender requestSender;

	protected ProxyRadioProtocolManager(byte protocolNum, String name, String channelIdentifier) {
		try{
			requestSender = RequestSender.lookup(channelIdentifier);
		} catch (NoSuchMailboxException ex) {
			throw new RuntimeException(ex.getMessage());
		}
	}

	public void closeConnection(ConnectionID cid) {
		ReplyEnvelope resultEnvelope = requestSender.send(new CloseConnectionCommand(cid));
		resultEnvelope.checkForRuntimeException();
	}

	public ConnectionID addOutputConnection(long macAddress, byte portNo) {
		ReplyEnvelope resultEnvelope = requestSender.send(new AddOutputConnectionCommand(macAddress, portNo));
		resultEnvelope.checkForRuntimeException();
		return (ConnectionID) resultEnvelope.getContents();
	}

	public ConnectionID addInputConnection(long macAddress, byte portNo) {
		ReplyEnvelope resultEnvelope = requestSender.send(new AddInputConnectionCommand(macAddress, portNo));
		resultEnvelope.checkForRuntimeException();
		return (ConnectionID) resultEnvelope.getContents();
	}

	public long send(ConnectionID cid, long toAddress, byte[] payload,
			int length) throws NoAckException, ChannelBusyException, NoRouteException {
		ReplyEnvelope resultEnvelope = requestSender.send(new SendRadioPacketCommand(cid, toAddress, payload, length));
		try {
			resultEnvelope.checkForThrowable();
		} catch (RuntimeException e) {
			throw e;
		} catch (NoAckException e) {
			throw e;
		} catch (ChannelBusyException e) {
			throw e;
		} catch (NoRouteException e) {
			throw e;
		} catch (Throwable e) {
			throw new SpotFatalException("Unexpected exception: " + e);
		}
		return ((NumberReplyEnvelope)resultEnvelope).getLongContents();
	}

	public IncomingData receivePacket(ConnectionID cid) {
		ReplyEnvelope resultEnvelope = requestSender.send(new ReceiveRadioPacketCommand(cid));
		resultEnvelope.checkForRuntimeException();
		return (IncomingData)resultEnvelope.getContents();
	}

	public IncomingData receivePacket(ConnectionID cid, long timeout) {
		ReplyEnvelope resultEnvelope = requestSender.send(new ReceiveRadioPacketWithTimeoutCommand(cid, timeout));
		resultEnvelope.checkForRuntimeException();
		return (IncomingData)resultEnvelope.getContents();
	}

	public boolean packetsAvailable(ConnectionID connectionID) {
		ReplyEnvelope resultEnvelope = requestSender.send(new PacketsAvailableCommand(connectionID));
		resultEnvelope.checkForRuntimeException();
		return ((BooleanReplyEnvelope)resultEnvelope).getBooleanContents();
	}
}
