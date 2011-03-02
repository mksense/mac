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
 * Defines the portion of the I802.15.4 MAC layer specification that this library
 * currently supports.
 * 
 * @see <a href="http://standards.ieee.org/getieee802/index.html">http://standards.ieee.org/getieee802/index.html</a>
 */
public interface I802_15_4_MAC {
	/**
	 * Returned from mcpsDataRequest on successful transmission
	 */
	public static final int SUCCESS 			   = 0x0;
	/**
	 * Returned from mcpsDataRequest if transmission is not acknowledged by remote
	 */
	public static final int NO_ACK 				   = 0xE9;
	/**
	 * Returned from mcpsDataRequest if transmission fails because the channel is not clear
	 */
	public static final int CHANNEL_ACCESS_FAILURE = 0xE1;

	/**
	 * Key for {@link #mlmeSet} to control whether RX is active when not explicitly enabled through {@link #mlmeRxEnable}.
	 */
	public static final int MAC_RX_ON_WHEN_IDLE = 0x52;
	/**
	 * Key for {@link #mlmeSet} to control the 64-bit IEEE address for this MAC layer.
	 */
	public static final int A_EXTENDED_ADDRESS 	= 0x170;
	
	/**
	 * Some MAC PIB attributes are defined as boolean: to simplify the interface
	 * they're returned as ints. These constants allow for checking their values.
	 */ 
	public static final int FALSE = 0;
	/**
	 * Some MAC PIB attributes are defined as boolean: to simplify the interface
	 * they're returned as ints. These constants allow for checking their values.
	 */ 
	public static final int TRUE = 1;

	/**
	 * Send a packet: blocks until ACK received if ACK requested.<br><br>
	 * 
	 * Request constraints<br>
	 *	SrcAddr=[my addr]<br>
	 *	SrcAddrMode=0x03<br>
	 *	DstAddrMode=0x03<br>
	 *	SrcPANId=DstPANId<br>
	 *	TxOptions=binary0000000x<br>
	 * @param rp - packet containing data to send<br>
	 * 
	 * @return SUCCESS | CHANNEL_ACCESS_FAILURE | NO_ACK
	 */
	public int mcpsDataRequest (RadioPacket rp);
	
	/**
	 * Receive a packet: blocks until a packet is received.<br><br>
	 * 
	 * Constraints<br>
	 *	DstAddr=[my addr]<br>
	 *	SrcAddrMode=0x03<br>
	 *	DstAddrMode=0x03<br>
	 *	SrcPANId=DstPANId<br>
	 *	SecurityUse=FALSE<br>
	 *	ACLEntry=0x08<br>
	 * 
	 * @param rp - the packet to fill with data
	 */
	public void mcpsDataIndication (RadioPacket rp);

	/**
	 * Start the MAC layer on a specific channel
	 * 
	 * @param panId - panId to use
	 * @param channel - channel to use
	 * @throws MAC_InvalidParameterException
	 */
	public void mlmeStart (short panId, int channel) throws MAC_InvalidParameterException;
	
	/**
	 * Reset the MAC layer
	 * 
	 * @param resetAttribs - reset PIB
	 */
	public void mlmeReset (boolean resetAttribs);
	
	/**
	 * Answer the value of the specified attribute.
	 * 
	 * @param attribute -- see Field Summary
	 * @return - the value of the attribute
	 * @throws MAC_InvalidParameterException if attribute is unknown
	 */
	public long mlmeGet(int attribute) throws MAC_InvalidParameterException;

	/**
	 * Set the value of a MAC attribute
	 * 
	 * @param attribute -- attribute to set (see Field Summary)
	 * @param value -- value to apply
	 * @throws MAC_InvalidParameterException if attribute is unknown
	 */
	public void mlmeSet(int attribute, long value) throws MAC_InvalidParameterException;
	
	/**
	 * Enable the receiver for a fixed period
	 * 
	 * @param rxOnDuration - the number of symbol-times for which to enable the receiver (max=0xFFFFFF, 0 = disable)
	 */
	public void mlmeRxEnable(int rxOnDuration);
}
