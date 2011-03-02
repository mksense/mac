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

package com.sun.spot.peripheral.radio;

import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Queue;
import com.sun.spot.util.Utils;
import com.sun.squawk.VM;

/**
 * Implements {@link com.sun.spot.peripheral.radio.I802_15_4_MAC} for the CC2420 radio
 */
final class MACLayer extends MACBase implements I802_15_4_MAC {

    static final int DEFAULT_MAX_CSMA_BACKOFFS = 5; // was 4;
    private static final int MAC_ACK_WAIT_DURATION = 10;

    private int[] RETRY_WAITS;
    private int macMinBE = 0;    // was 3; // use 0 so no initial wait before transmitting
    private int macMaxCSMABackoffs = DEFAULT_MAX_CSMA_BACKOFFS;
    private I802_15_4_PHY physical;
    private Thread rxEnableTimer;

    MACLayer(I802_15_4_PHY r) {
        physical = r;
        RETRY_WAITS = new int[A_MAX_FRAME_RETRIES];
        RETRY_WAITS[0] = 10;  // was 0;
        RETRY_WAITS[1] = 50;
        for (int i = 2; i < RETRY_WAITS.length; i++) {
            RETRY_WAITS[i] = 120; // was 200;
        }
        initialize();
    }

    /* (non-Javadoc)
     * @see com.sun.squawk.peripheral.radio.I802_15_4_MAC#mlmeStart(short, int)
     */
    public void mlmeStart(short panId, int channel) {
        super.mlmeStart(panId, channel);
        IProprietaryRadio proprietary = (IProprietaryRadio) physical;
        //proprietary.reset();
        proprietary.setAddressRecognition(panId, extendedAddress);

        physical.plmeSet(I802_15_4_PHY.PHY_CURRENT_CHANNEL, channel);
        if (rxOnWhenIdle) {
            physical.plmeSetTrxState(I802_15_4_PHY.RX_ON);
        }
    }

    /* (non-Javadoc)
     * @see com.sun.squawk.peripheral.radio.I802_15_4_MAC#mlmeReset(boolean)
     */
    public synchronized void mlmeReset(boolean resetAttribs) {
        physical.plmeSetTrxState(I802_15_4_PHY.TRX_OFF);
        rxEnableTimer = null;
        super.mlmeReset(resetAttribs);
        rxOnWhenIdleChanged();
    }

