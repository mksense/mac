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

package com.sun.spot.dmamemory.proxy;

import java.util.Hashtable;

import com.sun.spot.dmamemory.IDMAMemoryManager;
import com.sun.spot.dmamemory.NotEnoughDMAMemoryException;
import com.sun.spot.interisolate.NumberReplyEnvelope;
import com.sun.spot.interisolate.ReplyEnvelope;
import com.sun.spot.interisolate.RequestSender;
import com.sun.spot.peripheral.SpotFatalException;
import com.sun.squawk.io.mailboxes.NoSuchMailboxException;

/**
 * Acts as a proxy for DMAMemoryManager in child isolates
 */
public class ProxyDMAMemoryManager implements IDMAMemoryManager {

	public static final String DMA_MEMORY_SERVER = "DMA_MEMORY_SERVER";
	private RequestSender requestSender;

	/**
	 * For use by the SPOT library only - to get a handle to a DMA Memory Manager use
	 * Spot.getInstance().getDMAMemoryManager()
	 */
	public ProxyDMAMemoryManager() {
		try{
			requestSender = RequestSender.lookup(DMA_MEMORY_SERVER);						
		} catch (NoSuchMailboxException ex) {
			throw new RuntimeException(ex.getMessage());
		}
	}

	public Hashtable getAllocationDetails() {
		throw new SpotFatalException("getAllocationDetails is not implemented for child isolates");
	}

	public int getBuffer(int size, String comment) throws NotEnoughDMAMemoryException {
		ReplyEnvelope resultEnvelope = requestSender.send(new GetBufferCommand(size, comment));
		try {
			resultEnvelope.checkForThrowable();
		} catch (RuntimeException e) {
			throw e;
		} catch (NotEnoughDMAMemoryException e) {
			throw e;
		} catch (Throwable e) {
			throw new SpotFatalException("Unexpected exception: " + e);
		}
		return ((NumberReplyEnvelope) resultEnvelope).getIntContents();
	}

	public int getMaxAvailableBufferSize() {
		ReplyEnvelope resultEnvelope = requestSender.send(new GetMaxAvailableBufferSizeCommand());
		resultEnvelope.checkForRuntimeException();
		return ((NumberReplyEnvelope) resultEnvelope).getIntContents();
	}

	public void releaseBuffer(int memAddr) {
		ReplyEnvelope resultEnvelope = requestSender.send(new ReleaseBufferCommand(memAddr));
		resultEnvelope.checkForRuntimeException();
	}

}
