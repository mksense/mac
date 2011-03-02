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

package com.sun.spot.peripheral.radio;

//import com.sun.spot.util.Debug;
import java.util.Hashtable;
import java.util.TimerTask;

public class ReassemblyExpiration extends TimerTask {
    
    private String key;
    private Hashtable reassemblyBuffers;
    private LowPanStats lpStats;
    
    /**
     * construct a new instance of ReassemblyExpiration
     */
    public ReassemblyExpiration(java.lang.String key,
            java.util.Hashtable reassemblyBuffers,
            LowPanStats lpStats){
        this.key = key;
        this.reassemblyBuffers = reassemblyBuffers;
        this.lpStats = lpStats;
    }
    
    /**
     * this method will be called after the 15 seconds have passed that the
     * standard defines for discarding reassembly buffers. It tries to delete
     * the according reassemblyBuffer (if still existing)
     */    
    public void run(){
        Object o = reassemblyBuffers.remove(key);
        if (o != null) {
            lpStats.reassemblyExpired++;
            //Debug.print("15 sec expired - dumping reassembly buffer", 1);
        }
    }
}
