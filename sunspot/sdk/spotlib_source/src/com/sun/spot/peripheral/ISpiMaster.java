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
 * Interface to the SPI master controller.
 * <p>
 * The SPI transfers are done by C code (in spi.c).
 * The transfer is done using DMA in most cases (some small transfers don't bother with it).
 * <p>
 * Each call to the transfer operations defined in this interface require a SpiPcs parameter
 * which defines the peripheral chip select that is to be used (ie which of the 4 channels) plus
 * the configuration required.
 * <p>
 * The configuration is as per the SPI_CSRx register definition.
 * Constants for the fields in the CSR are defined in this interface. Details can be found in
 * the Atmel AT91RM9200 datasheet.
 * <p>
 * An example configuration would be:
 * <p>
 * (ISpiMaster.CSR_MODE0 | ISpiMaster.CSR_BITS_8 | ISpiMaster.CSR_SCBR_250K | ISpiMaster.CSR_DLYBCT_200)
 * <p>
 * Arbitrary SPI speeds can be obtained by manipulating the CSR register through the SpiPcs class.
 * <p>
 * SPI rate is given by MCK/(2*N), where MCK = 60e6 and N is the 8-bit number in Byte 1 (mask 0x0000ff00) of CSRx
 * <p>
 * Clock rates from 117.647kHz (N = 255) to 15MHz (N = 2) can be obtained.
 * <p>
 * For example, to get a 500k SPI with Mode 1, 8 bits and 1 cycle delay between consecutive transfers:
 * <p>
 * Solve 500000 = 60000000/(2*N) for N => N = 60000000 / (2 * 500000) = 60
 * <p>
 * Thus:
 *<code>
 * <pre>
 *    PeripheralChipSelect cSel = PeripheralChipSelect.SPI_PCS_BD_SEL1;
 *    int spiConfig = ISpiMaster.CSR_MODE1 | ISpiMaster.CSR_BITS_8 | (60 << 8) | ISpiMaster.CSR_DLYBCT_1;
 *    SpiPcs pcs = new SpiPcs(cSel, spiConfig);
 * </pre>
 *</code>
 * <p>
 * See also: section 28.6.9, page 394 of AT91RM9200 datasheet
 * <p>
 * @see <a href="http://www.atmel.com/dyn/resources/prod_documents/doc1354.pdf">AT91 Spec</a>
*/
public interface ISpiMaster {
	/**
	 * The inactive state value of SPCK is logic level one
	 */
	public static final int CSR_CPOL = 1<<0;
	/**
	 * Data is captured on the leading edge of SPCK and changed on the following edge of SPCK
	 */
	public static final int CSR_NCPHA = 1<<1;
	/**
	 * Mode 0, as defined in the AT91 spec (this is the most common mode).
	 * CPOL=0, NCPHA=1
	 * Clock is active high, data is captured on the leading edge.
	 */
	public static final int CSR_MODE0 = CSR_NCPHA;
	/**
	 * Mode 1, as defined in the AT91 spec.
	 * CPOL=0, NCPHA=0
	 * Clock is active high, data is captured on the trailing edge.
	 */
	public static final int CSR_MODE1 = 0;
	/**
	 * Mode 2, as defined in the AT91 spec.
	 * CPOL=1, NCPHA=1
	 * Clock is active low, data is captured on the leading edge.
	 */
	public static final int CSR_MODE2 = CSR_CPOL | CSR_NCPHA;
	/**
	 * Mode 3, as defined in the AT91 spec.
	 * CPOL=1, NCPHA=0
	 * Clock is active low, data is captured on the trailing edge.
	 */
	public static final int CSR_MODE3 = CSR_CPOL;

	/**
	 * The unit of transfer is 8 bits
	 */
	public static final int CSR_BITS_8 = 0<<4;
	/**
	 * The unit of transfer is 9 bits
	 */
	public static final int CSR_BITS_9 = 1<<4;
	/**
	 * The unit of transfer is 10 bits
	 */
	public static final int CSR_BITS_10 = 2<<4;
	/**
	 * The unit of transfer is 11 bits
	 */
	public static final int CSR_BITS_11 = 3<<4;
	/**
	 * The unit of transfer is 12 bits
	 */
	public static final int CSR_BITS_12 = 4<<4;
	/**
	 * The unit of transfer is 13 bits
	 */
	public static final int CSR_BITS_13 = 5<<4;
	/**
	 * The unit of transfer is 14 bits
	 */
	public static final int CSR_BITS_14 = 6<<4;
	/**
	 * The unit of transfer is 15 bits
	 */
	public static final int CSR_BITS_15 = 7<<4;
	/**
	 * The unit of transfer is 16 bits
	 */
	public static final int CSR_BITS_16 = 8<<4;

	/**
	 * Bit mask for clock rate selector
	 */
	public static final int CSR_SCBR = 0xFF << 8;
	/**
	 * 1 MHz clock rate
	 */
	public static final int CSR_SCBR_1MHZ = 30 << 8;
	/**
	 * 2 MHz clock rate
	 */
	public static final int CSR_SCBR_2MHZ = 15 << 8;
	/**
	 * 3 MHz clock rate
	 */
	public static final int CSR_SCBR_3MHZ = 10 << 8;
	/**
	 * 6 MHz clock rate
	 */
	public static final int CSR_SCBR_6MHZ = 5 << 8;
	/**
	 * 250 KHz clock rate
	 */
	public static final int CSR_SCBR_250K = 120 << 8;

	/**
	 * Bit mask for field that controls the delay before the transfer starts after CS goes active.
	 * Set this field to insert a delay (0 gives 8ns delay, 1 gives 16ns, 0xFF gives 4.25us).
	 */
	public static final int CSR_DLYBS = 0xFF << 16;

	/**
	 * Bit mask for field that controls the delay between consecutive transfers (e.g. between bytes if CSR_BITS_8),
	 * and the delay after the last transfer before CS is released.
	 * Set this non-zero to insert a delay. (0 is special and gives 66ns delay, 1 gives 533ns, 0xFF gives 136us).
	 */
	public static final int CSR_DLYBCT = 0xFF << 24;
	/**
	 * Select a value of 1 (533ns) for DLYBCT
	 */
	public static final int CSR_DLYBCT_1 = 1 << 24;
	/**
	 * Select a value of 2 (1.06us) for DLYBCT
	 */
	public static final int CSR_DLYBCT_2 = 2 << 24;
	/**
	 * Select a value of 10 (5.33us) for DLYBCT
	 */
	public static final int CSR_DLYBCT_10 = 10 << 24;
	/**
	 * Select a value of 50 (26.6us) for DLYBCT
	 */
	public static final int CSR_DLYBCT_50 = 50 << 24;
	/**
	 * Select a value of 100 (53.3us) for DLYBCT
	 */
	public static final int CSR_DLYBCT_100 = 100 << 24;
	/**
	 * Select a value of 200 (106.6us) for DLYBCT
	 */
	public static final int CSR_DLYBCT_200 = 200 << 24;

	/**
	 * SPI send of 8 bits, plus simultaneous receive of 8 bits
	 * 
	 * @param pcs SPI Peripheral Chip Select to use
	 * @param data the data to send in bits 7:0
	 * @return byte received in bits 7:0 of int
	 */
	public abstract int sendReceive8(SpiPcs pcs, int data);

	/**
	 * SPI send of 8 bits, plus simultaneous receive of 8 bits, then send 16 bits
	 * 
	 * @param pcs SPI Peripheral Chip Select to use
	 * @param first the data to send in bits 0:7
	 * @param subsequent the subsequent bytes to send, bits 15:8 then bits 7:0
	 * @return byte received in bits 7:0 of int
	 */
	public abstract int sendReceive8PlusSend16(SpiPcs pcs, int first, int subsequent);

	/**
	 * SPI send of 8 bits, simultaneous receive of 8 bits, then send of multiple 8 bits
	 * 
	 * @param pcs SPI Peripheral Chip Select to use
	 * @param first the first 8 bits to send in bits 7:0
	 * @param size number of bytes to send
	 * @param subsequent the bytes to send
	 * @return byte received in bits 0:7 of int
	 */
	public abstract int sendReceive8PlusSendN(SpiPcs pcs, int first, int size, byte[] subsequent);

	/**
	 * General SPI send and receive
	 * 
	 * @param pcs SPI Peripheral Chip Select to use
	 * @param txSize the number of bytes to send
	 * @param tx the bytes to send
	 * @param rxOffset positive integer indicating the number of bytes transmitted before rx begins
	 * @param rxSize the number of bytes to receive
	 * @param rx buffer for the received bytes
	 */
	public abstract void sendAndReceive(SpiPcs pcs, int txSize, byte[] tx, int rxOffset, int rxSize, byte[] rx);

	/**
	 * General SPI send and receive
	 * 
	 * @param pcs SPI Peripheral Chip Select to use
	 * @param deviceAddress Board device address to use
	 * @param txSize the number of bytes to send
	 * @param tx the bytes to send
	 * @param rxOffset positive integer indicating the number of bytes transmitted before rx begins
	 * @param rxSize the number of bytes to receive
	 * @param rx buffer for the received bytes
	 */
	public abstract void sendAndReceive(SpiPcs pcs, int deviceAddress, int txSize, byte[] tx, int rxOffset, int rxSize, byte[] rx);

	/**
	 * SPI send of 8 bits, plus simultaneous receive of 8 bits, then receive 16 bits
	 * 
	 * @param pcs SPI Peripheral Chip Select to use
	 * @param first the data to send in bits 0:7
	 * @return bytes received, first in bits 7:0, second in bits 23:16, third in bits 15:8
	 */
	public abstract int sendReceive8PlusReceive16(SpiPcs pcs, int first);

	/**
	 * Specialised SPI send of 8 bits, simultaneous receive of 8 bits, then receive of multiple 8 bits<br><br>
	 * 
	 * This routine is highly specialised for the CC2420<br><br>
	 * 
	 * the number of bytes received is determined by the 7 bit value read as the first byte after the initial 8 bits<br><br>
	 * 
	 * @param pcs SPI Peripheral Chip Select to use
	 * @param first the first 8 bits to send in bits 7:0
	 * @param subsequent byte array to receive data; length (max 127 not including length byte) will be written into first byte
	 * @param fifo_pin pin to test for overflow. The INVERSE state of this bit is written into the top bit of the length byte.
	 * @return bits 7:0 --> first 8 bits read
	 */
	public abstract int sendReceive8PlusVariableReceiveN(SpiPcs pcs, int first,
			byte[] subsequent, PIOPin fifo_pin);

	/**
	 * Allows the loopback feature to be turned on and off. Loopback is useful for testing.
	 * @param b true to turn loopback on, false to turn it off
	 */
	public abstract void setLoopback(boolean b);

	/**
	 * @return the maximum transfer size
	 */
	public abstract int getMaxTransferSize();
}
