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

import com.sun.squawk.util.StringTokenizer;
import java.util.Vector;

/**
 * Privide a string of the form 
 *  "protocolName://address:port/rest1/rest2/.../restN" 
 * WARNING currnelty the above for is REQUIRED, i.e., there must be a port, for example.
 *  
 * @author randy
 */
public class URL {
    
    private String protocol  = "";
    private String address   = "";
    private String port      = "";
    private Vector restOfURL = new Vector();
    
    public static boolean debugging = false;
    
    public URL(String url) {
        init(url);
    }
    
    public static void test(){
        debugging = true;
        new URL("serial:");
        new URL("serial://");
        new URL("remoteprint://ABCD.ADCF.1234.2345:90");
        new URL("http://ABCD.ADCF.1234.2345:2/foo/bar");
        new URL("snort://localhost/foo");
        new URL("snort://localhost/foo/bar/baz");
        debugging = false;
    }
    
    public void debug(String line){
        if(debugging) System.out.println("[URL] ----------------: " + line);
    }
    private void init(String url){ 
        debug("_______________________________________________________________");
        debug("  init() " + url);
        StringTokenizer st = new StringTokenizer(url, ":");
        protocol = st.nextToken();
        debug("  init() protocol " + protocol);
        if(! st.hasMoreTokens()) return;
        if(st.countTokens() == 1) {
            //Case there is address but no port
            String s = st.nextToken();
            debug(" init() ...with remainder " + s);
            if(s.equals("//")) return; //empty remainder.
            s = s.substring(2); //to skip past the leading "//" part
            st = new StringTokenizer(s, "/");
            address = st.nextToken(); //to skip past the leading "//" part
            debug(" init() address " + address);
        } else {
            //Case there is address and port
            address = st.nextToken().substring(2); //to skip past the leading "//" part
            debug(" init() address " + address);
            port = st.nextToken();
            debug(" init() port " + port);
            if(! st.hasMoreTokens()) return; //no remainders
            String s = st.nextToken();
            debug(" init() ...with remainder " + s);
            st = new StringTokenizer(s, "/");
        } 
        while(st.hasMoreTokens()){
            String piece = st.nextToken();
            debug(" init() piece " + piece);
            restOfURL.addElement(piece);
        }
    }

    public String getProtocol() {
        return protocol;
    }

    public String getAddress() {
        return address;
    }

    public String getPort() {
        return port;
    }

    public Vector getRestOfURL() {
        return restOfURL;
    }

    public static void setDebugging(boolean b) {
        debugging = b;
    }
}
