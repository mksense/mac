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

public interface IProprietaryMAC {

	/**
	 * @return the count of times we saw rx data waiting when polling for ack but didn't subsequently get an ACK packet
	 */
	int getNullPacketAfterAckWait();

	/**
	 * @return the count of times there was a channel access failure
	 */
	int getChannelAccessFailure();

	/**
	 * @return the count of times we timed out waiting for an ack
	 */
	int getNoAck();

	/**
	 * @return the count of times we got an ack that wasn't the one we expected
	 */
	int getWrongAck();
	
	/**
	 * @return the count of times we got a badly formed packet
	 */
	int getRxError();

	/**
	 * Reset the NullPacketAfterAckWait, ChannelAccessFailure, NoAck, WrongAck and RxError counters to zero.
	 */
	public void resetErrorCounters();

    /**
	 * Passthrough to allow direct access to the physical layer facility for setting the radio channel.
	 *
	 * @param channel number (between 11 and 26)
	 */
	void setPLMEChannel(int channel);

    /**
	 * Passthrough to allow direct access to the physical layer facility for setting the power.
	 * 
	 * @param power an integer in the range -32..+31
	 */
	void setPLMETransmitPower(int power);
	
	/**
	 * Passthrough to allow direct access to the physical layer facility for querying the power.
	 * @see I802_15_4_PHY#plmeGet(int)
	 * 
	 * @return current power setting
	 */
	int getPLMETransmitPower();

	/**
	 * Set the maximum number of packets that can be in the MAC layer's RX queue before the radio is turned off. The radio
	 * will remain off until the queue size drops below the value set in {@link #setReceiveQueueLengthToDropBroadcastPackets(int)}
	 *  
	 * @param maxPackets
	 */
	void setMaxReceiveQueueLength(int maxPackets);

	/**
	 * Set the maximum number of packets that that can be in the MAC layer's RX queue before it starts discarding
	 * broadcast packets. Note that until the limit in {@link #setMaxReceiveQueueLength(int)} is reached
	 * point-to-point packets will still be received.
	 * 
	 * @param maxPackets
	 */
	void setReceiveQueueLengthToDropBroadcastPackets(int maxPackets);
	
	/**
	 * Get the maximum number of packets that can be in the MAC layer's RX queue before the radio is turned off. The radio
	 * will remain off until the queue size drops below the value of {@link #getReceiveQueueLengthToDropBroadcastPackets()}.
	 * 
	 * @return the number of packets
	 */
	int getMaxReceiveQueueLength();
	
	/**
	 * Get the maximum number of packets that that can be in the MAC layer's RX queue before it starts discarding
	 * broadcast packets. Note that until the limit in {@link #getMaxReceiveQueueLength()} is reached
	 * point-to-point packets will still be received.
	 * 
	 * @return the number of packets
	 */
	int getReceiveQueueLengthToDropBroadcastPackets();
}
