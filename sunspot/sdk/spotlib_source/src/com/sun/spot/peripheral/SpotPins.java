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
 * A helper class that abstracts the different PIO pin usage of different Spot revisions.<br><br>
 * 
 */
class SpotPins implements ISpotPins {
	
	private PIOPinDefinition localGreenLEDPin;
	private PIOPinDefinition localRedLEDPin;
	private PIOPinDefinition CC2420_VREG_EN_Pin;
	private PIOPinDefinition CC2420_FIFOP_Pin;
	private PIOPinDefinition CC2420_FIFO_Pin;
	private PIOPinDefinition CC2420_SFD_Pin;
	private PIOPinDefinition CC2420_CCA_Pin;
	private PIOPinDefinition CC2420_RESET_Pin;
	private PIOPinDefinition tc_TIOA[];
	private PIOPinDefinition tc_TIOB[];
	private PIOPinDefinition tc_TCLK[];
	private PIOPinDefinition SPI_CLK_Pin;
	private PIOPinDefinition SPI_MISO_Pin;
	private PIOPinDefinition SPI_MOSI_Pin;
	private PIOPinDefinition USB_EN;
	private PIOPinDefinition USB_HP;
	private PIOPinDefinition USB_PWR_MON;
	private PIOPinDefinition ATTENTION_Pin;
	
	static final PIOPinDefinition BD_REV0 = new PIOPinDefinition(IAT91_PIO.PIOB, 1<<22, PIOPin.IO);
	static final PIOPinDefinition BD_REV1 = new PIOPinDefinition(IAT91_PIO.PIOB, 1<<19, PIOPin.IO);
	static final PIOPinDefinition BD_REV2 = new PIOPinDefinition(IAT91_PIO.PIOB, 1<<18, PIOPin.IO);

    private int[] pins_not_available_to_pio;
	private ISpot spot;

	SpotPins(int hardwareType, ISpot spot) {
		pins_not_available_to_pio = new int[] {
			(hardwareType>5?0:(1<<21)) | (1<<19) | 0x7F | (1<<16) | (1<<17) | (1<<18), 	/* usb pull up pin, usb high power pin, all the SPI pins, pins connected to USART0 */
			(hardwareType>5?(1<<4):0) | 0xC0300000 | (7 << 15), /* serial pins, top bits missing, P15-17 are board device select */
			0x00003f80, /* address lines */
			0xF0000000  /* missing bits */
		};
		this.spot = spot;
		initializePioPins(hardwareType);
	}

	public PIOPin getUSB_EN() {
		return createPIOPin(USB_EN);
	}

	public PIOPin getUSB_HP() {
		return createPIOPin(USB_HP);
	}

	public PIOPin getAttentionPin() {
		return createPIOPin(ATTENTION_Pin);
	}

	public PIOPin getUSB_PWR_MON() {
		return createPIOPin(USB_PWR_MON);
	}

	public PIOPin getCC2420_CCA_Pin() {
		return createPIOPin(CC2420_CCA_Pin);
	}

	public PIOPin getCC2420_FIFO_Pin() {
		return createPIOPin(CC2420_FIFO_Pin);
	}

	public PIOPin getCC2420_FIFOP_Pin() {
		return createPIOPin(CC2420_FIFOP_Pin);
	}

	public PIOPin getCC2420_RESET_Pin() {
		return createPIOPin(CC2420_RESET_Pin);
	}

	public PIOPin getCC2420_SFD_Pin() {
		return createPIOPin(CC2420_SFD_Pin);
	}

	public PIOPin getCC2420_VREG_EN_Pin() {
		return createPIOPin(CC2420_VREG_EN_Pin);
	}

	public PIOPin getLocalGreenLEDPin() {
		return createPIOPin(localGreenLEDPin);
	}

	public PIOPin getLocalRedLEDPin() {
		return createPIOPin(localRedLEDPin);
	}

	public int getPinsNotAvailableToPIO(int pio) {
		return pins_not_available_to_pio[pio];
	}

	public PIOPin getSPI_CLK_Pin() {
		return createPIOPin(SPI_CLK_Pin);
	}

	public PIOPin getSPI_MISO_Pin() {
		return createPIOPin(SPI_MISO_Pin);
	}

	public PIOPin getSPI_MOSI_Pin() {
		return createPIOPin(SPI_MOSI_Pin);
	}

	public PIOPin getTC_TCLK(int tcNum) {
		return createPIOPin(tc_TCLK[tcNum]);
	}

	public PIOPin getTC_TIOA(int tcNum) {
		return createPIOPin(tc_TIOA[tcNum]);
	}

	public PIOPin getTC_TIOB(int tcNum) {
		return createPIOPin(tc_TIOB[tcNum]);
	}

	public PIOPin getBD_REV0() {
		return createPIOPin(BD_REV0);
	}

	public PIOPin getBD_REV1() {
		return createPIOPin(BD_REV1);
	}

