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

import com.sun.spot.util.Queue;
import com.sun.spot.util.Utils;
import com.sun.squawk.VM;
import com.sun.squawk.io.mailboxes.AddressClosedException;
import com.sun.squawk.io.mailboxes.Channel;
import com.sun.squawk.io.mailboxes.MailboxClosedException;

class RequestReceiver implements Runnable {
	private Channel channel;
	private Object context;
	private String serverChannelName;
	private Queue availableThreads = new Queue();
	
	public RequestReceiver(Channel channel, Object context, String serverChannelName) {
		this.channel = channel;
		this.context = context;
		this.serverChannelName = serverChannelName;
	}

	public void run() {
		try {
			while (true) {
				final RequestEnvelope request = (RequestEnvelope) channel.receive();
//				Utils.log("[RequestReceiver] received " + request.getClass().getName() + " uid " + request.getUid());
				getNextWorkerThread().processRequest(request);				
			}
		} catch (AddressClosedException e) {
			Utils.log("AddressClosedException in worker thread for server channel "+serverChannelName);
            channel.close();
            terminateWorkerThreads();
		} catch (MailboxClosedException e) {
			Utils.log("MailboxClosedException in worker thread for server channel "+serverChannelName);
		}
	}

	/*
	 * Synchronise this methods with addAvailableWorkerThread, in case a worker thread is busily processing
	 * and attempts to rejoin the queue after we've emptied it and before we null the field.
	 */
	synchronized private void terminateWorkerThreads() {
		while (!availableThreads.isEmpty()) {
			WorkerThread thread = getNextWorkerThread();
			thread.processRequest(null);
		}
		availableThreads = null;
	}

	synchronized public void addAvailableWorkerThread(WorkerThread thread) {
		if (availableThreads != null) {
			availableThreads.put(thread);
		}
	}

	private WorkerThread getNextWorkerThread() {
		WorkerThread t = (WorkerThread)availableThreads.get(0);
		if (t == null) {
//			Utils.log("[RequestReceiver] making new worker thread");
			Thread thread = new Thread(new WorkerThread(this, channel, context), "worker thread for " + serverChannelName);
			VM.setAsDaemonThread(thread);
			int priority = Thread.currentThread().getPriority();
			VM.setSystemThreadPriority(thread, priority);
			thread.start();
			// the WorkerThread will put itself on the queue once it starts
			// so we can safely call get() with no timeout
			t = (WorkerThread)availableThreads.get();
		}
		return t;
	}
}
