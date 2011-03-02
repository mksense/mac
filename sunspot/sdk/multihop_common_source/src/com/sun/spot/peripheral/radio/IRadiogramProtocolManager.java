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


/**
 * A general purpose {@link IProtocolManager} designed to separate incoming radio packets into 
 * separate queues based on a unique identifying port number in the range 0-255, and manage
 * them as unreliable datagrams.<br><br>
 * 
 * Currently underpins {@link com.sun.spot.io.j2me.radiogram.RadiogramConnection com.sun.spot.io.j2me.radiogram.RadiogramConnection}
 */
public interface IRadiogramProtocolManager extends IRadioProtocolManager {
	/**
	 * The offset into data buffers at which data starts
	 */
	final int DATA_OFFSET = PORT_OFFSET + 1;
	final int SERVER = 2;
	final int BROADCAST = 3;
	
	/**
	 * Register a server connection
	 * 
	 * @param portNo - port number to communicate over
	 * @return resultant ConnectionID
	 */
	ConnectionID addServerConnection(byte portNo);

	/**
	 * Register a broadcast connection
	 * 
	 * @param portNo - port number to communicate over
	 * @return resultant ConnectionID
	 */
	ConnectionID addBroadcastConnection(byte portNo);
}
