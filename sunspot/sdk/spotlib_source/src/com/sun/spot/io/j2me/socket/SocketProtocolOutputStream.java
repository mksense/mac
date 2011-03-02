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
import java.io.OutputStream;

/**
 * <p>
 * Socket specific InputSteam. This class handles the passing of protocol information while remaining seamless to the user.
 * </p>
 * @author Martin Morissette
 */
public class SocketProtocolOutputStream extends OutputStream implements SpotSocketProtocol{
    
    private OutputStream out;
    
    /**
     * Create the output stream from another OutputStream.
     * @param out OutputStream to be used
     */
    public SocketProtocolOutputStream(OutputStream out){
        this.out = out;
    }
    
    /**
     * Write data in the stream.
     */
    public void write(int data) throws IOException {
        // If the user of this outputstream wishes to send a character representing a ESC character, 
        // send it with a preceding ESC character.	
        if((data&0xFF)==ESCAPE_CHAR){
            out.write(ESCAPE_CHAR);
        }
        out.write(data);
    }
    
    /**
     * Write an error to the stream.
     * 
     * @throws IOException
     */
    public void writeError(String message) throws IOException{
        out.write(ESCAPE_CHAR);
        out.write(IOEXCEPTION);
        writeProtocolString(message);
        out.flush();
    }

    /**
     * <p>
     * Writes a string within the socket protocol. Protocol strings are used to send extra information.
     * For example, remote exceptions
     * </p>
     * @return String transfered within the socket protocol (invisible to the user)
     * @throws IOException
     */

    private void writeProtocolString(String string) throws IOException{
        
        byte[] bytes = string.getBytes();
        
        int size = bytes.length;
        while (true) {
            int part = ((size > 0x7F)? 0x80:0x00) | (size & 0x7F);
            out.write(part);
            size >>>= 7;
            if (size == 0) {
                break;
            }
        }
        out.write(bytes);
    }
    
    /**
     * Flush the stream. All buffered data will be sent.
     */
    public void flush() throws IOException {
        out.flush();
    }
    
    /**
     * Close the OutputStream
     */
    public void close() throws IOException{
        out.write(ESCAPE_CHAR);
        out.write(CONNECTION_CLOSE);
        out.flush();
        out.close();
        super.close();
    }
    
}
