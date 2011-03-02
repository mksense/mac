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

package com.sun.squawk.io.j2me.radiogram;


import java.io.IOException;

import javax.microedition.io.Connection;

import com.sun.spot.io.j2me.radiogram.RadiogramConnImpl;
import com.sun.squawk.io.ConnectionBase;


/**
 * This class provides the "radiogram" protocol for accessing the SPOT radio using datagrams.
 * It is an implementor of {@link com.sun.spot.io.j2me.radiogram.RadiogramConnection}
 * 
 * @see com.sun.spot.io.j2me.radiogram.RadiogramConnection
 */
public class Protocol extends ConnectionBase {

	private RadiogramConnImpl conn;

	/**
	 * Default constructor - normally not called by user code which should use the GCF
	 * framework instead. See class comment for examples.
	 * 
	 */
	public Protocol () {
		super ();
	}

	public Connection open(String protocolName, String name, int mode, boolean timeouts) {
		int portNoAsInt = 0; // Special value if no port requested
		
		//System.out.println("Connection.open called");
		name = name.substring(2); // strip the two /s
		int split = name.indexOf(":");
		
		if (split >= 0 && split != (name.length()-1)) {
			portNoAsInt = Integer.parseInt(name.substring(split+1));
			if (portNoAsInt <= 0 || portNoAsInt > 255) {
				throw new IllegalArgumentException("Cannot open " + name + ". Port number is invalid");
			}
		} else if (split < 0) {
			split = name.length();
		} else {
			// trailing colon special case
			split = name.length()-1;
		}
		byte portNo = (byte) portNoAsInt;
		boolean isServer = (split == 0);
		String addr = name.substring(0, split);

		conn = new RadiogramConnImpl(addr,portNo,isServer,timeouts);
		return conn;
	}

	public void close() throws IOException {
		conn.close();
	}

}
