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

/**
 * Defines the portion of the I802.15.4 PHY layer specification that this library
 * currently supports.
 * 
 * Note: even if you intend to use this class to implement a physical-level transmitter only, you should still
 * call pdDataIndication in a separate thread in order to prevent the CC2420's RX FIFO from filling with radio noise.
 *  
 * @see <a href="http://standards.ieee.org/getieee802/index.html">http://standards.ieee.org/getieee802/index.html</a>
 */
public interface I802_15_4_PHY {
	
	// status definitions
	
	/**
	 * Return code for CCA requests when channel is not clear
	 */
	public static final int BUSY		= 0x0;
	/**
	 * Return code for CCA requests when channel is clear
	 */
	public static final int IDLE		= 0x4;
	/**
	 * Value for TRX state (see {@link #plmeSetTrxState}) to enable RX
	 */
	public static final int RX_ON		= 0x6;
	/**
	 * Return code for success in {@link #pdDataRequest pdDataRequest(RadioPacket)} and {@link #plmeSetTrxState(int)}
	 */
	public static final int SUCCESS		= 0x7;
	/**
	 * Value for TRX state (see {@link #plmeSetTrxState}) to disable RX
	 */
	public static final int TRX_OFF 	= 0x8;
	/**
	 * Return code for {@link #plmeCCARequest} if transmission is active 
	 */
	public static final int TX_ON		= 0x9;

	/**
	 * Attribute to set the current channel (in the range 11..26) (see {@link #plmeSet})
	 */
	public static final int PHY_CURRENT_CHANNEL		= 0x0;
	/**
	 * Attribute to set the transmit power (the parameter must be a six bit
	 * 2s-complement number in the range -32..31 - the easiest was to get this
	 * is to pass in "myPower & 0x3F") (see {@link #plmeSet})
	 */
	public static final int PHY_TRANSMIT_POWER		= 0x2;
	
	/**
	 * Send a packet.
	 * <br /><br />
	 * Note that this will turn the receiver on briefly, even
	 * if an ack is not requested, in order to sense whether the channel is clear.
	 * 
	 * @param rp - Packet to send
	 * @return SUCCESS | RX_ON
	 */
	public int pdDataRequest (RadioPacket rp);
	
	/**
	 * Receive a packet.
	 * Note: this method is not synchronized because we assume only one thread will call this at a time.
	 * 
	 * @param rp - Packet to fill in with data
	 */
	public void pdDataIndication (RadioPacket rp);
	
	/**
	 * Check to see if channel is clear
	 * 
	 * @return {@link #TRX_OFF} | {@link #BUSY} | {@link #IDLE}
	 */
	public int plmeCCARequest ();

	/**
	 * Sets the value of a PHY attribute.
	 * 
	 * @see #PHY_CURRENT_CHANNEL
	 * @see #PHY_TRANSMIT_POWER
	 * 
	 * @param attribute - key of attribute to set
	 * @param value - value to set
	 * @throws PHY_UnsupportedAttributeException
	 * @throws PHY_InvalidParameterException
	 */
	public void plmeSet (int attribute, int value) throws PHY_UnsupportedAttributeException, PHY_InvalidParameterException;

	/**
	 * Gets the value of a PHY attribute
	 * 
	 * @see #PHY_CURRENT_CHANNEL
	 * @see #PHY_TRANSMIT_POWER
	 * 
	 * @param attribute - key of attribute to get
	 * @return - attribute value
	 * @throws PHY_UnsupportedAttributeException
	 */
	public int plmeGet (int attribute) throws PHY_UnsupportedAttributeException;

	/**
	 * Set the TRX state.<br><br>
	 * 
	 * Currently not implemented for TX_ON in the CC2420.
	 * 
	 * @param newState -- TRX_OFF | RX_ON | TX_ON
	 */
	public int plmeSetTrxState(int newState);
}
