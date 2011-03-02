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

package com.sun.spot.peripheral.radio.policy.proxy;

import com.sun.spot.interisolate.BooleanReplyEnvelope;
import com.sun.spot.interisolate.NumberReplyEnvelope;
import com.sun.spot.interisolate.ReplyEnvelope;
import com.sun.spot.interisolate.RequestSender;
import com.sun.spot.peripheral.radio.IConnectionID;
import com.sun.spot.peripheral.radio.IRadioPolicyManager;
import com.sun.spot.peripheral.radio.RadioPolicy;
import com.sun.squawk.io.mailboxes.NoSuchMailboxException;

public class ProxyRadioPolicyManager implements IRadioPolicyManager {

	public static final String RADIO_POLICY_SERVER = "RADIO_POLICY_SERVER";
	private RequestSender requestSender;

	public ProxyRadioPolicyManager() {
		try{
			requestSender = RequestSender.lookup(RADIO_POLICY_SERVER);						
		} catch (NoSuchMailboxException ex) {
			throw new RuntimeException(ex.getMessage());
		}
	}

	public void policyHasChanged(IConnectionID conn, RadioPolicy selection) {
		ReplyEnvelope resultEnvelope = requestSender.send(new PolicyHasChangedCommand(conn, selection));
		resultEnvelope.checkForRuntimeException();
	}

	public void registerConnection(IConnectionID conn) {
		ReplyEnvelope resultEnvelope = requestSender.send(new RegisterConnectionCommand(conn));
		resultEnvelope.checkForRuntimeException();
	}

	public void deregisterConnection(IConnectionID conn) {
		ReplyEnvelope resultEnvelope = requestSender.send(new DeregisterConnectionCommand(conn));
		resultEnvelope.checkForRuntimeException();
	}

	public boolean isRadioReceiverOn() {
		ReplyEnvelope resultEnvelope = requestSender.send(new IsRadioReceiverOnCommand());
		resultEnvelope.checkForRuntimeException();
		return ((BooleanReplyEnvelope)resultEnvelope).getBooleanContents();
	}

	public boolean setRxOn(boolean rxState) {
		ReplyEnvelope resultEnvelope = requestSender.send(new SetRxOnCommand(rxState));
		resultEnvelope.checkForRuntimeException();
		return ((BooleanReplyEnvelope)resultEnvelope).getBooleanContents();
	}

	public int getChannelNumber() {
		ReplyEnvelope resultEnvelope = requestSender.send(new GetChannelNumberCommand());
		resultEnvelope.checkForRuntimeException();
		return ((NumberReplyEnvelope)resultEnvelope).getIntContents();
	}

	public long getIEEEAddress() {
		ReplyEnvelope resultEnvelope = requestSender.send(new GetIEEEAddressCommand());
		resultEnvelope.checkForRuntimeException();
		return ((NumberReplyEnvelope)resultEnvelope).getLongContents();
	}

	public int getOutputPower() {
		ReplyEnvelope resultEnvelope = requestSender.send(new GetOutputPowerCommand());
		resultEnvelope.checkForRuntimeException();
		return ((NumberReplyEnvelope)resultEnvelope).getIntContents();
	}

	public short getPanId() {
		ReplyEnvelope resultEnvelope = requestSender.send(new GetPanIdCommand());
		resultEnvelope.checkForRuntimeException();
		return ((NumberReplyEnvelope)resultEnvelope).getShortContents();
	}

	public void setChannelNumber(int channel) {
		ReplyEnvelope resultEnvelope = requestSender.send(new SetChannelNumberCommand(channel));
		resultEnvelope.checkForRuntimeException();
	}

	public void setOutputPower(int power) {
		ReplyEnvelope resultEnvelope = requestSender.send(new SetOutputPowerCommand(power));
		resultEnvelope.checkForRuntimeException();
	}

	public void setPanId(short pid) {
		ReplyEnvelope resultEnvelope = requestSender.send(new SetPanIdCommand(pid));
		resultEnvelope.checkForRuntimeException();
	}

	public void closeBaseStation() {
		throw new IllegalStateException("Not implemented");
	}

}
