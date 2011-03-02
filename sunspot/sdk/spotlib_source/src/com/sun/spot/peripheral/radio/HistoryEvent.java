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

package com.sun.spot.peripheral.radio;

import com.sun.spot.util.Utils;

class HistoryEvent {
	private int size;
	private boolean fcsOk;
	private boolean oflowOnEntry;
	private int fifoRemainingOnEntry;
	private boolean oflowDetected;
	private long time;
	private boolean flushed;
	private boolean discarded;
	private boolean rejected;
	private int fcfFromC;
	private int fcfFromRP;
	private int fsmState;
	private boolean fifopOnEntry;
	private boolean fifoOnEntry;
	private boolean fifopAfterSPI;
	private boolean fifoAfterSPI;
	private byte[] contents = new byte[128];
	
	void display() {
		Utils.log("Time=" + time + " Size=" + size + " FCSok=" + fcsOk + " Rejected=" + rejected);
		Utils.log("FIFORemainingOnEntry=" + fifoRemainingOnEntry + " Discarded=" + discarded + " Flushed=" + flushed);
		Utils.log("OflowDetected=" + oflowDetected + " OflowOnEntry=" + oflowOnEntry);
		Utils.log("FcfFromC=" + fcfFromC + " FcfFromRP=" + fcfFromRP + " fsmState=" + fsmState);
		Utils.log("fifoOnEntry=" + fifoOnEntry + " fifopOnEntry=" + fifopOnEntry);
		Utils.log("fifoAfterSPI=" + fifoAfterSPI + " fifopAfterSPI=" + fifopAfterSPI);
		Utils.log(Utils.stringify(contents));
	}

	void setRejected(boolean result) {
		rejected = !result;
	}

	void setDiscarded() {
		discarded = true;
	}

	void setFlushed() {
		flushed = true;
	}

	void setSPIData(RadioPacket rp, int spiResult, int size, boolean fifopPinHigh, boolean fifoPinHigh) {
		fcfFromRP = rp.getFrameControl();
		oflowDetected = (rp.getLength() > 127);
		this.size = size;
		fcsOk = (rp.buffer[size] & 0x80) != 0;
		fcfFromC = (spiResult & 0xFFFF00) >> 8;
		fifopAfterSPI = fifopPinHigh;
		fifoAfterSPI = fifoPinHigh;
		for (int i = 0; i < size + 1; i++) {
			contents[i] = rp.buffer[i];
		}
	}

	void setReceiveData(boolean overflowDetected, int fifoRemaining, int fsmState, boolean fifopPinHigh, boolean fifoPinHigh) {
		time = System.currentTimeMillis();
		oflowOnEntry = overflowDetected;
		fifoRemainingOnEntry = fifoRemaining;
		flushed = false;
		discarded = false;
		rejected = false;
		this.fsmState = fsmState;
		fifopOnEntry = fifopPinHigh;
		fifoOnEntry = fifoPinHigh;
	}
}
