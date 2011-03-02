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

/**
 * Implements a data structure that is used by the LowPan class to reassemble
 * fragmented data entities.
 *
 * @author Jochen Furthmueller
 */
class ReassemblyBuffer {
    private byte[] controlBuffer;
    byte[] buffer;
    byte protocolFamily;
    byte protocolNumber;
    
    /** Creates a new instance of ReassemblyBuffer */
    public ReassemblyBuffer(int length) {
        // NOTE: this is wasting memory because we need 2*n bytes for 
        // reassembling n bytes it would be more space efficient to do it with 
        // BitSets but this would be slower
        this.buffer = new byte[length];
        this.controlBuffer = new byte[length];
    }
    /**
     * writes fragment into reassembly buffer. Checks if this fragment overlaps
     * with one that has been written before, or if this fragment is to written 
     * outside the buffer
     * 
     * @param offset indicates where this fragment belongs
     * @param packet the packet that carries the fragment
     * @param firstByte index of the first byte of the fragment in the packet
     * @param length length of the fragment
     * @return success is true if the fragment was written without overlapping
     * older fragments or hurting buffer borders 
     */
    public boolean write(int offset, RadioPacket packet, int firstByte, 
            int fragmentLength) {
        //Debug.print("write: offset = "+offset*8+" bytes, " 
                //+ "firstByte = "+firstByte+", fragLength = " +fragmentLength
                //+ " packet.buffer.length = "+packet.buffer.length, 3);
        // check if we try to write a fragment that already has been written
        if (buffer.length < offset*8+fragmentLength) {
            //Debug.print("write: trying to access bytes outside the array", 1);
            return false;
        }
        for (int i = offset*8; i < offset*8+fragmentLength; i++){
            if (controlBuffer[i] == 1) {
                //Debug.print("write: trying to write byte "+ i +", that has " +
                        //"already been written", 3);
                return false;
                
            } else {
                controlBuffer[i] = 1;
            }
        }
        
        // if we do not overwrite existing data then write it into the buffer
        System.arraycopy(packet.buffer,firstByte,buffer,offset*8,fragmentLength);
        return true;
    }
    
    /**
     * @return true if all bytes of the fragmented datagram have been received
     * and written into the buffer properly
     */
    public boolean isComplete() {
        for (int i = 0; i < controlBuffer.length; i++){
            if (controlBuffer[i]==0) {
                return false;
            }
        }
        return true;
    }
}
