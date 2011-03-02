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

import java.util.Hashtable;

import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.util.Utils;
import com.sun.squawk.Isolate;
import com.sun.squawk.VM;
import com.sun.squawk.io.mailboxes.AddressClosedException;
import com.sun.squawk.io.mailboxes.Channel;
import com.sun.squawk.io.mailboxes.MailboxClosedException;
import com.sun.squawk.io.mailboxes.NoSuchMailboxException;
import com.sun.squawk.util.IntHashtable;

/**
 * @see com.sun.spot.interisolate.InterIsolateServer for details
 * 
 * Sends RPC requests and handles replies.
 */
public class RequestSender {

	private static Hashtable senders = new Hashtable();
	private Channel channel;
	private IntHashtable outstandingRequests = new IntHashtable();
	private String channelIdentifier;
	private Thread receiveThread;

	private class UnhibernateHook implements Isolate.LifecycleListener {
		public void handleLifecycleListenerEvent(Isolate iso, int eventKind) {
			if (receiveThread != null) {
				senders.remove(channelIdentifier);
				Utils.log("[RequestSender] for " + channelIdentifier + " unhibernating with unexpected live receive thread");
			} else {
				try {
					restart();
				} catch (NoSuchMailboxException e) {
					System.out.println("d");
					senders.remove(channelIdentifier);
					Utils.log("[RequestSender] for " + channelIdentifier + " unhibernating and now cannot find mailbox");
				}
			}
		}
	}

	private RequestSender(String channelIdentifier) throws NoSuchMailboxException {
		this.channelIdentifier = channelIdentifier;
		restart();
		Isolate.currentIsolate().addLifecycleListener(new UnhibernateHook(), Isolate.UNHIBERNATE_EVENT_MASK);
	}

	private void restart() throws NoSuchMailboxException {
		channel = Channel.lookup(channelIdentifier);
		receiveThread = new Thread(new ProxyReceiveThread(), "ProxyReceiveThread for " + channel);
		RadioFactory.setAsDaemonThread(receiveThread);
		
		// We're using MAX_SYS_PRIORITY because one RequestSender at least needs it (for inter
		// isolate comms when setting up drivers) and because this thread has a very short 
		// execution path
		VM.setSystemThreadPriority(receiveThread, VM.MAX_SYS_PRIORITY);
		receiveThread.start();
	}

	/**
	 * Testing only
	 */
	protected RequestSender() {}
	
	public static RequestSender lookup(String channelIdentifier) throws NoSuchMailboxException {
		// create a new sender if there is one there but it's closed. This way we'll attempt to recreate senders
		// to talk to isolates that have gone away, but might have come back.
		if (!senders.containsKey(channelIdentifier) || 
			!((RequestSender) senders.get(channelIdentifier)).isOpen()) {
			senders.put(channelIdentifier, new RequestSender(channelIdentifier));
		}
		return (RequestSender) senders.get(channelIdentifier);
	}

	public ReplyEnvelope send(RequestEnvelope envelope) {
//		Utils.log("[RequestSender] sending "  + envelope.getClass().getName() + " uid "+ envelope.getUid());
		registerRequest(envelope);
			return envelope.sendOn(channel);
				}

	public boolean isOpen() {
		return channel.isOpen();
	}

	private synchronized void registerRequest(RequestEnvelope envelope) {
		outstandingRequests.put(envelope.getUid(), envelope);
	}
	
	private synchronized RequestEnvelope findRequest(ReplyEnvelope reply) {
		return (RequestEnvelope)outstandingRequests.remove(reply.getUid());
	}
	
	private class ProxyReceiveThread implements Runnable {
		public void run() {
			while (true) {
				try {
					ReplyEnvelope reply = (ReplyEnvelope)channel.receive();
//					Utils.log("[RequestSender] received "  + reply.getClass().getName() + " uid " + reply.getUid());
					RequestEnvelope request = findRequest(reply);
					request.setReply(reply);
				} catch (AddressClosedException e) {
//                  Utils.log("[RequestSender] got AddressClosedException . outstandingRequests: " + outstandingRequests.size());
                    channel.close();
					receiveThread = null;
					break;
				} catch (MailboxClosedException e) {
//                    Utils.log("[RequestSender] got MailboxClosedException ");
					receiveThread = null;
					break;
				}
			}
		}
	}
}
