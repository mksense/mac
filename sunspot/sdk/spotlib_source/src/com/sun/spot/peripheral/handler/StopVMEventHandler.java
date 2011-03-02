/*
 * Copyright 2006-2009 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.spot.peripheral.handler;

import com.sun.spot.peripheral.IEventHandler;
import com.sun.spot.util.Utils;
import com.sun.squawk.*;

public class StopVMEventHandler implements IEventHandler, Runnable {

    private static final int SHUTDOWN_MIN_TIME = 1500;
    private static final int RESET_OVERRIDE = 2;
    private static int stopCount = 0;
    private static long stopTime;

    private boolean halt = false;

	public void signalEvent() {
        if (stopCount++ == 0) {
            stopTime = System.currentTimeMillis();
            Thread t = new Thread(this);
            t.setPriority(Thread.MAX_PRIORITY);
            t.start();  // don't define an anonymous class or else make sure emulator loads it
            Thread.yield();
            StopVMEventHandler backup = new StopVMEventHandler();
            backup.halt = true;
            t = new Thread(backup);
            t.setPriority(Thread.MAX_PRIORITY);
            Thread.yield();
        } else if (stopCount > RESET_OVERRIDE || (System.currentTimeMillis() - stopTime) > SHUTDOWN_MIN_TIME) {
            VM.haltVM(0);
	    }
    }

    public void run() {
        if (halt) {
            Utils.sleep(SHUTDOWN_MIN_TIME);
            VM.haltVM(0);
        } else {
            VM.stopVM(0);
        }
    }

}
