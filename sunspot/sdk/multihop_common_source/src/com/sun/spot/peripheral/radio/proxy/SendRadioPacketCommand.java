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

import com.sun.spot.interisolate.NumberReplyEnvelope;
import com.sun.spot.interisolate.ReplyEnvelope;
import com.sun.spot.peripheral.radio.ConnectionID;
import com.sun.spot.peripheral.radio.IRadioProtocolManager;

class SendRadioPacketCommand extends RadioRequestEnvelope {
	private ConnectionID cid;
	private long toAddress;
	private byte[] payload;
	private int length;
	
	public SendRadioPacketCommand(ConnectionID cid, long toAddress, byte[] payload, int length) {
		this.cid = cid;
		this.toAddress = toAddress;
		this.payload = payload;
		this.length = length;
	}

	public ReplyEnvelope execute(IRadioProtocolManager radioProtocolManager) throws Exception {
		long result = radioProtocolManager.send(cid, toAddress, payload, length);
		return new NumberReplyEnvelope(getUid(), result);
	}
}