	public PIOPin getBD_REV2() {
		return createPIOPin(BD_REV2);
	}

	
	private void initializePioPins(int hardwareType) {
		ATTENTION_Pin		 = new PIOPinDefinition(IAT91_PIO.PIOB, 1<<28, PIOPin.PERIPHERAL_A);
		localGreenLEDPin     = new PIOPinDefinition(IAT91_PIO.PIOB, 1<<23, PIOPin.IO);
		localRedLEDPin       = new PIOPinDefinition(IAT91_PIO.PIOB, 1<<24, PIOPin.IO);
		SPI_CLK_Pin		     = new PIOPinDefinition(IAT91_PIO.PIOA, 1<<2, PIOPin.IO);
		SPI_MISO_Pin		 = new PIOPinDefinition(IAT91_PIO.PIOA, 1<<0, PIOPin.IO);
		SPI_MOSI_Pin		 = new PIOPinDefinition(IAT91_PIO.PIOA, 1<<1, PIOPin.IO);
		CC2420_RESET_Pin     = new PIOPinDefinition(IAT91_PIO.PIOB, 1<<0, PIOPin.IO);
		CC2420_FIFOP_Pin     = new PIOPinDefinition(IAT91_PIO.PIOA, 1<<23, PIOPin.PERIPHERAL_B);
		CC2420_FIFO_Pin      = new PIOPinDefinition(IAT91_PIO.PIOB, 1<<2, PIOPin.IO);
		if (hardwareType > 5) {
			CC2420_SFD_Pin       = new PIOPinDefinition(IAT91_PIO.PIOA, 1<<21, PIOPin.IO);			
		} else {
			CC2420_SFD_Pin       = new PIOPinDefinition(IAT91_PIO.PIOB, 1<<4, PIOPin.IO);
		}
		CC2420_CCA_Pin       = new PIOPinDefinition(IAT91_PIO.PIOB, 1<<3, PIOPin.IO);
		CC2420_VREG_EN_Pin   = new PIOPinDefinition(IAT91_PIO.PIOB, 1<<1, PIOPin.IO);
		if (hardwareType < 7) {
			USB_HP			     = new PIOPinDefinition(IAT91_PIO.PIOA, 1<<19, PIOPin.IO);
			USB_EN			     = new PIOPinDefinition(IAT91_PIO.PIOA, 1<<20, PIOPin.IO);
		}
		USB_PWR_MON		     = new PIOPinDefinition(IAT91_PIO.PIOA, 1<<22, PIOPin.IO);
		tc_TIOA = new PIOPinDefinition[6];
		tc_TIOA[0]           = new PIOPinDefinition(IAT91_PIO.PIOA, 1<<17, PIOPin.PERIPHERAL_B);
		tc_TIOA[1]           = new PIOPinDefinition(IAT91_PIO.PIOA, 1<<19, PIOPin.PERIPHERAL_B);
		tc_TIOA[2]           = new PIOPinDefinition(IAT91_PIO.PIOA, 1<<21, PIOPin.PERIPHERAL_B);
		tc_TIOA[3]           = new PIOPinDefinition(IAT91_PIO.PIOB, 1<<6 , PIOPin.PERIPHERAL_B);
		tc_TIOA[4]           = new PIOPinDefinition(IAT91_PIO.PIOB, 1<<8 , PIOPin.PERIPHERAL_B);
		tc_TIOA[5]           = new PIOPinDefinition(IAT91_PIO.PIOB, 1<<10, PIOPin.PERIPHERAL_B);
		tc_TIOB = new PIOPinDefinition[6];
		tc_TIOB[0]           = new PIOPinDefinition(IAT91_PIO.PIOA, 1<<18, PIOPin.PERIPHERAL_B);
		tc_TIOB[1]           = new PIOPinDefinition(IAT91_PIO.PIOA, 1<<20, PIOPin.PERIPHERAL_B);
		tc_TIOB[2]           = new PIOPinDefinition(IAT91_PIO.PIOA, 1<<22, PIOPin.PERIPHERAL_B);
		tc_TIOB[3]           = new PIOPinDefinition(IAT91_PIO.PIOB, 1<<7 , PIOPin.PERIPHERAL_B);
		tc_TIOB[4]           = new PIOPinDefinition(IAT91_PIO.PIOB, 1<<9 , PIOPin.PERIPHERAL_B);
		tc_TIOB[5]           = new PIOPinDefinition(IAT91_PIO.PIOB, 1<<11, PIOPin.PERIPHERAL_B);
		tc_TCLK = new PIOPinDefinition[6];
		tc_TCLK[0]           = new PIOPinDefinition(IAT91_PIO.PIOA, 1<<13, PIOPin.PERIPHERAL_B);
		tc_TCLK[1]           = new PIOPinDefinition(IAT91_PIO.PIOA, 1<<14, PIOPin.PERIPHERAL_B);
		tc_TCLK[2]           = new PIOPinDefinition(IAT91_PIO.PIOA, 1<<15, PIOPin.PERIPHERAL_B);
		tc_TCLK[3]           = new PIOPinDefinition(IAT91_PIO.PIOA, 1<<27, PIOPin.PERIPHERAL_B);
		tc_TCLK[4]           = new PIOPinDefinition(IAT91_PIO.PIOA, 1<<28, PIOPin.PERIPHERAL_B);
		tc_TCLK[5]           = new PIOPinDefinition(IAT91_PIO.PIOA, 1<<29, PIOPin.PERIPHERAL_B);
	}

	private synchronized PIOPin createPIOPin(PIOPinDefinition pindef) {
		if (pindef == null) {
			throw new IllegalStateException("Attempt to access non-existent pin");
		}
		if (pindef.realPIOPin == null) {
			pindef.realPIOPin = new PIOPin(spot.getAT91_PIO(pindef.pio), pindef.pin, pindef.multiplex);
		}
		return pindef.realPIOPin;
	}

	static class PIOPinDefinition {
		int pin;
		int pio;
		int multiplex;
		PIOPin realPIOPin;

		public PIOPinDefinition(int pio, int pin, int multiplex) {
			this.pio = pio;
			this.pin = pin;
			this.multiplex = multiplex;
		}
	}
}
