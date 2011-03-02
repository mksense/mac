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

package com.sun.spot.interisolate;

import com.sun.spot.peripheral.SpotFatalException;
import com.sun.squawk.io.mailboxes.AddressClosedException;
import com.sun.squawk.io.mailboxes.Channel;
import com.sun.squawk.io.mailboxes.Envelope;

/**
 * @see com.sun.spot.interisolate.InterIsolateServer for details
 * 
 * The abstract superclass for all RPC request envelopes.
 */
public abstract class RequestEnvelope extends Envelope {

	private static int lastUid = 0;
	private ReplyEnvelope reply;
	private int uid = ++lastUid;

	public Object getContents() {
		throw new SpotFatalException("Can't get the contents of a RequestEnvelope");
	}

	/**
	 * @param context An arbitrary object that the request needs to execute itself 
	 * @return a subclass of ReplyEnvelope representing the return value of the execution
	 * @throws Exception
	 */
	public abstract ReplyEnvelope execute(Object context) throws Exception;

	/**
	 * Send the request off to the other isolate, where it will get executed.
	 * 
	 * @param channel
	 * @return the ReplyEnvelope representing the result of the remote execute.
	 */
	synchronized ReplyEnvelope sendOn(Channel channel) {
		try {
			channel.send(this);
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return reply;
		} catch (AddressClosedException e) {
			throw new RuntimeException("AddressClosedException: " + e.getMessage());
		}
	}

	/**
	 * Set the reply for this request and notify the waiter
	 * @param reply
	 */
	synchronized void setReply(ReplyEnvelope reply) {
		this.reply = reply;
		notify();
	}

	public int getUid() {
		return uid;
	}

}
