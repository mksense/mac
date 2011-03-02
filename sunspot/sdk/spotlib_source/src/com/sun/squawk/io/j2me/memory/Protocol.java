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

package com.sun.squawk.io.j2me.memory;

import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.Connection;
import javax.microedition.io.StreamConnection;

import com.sun.spot.io.j2me.memory.*;
import com.sun.squawk.io.ConnectionBase;

/**
 * This class allows access to the raw memory of the host device from the GCF
 * framework. The access provided is read-only, and so attempts to open output
 * streams will generate IllegalStateExceptions. If you want to
 * write to flash memory, see {@link com.sun.spot.peripheral.IFlashMemoryDevice}<br>
 * <br>
 * You should normally access this class indirectly via the GCF framework. For example:<br>
 * <br>
 * <code>
 * ...<br>
 * int memAddress= 0x10000;<br>
 * StreamConnection conn = (StreamConnection) Connector.open("memory://" + memAddress);<br>
 * MemoryInputStream mis = (MemoryInputStream)conn.openInputStream();<br>
 * ...<br>
 * </code>
 * @see MemoryInputStream
 */
public class Protocol extends ConnectionBase implements StreamConnection {

	private int startAddress;

	public Protocol () {
		super ();
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.io.ConnectionBase#open(java.lang.String, java.lang.String, int, boolean)
	 */
	public Connection open(String protocolName, String name, int mode, boolean timeouts) {
		name = name.substring(2);
		startAddress = Integer.parseInt(name);
		return this;
	}
 
    /**
     * Open and return an input stream for a connection.
     *
     * @return                 An input stream
     */
	public InputStream openInputStream() {
        return new MemoryInputStream(startAddress);
    }

    /**
     * Throws IllegalStateException (output not supported for memory streams). If you want to
     * write to flash memory, see {@link com.sun.spot.peripheral.IFlashMemoryDevice}
     *
     * @return                 An output stream
     */
    public OutputStream openOutputStream() {
		throw new IllegalStateException("Cannot open an output stream on a memory connection");
    }
}
