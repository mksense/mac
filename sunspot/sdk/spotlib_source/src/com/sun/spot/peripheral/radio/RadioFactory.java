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

import com.sun.spot.peripheral.CC2420Driver;
import com.sun.spot.peripheral.ISleepManager;
import com.sun.spot.peripheral.ISpot;
import com.sun.spot.peripheral.PeripheralChipSelect;
import com.sun.spot.peripheral.SpiPcs;
import com.sun.spot.peripheral.Spot;
import com.sun.spot.resourcesharing.IResourceRegistry;
import com.sun.squawk.VM;

/**
 * Static factory for public access to the radio in various guises.
 */
public class RadioFactory {

    private static CC2420 theCC2420;
    private static MACLayer theMACLayer;
    /*
     * This class should not be instantiated.
     */

    private RadioFactory() {
    }

    private static CC2420 getCC2420() {
        ISpot spot = Spot.getInstance();
        if (!spot.isMasterIsolate()) {
            throw new IllegalStateException("Cannot access physical radio from a child isolate");
        }
        if (theCC2420 == null) {
            CC2420Driver cc2420Spi = new CC2420Driver(spot.getSPI(), new SpiPcs(PeripheralChipSelect.SPI_PCS_CC2420, CC2420Driver.SPI_CONFIG), spot.getSpotPins());
            theCC2420 = new CC2420(cc2420Spi, spot.getAT91_AIC());
            spot.getDriverRegistry().add(theCC2420);
        }
        return theCC2420;
    }

    /**
     * Answer the interface for dealing with the radio at the I802.15.4 MAC level.
     * @return the radio MAC layer object
     */
    public static I802_15_4_MAC getI802_15_4_MAC() {
        if (!Spot.getInstance().isMasterIsolate()) {
            throw new IllegalStateException("Cannot access MAC from a child isolate");
        }
        if (theMACLayer == null) {
            theMACLayer = new MACLayer(getCC2420());
        }
        return theMACLayer;
    }

    /**
     * Answer the interface for accessing the socket MAC.
     * @return the socket MAC layer object
     */
    public synchronized static I802_15_4_MAC getSocketMAC() {
        return null;
    }
    
    /******
     * 
     * Some routines moved here so spotlibhost does not need to reference Spot.java
     * 
     ******/

    /**
     * @return true if running on the host, false if on the SPOT
     */
    public static boolean isRunningOnHost() {
        return false;
    }
	
    /**
     * @return true if this method has been called in the context of the master isolate
     */
    public static boolean isMasterIsolate() {
        return Spot.getInstance().isMasterIsolate();
    }

    /**
     * Set the system property "key" to have the value "value"
     * 
     * @param key
     * @param value
     */
    public static void setProperty(String key, String value) {
        Spot.getInstance().setProperty(key, value);
    }

    /**
     * Set a persistent property "key" to have the value "value"
     *
     * @param key
     * @param value the value required or null to erase
     */
    public static void setPersistentProperty(String key, String value) {
        Spot.getInstance().setPersistentProperty(key, value);
    }

    /**
     * Get the singleton resource registry
     * 
     * @return the resource registry
     */
    public static IResourceRegistry getResourceRegistry() {
        return Spot.getInstance().getResourceRegistry();
    }

    /**
     * Get access to the sleep manager for the Spot
     * 
     * @return The sleep manager
     */
    public static ISleepManager getSleepManager() {
        return Spot.getInstance().getSleepManager();
    }
    
    /**
     * Get the singleton radio policy manager
     * 
     * @return the radio policy manager
     */
    public static IRadioPolicyManager getRadioPolicyManager() {
        return Spot.getInstance().getRadioPolicyManager();
    }

    /**
     * Get access to the physical I802.15.4 radio device
     * 
     * @return the I802.15.4 physical radio device
     */
    public static I802_15_4_PHY getI802_15_4_PHY() {
        return getCC2420();
    }


    /**
     * Get access to the I802.15.4 MAC layers
     * 
     * @return the I802.15.4 MAC layers
     */
    public static I802_15_4_MAC[] getI802_15_4_MACs() {
        return Spot.getInstance().getI802_15_4_MACs();
    }

    /**
     * Get access to the radio via its proprietary (non-I802.15.4) interface.
     * 
     * @return the proprietary interface to the radio device
     */
    public static IProprietaryRadio getIProprietaryRadio() {
        return getCC2420();
    }


    public static void setAsDaemonThread(Thread thread) {
        VM.setAsDaemonThread(thread);
    }

    public static void closeBaseStation() {
        System.err.println("Ignoring call to closeBaseStation");
    }
}
