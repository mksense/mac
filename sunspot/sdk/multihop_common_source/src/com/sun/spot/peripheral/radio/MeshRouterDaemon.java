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

/**
 * A daemon that keeps the radio on and ensures that this SPOT
 * acts as a node in the mesh network 
 */
public class MeshRouterDaemon {
	
	private static boolean meshOn = false;

	public static void main(String[] args) {
		new MeshRouterDaemon(args);
	}

	private MeshRouterDaemon(String[] args) {
		if (args.length == 0) {
			// invoked using ant script to set command line
			if (!meshOn) {
				forceMeshOn();
			}
			waitForEver();
		} else {
			// invoked as startup class
			if (isMeshingOptionSet()) {
				forceMeshOn();
				meshOn  = true;
			}
		}
	}

	private synchronized void waitForEver() {
		try {
			wait();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private boolean isMeshingOptionSet() {
		return Utils.isOptionSelected("spot.mesh.enable", false);
	}

	private void forceMeshOn() {
		System.out.println("[enabling mesh routing]");
		IRadioPolicyManager radioPolicyManager = RadioFactory.getRadioPolicyManager();
		ConnectionID connectionID = new ConnectionID();
		radioPolicyManager.registerConnection(connectionID);
		radioPolicyManager.policyHasChanged(connectionID, RadioPolicy.ON);
		LowPan.getInstance();
	}
}
