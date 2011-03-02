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

public interface IPowerController {

	// PowerController command bytes:
	public static final byte GET_AND_CLEAR_STATUS_CMD = 0;
	public static final byte QUERY_V_CORE = 1;
	public static final byte QUERY_V_CC = 2;
	public static final byte QUERY_V_BATT = 3;      // possibly not in rev 7/pctrl2.00 ??
	public static final byte QUERY_TEMPERATURE = 4; // possibly not in rev 7/pctrl2.00 ??
	public static final byte QUERY_V_EXT = 5;
	public static final byte QUERY_V_USB = 6;
	public static final byte QUERY_I_CHARGE = 7;    // possibly not in rev 7/pctrl2.00 ??
	public static final byte QUERY_I_DISCHARGE = 8; // possibly not in rev 7/pctrl2.00 ??
	// GET_TIME_CMD = 9 not used
	public static final byte GET_ALARM_CMD = 10;
	public static final byte GET_STRING_LEN_CMD = 11;
	public static final byte GET_STRING_CMD = 12;
	public static final byte SET_TIME_CMD = 13;
	public static final byte SET_ALARM_CMD = 14;
	// SET_SLEEP_CMD = 15 not used
	public static final byte QUERY_I_MAX = 16;      // possibly not in rev 7/pctrl2.00 ??
	public static final byte SET_INDICATE_CMD = 17;
	public static final byte QUERY_STARTUP = 18;
	public static final byte GET_POWER_STATUS_CMD = 19;
	// RUN_BOOTLOADER = 20 not used
	// QUERY_PROGMEM = 21 not used
	// QUERY_NEXTPROGMEM = 22 not used
	// FORCE_ADC = 23 not used
	public static final byte SET_CONTROL_CMD = 24;
	// GET_STATUS_CMD = 25 not used
	public static final byte GET_USB_HP_CMD = 26;
	public static final byte SET_USB_HP_CMD = 27;

	// getStatus() flags:
	public static final byte COLD_BOOT_EVENT = 1<<0;
	public static final byte BUTTON_EVENT = 1<<1;
	public static final byte ALARM_EVENT = 1<<2;
	public static final byte SENSOR_EVENT = 1<<3;
	public static final byte BATTERY_EVENT = 1<<4; // new battery or discharged < 3.0V
	public static final byte SLEEP_EVENT = 1<<5;
	public static final byte LOW_BATTERY_EVENT = 1<<6; // battery at minimum voltage 3.2V
	public static final byte EXTERNAL_POWER_EVENT = (byte) (1<<7); // VUSB or VEXT applied during battery operation

	// getPowerStatus() flags:
	public static final byte VBATT_FAULT = 1<<0;
	public static final byte VUSB_FAULT = 1<<1;
	public static final byte VEXT_FAULT = 1<<2;
	public static final byte VCC_FAULT = 1<<3;
	public static final byte VCORE_FAULT = 1<<4;
	public static final byte POWERUP_FAULT = 1<<5;
	public static final byte OVERLOAD_FAULT = 1<<6;
	
	/**
	 * Bit mask value for the {@link #setIndicate(byte)} parameter. 1 indicates that the SPOT
	 * should display the power state using its LEDs.
	 */
	public static final byte SHOW_POWERSTATE = 1<<0;

	/**
	 * Bit mask value for the {@link #setIndicate(byte)} parameter. 1 indicates that the SPOT
	 * should display events using its LEDs.
	 */
	public static final byte SHOW_EVENTS = 1<<1;

	/**
	 * Bit mask value for the {@link #setControl(byte)} parameter. Setting the bit indicates
	 * that the power controller should NOT wake the SPOT main board when it detects external
	 * board interrupts. The default is that the bit is unset, i.e. wake on interrupt is enabled.
	 */
	public static final byte WAKE_ON_INTERRUPT = 1<<0;

	/**
	 * Bit mask value for the {@link #setControl(byte)} parameter. Setting the bit indicates that 
	 * the power controller SHOULD shut the SPOT down when it detects loss of external power.
	 * The default is unset, i.e. the SPOT does not shutdown on external power loss.
	 */
	public static final byte SHUTDOWN_EXTERNAL_POWERLOSS = 1<<1;

	/**
	 * Get the power control PowerController firmware revision string.
	 * @return the firmware revision string
	 */
	String getRevision();
	
	/**
	 * Get the power control PowerController's time.
	 * @return the time in milliseconds since midnight Jan 1, 1970
	 */
	long getTime();

	/**
	 * Set the power control PowerController's time.
	 * @param systemTimeMillis the time in milliseconds since midnight Jan 1, 1970
	 */
	void setTime(long systemTimeMillis);

