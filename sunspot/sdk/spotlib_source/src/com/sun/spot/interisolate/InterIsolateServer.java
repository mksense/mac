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
import com.sun.spot.util.Utils;
import com.sun.squawk.Isolate;
import com.sun.squawk.VM;
import com.sun.squawk.io.mailboxes.MailboxInUseException;
import com.sun.squawk.io.mailboxes.ServerChannel;

/**
 * The purpose of classes in this package is to provide a generic framework for building
 * remote-procedure-call (RPC) mechanisms between isolates.
 * <br /><br />
 * The "context" is the object that wishes to provide inter-isolate access to its services.
 * It doesn't need to know anything about the RPC mechanism.
 * The "proxy" is the object in the other isolate that wishes to use the context. Many
 * proxies can access the same context. The context is in a one-to-many relationship with
 * the proxies. The proxy normally has the same api as the context, or a subset of it (the
 * subset that needs to be available remotely).
 * <br /><br />
 * The mechanism uses a ServerChannel for each context. This ServerChannel is created at
 * startup time, and is given a name that reflects the context being served. E.g.:
 * <p><code>
 *   InterIsolateServer.run("CONTEXT_ID_STRING", myContext);
 * </code></p>
 * When a new proxy object is created it asks the RequestSender class to create a Channel
 * by which it can talk to a context, where the context is identified by the name given to
 * its server channel. E.g.:
 * <p><code>
 *   RequestSender myRequestSender = RequestSender.lookup("CONTEXT_ID_STRING");
 * </code></p>
 * That call returns a RequestSender that the proxy can use to talk to the context. To make
 * a call the proxy creates a RequestEnvelope and asks the RequestSender to send it to the
 * context. Different subclasses of RequestEnvelope are used for each different request
 * that can be sent.
 * <br /><br />
 * Imagine that the context provides a function:
 * <p><code>
 *   public int doSomething();
 * </code></p>
 * The proxy might invoke this using:
 * <p><code>
 *   public int doSomething() {
 * 	 	ReplyEnvelope reply = myRequestSender.send(new DoSomethingCommand());
 *   	resultEnvelope.checkForRuntimeException();
 *   	return ((Integer)resultEnvelope.getContents()).intValue();
 *   }
 * </code></p>
 * The class DoSomethingCommand would be subclass of RequestEnvelope, and it would have
 * an implementation of execute(Object context) that looks like this:
 * <p><code>
 * 	 public ReplyEnvelope execute(Object context) {
 *		 int result = ((MyContext)context).doSomething();
 *		 return new ObjectReplyEnvelope(getUid(), new Integer(result));
 *	 }
 * </code></p>
 * If the function is void use a VoidReplyEnvelope instead. You can create other
 * reply envelopes if you wish. We also provide BooleanReplyEnvelope, but you
 * could achieve the same effect by wrapping the boolean in a Boolean, as with
 * the int example above. Exceptions are caught and propagated back to the proxy.
 * <br /><br />
 * 
 * This class defines a thread that listens on the ServerChannel
 */
public class InterIsolateServer {
	
	private ServerThread masterThread;
	private String channelName;
	private Object context;
	private int threadPriority;

	public static void run(String channelName, Object context) {
		run(channelName, context, Thread.currentThread().getPriority());
	}

	public static void run(String channelName, Object context, int threadPriority) {
		new InterIsolateServer(channelName, context, threadPriority);
	}
	
	private InterIsolateServer(String channelName, Object context, int priority) {
		this.channelName = channelName;
		this.context = context;
		this.threadPriority = priority;
		try {
			restart();
		} catch (MailboxInUseException e) {
			throw new SpotFatalException(e.getMessage());
		}
		Isolate.currentIsolate().addLifecycleListener(new UnhibernateHook(), Isolate.UNHIBERNATE_EVENT_MASK);
	}

	private void restart() throws MailboxInUseException {
		ServerChannel serverChannel = ServerChannel.create(channelName);
		masterThread = new ServerThread(serverChannel, context, threadPriority);
		VM.setAsDaemonThread(masterThread);
		VM.setSystemThreadPriority(masterThread, threadPriority);
		masterThread.start();
	}

	private class UnhibernateHook implements Isolate.LifecycleListener {
		public void handleLifecycleListenerEvent(Isolate iso, int eventKind) {
			if (masterThread.isAlive()) {
				Utils.log("[InterIsolateServer] for " + channelName + " unhibernating with unexpected live server thread");
			} else {
				try {
					restart();
				} catch (MailboxInUseException e) {
					Utils.log("[InterIsolateServer] for " + channelName + " unhibernating but can't relaunch server as mailbox taken by someone else");
				}
			}
		}
	}
	
}
