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

package com.sun.spot.peripheral.ota;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;

import com.sun.spot.peripheral.IRadioControl;
import com.sun.spot.util.IEEEAddress;

interface IOTACommandProcessor {

	/**
	 * Start up a OTACommandProcessor. Sets up its radio connections and then
	 * spawns a thread to respond to remote requests.
	 * @param dataInputStream 
	 * @param dataOutputStream 
	 * @param conn an IRadioControl used to adjust policy
	 * @throws IOException if the radio connection cannot be created.
	 */
	void initialize(DataInputStream dataInputStream, DataOutputStream dataOutputStream, IRadioControl conn) throws IOException;

	/**
	 * Attach a listener to be notified of the start and stop of flash
	 * operations.
	 * 
	 * @param sml --
	 *            the listener
	 */
	void addListener(IOTACommandServerListener sml);

	/**
	 * @return Returns true if the server has been suspended by software.
	 */
	boolean isSuspended();

	/**
	 * @param suspended Suspends or resumes the server (it is initially running).
	 */
	void setSuspended(boolean suspended);

	/**
	 * @return The time when the server last received a message from the host
	 */
	Date timeOfLastMessageFromHost();

    /**
     * @return the time that this OTA session was started
     */
    public long getStartTime();

	IEEEAddress getBasestationAddress();

	boolean isAlive();

	void closedown();

}
