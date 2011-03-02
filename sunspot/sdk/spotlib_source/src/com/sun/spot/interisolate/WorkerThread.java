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

import com.sun.squawk.io.mailboxes.AddressClosedException;
import com.sun.squawk.io.mailboxes.Channel;
import com.sun.spot.util.Utils;

class WorkerThread implements Runnable {

	private final Channel channel;
	private RequestEnvelope request;
	private final Object context;
	private final RequestReceiver owner;

	public WorkerThread(RequestReceiver owner, Channel channel, Object context) {
		this.channel = channel;
		this.context = context;
		this.owner = owner;
	}

	public void run() {
		while (true) {
			waitForRequest();
            if (!channel.isOpen()) {
                break;
            }
            if (request == null) {
                Utils.log("WorkerThread.run got null request, but channel is still open.");
                break;
            }
			try {
				ReplyEnvelope response = executeRequest(request);
//				Utils.log("[RequestReceiver] sending "  + response.getClass().getName() + " uid " + response.getUid());
				channel.send(response);
			} catch (AddressClosedException e) {
//				Utils.log("AddressClosedException in worker thread "+Thread.currentThread().getName());
			}
		}
	}

	public synchronized void processRequest(RequestEnvelope request) {
		this.request = request;
		notify();
	}
	
	private synchronized void waitForRequest() {
		owner.addAvailableWorkerThread(this);
		if (channel.isOpen()) {
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private ReplyEnvelope executeRequest(RequestEnvelope request) {
		try {
			return request.execute(context);
		} catch (Throwable e) {
//			Utils.log("++++++++++++++ caught exception in worker thread ++++++++++++++++");
//			e.printStackTrace();
//			Utils.log("++++++++++++++ end of caught exception in worker thread ++++++++++++++++");
			return new ExceptionReplyEnvelope(request.getUid(), e);
		}
	}

}
