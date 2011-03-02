/*
 * Copyright 2007-2008 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.spot.peripheral.proxy;

import com.sun.spot.interisolate.NumberReplyEnvelope;
import com.sun.spot.interisolate.ReplyEnvelope;
import com.sun.spot.interisolate.RequestSender;
import com.sun.spot.peripheral.AbstractAT91_PIO;
import com.sun.spot.peripheral.IAT91_PIO;
import com.sun.spot.peripheral.Spot;
import com.sun.squawk.io.mailboxes.NoSuchMailboxException;

/**
 * ProxyAT91_PIO: do not use directly.
 * 
 * @see IAT91_PIO
 *
 */
public class ProxyAT91_PIO extends AbstractAT91_PIO {

	public static final String AT91_PIO_SERVER = "AT91_PIO_SERVER";
	private RequestSender requestSender;
	private int localClaims;

	/**
	 * For use by the SPOT library only - to get a handle to an AT91_PIO use
	 * {@link Spot#getInstance()} and {@link Spot#getAT91_PIO(int)}.
	 */
	public ProxyAT91_PIO(int pioSelector) {
		super(pioSelector);
		try{
			requestSender = RequestSender.lookup(AT91_PIO_SERVER);						
		} catch (NoSuchMailboxException ex) {
			throw new RuntimeException(ex.getMessage());
		}
	}

	protected void checkOwned(int mask) {
		if ((localClaims & mask) != mask) {
    		throw new IllegalArgumentException ("Attempt to open pins that are not claimed");
    	}
	}

	public int available() {
		ReplyEnvelope resultEnvelope = requestSender.send(new GetAvailableCommand(pioSelector));
		resultEnvelope.checkForRuntimeException();
		return ((NumberReplyEnvelope) resultEnvelope).getIntContents();
	}

	public void claim(int mask, int drive, boolean claimForPeriphA) {
		ReplyEnvelope resultEnvelope = requestSender.send(new ClaimCommand(pioSelector, mask, drive, claimForPeriphA));
		resultEnvelope.checkForRuntimeException();
		localClaims |= mask;
	}
	
	public void release(int mask) {
		ReplyEnvelope resultEnvelope = requestSender.send(new ReleaseCommand(pioSelector, mask));
		resultEnvelope.checkForRuntimeException();
		localClaims &= ~mask;
	}

	public void disableIrq(int irq) {
		ReplyEnvelope resultEnvelope = requestSender.send(new DisableIrqCommand(pioSelector, irq));
		resultEnvelope.checkForRuntimeException();
	}
	
	public void enableIrq(int mask) {
		ReplyEnvelope resultEnvelope = requestSender.send(new EnableIrqCommand(pioSelector, mask));
		resultEnvelope.checkForRuntimeException();
	}

	public void waitForIrq(int irq) throws InterruptedException {
		ReplyEnvelope resultEnvelope = requestSender.send(new WaitForIrqCommand(pioSelector, irq));
		resultEnvelope.checkForRuntimeException();
	}
}

