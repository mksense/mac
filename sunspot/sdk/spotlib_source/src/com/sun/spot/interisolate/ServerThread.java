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

import com.sun.spot.util.Utils;
import com.sun.squawk.VM;
import com.sun.squawk.io.mailboxes.Channel;
import com.sun.squawk.io.mailboxes.MailboxClosedException;
import com.sun.squawk.io.mailboxes.ServerChannel;

class ServerThread extends Thread {
	private ServerChannel serverChannel;
	private Object context;
	private int threadPriority;
	
	public ServerThread(ServerChannel channel, Object context, int threadPriority) {
		super("ServerThread for " + channel.getName());
		this.serverChannel = channel;
		this.context = context;
		this.threadPriority = threadPriority;
	}

	public void run() {
		while (true) {
			Channel channel;
			try {
				channel = serverChannel.accept();
			} catch (MailboxClosedException e) {
				// TODO this probably isn't the correct behaviour
				Utils.log("ServerThread for " + serverChannel.getName() + " has terminated");
				break;
			}
			runRequestReceiverThreadFor(channel);
			Thread.yield();
		}
	}
	
	private void runRequestReceiverThreadFor(Channel channel) {
		RequestReceiver requestReceiver = new RequestReceiver(channel, context, serverChannel.getName());
		Thread t = new Thread(requestReceiver, serverChannel.getName());
		VM.setAsDaemonThread(t);
		VM.setSystemThreadPriority(t, threadPriority);
		t.start();
	}
}
