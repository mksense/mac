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

import java.io.IOException;

/**
 * Interface for communicating with devices using the I2C protocol.
 *
 * @author Ron Goldman
 */
public interface II2C {

    /**
     * Initialize the I2C hardware.
     *
     * @throws java.io.IOException
     */
    void open() throws IOException;

    /**
     * Release any resources associated with the I2C hardware.
     *
     * @throws java.io.IOException
     */
    void close() throws IOException;

    /**
     * Set the clock speed to use for I2C data transfers.
     * 
     * @param clockSpeed I2C bus clock in Hertz
     */
    void setClockSpeed(int clockSpeed);

    /**
     * Write data to the specified I2C slave device.
     *
     * @param slaveAddress 7-bit slave address, shifted left one bit so LSB is not used
     * @param data Array of bytes to write
     * @param off Offset from which to start writing data
     * @param len How many bytes of data to write. Note: some implementations may limit
     *              the number of bytes that can be written in a single call, e.g. the
     *              eDemo board firmware has a fixed buffer that can only hold 40 bytes.
     *
     * @throws java.io.IOException
     */
    void write(int slaveAddress, byte[] data, int off, int len) throws IOException;

    /**
     * Write data to the specified I2C slave device.
     *
     * @param slaveAddress 7-bit slave address, shifted left one bit so LSB is not used
     * @param internalAddress 0-4 bytes of internal address information sent to the slave
     *              device before any data; Note: some implementations may not support
     *              sending a full 4 bytes of data, e.g. the ARM9 can only send 0-3 bytes
     * @param internalAddressSize how many bytes of internal address information are to be sent
     * @param data Array of bytes to write
     * @param off Offset from which to start writing data
     * @param len How many bytes of data to write. Note: some implementations may limit
     *              the number of bytes that can be written in a single call, e.g. the
     *              eDemo board firmware has a fixed buffer that can only hold 40 bytes.
     *
     * @throws java.io.IOException
     */
    void write(int slaveAddress, int internalAddress, int internalAddressSize, byte[] data, int off, int len) throws IOException;

    /**
     * Read data from the specified I2C slave device.
     *
     * @param slaveAddress 7-bit slave address, shifted left one bit so LSB is not used
     * @param data Array of bytes to read into
     * @param off Offset from which to start reading data
     * @param len How many bytes of data to read. Note: some implementations may limit
     *              the number of bytes that can be written in a single call, e.g. the
     *              eDemo board firmware has a fixed buffer that can only hold 40 bytes.
     *
     * @throws java.io.IOException
     */
    void read(int slaveAddress, byte[] data, int off, int len) throws IOException;

    /**
     * Read data from the specified I2C slave device.
     *
     * @param slaveAddress 7-bit slave address, shifted left one bit so LSB is not used
     * @param internalAddress 0-4 bytes of internal address information sent to the slave
     *              device before any data; Note: some implementations may not support
     *              sending a full 4 bytes of data, e.g. the ARM9 can only send 0-3 bytes
     * @param internalAddressSize how many bytes of internal address information are to be sent
     * @param data Array of bytes to read into
     * @param off Offset from which to start reading data
     * @param len How many bytes of data to read. Note: some implementations may limit
     *              the number of bytes that can be written in a single call, e.g. the
     *              eDemo board firmware has a fixed buffer that can only hold 40 bytes.
     *
     * @throws java.io.IOException
     */
    void read(int slaveAddress, int internalAddress, int internalAddressSize, byte[] data, int off, int len) throws IOException;

    /**
     * Check if an I2C data transfer is currently in process.
     *
     * @return true if a data transfer is currently in process
     */
    boolean isBusy();

    /**
     * Check if a slave device is present at the given address.
	 * Tries to write a byte to the device and sees if it gets a NACK.
     *
     * @param slaveAddress 7-bit slave address, shifted left one bit so LSB is not used
     * @param probeData - byte value to try writing to device
     * @return false if no device is present
     */
    boolean probe(int slaveAddress, int probeData);

}
