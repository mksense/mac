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
 * Interface to the AT91 power manager for the Sun SPOT, allowing peripheral
 * power consumption to be controlled by switching the peripheral clocks
 * on or off.
 * 
 * @author Syntropy
 */

public interface IAT91_PowerManager extends IDriver {

	/**
	 * Mode to specify in {@link #setShallowSleepClockMode(int)} to select PCK/MCK of 180MHz/60MHz.
	 * This is the default setting.
	 */
	int SHALLOW_SLEEP_CLOCK_MODE_NORMAL = 0;
	/**
	 * Mode to specify in {@link #setShallowSleepClockMode(int)} to select PCK/MCK of 45MHz/45MHz.
	 * NOTE: in this mode timer-counters active during shallow sleep that are connected to MCK will count
	 * at the slower speed during shallow sleep
	 * NOTE: USARTs will not function at the correct baud rate during shallow sleep with this mode selected
	 */
	int SHALLOW_SLEEP_CLOCK_MODE_45_MHZ = 1;
	/**
	 * Mode to specify in {@link #setShallowSleepClockMode(int)} to select PCK/MCK of 18.432MHz/18.432MHz.
	 * NOTE: in this mode timer-counters active during shallow sleep that are connected to MCK will count
	 * at the slower speed during shallow sleep
	 * NOTE: USARTs will not function at the correct baud rate during shallow sleep with this mode selected
	 */
	int SHALLOW_SLEEP_CLOCK_MODE_18_MHZ = 2;
	/**
	 * Mode to specify in {@link #setShallowSleepClockMode(int)} to select PCK/MCK of 9.216MHz/9.216MHz.
	 * NOTE: in this mode timer-counters active during shallow sleep that are connected to MCK will count
	 * at the slower speed during shallow sleep
	 * NOTE: USARTs will not function at the correct baud rate during shallow sleep with this mode selected
	 */
	int SHALLOW_SLEEP_CLOCK_MODE_9_MHZ = 3;
	
	/**
	 * Speed of the peripheral bus in each of the shallow sleep modes.
	 * For example {@link #PERIPHERAL_BUS_SPEEDS}[{@link #SHALLOW_SLEEP_CLOCK_MODE_18_MHZ}]
	 */
	int[] PERIPHERAL_BUS_SPEEDS = new int[] {
			ISpot.MCLK_FREQUENCY,
			ISpot.MCLK_FREQUENCY*3/4,
			18432000,
			18432000/2
	};

	/**
	 * Enable the clocks for peripherals whose bits are set in the mask.
	 * 
	 * @param mask containing one bit that is the peripheral clock to enable
	 */
	void enablePeripheralClock(int mask);

	/**
	 * Disable the clocks for peripherals whose bits are set in the mask.
	 * 
	 * @param mask containing one bit that is the peripheral clock to disable
	 */
	void disablePeripheralClock(int mask);
	
	/**
	 * Return a mask containing a set bit for each peripheral whose clock is enabled.
	 * 
	 * @return mask containing one set bit for each enabled peripheral clock
	 */
	int getEnabledPeripheralClocks();

	/**
	 * Enable or disable USB support (initially it is enabled). The only reason to disable this
	 * is to save power. Do not disable the USB support if the SPOT is connected to a USB host.
	 * If disabled, USART support will not be reenabled when the VM exits; it will
	 * be reenabled next time the SPOT is turned on.
	 * @param enable true if USB support is to be enabled, false otherwise
	 */
	void setUsbEnable(boolean enable);

	/**
	 * Enable or disable UART0 and USART1 support (initially it is enabled). The only reason to
	 * disable this is to save power. Do not disable the USB support if the SPOT is using a USART
	 * connection. If disabled, USART support will not be reenabled when the VM exits; it will
	 * be reenabled next time the SPOT is turned on.
	 * @param enable true if USART support is to be enabled, false otherwise
	 */
	void setUsartEnable(boolean enable);

	/**
	 * Select the clock mode to use when shallow sleeping. The default mode is selected each time
	 * the VM starts.
	 * 
	 * @param mode a mode as specified in {@link IAT91_PowerManager}.
	 */
	void setShallowSleepClockMode(int mode);
}
