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

import com.sun.squawk.util.Arrays;

/**
 * A retransmit buffer is used by the SpotstreamProtocolManager to store
 * data that has to be retransmitte in case that no acknowledgment arrives.
 * Therfor it contains all data that is needed for calling the send method of
 * low pan again. Further it stores the number of times this piece of data has
 * been tried to send.
 */

public class RetransmitBuffer {
    
    byte [] buffer;
    int retransCounter;
	RetransmitTimer retransmitTimer;
    
    /** Creates a new instance of RetransmitBuffer */
    public RetransmitBuffer(byte [] buffer, int length, int retransCounter) {
        this.buffer = Arrays.copy(buffer, 0, length, 0, length);
        this.retransCounter = retransCounter;
    }
}
