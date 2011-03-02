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


public class CC2420Driver {
	
	public static final int SPI_CONFIG = (ISpiMaster.CSR_NCPHA | ISpiMaster.CSR_BITS_8 | ISpiMaster.CSR_SCBR_1MHZ);

	private SpiMaster spi;
	private SpiPcs ce_pin;
	
	private PIOPin vreg_en_pin;
	private PIOPin reset_pin;
	private PIOPin fifop_pin;
	private PIOPin fifo_pin;
	private PIOPin sfd_pin;
	private PIOPin cca_pin;
	
	private AT91_PIO pioA;
	private AT91_PIO pioB;
	
	public CC2420Driver(ISpiMaster spi, SpiPcs ce_pin, ISpotPins spotPins) {
		this.spi = (SpiMaster) spi;
		this.ce_pin = ce_pin;
		vreg_en_pin = spotPins.getCC2420_VREG_EN_Pin();
		fifop_pin = spotPins.getCC2420_FIFOP_Pin();
		fifo_pin = spotPins.getCC2420_FIFO_Pin();
		sfd_pin = spotPins.getCC2420_SFD_Pin();
		cca_pin = spotPins.getCC2420_CCA_Pin();
		reset_pin = spotPins.getCC2420_RESET_Pin();
		pioA = (AT91_PIO) fifop_pin.pio;
		pioB = (AT91_PIO) fifo_pin.pio;
	}

	public void sendAndReceive(int txSize, byte[] tx, int rxOffset, int rxSize, byte[] rx) {
		spi.sendAndReceive(ce_pin, txSize, tx, rxOffset, rxSize, rx);
	}
	
	public int sendReceive8PlusSendN(int first, int size, byte[] subsequent) {
		return spi.sendReceive8PlusSendN(ce_pin, first, size, subsequent);
	}
	
	public int getRegister(int first) {
		return spi.sendReceive8PlusReceive16(ce_pin, first);
	}
	
	public int setRegister(int first, int subsequent){
		return spi.sendReceive8PlusSend16(ce_pin, first, subsequent);
	}
	
	public int sendStrobe(int data) {
		return spi.sendReceive8(ce_pin, data);
	}
	
	public int sendReceive8PlusVariableReceiveN(int first, byte[] subsequent){
		return spi.sendReceive8PlusVariableReceiveN(ce_pin, first, subsequent, fifo_pin);
	}
	
	/**
	 * Write the chip's ram. <b>To support testing</b>
	 * 
	 * @param ramAddress -- the address at which to write
	 * @param data -- the byte to write
	 * @return result of the SPI command
	 */
	public int sendRAM(int ramAddress, byte data) {
		return sendRAM(ramAddress, data, 1);
	}
	
	public int sendRAM(int ramAddress, short data) {
		return sendRAM(ramAddress, data, 2);
	}
	
	public int sendRAM(int ramAddress, long data) {
		return sendRAM(ramAddress, data, 8);
	}
	
	private int sendRAM(int ramAddress, long data, int numberOfBytesInData) {
		byte[] buff = new byte[numberOfBytesInData+1];
		// Send 2 MSB of address as top two bits, plus "0" for write in bit 5
		buff[0] = (byte)((ramAddress & 0x180) >> 1);
		// Set data
		for (int i=1; i<numberOfBytesInData+1; i++) {
			buff[i] = (byte)(data & 0xFF);
			data >>= 8;
		}
		// Send 1 as top bit for RAM Access, plus 7 LSB of address
		return spi.sendReceive8PlusSendN(ce_pin, 0x80 | (ramAddress & 0x7F), numberOfBytesInData+1, buff);		
	}
	
	/**
	 * Read the chip's ram. <b>To support testing</b>
	 * 
	 * @param ramAddress -- the address at which to read ram
	 * @return -- the byte of ram read
	 */
	public byte receiveRAM(int ramAddress) {
		byte[] rxBuff = new byte[3];
		// First byte:  Send 1 as top bit for RAM Access, plus 7 LSB of address
		// Second byte: Send 2 MSB of address as top two bits, plus "1" for read in bit 5
		byte[] txBuff = new byte[]{	(byte)(0x80 | (ramAddress & 0x7F)), 
									(byte)(((ramAddress & 0x180) >> 1) | 0x20)};
		sendAndReceive(2, txBuff, 0, 3, rxBuff);
		return rxBuff[2];
	}

	/**
	 * Read the chip's rx fifo. <b>To support testing</b>
	 * 
	 * @return -- the contents of the fifo
	 */
	public byte[] readRxFifo() {
		byte[] rxBuff = new byte[128];
		// First byte:  Send 1 as top bit for RAM Access, plus 7 LSB of address
		// Second byte: Send 2 MSB of address as top two bits, plus "1" for read in bit 5
		byte[] txBuff = new byte[]{	(byte)(0x80), 
									(byte)((0x80 >> 1) | 0x20)};
		spi.sendAndReceive(ce_pin, 2, txBuff, 2, rxBuff.length, rxBuff);
		return rxBuff;
	}

	public boolean isSfdHigh() {
		return sfd_pin.isHigh();
	}

	public boolean isFifopHigh() {
		return fifop_pin.isHigh();
	}

	public boolean isFifoHigh() {
		return fifo_pin.isHigh();
	}

	public boolean isCcaHigh() {
		return cca_pin.isHigh();
	}
	
	public boolean isVRegEnHigh() {
		return vreg_en_pin.isHigh();
	}
	
	public void setVRegEn(boolean b) {
		vreg_en_pin.setState(b);
	}
	
	public void setReset(boolean b) {
		reset_pin.setState(b);
	}

	public void tearDown() {
		reset_pin.release();
		fifo_pin.release();
		fifop_pin.release();
		cca_pin.release();
		vreg_en_pin.release();
		sfd_pin.release();
	}

	public void setUp() {
		reset_pin.claim();
		fifo_pin.claim();
		fifop_pin.claim();
		cca_pin.claim();
		vreg_en_pin.claim();
		sfd_pin.claim();
		reset_pin.openForOutput();
		vreg_en_pin.openForOutput();
		fifo_pin.openForInput();
		cca_pin.openForInput();
		sfd_pin.openForInput();
		reset_pin.setHigh();
	}
}
