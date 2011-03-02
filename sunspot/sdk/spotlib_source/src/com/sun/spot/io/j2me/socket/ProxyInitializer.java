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


/**
 * Handles the string used to initialize a connection between a SocketConnection and the socket proxy on the host. 
 * @author Martin Morissette <martin.morissette@sun.com>
 *
 */
public class ProxyInitializer {
    
    /*
     * Init command string to initialize a connection in the proxy
     */
    public static final String INITIALIZER = "connect";
    
    /*
     * Sperator used in the init string.
     */
    public static final String SEPERATOR = " ";
    
    private String host;
    private String port;
    
    /**
     * Create a ProxyInitializer object from a host and port string
     * @param host hostname to connect to (exmaple: www.sun.com)
     * @param port port number to use for connection (example 80)
     */
    public ProxyInitializer(String host, String port){
        this.host = host;
        this.port = port;
    }
    
    /**
     * Create a ProxyInitializer from a initializer string formatted using the ProxyInitializer format
     *  "connect <hostname> <port>"
     * @param initStr initializer string
     * @throws IllegalArgumentException thrown when the initStr is not properly formatted.
     */
    public ProxyInitializer(String initStr) throws IllegalArgumentException {
        if(!initStr.startsWith(INITIALIZER + SEPERATOR)){
            throw new IllegalArgumentException("Invalid initializer: should start with " + INITIALIZER);
        }
        
        int posHost = initStr.indexOf(SEPERATOR);
        if(posHost==-1){
            throw new IllegalArgumentException("Invalid initializer: \"" + SEPERATOR + "\" expected to reperate INITIALIZER and hostname");
        }
        posHost++;
        
        
        int posPort = initStr.indexOf(SEPERATOR, posHost);
        if(posPort==-1){
            throw new IllegalArgumentException("Invalid initializer: \"" + SEPERATOR + "\" expected to reperate hostname and port number");
        }
        posPort++;        
        
        host = initStr.substring(posHost,posPort-1);
        port = initStr.substring(posPort);

    }
    
    /**
     * Returns the hostname to connect to
     * @return hostname to connect to
     */
    public String getHost() {
        return host;
    }

    /**
     * Returns the port number to connect to
     * @return port number to connect to
     */
    public String getPort(){
        return port;
    }
    
    /**
     * Returns the port number to connect to
     * @return port number to connect to
     */
    public int getPortInt() {
        return Integer.parseInt(port);
    }
    
    /**
     * Returns a formatted string representing the initStr to be sent to the proxy
     */
    public String toString(){
        return INITIALIZER + SEPERATOR + host + SEPERATOR + port;
    }
    
}
