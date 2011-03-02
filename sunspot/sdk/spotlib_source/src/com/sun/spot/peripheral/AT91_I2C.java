/*
 * Copyright 2008 Sun Microsystems, Inc. All Rights Reserved.
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

import com.sun.squawk.VM;
import com.sun.squawk.vm.ChannelConstants;
import java.io.IOException;

/**
 * Class for communicating with devices using the I2C protocol using the ARM9
 * TWI hardware. Uses the I2C_DATA and I2C_CLK pins in the top connector
 * for SDA and SCK of the TWI interface.
 *
 * @author Ron Goldman
 */
public class AT91_I2C implements II2C {

    private static final int AT91C_TWI_COMPLETE = 0x01;  // (TWI) Transmission completed
    private static final int AT91C_TWI_OVERRUN  = 0x40;  // (TWI) Overrun Error
    private static final int AT91C_TWI_UNDERRUN = 0x80;  // (TWI) Underrun Error
    private static final int AT91C_TWI_NACK     = 0x100; // (TWI) Not Acknowledged

    private boolean open = false;

    private String getError(int status) {
        if ((status & AT91C_TWI_NACK) != 0) {
            return "NACK";
        } else if ((status & AT91C_TWI_OVERRUN) != 0) {
            return "Over run";
        } else if ((status & AT91C_TWI_UNDERRUN) != 0) {
            return "Under run";
        } else if ((status & AT91C_TWI_COMPLETE) != 0) {
            return "TX Completed early";
        } else {
            return Integer.toHexString(status);
        }
    }

    private IOException error(String msg, int slaveAddress, int status) {
        return new IOException("[AT91_I2C] device: " + Integer.toHexString(slaveAddress & 0xFF)
                + ": "+ msg + getError(status));
    }

    private void checkOpen() {
        if (!open) {
            throw new IllegalStateException("[AT91_I2C] I2C hardware has not been opened");
        }
    }

    /**
     * Initialize the I2C hardware.
     *
     * @throws java.io.IOException
     */
    public void open() throws IOException {
        // claim TWI pins if/when SpotPins defines them
        VM.execSyncIO(ChannelConstants.I2C_OPEN, 0);
        open = true;
    }

    /**
     * Release any resources associated with the I2C hardware.
     *
     * @throws java.io.IOException
     */
    public void close() throws IOException {
        // release TWI pins if/when SpotPins defines them
        VM.execSyncIO(ChannelConstants.I2C_CLOSE, 0);
        open = false;
    }

    /**
     * Set the clock speed to use for I2C data transfers.
     * The initial clock speed is set to 100KHz.
     * 
     * @param clockSpeed I2C bus clock in Hertz
     */
    public void setClockSpeed(int clockSpeed) {
        checkOpen();
        VM.execSyncIO(ChannelConstants.I2C_SET_CLOCK_SPEED, clockSpeed);
    }

    /**
     * Write data to the specified I2C slave device.
     *
     * @param slaveAddress 7-bit slave address, shifted left one bit so LSB is not used
     * @param data Array of bytes to write
     * @param off Offset from which to start writing data
     * @param len How many bytes of data to write
     *
     * @throws java.io.IOException
     */
    public void write(int slaveAddress, byte[] data, int off, int len) throws IOException {
        if ((off + len) > data.length) {
            throw new IllegalArgumentException("Offset + length parameters exceed array size.");
        }
        checkOpen();
        int status = VM.execSyncIO(ChannelConstants.I2C_WRITE, slaveAddress >> 1, 0, 0, off, len, 0, data, null);
        if (status != 0) {
            throw error("Error during I2C write: ", slaveAddress, status);
        }
    }

    /**
     * Write data to the specified I2C slave device.
     *
     * @param slaveAddress 7-bit slave address, shifted left one bit so LSB is not used
     * @param internalAddress 0-3 bytes of internal address information sent to the slave
     *              device before any data.
     * @param internalAddressSize how many bytes of internal address information are to be sent
     * @param data Array of bytes to write
     * @param off Offset from which to start writing data
     * @param len How many bytes of data to write
     *
     * @throws java.io.IOException
     */
    public void write(int slaveAddress, int internalAddress, int internalAddressSize, byte[] data, int off, int len) throws IOException {
        if ((off + len) > data.length) {
            throw new IllegalArgumentException("Offset + length parameters exceed array size.");
        }
        if (internalAddressSize > 3 || internalAddressSize < 0) {
            throw new IllegalArgumentException("For ARM9 the internal address size must be 0-3 bytes");
        }
        checkOpen();
        int status = VM.execSyncIO(ChannelConstants.I2C_WRITE, slaveAddress >> 1, internalAddress, internalAddressSize, off, len, 0, data, null);
        if (status != 0) {
            throw error("Error during I2C write: ", slaveAddress, status);
        }
    }

    /**
     * Read data from the specified I2C slave device.
     *
     * @param slaveAddress 7-bit slave address, shifted left one bit so LSB is not used
     * @param data Array of bytes to read into
     * @param off Offset from which to start reading data
     * @param len How many bytes of data to read
     *
     * @throws java.io.IOException
     */
    public void read(int slaveAddress, byte[] data, int off, int len) throws IOException {
        if ((off + len) > data.length) {
            throw new IllegalArgumentException("Offset + length parameters exceed array size.");
        }
        checkOpen();
        int status = VM.execSyncIO(ChannelConstants.I2C_READ, slaveAddress >> 1, 0, 0, off, len, 0, null, data);
        if (status != 0) {
            throw error("Error during I2C read: ", slaveAddress, status);
        }
    }

    /**
     * Read data from the specified I2C slave device.
     *
     * @param slaveAddress 7-bit slave address, shifted left one bit so LSB is not used
     * @param internalAddress 0-3 bytes of internal address information sent to the slave
     *              device before any data
     * @param internalAddressSize how many bytes of internal address information are to be sent
     * @param data Array of bytes to read into
     * @param off Offset from which to start reading data
     * @param len How many bytes of data to read
     *
     * @throws java.io.IOException
     */
    public void read(int slaveAddress, int internalAddress, int internalAddressSize, byte[] data, int off, int len) throws IOException {
        if ((off + len) > data.length) {
            throw new IllegalArgumentException("Offset + length parameters exceed array size.");
        }
        if (internalAddressSize > 3 || internalAddressSize < 0) {
            throw new IllegalArgumentException("For ARM9 the internal address size must be 0-3 bytes");
        }
        checkOpen();
        int status = VM.execSyncIO(ChannelConstants.I2C_READ, slaveAddress >> 1, internalAddress, internalAddressSize, off, len, 0, null, data);
        if (status != 0) {
            throw error("Error during I2C read: ", slaveAddress, status);
        }
    }

    /**
     * Check if an I2C data transfer is currently in process.
     *
     * @return true if a data transfer is currently in process
     */
    public boolean isBusy() {
        checkOpen();
        return (VM.execSyncIO(ChannelConstants.I2C_BUSY, 0) != 0);
    }

    /**
     * Check if a slave device is present at the given address.
	 * Tries to write a byte to the device and sees if it gets a NACK.
     *
     * @param slaveAddress 7-bit slave address, shifted left one bit so LSB is not used
     * @param probeData - byte value to try writing to device
     * @return false if no device is present
     */
    public boolean probe(int slaveAddress, int probeData) {
        checkOpen();
        return (VM.execSyncIO(ChannelConstants.I2C_PROBE, slaveAddress >> 1, probeData, 0, 0, 0, 0, null, null) != 0);
    }

}
