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
import com.sun.spot.peripheral.IAT91_AIC;
import com.sun.spot.peripheral.IAT91_Peripherals;
import com.sun.spot.peripheral.IDriver;
import com.sun.spot.peripheral.ISleepManager;
import com.sun.spot.peripheral.Spot;
import com.sun.spot.peripheral.SpotFatalException;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Utils;
import com.sun.squawk.Address;
import com.sun.squawk.Unsafe;
import com.sun.squawk.VM;
import com.sun.squawk.vm.ChannelConstants;

/**
 * Sets and queries the state of a CC2420 radio chip.
 * 
 * The non-static methods should normally be accessed via one of the interfaces 
 * {@link com.sun.spot.peripheral.radio.I802_15_4_PHY}
 * and {@link com.sun.spot.peripheral.radio.IProprietaryRadio}
 * 
 */
class CC2420 implements I802_15_4_PHY, IProprietaryRadio, IDriver {
	
	private CC2420Driver driver;
	private int lastStatus;
	private int desiredTransmitPowerInDB;
	private int phyCurrentChannel;
	
	private History history;
	/* State variable
	 * 
	 * This partial implementation of the 802.15.4 MAC trx state only implements these states: TRX_OFF, RX_ON
	 * 
	 * TRX_OFF -> RX_ON
	 * Client can explicitly do this to start receiving. Also, if client issues pdDataRequest we enter RX_ON 
	 * after sending packet.
	 * 
	 * RX_ON -> TRX_OFF
	 * Client can explicitly do this to stop receiving. Waits for current RX or TX to complete. NB. FORCE_TRX_OFF
	 * is not implemented.
	 *  
	 */
	private int trxState;
	private IAT91_AIC aic;
	private boolean overflowDetected;
	private int fifoRemaining;

	// stats
	private int txMissed;
	private int rxOverflow;
	private int shortPacket;
	private int crcError;

	private static final int REG_SNOP     = 0;
	private static final int REG_SXOSCON  = 1;
	private static final int REG_SRXON    = 3;
	private static final int REG_STXON    = 4;
	private static final int REG_STXONCCA = 5;
	private static final int REG_SRFOFF   = 6;
	private static final int REG_SXOSCOFF = 7;
	private static final int REG_SFLUSHRX = 8;
	private static final int REG_SFLUSHTX = 9;
	private static final int REG_MAIN     = 0x10;
	private static final int REG_MDMCTRL0 = 0x11;
	private static final int REG_MDMCTRL0_AUTOACK = 1 << 4;
	private static final int REG_MDMCTRL0_ADR_DECODE = 1 << 11;
	private static final int REG_MDMCTRL0_CCA_MODE_HIGH = 1 << 7;
	private static final int REG_MDMCTRL0_CCA_MODE_LOW = 1 << 6;
	private static final int REG_MDMCTRL1 = 0x12;
	private static final int REG_MDMCTRL1_CORR_THR_20 = 20 << 6;
	        static final int REG_TXCTRL   = 0x15;
	private static final int REG_RXCTRL1  = 0x17;
	private static final int REG_RXCTRL1_RXBPF_LOCUR  = 1 << 13;
	private static final int REG_FSCTRL   = 0x18;
	private static final int REG_FSCTRL_FREQ = 0x3FF;
	private static final int REG_SECCTRL0 = 0x19;
	private static final int REG_IOCFG0   = 0x1C;
	public static final int REG_AGCCTRL  = 0x23;
	private static final int AUT0_VGA_CONTROL = 0;
	private static final int MANUAL_VGA_CONTROL = 1;
	private static final int AGC_GAIN_MODE = 0;
	private static final int LOW_GAIN_MODE = 1;
	private static final int MEDIUM_GAIN_MODE = 2;
	private static final int HIGH_GAIN_MODE = 3;
	private static final int MIN_VGA_GAIN = 0x00;
	private static final int MAX_VGA_GAIN = 0x7F;
    private static final int REG_FSMSTATE = 0x2C;
	private static final int REG_TXFIFO   = 0x3E;
	private static final int REG_RXFIFO   = 0x3F;

	private static final int STATUS_XOSC16M_STABLE	= 1 << 6;
	private static final int STATUS_TX_ACTIVE 		= 1 << 3;
	private static final int STATUS_RSSI_VALID		= 1 << 1;

