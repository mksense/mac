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

package com.sun.spot.io.j2me.remoteprinting;

import com.sun.spot.peripheral.Spot;
import com.sun.spot.peripheral.ota.ISpotAdminConstants;
import com.sun.spot.util.Utils;
import com.sun.squawk.Isolate;

/**
 * Manages the creation and removal of remote print connections
 */
public class RemotePrintManager implements IRemotePrintManager {

	private static IRemotePrintManager remotePrintManager;

	public synchronized static IRemotePrintManager getInstance() {
		if (remotePrintManager == null) {
			remotePrintManager = new RemotePrintManager();
		}
		return remotePrintManager;
	}

	private String divertingTo;
	private boolean diverted;

	RemotePrintManager() {}
	
	public synchronized void redirectOutputStreams(String basestationAddr) {
		if (Utils.isOptionSelected("spot.remote.print.disabled", false)) {
			return;
		}
		Isolate isolate = Isolate.currentIsolate();
		if (diverted && !divertingTo.equals(basestationAddr)) {
			diverted = false;
			isolate.removeOut("remoteprint://"+divertingTo+":" + getEchoPort());
			isolate.removeErr("remoteprint://"+divertingTo+":" + getEchoPort());			
		}
		if (!diverted ) {
			divertingTo = basestationAddr;
//			isolate.removeOut("serial:");
//			isolate.removeErr("serial:");
			isolate.addOut("remoteprint://"+basestationAddr+":" + getEchoPort());
			isolate.addErr("remoteprint://"+basestationAddr+":" + getEchoPort());
			diverted = true;
		}
	}

	public synchronized void cancelRedirect() {
		Isolate isolate = Isolate.currentIsolate();
		if (diverted) {
			diverted = false;
			isolate.removeOut("remoteprint://"+divertingTo+":" + getEchoPort());
			isolate.removeErr("remoteprint://"+divertingTo+":" + getEchoPort());			
		}
	}

	private int getEchoPort() {
		return Spot.getInstance().isMasterIsolate() ?
				ISpotAdminConstants.MASTER_ISOLATE_ECHO_PORT :
				ISpotAdminConstants.CHILD_ISOLATE_ECHO_PORT;
	}

	public void noteRedirection(String remoteAddress) {
		if(!diverted) {
			diverted = true;
			divertingTo = remoteAddress;
		}
	}
}
