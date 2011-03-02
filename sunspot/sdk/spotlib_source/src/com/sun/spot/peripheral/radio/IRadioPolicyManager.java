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

public interface IRadioPolicyManager {

	/**
	 * Default PAN id to use.
	 */
	public static final short DEFAULT_PAN_ID = 3;

	/**
	 * Notify the policy manager that a policy has changed
	 * @param conn the connection owning the policy that has changed
	 * @param selection the required radio state
	 */
	void policyHasChanged(IConnectionID conn, RadioPolicy selection);
	
	/**
	 * Notify the policy manager of a new connection
	 * @param conn the connection being registered
	 */
	void registerConnection(IConnectionID conn);
	
	/**
	 * Notify the policy manager that a connection has closed
	 * @param conn the connection being deregistered
	 */
	void deregisterConnection(IConnectionID conn);

	
	/**
	 * @return true if the radio receiver is currently enabled
	 */
	boolean isRadioReceiverOn();
	
	/**
	 * Attempt to set the radio receiver to on or off
	 * @param rxState true for rx on, false for rx off
	 * 
	 * @return true if the radio receiver is in the requested state after this call
	 */
	boolean setRxOn(boolean rxState);

	/**
	 * Answer the current channel number (between 11 and 26). 
	 * @return current channel number
	 */
	public int getChannelNumber();

	/**
	 * @return the 64-bit IEEE address of this device
	 */
	public long getIEEEAddress();

	/**
	 * Answer the radio output power in decibels.
	 * 
	 * @return - radio output power
	 */
	public int getOutputPower();

	/**
	 * Answer the current pan ID. 
	 * @return current pan ID
	 */
	public short getPanId();

	/**
	 * Set the current channel number (between 11 and 26). 
	 * @param channel - the new channel number
	 */
	public void setChannelNumber(int channel);

	/**
	 * Set the radio output power in decibels (between -32 and +31).
	 * 
	 * NB. The range given is the I802.15.4 standard range. In practice,
	 * the CC2420 radio has a range of power settings that do not map precisely to
	 * particular decibel levels. In particular, for all channels other than 26,
	 * any value from 0 to 31 db will set to radio to maximum power which is 0 db. For
	 * channel 26, the maximum power is -3 db, which will be set for all input values 
	 * from -3 to 31 db. In both cases, setting a lower decibel level may result in 
	 * reading back a different value, because only 22 CC2420 power levels are available 
	 * and so some correspond to more than one decibel level.  
	 * 
	 * @param power - new power setting in decibels.
	 */
	public void setOutputPower(int power);

	/**
	 * Set the pan ID (should not be -1 or -2, which are reserved in the I802.15.4
	 * standard). 
	 * @param pid - the new pan ID
	 */
	public void setPanId(short pid);
	
	/**
	 * Used in the host to close down the base station - a no-op if called on a Spot
	 */
	public void closeBaseStation();

}
