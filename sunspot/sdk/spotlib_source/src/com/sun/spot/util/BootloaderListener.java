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

package com.sun.spot.util;

import java.io.*;
import javax.microedition.io.*;

import com.sun.squawk.VM;

/**
 * Simple class to listen to the serial input over the USB connection and
 * pass control to the bootloader. Means you do not have to push the reset
 * button on the SPOT when downloading new code.
 * 
 * To use (simplest way):
 * 
 * new BootloaderListener().start();
 * 
 * the BootloaderListener will exit the VM when a bootloader command is detected.
 * 
 * If you want to take your own action:
 * 
 * new BootloaderListener(myCallbackObject).start();
 * 
 * where myCallbackObject implements {@link com.sun.spot.util.IBootloaderListenerCallback IBootloaderListenerCallback}. 
 *
 * You can cancel the bootloader listener by calling cancel()
 * 
 * @author Ron Goldman / Syntropy
 */
 public class BootloaderListener extends Thread {   // Used to monitor the USB serial line
        
    private InputStream in;
    private boolean runBootLoaderListener = true;
    private IBootloaderListenerCallback callback = null;

    public BootloaderListener() {
        super("BootloaderListener");
    }
    
    public BootloaderListener(IBootloaderListenerCallback callback) {
        super("BootloaderListener");
    	this.callback = callback;
    }

    /**
     * Cleanup after ourself and stop running.
     */
    public void cancel() {
    	runBootLoaderListener = false;
        try {
            in.close();
        } catch (IOException ex) {
            // ignore any exceptions
        }
    }

    /**
     * Loop reading characters sent over USB connection and dispatch to bootloader when requested.
     */
    public void run () {
        try {
            in = Connector.openInputStream("serial://");

            while (runBootLoaderListener) {
                char c = (char)in.read();
                if ('A' <= c && c <= 'P') {
                	if (callback != null) {
                		callback.prepareToExit();
                	}
                	System.out.println("Exiting - detected bootloader command");
                    VM.stopVM(0);         // return control to bootloader
                }
            }
        } catch (IOException ex) {
            System.err.println("Exception while listening to serial line: " + ex);
        }
    }
}        
