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

import com.sun.spot.peripheral.ChannelBusyException;
import com.sun.spot.peripheral.NoAckException;

/**
 * Acts like a basic pass through layer that's responsible for sending/receiving
 * packets from the MAC layer IRadioPacketDispatcher also provides a set of radio
 * control facilities for application developers such as controlling power output,
 * channel and PAN ID.<br><br>
 *
 * @author Allen Ajit George, Jochen Furthmueller
 * @version 0.1
 */
public interface IRadioPacketDispatcher {

    /**
     * Called by LowPan to initialize the dispatcher.
     *
     * <strong>Note:</strong> This is only called after LowPan is completely 
     * started up.  This method is there to prevent cycles in the initialization 
     * process
     *
     * @param lowPanLayer reference to the fully started LowPan instance
     */
    public void initialize(ILowPan lowPanLayer);
    
    /**
     * Send a radio packet
     *
     * @param rp the radio packet to send
     * @throws com.sun.spot.peripheral.NoAckException
     * @throws com.sun.spot.peripheral.ChannelBusyException
     */
    public void sendPacket(RadioPacket rp)
    throws NoAckException, ChannelBusyException;
    
    /**
     * Register to be notified with Link Quality information.
     * @param packetListener the class that wants to be called back
     * @deprecated use addPacketQualityListener()
     */
    public void registerPacketQualityListener(IPacketQualityListener packetListener);
    
    /**
     * Undo a previous call of registerPacketListener()
     * @param listener the class that wants to be deregistered
     * @deprecated use removePacketQualityListener()
     */
    public void deregisterPacketQualityListener(IPacketQualityListener listener);

    /**
     * Register to be notified with Link Quality information.
     * @param packetListener the class that wants to be called back
     */
    public void addPacketQualityListener(IPacketQualityListener packetListener);

    /**
     * Undo a previous call of registerPacketListener()
     * @param listener the class that wants to be deregistered
     */
    public void removePacketQualityListener(IPacketQualityListener listener);

}
