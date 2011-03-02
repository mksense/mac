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

package com.sun.squawk.io.j2me.remoteprint;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;

import javax.microedition.io.*;

import com.sun.spot.io.j2me.remoteprinting.RemotePrintConnection;
import com.sun.spot.io.j2me.remoteprinting.RemotePrintManager;
import com.sun.spot.io.j2me.remoteprinting.RemotePrintOutputStream;
import com.sun.spot.util.IEEEAddress;
import com.sun.squawk.io.ConnectionBase;

/**
 * Implementor of {@link RemotePrintConnection} using the Generic Connection Framework (GCF)
 * used by J2ME.
 * <p>
 * We want each newline ('\n') to automatically flush the buffer and send the line over the 
 * radio connection to the remote print server to be displayed. We cannot just use a Radio 
 * stream directly since it does not autoflush. So we use a RemotePrintOutputStream and 
 * connect it to a RadioOutputStream.
 * 
 * @author Ron Goldman
 * @see RemotePrintConnection
 */
public class Protocol extends ConnectionBase implements RemotePrintConnection {

    private static Hashtable printstreams = new Hashtable();    

    private String macAddress;
    private byte portNo;
    private OutputStream out = null;
    private String key;

    /**
     * Open a new "remoteprint" connection to the specified SPOT.
     *<p>
     * Note: the actual "radio" stream connection is opened by RemotePrintOutputStream.
     *
     * @param protocolName = "remoteprint:"
     * @param name the IEEE radio address of the SPOT to connect to and the port to use
     *             for the connection, e.g. "123456:119"
     * @param mode the access mode (ignored)
     * @param timeouts a flag to indicate that the caller wants timeout exceptions (ignored)
     * @return the new remoteprint conection
     *
     * @see com.sun.squawk.io.ConnectionBase#open(java.lang.String, java.lang.String, int, boolean)
     */
    public Connection open(String protocolName, String name, int mode, boolean timeouts) {
        name = name.substring(2); // strip the two /s
        int split = name.indexOf(":");
        if (split < 0) {
                throw new IllegalArgumentException("Cannot open " + name + ". No port number specified");
        }
		macAddress = name.substring(0, split);
        portNo = Byte.parseByte(name.substring(split+1));
        if (portNo < 0) {
                throw new IllegalArgumentException("Cannot open " + name + ". Port number is invalid");
        }
        // see if connection is already open
        key = Long.toString(IEEEAddress.toLong(macAddress)) + ":" + Integer.toString(portNo);
        return this;
    }

    /**
     * Close this remoteprint connection.
     * <p>
     * Note: this does not close the radio stream used by the remote printing.
     *
     * @see javax.microedition.io.Connection#close()
     */
    public void close() throws IOException {
        super.close();
    }

    /**
     * Open an InputStream for this connection.
     * <p>
     * Note: remoteprint connections do not support an InputStream.
     *
     * @return null
     *
     * @see com.sun.squawk.io.ConnectionBase#openInputStream()
     */
    public InputStream openInputStream() {
        return null;
    }

    /**
     * Open an OutputStream for this connection.
     * <p>
     * Note: all "remoteprint" connections from this SPOT to the remote print server
     * will share the same OutputStream.
     *
     * @return an OutputStream that goes to the remote print server
     *
     * @see com.sun.squawk.io.ConnectionBase#openOutputStream()
     */
    public OutputStream openOutputStream() {
        if (out == null) {
            out = (OutputStream)printstreams.get(key);
            if (out == null) {
                out = new RemotePrintOutputStream(key);
                printstreams.put(key, out);
                RemotePrintManager.getInstance().noteRedirection(macAddress);
            }
        }
        return out;
    }

}
