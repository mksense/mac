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

import java.io.IOException;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

import com.sun.spot.peripheral.IRadioControl;
import com.sun.spot.peripheral.radio.RadioPolicy;

/**
 * Helper class for "remoteprint:" connections. This class provides an OutputStream
 * that will stream data to another Spot using a radio connection. This class is needed
 * to automatically flush the radio stream whenever a newline is printed. It also reports
 * back to the RemotePrinting client if the connection is broken.
 *
 * @author Ron Goldman
 */
public class RemotePrintOutputStream extends OutputStream {

    private OutputStream out;
    private boolean recursing = false;

    /**
     * Creates a new RemotePrintOutputStream piping its output to a new RadioOutputStream.
     *
     * @param address the IEEE radio address of the remote print server and the port to use
     */
    public RemotePrintOutputStream(String address) {
        try {
            StreamConnection conn = (StreamConnection) Connector.open("radiostream://" + address);
            ((IRadioControl)conn).setRadioPolicy(RadioPolicy.AUTOMATIC);
            out = conn.openOutputStream();
        } catch (IOException ex) {
            System.err.println("Error opening radio output stream for remote printing: " + address + " : " + ex.toString());
        }
    }

    /**
     * Writes out a character, flushing the buffer if a newline.
     *
     * @param arg0 the character to write to the remote print server
     *
     * @see java.io.OutputStream#write(int)
     */
    public synchronized void write(int arg0) throws IOException {
        try {
        	if (out != null) {  // out may be null if the stream has been closed. This will happen if, for example
        						// it is shared between System.out and System.err and one has already detected that
        						// the remote Host has gone away.
                if (recursing) {
                    return;     // ignore any messages printed because of prior write
                } else {
                    try {
                        recursing = true;
                        out.write(arg0);
                        if (arg0 == '\n') {
                            out.flush();
                        }
                    } finally {
                        recursing = false;
                    }
                }
        	}
		} catch (IOException e) {
			RemotePrintManager.getInstance().cancelRedirect();
		}
    }

    /**
     * Flush any characters in our buffer, sending them over the radio to the remote print server.
     *
     * @see java.io.OutputStream#write(int)
     */
    public synchronized void flush() throws IOException {
        try {
        	if (out != null) {
    	        out.flush();
        	}
		} catch (IOException e) {
			RemotePrintManager.getInstance().cancelRedirect();
		}
    }

    /**
     * Close this OutputStream along with the underlying RadioOutputStream.
     * <p>
     * Flushes any characters in the buffer before closing the OutputStream.
     *
     * @see java.io.OutputStream#close()
     */
    public synchronized void close() throws IOException {
    	if (out != null) {
	        out.flush();
	        out.close();
	        super.close();
	        out = null;
    	}
    }

}