    /* (non-Javadoc)
     * @see com.sun.squawk.peripheral.radio.I802_15_4_MAC#mlmeRxEnable(int)
     */
    public synchronized void mlmeRxEnable(final int rxOnDuration) {
        if (rxOnDuration > 0xFFFFFF) {
            throw new MAC_InvalidParameterException("mlmeRxEnable: duration of " + rxOnDuration + " too large");
        }
        if (!rxOnWhenIdle) {
            if (rxOnDuration == 0) {
                // disable rx
                rxEnableTimer = null;
                physical.plmeSetTrxState(I802_15_4_PHY.TRX_OFF);
            } else {
                // turn on rx
                if (!isRxQueueOverUpperLimit()) {
                    physical.plmeSetTrxState(I802_15_4_PHY.RX_ON);
                }
                // start timer
                rxEnableTimer = new Thread("MAC rx enable timer") {

                    public void run() {
                        try {
                            Thread.sleep(symbolsAsMillis(rxOnDuration));
                            synchronized (MACLayer.this) {
                                //Utils.log("[mlmeRxEnable] timer awoke...");
                                if (this == rxEnableTimer) {
                                    //Utils.log("[mlmeRxEnable] setting rxEnableTimer to null...");
                                    rxEnableTimer = null;
                                    if (!rxOnWhenIdle) {
                                        //Utils.log("[mlmeRxEnable] turning off rx...");
                                        physical.plmeSetTrxState(I802_15_4_PHY.TRX_OFF);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    private long symbolsAsMillis(int symbols) {
                        return symbols * 4 / 250;
                    }
                };
                rxEnableTimer.start();
            }
        }
    }

    public void setPLMEChannel(int channel) {
        physical.plmeSet(I802_15_4_PHY.PHY_CURRENT_CHANNEL, channel);
    }

    public void setPLMETransmitPower(int power) {
        physical.plmeSet(I802_15_4_PHY.PHY_TRANSMIT_POWER, power & 0x3F);
    }

    public int getPLMETransmitPower() {
        int plmePower = physical.plmeGet(I802_15_4_PHY.PHY_TRANSMIT_POWER);
        return plmePower << 26 >> 26;
    }

    /* (non-Javadoc)
     * @see com.sun.squawk.peripheral.radio.MACBase#checkChannelClear()
     */
    protected boolean   sendIfChannelClear(RadioPacket rp) {
        IProprietaryRadio propRadio = (IProprietaryRadio) physical;
        boolean channelClear = false;
        int numberOfBackoffs = 0;
        int backoffExponent = macMinBE;
        do {
            waitBackoffPeriods(random((1 << backoffExponent) - 1));
            if (propRadio.dataRequest(rp, numberOfBackoffs != 0) == I802_15_4_PHY.SUCCESS) {
                channelClear = true;
                break;
            }
            channelAccessFailure++;
            numberOfBackoffs++;
            backoffExponent = Math.min((backoffExponent == 0 ? 3 : (backoffExponent + 1)), A_MAX_BE);
        } while (numberOfBackoffs <= macMaxCSMABackoffs);
        return channelClear;
    }

    protected void dataIndication(RadioPacket recvPacket) {
        physical.pdDataIndication(recvPacket);
    }

    protected Queue rxDataQueue() {
        return dataQueue;
    }

    protected boolean isPhysicalRxDataWaiting() {
        return ((IProprietaryRadio) physical).isRxDataWaiting();
    }

    protected boolean isPhysicalActive() {
        return ((IProprietaryRadio) physical).isActive();
    }

    /**
     * Wait until the specified number of backoff periods have elapsed
     *
     * @param periods
     */
    private void waitBackoffPeriods(int periods) {
        if (periods > 0) {
            try {
                // a period is 0.32mS
                Thread.sleep(periods / 3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /* (non-Javadoc)
     * @see com.sun.squawk.peripheral.radio.MACBase#getMacAckWaitDuration()
     * @return time to wait for an ack (millis)
     */
    protected int getMacAckWaitDuration() {
        return MAC_ACK_WAIT_DURATION;
    }

    /*
     * If you don't get an ACK, how long to wait before each retry
     */
    protected int getTimeBeforeRetry(int retry) {
        return RETRY_WAITS[retry];
    }

    /* (non-Javadoc)
     * @see com.sun.squawk.peripheral.radio.MACBase#validateDestAddr(com.sun.squawk.peripheral.radio.RadioPacket)
     */
    protected void validateDestAddr(RadioPacket recvPacket) throws MACException {
        long addr = recvPacket.getDestinationAddress();
        if (addr != extendedAddress && addr != 0xFFFF) {
            throw new MACException("Received rp with dest = " + recvPacket.getDestinationAddress());
        }
    }

    /* (non-Javadoc)
     * @see com.sun.squawk.peripheral.radio.MACBase#dump()
     */
    protected void dump() {
        ((IProprietaryRadio) physical).dumpHistory();
    }

    /* (non-Javadoc)
     * @see com.sun.squawk.peripheral.radio.MACBase#setIEEEAddress()
     */
    protected void setIEEEAddress() {
        IEEEAddress address = new IEEEAddress(System.getProperty("IEEE_ADDRESS"));
        extendedAddress = address.asLong();
        Utils.log("My IEEE address is " + address);
    }

    protected void disableRx() {
        physical.plmeSetTrxState(I802_15_4_PHY.TRX_OFF);
    }

    protected void resetRx() {
        ((IProprietaryRadio) physical).resetRX();
    }

    /*
     * return true if the user expects the rx to be on
     */
    protected boolean isRxOnDesired() {
        return rxOnWhenIdle || rxEnableTimer != null;
    }

    /* (non-Javadoc)
     * @see com.sun.squawk.peripheral.radio.MACBase#enableRx()
     */
    protected void enableRx() {
        physical.plmeSetTrxState(I802_15_4_PHY.RX_ON);
    }

    protected void setAsDaemonThread(Thread dispatcherThread) {
        VM.setAsDaemonThread(dispatcherThread);
    }
}
