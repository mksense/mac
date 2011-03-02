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
 * Interface to provide PIOPin objects representing pins assigned to
 * various external functions on the Spot board. This allows the state
 * of the physical pins to be read or written.
 * 
 * @author Syntropy
 */

public interface ISpotPins {
	
	
	/**
	 * The pin used by the power control PowerController to signal to the 9200
	 * @return the pin
	 */
	PIOPin getAttentionPin();
	
	/**
	 * The pin used to select USB high power mode of the LTC3455
	 * @return the pin
	 */
	PIOPin getUSB_HP();
	
	/**
	 * The pin used to select USB "suspend" mode in the LTC3455
	 * @return the pin
	 */
	PIOPin getUSB_EN();
	
	/**
	 * The pin used to monitor power on the USB connector
	 * @return the pin
	 */
	PIOPin getUSB_PWR_MON();
	
	/**
	 * The Channel Clear pin of the CC2420
	 * @return Returns the CC2420_CCA_Pin.
	 */
	PIOPin getCC2420_CCA_Pin();

	/**
	 * The FIFO empty pin of the CC2420
	 * @return Returns the CC2420_FIFO_Pin.
	 */
	PIOPin getCC2420_FIFO_Pin();

	/**
	 * The FIFO threshold reached pin of the CC2420
	 * @return Returns the CC2420_FIFOP_Pin.
	 */
	PIOPin getCC2420_FIFOP_Pin();

	/**
	 * The reset pin of the CC2420
	 * @return Returns the CC2420_RESET_Pin.
	 */
	PIOPin getCC2420_RESET_Pin();

	/**
	 * The Start of Frame Delimiter pin of the CC2420
	 * @return Returns the CC2420_SFD_Pin.
	 */
	PIOPin getCC2420_SFD_Pin();

	/**
	 * The voltage regulator enable pin of the CC2420
	 * @return Returns the CC2420_VREG_EN_Pin.
	 */
	PIOPin getCC2420_VREG_EN_Pin();

	/**
	 * Return the pin controlling the green LED on the Spot board.
	 * @return Returns the localGreenLEDPin.
	 */
	PIOPin getLocalGreenLEDPin();

	/**
	 * Return the pin controlling the red LED on the Spot board.
	 * @return Returns the localRedLEDPin.
	 */
	PIOPin getLocalRedLEDPin();

	/**
	 * Return the pins not available to the given PIO because they are used for
	 * low-level functions (i.e. are not to be controlled by Java).
	 * 
	 * @param pio the PIO whose unavailable pins are to be queried
	 * @return Returns the pins not available to pio.
	 */
	int getPinsNotAvailableToPIO(int pio);

	/**
	 * Returns the SPI CLK (clock) pin.
	 * 
	 * @return Returns the SPI_CLK_Pin.
	 */
	PIOPin getSPI_CLK_Pin();

	/**
	 * Returns the SPI MISO (master in/slave out) pin.
	 * 
	 * @return Returns the SPI_MISO_Pin.
	 */
	PIOPin getSPI_MISO_Pin();

	/**
	 * Returns the SPI MOSI (master out/slave in) pin.
	 * 
	 * @return Returns the SPI_MOSI_Pin.
	 */
	PIOPin getSPI_MOSI_Pin();

	/**
	 * The clock input pin for a timer-counter
	 * @return Returns the TC_TCLK.
	 */
	PIOPin getTC_TCLK(int tcNum);

	/**
	 * The A pin for a timer-counter
	 * @return Returns the TC_TIOA.
	 */
	PIOPin getTC_TIOA(int tcNum);

	/**
	 * The B pin for a timer-counter
	 * @return Returns the TC_TIOB.
	 */
	PIOPin getTC_TIOB(int tcNum);

	/**
	 * The low-order bit of the hardware rev
	 * @return Returns the BD_REV0
	 */
	PIOPin getBD_REV0();
	
	/**
	 * The mid-order bit of the hardware rev
	 * @return Returns the BD_REV1
	 */
	PIOPin getBD_REV1();
	
	/**
	 * The high-order bit of the hardware rev
	 * @return Returns the BD_REV2
	 */
	PIOPin getBD_REV2();
}
