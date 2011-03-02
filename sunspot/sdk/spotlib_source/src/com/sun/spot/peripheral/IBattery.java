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

package com.sun.spot.peripheral;

/**
 * IBattery access to information about the SPOT's battery.
 * 
 * <p>This requires pctrl-1.99 or greater and rev 4.0 to rev 6.0 eSPOT main board.
 * <p>An instance of this object can be accessed through the IPowerController 
 * with <tt>Spot.getInstance().getPowerController().getBattery()</tt>
 * <p><b>Setting the Battery Information</b>
 * <p>The battery information is set through system properties. Supported batteries are 720mahr (LP523436B)
 * and 770mahr (LP523436D). The battery can be identified by opening the case and examining the battery label.
 * The capacity printed on the label will be either 720maH or 770maH. This capacity will be used to identify the battery and
 * entered in the system-property spot.battery.model<br>
 * Reassemble the unit, connect the SPOT and enter the system property  replacing <rated capacity> with 720maH or 770maH:
 * <br><tt>ant set-system-property -Dkey=spot.battery.model -Dvalue=<rated capacity></tt></br><br>
 * Note that spot.battery.model will change to the actual model number when the battery class has been instantiated.
 * 
 * @author Bob Alkire
 */

import java.io.IOException;

public interface IBattery {
	/* 
	 * enumeration for getState()
	 */
	public static final byte NO_BATTERY = 0;
	public static final byte DEAD_BATTERY = 1;
	public static final byte LOW_BATTERY = 2;
	public static final byte DISCHARGING = 3;
	public static final byte CHARGING = 4;
	public static final byte EXT_POWERED = 5;
	public static final byte OUT_OF_RANGE_TEMP = 6;
	/*
	 * Indices for long array returned by getTime()
	 */
	public static final int SLEEPTIME = 0;
	public static final int RUNTIME = 1;
	public static final int CHARGETIME = 2;

	/**
	 * Battery model must match battery used for accurate measurements.
	 * Use the BattSet.java program to set the battery information.
	 * @return the battery model number as a string
	 */
	String getModelNumber();
	
	/**
	 * Battery level returns an integer indicating remaining capacity in percentage (0 is empty, 100 is fully charged)
	 * @return percentage of battery level 
	 */
	int getBatteryLevel();
	
	/**
	 * Get the batteries remaining capacity in maHr.
	 * @return the capacity in milliamphours
	 */
	double getAvailableCapacity();
	
	/**
	 * Get the batteries measured maximum capacity in maHr.
	 * This value is set to the measured capacity when the battery is fully charged.
	 * @return the maximum capacity in milliamphours
	 */
	double getMaximumCapacity();
	
	/**
	 * Battery time measurements of sleeping since last charge, running since last charge and length of charge time.
	 * @return long array of three indexed by SLEEPTIME, RUNTIME or CHARGETIME, each value is time in seconds 
	 */
	long[] getTime();
	

	/**
	 * Return the number of times the SPOT has seen a full charge. This value is reset to zero when
	 * the battery is replaced or removed.
	 * @return count of charge cycles
	 */
	short getChargeCount();
	
	
	/**
	 * The state of battery is returned. States are static final ints:
	 * NO_BATTERY, DEAD_BATTERY, LOW_BATTERY, DISCHARGING, CHARGING, EXT_POWERED, TEMPERATURE_OUT_OF_RANGE
	 * @return state of battery 
	 */
	byte getState();

	/**
	 * The state of battery is returned as a string:
	 * no battery, dead battery, low battery, discharging, charging, fully charged
	 * @param state enumerated value returned by getState()
	 * @return state of battery as a string
	 */
	String getStateAsString(int state);
	
	/**
	 * SetSleepcurrent writes the sleep current in ADC units to estimate how much power is being consumed while the SPOT is in deep sleep.
	 * The method will convert to ADC units (uA * 65536)/25000 and send that to the SPOT to use until changed again
	 * Default value is 32uA as typical sleep current of an eSPOT. If eDemo board is jumpered to run on standby power, this value should be 
	 * increased to 102uA. Other add on boards will have to adjust this value accordingly. 
	 * @param microamps current consumed during deep sleep in microamps
	 */
	void setSleepCurrent(int microamps);

	/**
	 * Returns true if the SPOT has detected a temperature sensor.
	 * Rev 5 and before did not have built in temperature sensors. A sensor can be added to the rev 5 SPOT with a simple modification.
	 * The temperature sensor can change the state of battery to indicate out of range temperature
	 * @return true if eSPOT has a temperature sensor 
	 */
	boolean hasTemperatureSensor();
	
	/**
	 * Attempts to detect the presence of the battery. Either battery is charging while externally powered
	 * or no external power. 
	 * @return true if battery detected 
	 */
	boolean hasBattery();
	
	/**
	 * Returns true when the battery has been depleted to low battery and then 
	 * charged fully. This cycle will set the measuredCapacity. The measured capacity is originally set to the 
	 * rated capacity until it detects this cycle.
     * @return true if SPOT has detected a complete calibration cycle
	 */
	boolean calibrationCycleDetected();

	/**
	 * Sleepcurrent is microamps written to the SPOT for use in calculating sleep current. This is added to 
	 * temperature self discharge current during deep sleep. 
	 * @return the value written by setSleepCurrent or default value (32uA). Value is microamps and does not inlcude temperature self discharge
	 */
	int getSleepCurrent();

	/**
	 * Returns rated capacity of the battery (as written on the label) .
	 * @return the rated capacity in milliamphour (maHr)
	 */
	double getRatedCapacity();

	/**
	 * All raw battery data as colon separated one entry per line string.
	 * @return a string of all raw values from the SPOT. 
	 */
	String rawBatteryData();

	/**
	 * force write of battery data from SRAM into EEPROM. This should be done whenever the SPOT is reset
	 * and tries to reinitialize with the same battery. This is done when using the pctrl bootloader which 
	 * forces a reset. 
	 * @throws IOException if the power controller does not respond within 10 seconds
	 */
	void updatePersistantInfo() throws IOException;

	void setBatteryInfo(String modelNumber, int rated, int lowbattery);
	
}
