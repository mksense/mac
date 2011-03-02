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
 * This interface represents the parts of the CC2420's functionality that are not  
 * exposed by the I802.15.4 PHY layer but are still useful. Some of these are required
 * for the current I802.15.4 MAC layer implementation.
 */
public interface IProprietaryRadio {
	
	/**
	 * Default output power setting.
	 */
	public static final int DEFAULT_TRANSMIT_POWER = 0;

	/**
	 * Default radio channel to use.  
	 */
	public static final int DEFAULT_CHANNEL = 26;

	/**
	 * Parameter for setting RX off mode to disable the chip's voltage regulator
	 * (see {@link #setOffMode(int)})  
	 */
	public static int OFF_MODE_VREG_OFF = 0;
	/**
	 * Parameter for setting RX off mode to enable the chip's voltage regulator
	 * but disable the oscillator (see {@link #setOffMode(int)})  
	 */
	public static int OFF_MODE_POWER_DOWN = 1;
	/**
	 * Parameter for setting RX off mode to leave the chip's oscillator enabled
	 * (see {@link #setOffMode(int)})  
	 */
	public static int OFF_MODE_IDLE = 2;

	/**
	 * Reset the hardware device
	 */
	void reset();

	/**
	 * Reset the hardware device receive buffer
	 */
	void resetRX();

    /**
	 * Set the hardware device to ignore non-broadcast messages that don't
	 * match our panID and extendedAddress
	 * 
	 * @param panId the pan id to which this Spot should respond
	 * @param extendedAddress the 64 bit IEEE address to which this Spot should respond
	 */
	void setAddressRecognition(short panId, long extendedAddress);

	/**
	 * The driver can maintain a history of recent radio events which can be displayed by
	 * dumpHistory. This call enables or disables history recording.
	 * 
	 *  @param shouldRecordHistory - true to record history, false to not record
	 */
	public void setRecordHistory(boolean shouldRecordHistory);	

	/**
	 * Dump information about the last ten packets to log. <b>Intended to support testing</b>
	 */
	void dumpHistory();
	
	/**
	 * Answer the transceiver's state as defined by the I802.15.4. PHY layer spec. 
	 * 
	 * @return {@link I802_15_4_PHY#RX_ON} | {@link I802_15_4_PHY#TRX_OFF} | {@link I802_15_4_PHY#TX_ON}
	 */
	int getTransceiverState();

	/**
	 * Set the off mode for the radio to one of the modes defined above.
	 * The radio will enter this mode whenever the rx is turned off.<br><br>
	 * 
	 * NOTE - If you select OFF_MODE_VREG_OFF then the radio will forget
	 * all its configuration settings when it enters that mode. You will
	 * need to reset the channel, the power and the address recognition
	 * after turning the radio back on. In the other power off modes the
	 * radio always remembers its settings when powered-down.
	 * 
	 * @param offState -- {@link #OFF_MODE_VREG_OFF} | {@link #OFF_MODE_POWER_DOWN} | {@link #OFF_MODE_IDLE}
	 */
	void setOffMode(int offState);
	
	/**
	 * Set the AGC gain range to be auotmatic selected
	 * 
	 */
	void setAutoAGCGainMode();

    /**
	 * Set the AGC gain range to be the low range
     * 
     */
	void setAutoLowGainMode();

    /**
	 * Set the AGC gain range to be the medium range
     * 
     */
	void setAutoMediumGainMode();

    /**
	 * Set the AGC gain range to be the high range
     * 
     */
	void setAutoHighGainMode();
    
    /**
	 * Turn off AGC, set gain range to be the low range
     * 
     * @param gain - specific gain value to be used
     */
	void setManualLowGainMode(int gain);

    /**
 	 * Turn off AGC, set gain range to be the medium range
     * 
     * @param gain - specific gain value to be used
     */
	void setManualMediumGainMode(int gain);

    /**
	 * Turn off AGC, set gain range to be the medium range
     * 
     * @param gain - specific gain value to be used
     */
	void setManualHighGainMode(int gain);
	
	/**
	 * Answer the count of the number of overflows recorded while receiving packets.
	 * <b>To support testing</b>.
	 * 
	 * @return -- the count.
	 */
	public int getRxOverflow();

	/**
	 * Answer the count of the number of CRC errors recorded. <b>To support testing</b>.
	 * 
	 * @return -- the count.
	 */
	public int getCrcError();

	/**
	 * Answer the count of the number of times we started a transmit and apparently
	 * didn't manage to start waiting for it to complete until after the interrupt was
	 * already signalled.  <b>To support testing</b>.
	 * 
	 * @return -- the count
	 */
	public int getTxMissed();
	
	/**
	 * Answer the count of the number of short packets recorded.
	 * <b>To support testing</b>.
	 * 
     * @return -- the count.
	 */
	public int getShortPacket();

	/**
	 * Reset the CrcError, ShortPacket, TxMissed and RxOverflow counters to zero.
	 */
	public void resetErrorCounters();

	/**
	 * Attempt to send rp. Return either SUCCESS or BUSY if the channel is not clear.
	 * If retry is true, we assume the packet is already in the radio chip TXFIFO. If not,
	 * for example if sending code is multithreading, this will break.
	 * <br /><br />
	 * Note that this will turn the receiver on briefly, even
	 * if an ack is not requested, in order to sense whether the channel is clear.
	 * 
	 * @param rp radio packet to send
     * @param retry whether this radio packet is a resend of the previous call to this method.
	 * @return -- SUCCESS or BUSY.
	 */
	public int dataRequest(RadioPacket rp, boolean retry);

	/**
	 * @return Whether there is data waiting in the chip's receive buffer 
	 */
	public boolean isRxDataWaiting();

	/**
	 * @return Whether data is being received or transmitted
	 */
	public boolean isActive();

}
