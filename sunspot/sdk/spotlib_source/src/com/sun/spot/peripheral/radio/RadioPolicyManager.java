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

import java.util.Vector;

import com.sun.spot.peripheral.IDriver;


public class RadioPolicyManager implements IRadioPolicyManager, IDriver {

	private short panId;
	private int channelNumber;
    private int outputPower;
	private I802_15_4_MAC macDevice;
	private boolean macStarted = false;

	private Vector allConnections = new Vector();
	private Vector connectionsKeepingRadioOn = new Vector();
	private Vector connectionsRequestingRadioOff = new Vector();
	private boolean radioIsOn = false;
	private boolean radioStateBeforeTearDown;
	
	public RadioPolicyManager(I802_15_4_MAC macDevice, int initialChannel, short initialPanId, int initialOutputPower) {
		this.macDevice = macDevice;
		this.panId = initialPanId;
        if (RadioFactory.isRunningOnHost()) {
            this.channelNumber = initialChannel;
            this.outputPower = initialOutputPower;
        } else {
            setChannelNumber(initialChannel);
            setOutputPower(initialOutputPower);
        }
	}

	public synchronized void policyHasChanged(IConnectionID conn, RadioPolicy selection) {
		if (!allConnections.contains(conn)) {
			throw new IllegalArgumentException("Attempt to change policy for unregistered connection: " + conn);
		}
		connectionsKeepingRadioOn.removeElement(conn);
		connectionsRequestingRadioOff.removeElement(conn);
		if (selection == RadioPolicy.ON) {
			connectionsKeepingRadioOn.addElement(conn);
		} else if (selection == RadioPolicy.OFF) {
			connectionsRequestingRadioOff.addElement(conn);
		}
		updateRadioState();
	}

	public synchronized void registerConnection(IConnectionID conn) {
		allConnections.addElement(conn);
	}
	
	public synchronized void deregisterConnection(IConnectionID conn) {
		allConnections.removeElement(conn);
		connectionsKeepingRadioOn.removeElement(conn);
		connectionsRequestingRadioOff.removeElement(conn);
		updateRadioState();
	}
	 
	public boolean isRadioReceiverOn() {
		 return radioIsOn;
	}

	public synchronized boolean setRxOn(boolean rxState) {
		if (rxState == radioIsOn) {
			return true;
		} else if (rxState) {
			primTurnRxOn();
			return true;
		} else if (connectionsKeepingRadioOn.isEmpty()) {
			primTurnRxOff();
			return true;
		} else {
			return false;
		}
	}

	public String getDriverName() {
		return "RadioPolicyManager";
	}

	public boolean tearDown() {
		radioStateBeforeTearDown = radioIsOn;
		return setRxOn(false);
	}

	public void shutDown() {
	}

	public void setUp() {
		setRxOn(radioStateBeforeTearDown);
	}

	public int getChannelNumber() {
		return channelNumber;
	}

	public long getIEEEAddress() {
		return macDevice.mlmeGet(I802_15_4_MAC.A_EXTENDED_ADDRESS);
	}

	public int getOutputPower() {
		return outputPower;
	}

	public short getPanId() {
		return panId;
	}

	public void setChannelNumber(int channel) {
		if (channel == channelNumber) return;
		if (macStarted) {
			macDevice.mlmeStart(panId, channel);
		} else {
            getProprietaryMacDevice().setPLMEChannel(channel);
        }
		channelNumber = channel;
	}

	public void setOutputPower(int power) {
		if (power < -32 || power > 31) {
			throw new IllegalArgumentException("output power should be between -32dB and +31dB");
		}
		outputPower = setPLMETransmitPower(power & 0x3F);
	}

	public void setPanId(short pid) {
		if (pid == panId) return;
		if (macStarted) {
			// restart it with the new pan id
			macDevice.mlmeStart(pid, channelNumber);
		}
		panId = pid;
	}

	public void closeBaseStation() {
		RadioFactory.closeBaseStation();
	}
	
	Vector getAllConnections() {
		return allConnections;
	}

	Vector getConnectionsKeepingRadioOn() {
		return connectionsKeepingRadioOn;
	}

	/**
	 * Testing support only.
	 */
	void unsetMacStarted() {
		macStarted = false;
	}

	private synchronized void startMAC() {
		if (!macStarted) {
			macDevice.mlmeStart(panId, channelNumber);
            outputPower = setPLMETransmitPower(outputPower);          // must set power after channel
			macStarted = true;
		}
	}

	private int getPLMETransmitPower() {
		return getProprietaryMacDevice().getPLMETransmitPower();
	}

	private IProprietaryMAC getProprietaryMacDevice() {
		return (IProprietaryMAC) macDevice;
	}

	private int setPLMETransmitPower(int power) {
		getProprietaryMacDevice().setPLMETransmitPower(power);
		return getPLMETransmitPower() << 26 >> 26;
	}
	
	private void updateRadioState() {
		if (!connectionsKeepingRadioOn.isEmpty() || (connectionsRequestingRadioOff.isEmpty() && !allConnections.isEmpty())) {
			primTurnRxOn();
		} else {
			primTurnRxOff();
		}
	}

	private void primTurnRxOn() {
		if (!radioIsOn) {
			startMAC();
			macDevice.mlmeSet(I802_15_4_MAC.MAC_RX_ON_WHEN_IDLE, I802_15_4_MAC.TRUE);
			radioIsOn = true;
		}
	}

	private void primTurnRxOff() {
		if (radioIsOn ) {
			startMAC(); // need to startMAC so the turnOff sticks even if no protocols have been added
			macDevice.mlmeSet(I802_15_4_MAC.MAC_RX_ON_WHEN_IDLE, I802_15_4_MAC.FALSE);
			macDevice.mlmeRxEnable(0);
			radioIsOn = false;
		}
	}

}
