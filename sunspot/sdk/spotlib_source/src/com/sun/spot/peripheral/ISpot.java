/*
 * Copyright 2006-2009 Sun Microsystems, Inc. All Rights Reserved.
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

//import java.io.IOException;
//import java.util.Properties;

import java.io.IOException;
import java.util.Hashtable;

import com.sun.spot.dmamemory.IDMAMemoryManager;
import com.sun.spot.io.j2me.remoteprinting.IRemotePrintManager;
import com.sun.spot.peripheral.ota.IOTACommandServer;
import com.sun.spot.peripheral.radio.I802_15_4_MAC;
import com.sun.spot.peripheral.radio.I802_15_4_PHY;
import com.sun.spot.peripheral.radio.IProprietaryRadio;
import com.sun.spot.peripheral.radio.IRadioPolicyManager;
import com.sun.spot.resourcesharing.IResourceRegistry;
import com.sun.spot.service.IService;
import com.sun.spot.util.Properties;


/**
 * The interface to the root object of the Spot base library.<br><br>
 * 
 * This interface provides access to the LED on the Spot board, to the AT91 peripherals,
 * and to other resources such as the software SPI implementation.
 */
public interface ISpot {
	
	/**
	 * Frequency of ARM master clock
	 */
	public static final int MCLK_FREQUENCY = 59904000;

	public static final int SYSTEM_TICKER_TICKS_PER_MILLISECOND = ((MCLK_FREQUENCY  / 128) / 1000);

	/**
	 * System property that if true enables output of log messages on System.err
	 */
	public static final String PROPERTY_SPOT_DIAGNOSTICS = "spot.diagnostics";

	/**
	 * @return true if running on the host, false if on the SPOT
	 */
	boolean isRunningOnHost();
	
	/**
	 * @return true if this method has been called in the context of the master isolate
	 */
	boolean isMasterIsolate();

	/**
	 * Get the hardware type code for this device.
	 * @return the hardware type code
	 */
	int getHardwareType();
	
	/**
	 * Get the singleton SpotPins instance.
	 * @return ISpotPins the SpotPins
	 */
	ISpotPins getSpotPins();
	
	/**
	 * Get access to the green LED on the Spot processor board.
	 * Use with caution as this LED is intended for system use.
	 * @return ILed the green LED on the Spot processor board
	 */
	ILed getGreenLed();

	/**
	 * Get access to the red LED on the Spot processor board.
	 * Use with caution as this LED is intended for system use.
	 * @return The red LED on the Spot processor board
	 */
	ILed getRedLed();

	/**
	 * Get the FiqInterruptDaemon. This handles interrupts from the power controller.
	 * @return the FiqInterruptDaemon
	 */
	IFiqInterruptDaemon getFiqInterruptDaemon();

	/**
	 * Get access to the AT91 Parallel I/O Controller.
	 * @param pioSelector selects the PIO required (use one of the selector symbols found in IAT91_PIO)
	 * @return the AT91 Parallel Input-Output Controller.
	 */
	IAT91_PIO getAT91_PIO(int pioSelector);

	/**
	 * Get access to the AT91 Interrupt Controller
	 * @return the AT91 Interrupt Controller
	 */
	IAT91_AIC getAT91_AIC();

	/**
	 * Get access to the physical I802.15.4 radio device
	 * @return the I802.15.4 physical radio device
	 */
	I802_15_4_PHY getI802_15_4_PHY();

	/**
	 * Get access to the I802.15.4 MAC layers
	 * @return the I802.15.4 MAC layers
	 */
	I802_15_4_MAC[] getI802_15_4_MACs();

	/**
	 * Get access to the radio via its proprietary (non-I802.15.4) interface.
	 * @return the proprietary interface to the radio device
	 */
	IProprietaryRadio getIProprietaryRadio();

	/**
	 * Get access to the SPI interface.
	 * This interface is used to communicate with the radio and the sensor boards
	 * @return the SPI interface.
	 */
	ISpiMaster getSPI();
	
	/**
	 * Get access to the I2C interface.
	 * This interface is used to communicate with external devices using TWI
	 * @return the I2C interface.
	 */
	II2C getI2C();

	/**
	 * Get the Driver Registry.
	 * @return the Driver Registry
	 */
	IDriverRegistry getDriverRegistry();	

