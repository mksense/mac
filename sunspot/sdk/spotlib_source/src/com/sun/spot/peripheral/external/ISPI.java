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

package com.sun.spot.peripheral.external;

/**
 * An interface defining the SPI operations used by external boards.
 * 09-Mar-2005
 */
public interface ISPI {

    /**
     * Send 16 bits of data.
     * 
     * @param i The data to send (least sig. 16 bits only), MSB first.
     */
    void send16bits(int i);
    
    /**
     * Send and receive using SPI. Data is sent and received at the same time.
     * All the data in the commandSequence is sent, and n bytes are simultaneously
     * received, where n is the length of the readByteSequence.
     * 
     * @param commandSequence The data to send
     * @param readByteSequence An array into which the received data is placed
     */
    void sendSPICommand(byte[] commandSequence, byte[] readByteSequence);

    /**
     * Send and receive using SPI. Data is sent and received at the same time.
     * 
     * @param commandSequence The data to send
     * @param commandLength The number of bytes to send
     * @param readByteSequence An array into which the received data is placed
     * @param readLength The number of bytes to receive
     */
    void sendSPICommand(byte[] commandSequence, int commandLength, byte[] readByteSequence, int readLength);

    /**
     * Send and receive using SPI. Receiving can be delayed relative to sending.
     * 
     * @param commandSequence The data to send
     * @param commandLength The number of bytes to send
     * @param readByteSequence An array into which the received data is placed
     * @param readLength The number of bytes to receive
     * @param readOffset The number of bytes to send before beginning reception
     */
    void sendSPICommand(byte[] commandSequence, int commandLength, byte[] readByteSequence, int readLength, int readOffset);
    
    /**
     * Set the configuration of the SPI communications for this device. The configuration
     * sets up speed etc as per SPI_CSRx. Constants for the configuration are defined in:
     * {@link com.sun.spot.peripheral.ISpiMaster}
     * 
     * @param config The configuration of the SPI communications to use for this device.
     */
    void setConfiguration(int config);
}