	private static final int RAM_PANID 		= 0x168;
	private static final int RAM_IEEE_ADR64 	= 0x160;
	private static final int RAM_IEEE_ADR16 	= 0x16A;
	
	/*
	 * FIFOP_THR controls when the CC2420 raises FIFOP. It raises it when FIFOP_THR bytes are unread
	 * in the RX FIFO or when the last byte of a full frame has been received.
	 * 
	 * If you set FIFOP_THR high you increase the likelihood of the RX FIFO overflowing.
	 * If you set FIFOP_THR too low the RX FIFO might underflow.
	 * 
	 * The setting here was determined by an analysis of the rate at which the fifo is emptied compared
	 * to the rate at which it fills. See fifo.xls for details.
	 */
	private static final int FIFOP_THR = 95;
	private static final int RX_FIFO_SIZE = 128;
	
	private static final int FIFOP_INTERRUPT = IAT91_Peripherals.IRQ3_ID_MASK;

	private int offMode;
	private boolean addressRecognitionEnabled;
	private short panId;
	private long extendedAddress;
	private int agcRegValue;
	private int chipPower;
	private boolean recordHistory = false;
	private Object rxTxInterlockMonitor = new Object();
	private Address addressOfLastDeviceInterruptTime;

    private boolean waitingForRxToClear = false;
	
	//For testing only
	CC2420(CC2420Driver driver) {
		this.driver = driver;
	};
	
	/*
	 * Package constructor - please use getInstance() instead.
	 */

	CC2420(CC2420Driver driver, IAT91_AIC aic) {
		this.driver = driver;
		this.aic = aic;

		addressOfLastDeviceInterruptTime = Address.fromPrimitive(VM.execSyncIO(ChannelConstants.GET_LAST_DEVICE_INTERRUPT_TIME_ADDR, 0));
		
		resetErrorCounters();
        addressRecognitionEnabled = false;
        offMode = OFF_MODE_IDLE;
        trxState = TRX_OFF;

       	history = new History();
        resetDefaults();
        setUp();
	}

	public void resetErrorCounters() {
		txMissed = 0;
        crcError = 0;
        shortPacket = 0;
        rxOverflow = 0;
	}