	/**
	 * Get the reason for the last power control PowerController interrupt.
	 * This is a bitmask with the following bits ORed together:<ul>
	 * <li>{@link #COLD_BOOT_EVENT} occurs when the attention button is pushed while powered down</li>
	 * <li>{@link #BUTTON_EVENT} occurs when the attention button is pushed while not powered down</li>
	 * <li>{@link #ALARM_EVENT} occurs when a timer alarm has expired</li>
	 * <li>{@link #SENSOR_EVENT} occurs when the sensor board issues an interrupt</li>
	 * <li>{@link #BATTERY_EVENT} occurs when either a new battery is attached or the existing battery is discharged below 3.0V</li>
	 * <li>{@link #SLEEP_EVENT} occurs on wake up from deep sleep</li>
	 * <li>{@link #LOW_BATTERY_EVENT} occurs when the battery reaches the minimum voltage for safe operation (3.2V)</li>
	 * <li>{@link #EXTERNAL_POWER_EVENT} occurs when external power is applied to the USB interface (VUSB or VEXT)</li>
	 *</ul>
	 * @return the PowerController status bits
	 */
	int getStatus();

	/**
	 * Return the ARM CPU core voltage in millivolts (nominally 1800mv).
	 * @return the ARM CPU core voltage (mv)
	 */
	int getVcore();

	/**
	 * Return the main board IO voltage in millivolts (nominally 3000mv).
	 * @return the IO voltage (mv)
	 */
	int getVcc();

	/**
	 * Return the battery supply voltage in millivolts (nominally 2700mv - 4700mv).
	 * This is a rough indicator of remaining battery life. 
         * The battery is nominally 3700mv through most of its state of charge and drops off 
         * pretty quickly towards full discharge. 
         * 
         * <p>At 3500mv the SPOT will start to indicate low battery (power LED switches from green to red) 
         * <p>At 3300mv the SPOT will shutdown automatically into deep sleep 
         * 
	 * @return the battery voltage (mv)
	 */
	int getVbatt();

	/**
	 * Return the voltage supplied by an external power source (if any) in millivolts
	 * (nominally 0mv - 5500mv).
	 * @return the external voltage (mv)
	 */
	int getVext();

	/**
	 * Return the externally supplied USB voltage (if any) in millivolts
	 * (nominally 5000mv).
	 * @return the USB voltage (mv)
	 */
	int getVusb();

	/**
	 * Return the current charging the battery in milliamps.
	 * Only one of Icharge or Idischarge will be be positive at any time. The other will be zero. 
	 * @return the discharge current (mA)
	 */
	int getIcharge();

	/**
	 * Return the current being drawn from the battery in milliamps.
	 * Only one of Icharge or Idischarge will be be positive at any time. The other will be zero. 
	 * @return the discharge current (mA)
	 */
	int getIdischarge();

	/**
	 * Disable automatic synchronisation between PowerController time and System time. This will cause the two
	 * to gradually drift apart and is not recommended for general use. The main legitimate use of
	 * this function is to stop SPI activity when an app needs exclusive use of the SPI pins.
	 * 
	 */
	void disableSynchronisation();

	/**
	 * Re-enable automatic synchronisation after a previous call to disableSynchronisation.
	 * 
	 */
	void enableSynchronisation();

	/**
	 * Return the maximum current (in milliamps) that has been drawn from the battery since the last time this was called.
	 * @return the maximum discharge current (mA) since the last call
	 */
	int getIMax();

	/**
	 * Return the time it took (in microseconds) for the power to stabilize from startup.
	 * @return the time it took (in microseconds) for the power to stabilize from startup.
	 */
	int getStartupTime();

	/**
	 * Return a bit mask of possible power faults.
	 * @return a bit mask of possible power faults.
	 */
	int getPowerStatus();

	/**
	 * Set a bit mask to control the power controller LED.
	 * See {@link IPowerController#SHOW_EVENTS} and {@link IPowerController#SHOW_POWERSTATE}
	 */
	void setIndicate(byte mask);

	/**
	 * Set a bit mask to control whether the power controller accepts interrupts from the sensor board
	 * and uses them to wake the SPOT if it is sleeping, and how the power controller deals with loss of 
	 * external power. See {@link IPowerController#WAKE_ON_INTERRUPT} and {@link IPowerController#SHUTDOWN_EXTERNAL_POWERLOSS}
	 */
	void setControl(byte mask);

	/**
	 * Retrieve the temperature measured from the main board temperature sensor. returned in degrees centigrade.
	 */
	double getTemperature();
	
	/**
	 * @return Answer an {@link IBattery} for access to information about the battery if any.
	 */
	IBattery getBattery();
	
	/**
	 * Set the state of the USB high power pin
	 * @param high true to set the usb into high power mode, false in lower power mode.
	 */
	void setUSBHP(boolean high);
	
	/**
	 * @return Whether the USB is in high power (true) or low power (false) mode.
	 */
	boolean getUSBHP();

}