	/**
	 * Get the SecuredSiliconArea
	 * @return the SecuredSiliconArea
	 */
	ISecuredSiliconArea getSecuredSiliconArea();
	
	/**
	 * Get access to an AT91 Timer-Counter.
	 * @param index The index of the required TC in the range 0-5
	 * @return The AT91 TC
	 */
	IAT91_TC getAT91_TC(int index);

	/**
	 * Get the configuration page held in flash
	 * @return the configuration page
	 */
	ConfigPage getConfigPage();
	
	/**
	 * Get the Public Key used to sign and verify application and library suites, and the config page. 
	 * @return byte[] The Public Key
	 */
	byte[] getPublicKey();
	
	/**
	 * Write a ConfigPage into the flash.
	 */
	void flashConfigPage(ConfigPage configPage);

	/**
	 * Get the map of external boards. This returns a Hashtable containing an entry for each detected board.
	 * The key of the entry is the PeripheralChipSelect used as the chip select for the board, and the value
	 * is a Properties object containing the properties read from its serial flash memory.
	 * 
	 * @return a Hashtable as described above
	 */
	Hashtable getExternalBoardMap();

	/**
	 * Clear the cache of external board properties. The cache will be re-read from the boards
	 * next time it is used.
	 */
	void resetExternalBoardMap();

	/**
	 * Get access to the flash memory on the Spot
	 * @return the flash memory controller as an IFlashMemoryDevice
	 */
	IFlashMemoryDevice getFlashMemoryDevice();

	/**
	 * Get access to the sleep manager for the Spot
	 * @return The sleep manager
	 */
	ISleepManager getSleepManager();

	/**
	 * power control chip driver
	 * @return the power control chip driver
	 */
	ILTC3455 getLTC3455();
	
	/**
	 * Get the usb power daemon
	 * @return the usb power daemon
	 */
	IUSBPowerDaemon getUsbPowerDaemon();
	
	/**
	 * Get the PowerController - the AVR on the spot.
	 * @return the PowerController
	 */
	IPowerController getPowerController();

	/**
	 * Set a persistent property in the flash memory
	 * @param key
	 * @param value the value required or null to erase
	 */
	void setPersistentProperty(String key, String value);

	/**
	 * Set one or more persistent property in the flash memory
	 * @param props the properties to set
	 */
	void setPersistentProperties(Properties props);

	/**
	 * Replace the persistent properties held in the flash memory
	 * @param props the properties to set
	 * @throws IOException 
	 */
	void storeProperties(Properties props) throws IOException;

	/**
	 * Get a persistent property, as held in the flash memory
	 * @param key
	 * @return the value or null if no property with the specified key
	 */
	String getPersistentProperty(String key);
	
	/**
	 * Get all the persistent properties, as held in the flash memory
	 * @return the set of persistent properties
	 */
	Properties getPersistentProperties();

	/**
	 * Get the OTA Command Server, creating one if necessary
	 * @return the OTA command server
	 * @throws IOException 
	 */
	IOTACommandServer getOTACommandServer() throws IOException;

	/**
	 * Get the singleton radio policy manager
	 * @return the radio policy manager
	 */
	IRadioPolicyManager getRadioPolicyManager();

	/**
	 * Set the system property "key" to have the value "value"
	 * 
	 * @param key
	 * @param value
	 */
	void setProperty(String key, String value);

	/**
	 * Return a tick count in the range of zero to {@link #SYSTEM_TICKER_TICKS_PER_MILLISECOND}.
	 * The count resets to zero every millisecond
	 * 
	 * @return the tick count
	 */
	int getSystemTicks();

	/**
	 * Get the singleton resource registry
	 * @return the resource registry
	 */
	IResourceRegistry getResourceRegistry();
	
	/**
	 * Get the singleton AT91 chip power manager
	 * @return the AT91 power manager
	 */
	IAT91_PowerManager getAT91_PowerManager();

	/**
	 * Get the singleton DMA Memory Manager.
	 * The DMA Memory Manager allocates uncached memory suitable for use as DMA buffers.
	 * @return the DMA Memory Manager
	 */
	IDMAMemoryManager getDMAMemoryManager();
	
	/**
	 * Get the singleton remote print manager
	 * @return the remote print manager
	 */
	IRemotePrintManager getRemotePrintManager();
}