	/*
	 * Answer the number of symbol periods corresponding to a given number of milliseconds
	 * 
	 * @param millisecs
	 * @return symbol periods
	 */
	public static int symbolPeriods(int millisecs) {
		/*
		 * Each symbol corresponds to 4 bits (with 250Kbit/sec radio)
		 */
		return (millisecs * 125) / 2;
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.radio.IProprietaryRadio#reset()
	 */
	public void reset() {
		resetDefaults();
		resetPrim();
	}

	/**
	 * Reset the hardware device receive buffer.
	 */
	public void resetRX() {
        // just like flushRX(), but increment rxError counter
		driver.sendStrobe(REG_SFLUSHRX);
		lastStatus = driver.sendStrobe(REG_SFLUSHRX);
    }


	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.radio.I802_15_4_PHY#pdDataRequest(com.sun.squawk.peripheral.radio.RadioPacket)
	 */
	public int pdDataRequest(RadioPacket rp) {
		dataRequest(rp, false);
		return SUCCESS; // pretend it worked: I802.15.4 doesnt allow us to say it failed for CCA
	}
	
	public int dataRequest(RadioPacket rp, boolean retry) {
		int result = SUCCESS;
		while (isTxActive()) { // wait for any current tx to finish, so we don't flush TX_FIFO too soon
			Thread.yield();
		}
		// clear tx buffer, then load it, then send strobe to perform tx if channel is clear
		if (!retry) {
			driver.sendStrobe(REG_SFLUSHTX);
			sendRP(REG_TXFIFO, rp);
		}
		// Temporarily increase priority to max. This reduces the chance
		// that we will fail to start waiting for tx to start until it has
		// already started and finished. This in turn will reduce our likelihood
		// of sending duplicate packets.
		int currentPriority = Thread.currentThread().getPriority();
		VM.setSystemThreadPriority(Thread.currentThread(), VM.MAX_SYS_PRIORITY);
		waitForRxToClear();
		lastStatus = driver.sendStrobe(REG_STXONCCA);

		// wait for tx to start
		int i = 0, j = 0;
		while (!isTxActive() && i<10) {
			i++;
		}
		VM.setSystemThreadPriority(Thread.currentThread(), currentPriority);

		if (i==10) { // transmission did not start.
			txMissed++; 
			result = BUSY;
		} else {
			while (!driver.isSfdHigh() && j < 10000) {
				// Now wait for SFD to go high. In theory, this shouldn't be necessary, but tests
				// fail if you don't.
				j++;
			}
		}
		rp.timestamp = System.currentTimeMillis();

		// after a transmit, the CC2420 always enables the recevier
		trxState = RX_ON;
		
		return result;
	}

	private boolean isTxActive() {
		return (driver.sendStrobe(REG_SNOP) & STATUS_TX_ACTIVE) != 0;
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.radio.I802_15_4_PHY#pdDataIndication(com.sun.squawk.peripheral.radio.RadioPacket)
	 */
	public void pdDataIndication(RadioPacket rp) {
		boolean good = false;
		// don't return from this method until a good packet has been received
		while (!good) {
			// get the current pin status
			while (!driver.isFifopHigh()) {
				// FIFOP is low, so wait for int
				// don't enable the interrupt if the receiver is off, as we don't want to prohibit deep sleep
				if (trxState == RX_ON) {
					aic.enableIrq(FIFOP_INTERRUPT);
				}
				aic.waitForInterrupt(FIFOP_INTERRUPT);
			}
			synchronized (rxTxInterlockMonitor) {
				good = readPacketFromFIFO(rp);
				rxTxInterlockMonitor.notifyAll();
			}
		}
		rp.timestamp = Unsafe.getLong(addressOfLastDeviceInterruptTime, 0);
	}

	private void waitForRxToClear() {
		synchronized (rxTxInterlockMonitor) {
			while (driver.isSfdHigh() || overflowDetected || driver.isFifopHigh()) {
				try {
                    waitingForRxToClear = true;
                    rxTxInterlockMonitor.wait();
                    waitingForRxToClear = false;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private boolean readPacketFromFIFO(RadioPacket rp) {
		if (recordHistory) {
			history.newEvent(overflowDetected, fifoRemaining, getRegValue(CC2420.REG_FSMSTATE), driver.isFifopHigh(), driver.isFifoHigh());
		}
		
		int spiResult = driver.sendReceive8PlusVariableReceiveN(REG_RXFIFO | 0x40, rp.buffer);
		lastStatus = spiResult & 0xFF;
		
		if (recordHistory) {
			int size = rp.getLength() & 0x7F;
			history.setSPIData(rp, spiResult, size, driver.isFifopHigh(), driver.isFifoHigh());
		}

		return processRP(rp);
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.radio.I802_15_4_PHY#plmeCCARequest()
	 */
	public int plmeCCARequest() {
		int result = IDLE;
		if (trxState == TRX_OFF) {
			result = TRX_OFF;
		} else {
			// see if TX is active
			lastStatus = driver.sendStrobe(REG_SNOP);
			if ((lastStatus & STATUS_TX_ACTIVE) != 0) {
				result = TX_ON;
			} else {
				// make sure CCA is valid before checking it
				while ((lastStatus & STATUS_RSSI_VALID) == 0) {
					lastStatus = driver.sendStrobe(REG_SNOP);
				}
				// now check the CCA pin
				if (!driver.isCcaHigh()) {
					result = BUSY;
				}
			}
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.radio.I802_15_4_PHY#plmeSet(int, int)
	 */
	public void plmeSet(int attribute, int value) {
		if (attribute == PHY_CURRENT_CHANNEL) {
			if (value < 11 || value > 26) {
				throw new PHY_InvalidParameterException("Attribute " + attribute + " cannot be set to " + value);
			}
			phyCurrentChannel = value;
			setChannelNumber(value);
		}
		else if (attribute == PHY_TRANSMIT_POWER) {
			if ((value & ~0xff) != 0) {
				throw new PHY_InvalidParameterException("Attribute " + attribute + " should be an 8 bit value, not " + value);				
			}
			// ignore tolerance in bits 6 and 7
			desiredTransmitPowerInDB = convertSixBitValueToDecibels(value);			
		}
		else throw new PHY_UnsupportedAttributeException(Integer.toString(attribute));

		setChipOutputPower();
	}

	private int convertSixBitValueToDecibels(int value) {
		return Utils.as6BitNumber(value) << 26 >> 26;
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.radio.I802_15_4_PHY#plmeGet(int)
	 */
	public int plmeGet(int attribute) throws PHY_UnsupportedAttributeException {
		if (attribute == PHY_CURRENT_CHANNEL) {
			return phyCurrentChannel;
		} else if (attribute == PHY_TRANSMIT_POWER) {
			return Utils.as6BitNumber(convertFromChipPowerToDB());
		}
		throw new PHY_UnsupportedAttributeException(Integer.toString(attribute));
	}

	/* 
	 * @see com.sun.squawk.peripheral.radio.IProprietaryRadio#setAddressRecognition(short, long)
	 */
	public void setAddressRecognition(short panId, long extendedAddress) {
		addressRecognitionEnabled = true;
		this.panId = panId;
		this.extendedAddress = extendedAddress;
		// write values to ram
		driver.sendRAM(RAM_PANID, panId);
		
		driver.sendRAM(RAM_IEEE_ADR64, extendedAddress);
		//16 bit address addition
		short sh_addr = IEEEAddress.To16Bit(extendedAddress);
		driver.sendRAM(RAM_IEEE_ADR16, sh_addr);

		//turn on address decoding
		int regValue = getRegValue(REG_MDMCTRL0);
		regValue = regValue | REG_MDMCTRL0_ADR_DECODE | REG_MDMCTRL0_AUTOACK;
		// set a register
		driver.setRegister(REG_MDMCTRL0, regValue);
	}

	/* 
	 * @see com.sun.squawk.peripheral.radio.I802_15_4_PHY#plmeSetTrxState(int)
	 */
	public int plmeSetTrxState(int newState) {
		int result = SUCCESS;
		if (newState == trxState) {
			result = newState; // signal that we were already in that state!
		} else if (newState == TX_ON) {
			throw new PHY_InvalidParameterException("Current implementation does not allow explicit TX_ON - just do mlmeDataRequest");
		} else if (newState == RX_ON && trxState == TRX_OFF) {
			startRX();
			trxState = RX_ON;
//			Utils.log("[CC2420] setting trxState = RX_ON");
		} else if (newState == TRX_OFF && trxState == RX_ON) {
			// make sure we aren't transmitting - ok to turn off in middle of receiving
            long cnt = 0;
			while (driver.isSfdHigh() && isTxActive() && cnt < 100000) cnt++;
			trxState = TRX_OFF;
			stopRX();
//			Utils.log("[CC2420] setting trxState = TRX_OFF");
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.radio.IProprietaryRadio#setOffMode(int)
	 */
	public void setOffMode(int offState) {
		this.offMode = offState;
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.radio.IProprietaryRadio#setManualMediumGainMode(int)
	 */
	public void setManualMediumGainMode(int gain) {
	    if (gain<MIN_VGA_GAIN || gain>MAX_VGA_GAIN) throw new IllegalArgumentException("Illegal VGA gain");
	    setAGCRegister(MANUAL_VGA_CONTROL,gain,MEDIUM_GAIN_MODE);
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.radio.IProprietaryRadio#setManualLowGainMode(int)
	 */
	public void setManualLowGainMode(int gain) {
	    if (gain<MIN_VGA_GAIN || gain>MAX_VGA_GAIN) throw new IllegalArgumentException("Illegal VGA gain");
	    setAGCRegister(MANUAL_VGA_CONTROL,gain,LOW_GAIN_MODE);
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.radio.IProprietaryRadio#setManualHighGainMode(int)
	 */
	public void setManualHighGainMode(int gain) {
	    if (gain<MIN_VGA_GAIN || gain>MAX_VGA_GAIN) throw new IllegalArgumentException("Illegal VGA gain");
	    setAGCRegister(MANUAL_VGA_CONTROL,gain,HIGH_GAIN_MODE);
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.radio.IProprietaryRadio#setAutoMediumGainMode()
	 */
	public void setAutoMediumGainMode() {
	    setAGCRegister(AUT0_VGA_CONTROL,MAX_VGA_GAIN,MEDIUM_GAIN_MODE);
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.radio.IProprietaryRadio#setAutoLowGainMode()
	 */
	public void setAutoLowGainMode() {
	    setAGCRegister(AUT0_VGA_CONTROL,MAX_VGA_GAIN,LOW_GAIN_MODE);
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.radio.IProprietaryRadio#setAutoHighGainMode()
	 */
	public void setAutoHighGainMode() {
	    setAGCRegister(AUT0_VGA_CONTROL,MAX_VGA_GAIN,HIGH_GAIN_MODE);
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.radio.IProprietaryRadio#setAutoAGCGainMode()
	 */
	public void setAutoAGCGainMode() {
	    setAGCRegister(AUT0_VGA_CONTROL,MAX_VGA_GAIN,AGC_GAIN_MODE);		
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.radio.IProprietaryRadio#getTransceiverState()
	 */
	public int getTransceiverState() {
		return trxState;
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.radio.IProprietaryRadio#dumpHistory()
	 */
	public void dumpHistory() {
		if(recordHistory){
			Utils.log("FIFO: " + Utils.stringify(getDriver().readRxFifo()));
			history.dump();
		}
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.spot.IDriver#name()
	 */
	public String getDriverName() {
		return "CC2420";
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.spot.IDriver#tearDown()
	 */
	public boolean tearDown() {
		if (!waitingForRxToClear && trxState == TRX_OFF) {
			driver.tearDown();
			return true;
		}
		return false;
	}

	public void shutDown() {
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.spot.IDriver#setUp()
	 */
	public void setUp() {
		driver.setUp();
		aic.configure(FIFOP_INTERRUPT, IAT91_AIC.AIC_IRQ_PRI_NORMAL, IAT91_AIC.SRCTYPE_HIGH_LEVEL);
        resetPrim();
        if (addressRecognitionEnabled) {
        	setAddressRecognition(panId, extendedAddress);
        }
	}

	/**
	 * Answer the internal trx state. <b>To support testing</b>.
	 * 
	 * @return - the state
	 */
	int getTrxState() {
		return trxState;
	}

	/**
	 * Answer the count of the number of short packets recorded.
	 * <b>To support testing</b>.
	 * 
	 * @return -- the count.
	 */
	public int getShortPacket() {
		return shortPacket;
	}

	/**
	 * Answer the count of the number of overflows recorded while receiving packets.
	 * <b>To support testing</b>.
	 * 
	 * @return -- the count.
	 */
	public int getRxOverflow() {
		return rxOverflow;
	}

	/**
	 * Answer the count of the number of CRC errors recorded. <b>To support testing</b>.
	 * 
	 * @return -- the count.
	 */
	public int getCrcError() {
		return crcError;
	}

	/**
	 * Answer the count of the number of times we started a transmit and apparently
	 * didn't manage to start waiting for it to complete until after the interrupt was
	 * already signalled.  <b>To support testing</b>.
	 * 
	 * @return -- the count
	 */
	public int getTxMissed() {
		return txMissed;
	}

	/**
	 * Returns status the chip reported on the last SPI operation. <b>To support testing</b>
	 * 
	 * @return -- the status
	 */
	int getLastStatus() {
		return lastStatus;
	}

	/**
	 * Answer the value of a radio chip register.
	 * 
	 * @param -- regNo
	 * @return -- the register value
	 */
	int getRegValue(int regNo) {
		int result = driver.getRegister(regNo | 0x40);
		
		lastStatus = result & 0xFF;
		
		return result >> 8;
	}

	private void resetDefaults() {
		phyCurrentChannel = DEFAULT_CHANNEL;
		desiredTransmitPowerInDB = DEFAULT_TRANSMIT_POWER;
		agcRegValue = calcAGCRegValue(AUT0_VGA_CONTROL,MAX_VGA_GAIN,AGC_GAIN_MODE);
	}

	private void resetPrim() {
		if (trxState == RX_ON) {
			stopRX();
		}
		trxState = TRX_OFF;
		startChip();
		driver.setRegister(REG_SECCTRL0, 0x01C4); // clear RXFIFO protection bit
		driver.setRegister(REG_MDMCTRL0, getRegValue(REG_MDMCTRL0) & ~REG_MDMCTRL0_ADR_DECODE); // clear address decode bit
		//driver.setRegister(REG_MDMCTRL0, getRegValue(REG_MDMCTRL0) | REG_MDMCTRL0_CCA_MODE_HIGH);
	        //driver.setRegister(REG_MDMCTRL0, getRegValue(REG_MDMCTRL0) & ~REG_MDMCTRL0_CCA_MODE_LOW);
		driver.setRegister(REG_MDMCTRL1, getRegValue(REG_MDMCTRL1) | REG_MDMCTRL1_CORR_THR_20);
		driver.setRegister(REG_IOCFG0, FIFOP_THR); // set RX FIFO threshold
		driver.setRegister(REG_RXCTRL1, getRegValue(REG_RXCTRL1) | REG_RXCTRL1_RXBPF_LOCUR); // set to recommended value
		overflowDetected = false;
		fifoRemaining = 0;
		powerUp();
		setAGCRegisterPrim(agcRegValue);
		setChannelNumber(phyCurrentChannel);
		setChipOutputPower();
		
//		Utils.log("[CC2420 post reset] setting trxState = TRX_OFF");
	}

	private void setChipOutputPower() {
		int powerInDb = desiredTransmitPowerInDB;
		if (phyCurrentChannel == 26) {
			powerInDb = Math.min(-3, desiredTransmitPowerInDB);
		}

		switch (powerInDb) {
			case -32: chipPower = 0; break;
			case -31: chipPower = 1; break;
			case -30: chipPower = 2; break;
			case -29:
			case -28:
			case -27:
			case -26:
			case -25: chipPower = 3; break;
			case -24:
			case -23:
			case -22: chipPower = 4; break;
			case -21:
			case -20:
			case -19: chipPower = 5; break;
			case -18:
			case -17: chipPower = 6; break;
			case -16:
			case -15: chipPower = 7; break;
			case -14:
			case -13: chipPower = 8; break;
			case -12: chipPower = 9; break;
			case -11: chipPower = 10; break;
			case -10: chipPower = 11; break;
			case -9: chipPower = 12; break;
			case -8: chipPower = 14; break;
			case -7: chipPower = 15; break;
			case -6: chipPower = 17; break;
			case -5: chipPower = 19; break;
			case -4: chipPower = 21; break;
			case -3: chipPower = 23; break;
			case -2: chipPower = 25; break;
			case -1: chipPower = 27; break;
			case 0:
			default: chipPower = 31; break;
		}
		// set a register
		driver.setRegister(REG_TXCTRL, 0xA0E0 | chipPower);
	}

	private int convertFromChipPowerToDB() {
		switch (chipPower) {
			case 0: return -32;
			case 1: return -31;
			case 2: return -30;
			case 3: return -25;
			case 4: return -22;
			case 5: return -19;
			case 6: return -17;
			case 7: return -15;
			case 8: return -13;
			case 9: return -12;
			case 10: return -11;
			case 11: return -10;
			case 12: return -9;
			case 14: return -8;
			case 15: return -7;
			case 17: return -6;
			case 19: return -5;
			case 21: return -4;
			case 23: return -3;
			case 25: return -2;
			case 27: return -1;
			case 31: return 0;
		}
		throw new SpotFatalException("Should never have chipPower set as any other value (was "+chipPower+")");
	}

	public int getChipOutputPower() {
		return getRegValue(REG_TXCTRL) & 0x1F;
	}
	
	/**
	 * @param rp - empty RadioPacket; this routine must fill in buffer and length
	 * @return - true if CRC and packaet length ok
	 */
	boolean processRP(RadioPacket rp) {
		if (!overflowDetected && (rp.getLength() > 127)) {
			// The top bit of the length byte is set. This means that the fifo_pin was low at the start
			// of reading this packet from the fifo, which, in turn, means that an overflow has occurred.
			// The current packet is fine, and there may be other valid packets in the fifo.
			// If overflowDetected was already true, then we detected overflow in a previous read, so 
			// we already have a correct (lower) value for fifoRemaining.
			overflowDetected = true;
			fifoRemaining = RX_FIFO_SIZE;
		}
		
		int size = rp.getLength() & 0x7F;
		if (overflowDetected) {
			// we are in an overflow state
			//Utils.log("reading pkt in oflow state, pkt size was " + size);
			fifoRemaining = fifoRemaining - (size + 1); // add 1 to account for length byte
			if (fifoRemaining < 3 || !driver.isFifopHigh()) {
				// can't be any more valid packets
				// Note that fifop doesn't seem to go low when you read the last valid packet after an overflow,
				// even though, logically, it should. However, there do seem to be cases where fifop goes low
				// before fifoRemaining < 3.
				if (recordHistory) {
					history.setFlushed();
				}
				// flush the fifo
				flushRX();
			}
			if (fifoRemaining < 0) {
				// the packet just read didn't fit - it caused the overflow
				if (recordHistory) {
					history.setDiscarded();
				}
				return false;
			}
		}

		boolean result = false;
		if (size < 3) {
			shortPacket++;
//			if (overflowDetected) {
//				Utils.log("[overflow] short packet of size " + size + ", fifoRemaining=" + fifoRemaining);
//			} else {
//				Utils.log("short packet of size " + size);
//			}
			
		} else {
			size = size - 2; // to allow for the FCS
			rp.setLength(size);
//			if (overflowDetected) {
//				Utils.log("[overflow] packet of size " + size + ", fifoRemaining=" + fifoRemaining);
//				Utils.log(rp);
//			}
			rp.rssi = rp.buffer[size+1];
			int fcs = rp.buffer[size+2];
			rp.corr = fcs & 0x7F;
			result = (fcs & 0x80) != 0; // test CRC valid flag
			if (!result) crcError++;
		}
		if (recordHistory) {
			history.setRejected(result);
		}
		return result;
	}

	private void flushRX() {
		driver.sendStrobe(REG_SFLUSHRX);
		lastStatus = driver.sendStrobe(REG_SFLUSHRX);
		rxOverflow++;
		overflowDetected = false;
	}

	private void startRX() {
//		Utils.log("[CC2420] starting rx");
		if (!driver.isVRegEnHigh()) {
			startChip();
			powerUp();
		}
		if (isPowerDown()) {
			powerUp();
		}
		// start the RX
		aic.enableIrq(FIFOP_INTERRUPT);
		driver.sendStrobe(REG_SRXON);
		driver.sendStrobe(REG_SFLUSHRX);
		lastStatus = driver.sendStrobe(REG_SFLUSHRX);
	}
	
	private void stopRX() {
//		Utils.log("[CC2420] stopping rx");
		lastStatus = driver.sendStrobe(REG_SRFOFF);
		aic.disableIrq(FIFOP_INTERRUPT);
		
		if (offMode == OFF_MODE_POWER_DOWN) {
			powerDown();
		} else if (offMode == OFF_MODE_VREG_OFF) {
			stopChip();
		} // OFF_MODE_IDLE already handled 
	}

	private void sendRP(int regNo, RadioPacket rp) {
		int dataLength = rp.getLength();
		rp.setLength(dataLength+2); // allow for crc
		driver.sendReceive8PlusSendN(regNo, dataLength+1, rp.buffer);
		rp.setLength(dataLength); // reset length
	}

	/**
	 * @param i - channel number in range 11-26
	 */
	void setChannelNumber(int i) {
		int fsctrl = getRegValue(REG_FSCTRL);
		fsctrl = (fsctrl & ~REG_FSCTRL_FREQ) | (357 + (5 * (i-11)));
		// set a register
		driver.setRegister(REG_FSCTRL, fsctrl);
		// Changing the channel number stops the receiver, so
		// we may need to restart it.
		if (trxState == RX_ON) {
			startRX();
		}
	}
	
	private void setAGCRegister(int vga_gain_mode, int vga_gain, int agc_mode) {
        setAGCRegisterPrim(calcAGCRegValue(vga_gain_mode, vga_gain, agc_mode));
    }

	private int calcAGCRegValue(int vga_gain_mode, int vga_gain, int agc_mode) {
        return (agc_mode << 2) | (vga_gain << 4) | (vga_gain_mode << 11);
    }

    private void setAGCRegisterPrim(int val) {
    	agcRegValue = val;
		driver.setRegister(REG_AGCCTRL, agcRegValue);
	}

	private boolean isPowerDown() {
		lastStatus = driver.sendStrobe(REG_SNOP);
		return (lastStatus & STATUS_XOSC16M_STABLE) == 0;
	}

	private void powerDown() {
		lastStatus = driver.sendStrobe(REG_SXOSCOFF);
	}

	private void powerUp() {
		// power up
		lastStatus = driver.sendStrobe(REG_SXOSCON);
		// wait for stable oscillator
		while ((lastStatus & STATUS_XOSC16M_STABLE) == 0) {
			lastStatus = driver.sendStrobe(REG_SNOP);
		}
	}

	private void stopChip() {
		driver.setVRegEn(false);
	}

	private void startChip() {
		// turn on the voltage regulator
		driver.setVRegEn(true);
		Utils.sleep(10);
		driver.setReset(false);
		Utils.sleep(10);
		driver.setReset(true);
		Utils.sleep(10);
	}

	CC2420Driver getDriver() {
		return driver;
	}
	
	public void setRecordHistory(boolean shouldRecordHistory) {
		this.recordHistory = shouldRecordHistory;
	}

	public boolean isRxDataWaiting() {
		return driver.isFifopHigh();
	}

	public boolean isActive() {
		return driver.isSfdHigh();
	}
}
