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

package com.sun.spot.peripheral.external;

import com.sun.spot.peripheral.ISpiMaster;
import com.sun.spot.peripheral.PeripheralChipSelect;
import com.sun.spot.peripheral.SpiPcs;
import com.sun.spot.peripheral.Spot;


/**
 * SPI implementation that hides the use of a DeviceSelector before sending SPI
 * commands.<p/>
 * 
 * See {@link ISPI} for details of usage.
 */
public class BoardDeviceSPI implements ISPI {
	private SpiPcs cs_pin;
    private ISpiMaster spi;
    private int deviceAddress;
	private byte[] emptyByteArray = new byte[0];
	private byte[] preAllocSend16Array = new byte[2];
  
    //for testing and internal use only
    BoardDeviceSPI(int deviceAddress, SpiPcs cs_pin, ISpiMaster spi) {
        this.deviceAddress = deviceAddress;
        this.cs_pin = cs_pin;
        this.spi = spi;
    }
    
    /**
     * Create a BoardDeviceSPI using the supplied device address. See {@link ISpiMaster} for
     * details of SPI configuration settings.
     * @param deviceAddress The device address to use when accessing the SPI
     * @param chipSelect The board chip select of the board that this device is on
     * @param spiConfiguration Value to be written into SPI_CSR for transfers to this device
     */
    public BoardDeviceSPI(int deviceAddress, PeripheralChipSelect chipSelect, int spiConfiguration){
    	this(deviceAddress, new SpiPcs(chipSelect, spiConfiguration), Spot.getInstance().getSPI());
    }

    public void send16bits(int i) {
        preAllocSend16Array[0] = (byte)((i>>8) & 0xff);
        preAllocSend16Array[1] = (byte)(i & 0xff);
        spi.sendAndReceive(cs_pin, deviceAddress, 2, preAllocSend16Array, 0, 0, emptyByteArray);
    }

    public void sendSPICommand(byte[] commandSequence, byte[] readByteSequence) {
        spi.sendAndReceive(cs_pin, deviceAddress, commandSequence.length, commandSequence, 0, readByteSequence.length, readByteSequence);
    }

	public void sendSPICommand(byte[] commandSequence, int commandLength, byte[] readByteSequence, int readLength) {
        spi.sendAndReceive(cs_pin, deviceAddress, commandLength, commandSequence, 0, readLength, readByteSequence);
	}

	public void sendSPICommand(byte[] commandSequence, int commandLength, byte[] readByteSequence, int readLength, int readOffset) {
        spi.sendAndReceive(cs_pin, deviceAddress, commandLength, commandSequence, readOffset, readLength, readByteSequence);
	}
	
	public void setConfiguration(int config) {
		cs_pin.setConfiguration(config);
	}
}
