/*
 * Copyright 2005-2008 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.spot.io.j2me.socket;

import java.io.IOException;
import java.io.InputStream;

/**
 * <p>
 * Socket specific InputSteam. This class handles the passing of protocol information while remaining seamless to the user.
 * </p>
 * @author Martin Morissette
 *
 */
public class SocketProtocolInputStream extends InputStream implements SpotSocketProtocol {

    private InputStream in;

    /**
     * Create the input stream from another InputStream.
     * @param in InputStream to be used
     */
    public SocketProtocolInputStream(InputStream in) {
        this.in = in;
    }

    /**
     * Read data from the stream. 
     */
    public int read() throws IOException {
        int data = in.read();

        if (data == ESCAPE_CHAR) {
            data = in.read();
            switch (data) {
                case CONNECTION_CLOSE:
                    data = -1;
                    break;
                case IOEXCEPTION:
                    throw new IOException(readProtocolString());
                case ESCAPE_CHAR:
                    break;
                default:
                    throw new IOException("Unexpected escape character in protocol: " + data);
            }
        }
        return data;
    }

    /**
     * <p>
     * Reads a string within the socket protocol. Protocol strings are used to send extra information.
     * For example, remote exceptions
     * </p>
     * @return String transfered within the socket protocol (invisible to the user)
     * @throws IOException
     */
    private String readProtocolString() throws IOException {
        int data;
        int size = 0;
        int shiftBy = 0;
        
        while (true) {
            data = in.read();
            size = ((data & 0x7F) << shiftBy) | size;
            shiftBy += 7;
            if ((data & 0x80) == 0) {
                break;
            }
        }
        
        byte[] bytes = new byte[size];
        int read = in.read(bytes, 0, size);
        return new String(bytes, 0, read);
    }
    
    public int available() throws IOException {
    	return in.available();
    }

    /**
     * Close the InputStream.
     */
    public void close() throws IOException{
        in.close();
        super.close();
    }

}
