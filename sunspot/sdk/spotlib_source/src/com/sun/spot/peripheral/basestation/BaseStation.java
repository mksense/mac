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

package com.sun.spot.peripheral.basestation;

import java.io.IOException;

import com.sun.spot.peripheral.Spot;
import com.sun.spot.peripheral.radio.SpotSerialPipe;

/**
 * This class implements a base station that allows a PC (the "host") to communicate
 * to a standalone Spot ("the target") through a second Spot (the "base station")
 * connected to the host via a test board and usb cable.<br><br>
 * 
 * To write applications for the host, use the hostagent jar supplied in the devkit. Target Spots should 
 * be addressed directly from the host using their IEEE addresses. The target spots
 * should address the base station's IEEE address - the host doesn't expose an address
 * to the wider network.<br><br>
 * 
 * See "Spot Base Station.doc" for further guidance.<br>
 */
public class BaseStation {

	/**
	 * A main method that allows this class to be used as a startup class
	 * 	 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// Disable deep sleep as base stations can't, and there's 2% performance to gain
		Spot.getInstance().getSleepManager().disableDeepSleep();
		new MACProxyServer(new SpotSerialPipe()).run();
	}
}
