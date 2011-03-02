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

import com.sun.spot.peripheral.ILed;
import java.util.Random;

import com.sun.spot.peripheral.ISpot;
import com.sun.spot.peripheral.Spot;
import com.sun.spot.peripheral.SpotFatalException;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Queue;
import com.sun.spot.util.Utils;
import com.sun.squawk.VM;

abstract class MACBase implements I802_15_4_MAC, IProprietaryMAC {

    /* In a non-beaconed network, any value for this in the range 34 to 62 symbol periods should be ok.
     * The ACK should arrive after exactly 34 symbols and no other legal ack (for a different SPOT)
     * can arrive before 62 symbols. 
     */
    private static final int TIME_TO_WAIT_FOR_ACK_MICROSECS = 864; // = 54 symbol periods
    public static final int DEFAULT_MAX_RECEIVE_QUEUE_LENGTH = 1500;
    private static final int DEFAULT_RECEIVE_QUEUE_LENGTH_TO_DROP_BROADCAST_PACKETS = 1000;
    private Thread receiveThread;
    private byte macDSN;
    private Object ackMonitor = new Object();
    private Object sendMonitor = new Object();
    private boolean awaitingAck;
    private byte ackDSN;
    private RadioPacket lastAck;
    protected long extendedAddress;
    protected boolean rxOnWhenIdle;
    protected Queue dataQueue;
    private Random random;
    protected int channelAccessFailure = 0;
    private int rxError = 0;
    private int wrongAck = 0;
    private int noAck = 0;
    private int nullPacketAfterAckWait = 0;
    private int maxReceiveQueueLength = DEFAULT_MAX_RECEIVE_QUEUE_LENGTH;
    private int receiveQueueLengthToDropBroadcastPackets = DEFAULT_RECEIVE_QUEUE_LENGTH_TO_DROP_BROADCAST_PACKETS;
    static final int A_MAX_FRAME_RETRIES = 4;  // was 3
    static final int A_MAX_BE = 5;

    private ILed receiveLed = Spot.getInstance().getGreenLed();
    private ILed sendLed = Spot.getInstance().getRedLed();
    private boolean showUse = false;

    private boolean filterPackets = false;
    private boolean filterWhitelist = true;
    private long filterList[];
    
    /*
     * (non-Javadoc)
     * @see com.sun.squawk.peripheral.radio.I802_15_4_MAC#mcpsDataRequest(com.sun.squawk.peripheral.radio.RadioPacket)
     * 
     * This routine sends a packet
     */
    public final int mcpsDataRequest(RadioPacket rp) {
        // TODO Check RadioPacket params (or should the RadioPacket do its own checking?)
        synchronized (sendMonitor) {

            byte myDSN = getDSN();
            rp.setDSN(myDSN);

            int result = I802_15_4_MAC.NO_ACK;

            // Enable RX. Note that we do this *even* if we aren't expecting to receive an ack,
            // as otherwise sendIfChannelClear() will be unable to detect whether the channel is clear
            enableRx();
            for (int i = 0; i <= A_MAX_FRAME_RETRIES; i++) {

                int currentPriority = Thread.currentThread().getPriority();
                VM.setSystemThreadPriority(Thread.currentThread(), VM.MAX_SYS_PRIORITY);
                try {
                    if (!sendIfChannelClear(rp)) {
                        result = I802_15_4_MAC.CHANNEL_ACCESS_FAILURE;
                        VM.setSystemThreadPriority(Thread.currentThread(), currentPriority);
                        break;
                    } else {
                        if (rp.ackRequest()) {
                            if (pollForAckPacket(myDSN)) {
                                result = I802_15_4_MAC.SUCCESS;
                                VM.setSystemThreadPriority(Thread.currentThread(), currentPriority);
                                break;
                            } else {
                                noAck++;
//							Utils.log("Timed out waiting for ack of my packet with DSN " + myDSN + " for retry (i)=" + i);
                            }
                            VM.setSystemThreadPriority(Thread.currentThread(), currentPriority);

                            // didn't break out, so didn't find ack: don't bother to sleep if we aren't going around again
                            if (i < A_MAX_FRAME_RETRIES) {
                                int timeBeforeRetry = getTimeBeforeRetry(i);
                                if (timeBeforeRetry != 0) {
                                    int initialDelay = 2 * timeBeforeRetry / 3;
                                    Utils.sleep(initialDelay + random(timeBeforeRetry - initialDelay));
                                }
                            }
                        } else {
                            VM.setSystemThreadPriority(Thread.currentThread(), currentPriority);
                            result = I802_15_4_MAC.SUCCESS;
                            break;
                        }
                    }
                } catch (RuntimeException e) {
                    VM.setSystemThreadPriority(Thread.currentThread(), currentPriority);
                    throw e;
                }
            }
            conditionallyDisableRx();
            if (showUse) {
                sendLed.setOn(!sendLed.isOn());
            }
            return result;
        }
    }

    /**
     * return true if the rx queue is full
     */
    protected boolean isRxQueueOverUpperLimit() {
        return rxDataQueue().size() >= maxReceiveQueueLength;
    }

    /**
     * return true if the rx queue has space
     */
    protected boolean isRxQueueUnderLowerLimit() {
        return rxDataQueue().size() < receiveQueueLengthToDropBroadcastPackets;
    }

    private boolean pollForAckPacket(byte myDSN) {
        ISpot spot = Spot.getInstance();
        while (isPhysicalActive()); // wait for tx to finish
        int startTicks = spot.getSystemTicks();
        int now = startTicks;
        int targetTicks = startTicks + ((ISpot.SYSTEM_TICKER_TICKS_PER_MILLISECOND * TIME_TO_WAIT_FOR_ACK_MICROSECS) / 1000);
        do {
            if (isPhysicalRxDataWaiting()) {
                RadioPacket ackPacket = waitForAckPacket(myDSN);
                if (ackPacket == null) {
                    // whatever it was that came, it wasn't our ACK
                    // waitForAckPacket waits for longer than the ACK wait period, so
                    // we can just give up
                    nullPacketAfterAckWait++;
                    return false;
                }
                if (ackPacket.getDataSequenceNumber() == myDSN) {
                    return true;
                } else {
                    wrongAck++;
                // don't just return false here in case this is an ack that has been
                // waiting around for a while, and the one we want is just coming
                }
            }
            now = spot.getSystemTicks();
            if (now < startTicks) {
                now += ISpot.SYSTEM_TICKER_TICKS_PER_MILLISECOND;
            }
        } while (now < targetTicks || isPhysicalRxDataWaiting());
        return false;
    }

    protected boolean isPhysicalActive() {
        return false;
    }

    protected boolean isPhysicalRxDataWaiting() {
        return false;
    }

    /*
     * Return how long to sleep before retrying a send. retry=0 implies the first retry 
     */
    protected int getTimeBeforeRetry(int retry) {
        // no delay unless overridden
        return 0;
    }

    protected void enableRx() {
        // Noop unless overridden		
    }

    public void mcpsDataIndication(RadioPacket rp) {
        RadioPacket internalRP = (RadioPacket) dataQueue.get();
        if (isRxOnDesired() && isRxQueueUnderLowerLimit()) {
            enableRx();
        }
//		Utils.log("got dsn =" + internalRP.getDataSequenceNumber() + " " + System.currentTimeMillis());
        rp.copyFrom(internalRP);
        if (showUse) {
            receiveLed.setOn(!receiveLed.isOn());
        }
    }

    public void mlmeStart(short panId, int channel) throws MAC_InvalidParameterException {
    }

    public synchronized void mlmeReset(boolean resetAttribs) {
        // empty queues
        while (!dataQueue.isEmpty()) {
            dataQueue.get();
        }
        if (resetAttribs) {
            resetAttributes();
        }
    }

    public void setMaxReceiveQueueLength(int maxPackets) {
        maxReceiveQueueLength = maxPackets;
    }

    public void setReceiveQueueLengthToDropBroadcastPackets(int maxPackets) {
        receiveQueueLengthToDropBroadcastPackets = maxPackets;
    }

    public int getMaxReceiveQueueLength() {
        return maxReceiveQueueLength;
    }

    public int getReceiveQueueLengthToDropBroadcastPackets() {
        return receiveQueueLengthToDropBroadcastPackets;
    }

    public long mlmeGet(int attribute) throws MAC_InvalidParameterException {
        long result;
        switch (attribute) {
            case A_EXTENDED_ADDRESS:
                result = extendedAddress;
                break;
            case MAC_RX_ON_WHEN_IDLE:
                result = rxOnWhenIdle ? TRUE : FALSE;
                break;
            default:
                throw new MAC_InvalidParameterException();
        }
        return result;
    }

    public void mlmeSet(int attribute, long value) throws MAC_InvalidParameterException {
        switch (attribute) {
            case MAC_RX_ON_WHEN_IDLE:
                rxOnWhenIdle = value == TRUE;
                rxOnWhenIdleChanged();
                break;
            default:
                throw new MAC_InvalidParameterException();
        }
    }

    /*
     * return true if the user expects the rx to be on
     */
    protected boolean isRxOnDesired() {
        return rxOnWhenIdle;
    }

    /**
     * Set the IEEE Address. Need to call mlmeStart(short, int) to have it take effect.
     *
     * @param ieeeAddr new radio address set after next call to mlmeStart()
     */
    public void setIEEEAddress(long ieeeAddr) {
        extendedAddress = ieeeAddr;
        Utils.log("My IEEE address modified to be " + IEEEAddress.toDottedHex(extendedAddress));
    }

    protected abstract void dataIndication(RadioPacket recvPacket);

    /**
     * @return time (in millis) to wait for an ack
     */
    protected abstract int getMacAckWaitDuration();

    protected abstract Queue rxDataQueue();

    protected boolean sendIfChannelClear(RadioPacket rp) {
        return true;
    }

    protected abstract void setIEEEAddress();

    protected void validateDestAddr(RadioPacket recvPacket) throws MACException {
    }

    protected void dump() {
    }

    protected void startReceiveThread() {
        receiveThread = new ReceiveThread();
        receiveThread.setPriority(Thread.MAX_PRIORITY - 1);
        setAsDaemonThread(receiveThread);
        receiveThread.start();
        Thread.yield();
    }

    /**
     * Return a random in the range 0 to i
     * 
     * @param i
     * @return
     */
    protected int random(int i) {
        return random.nextInt(i + 1);
    }

    protected void initialize() {
        dataQueue = new Queue();
        setIEEEAddress();
        random = new Random(extendedAddress);
        resetAttributes();
        showUse = "true".equalsIgnoreCase(Utils.getSystemProperty("radio.traffic.show.leds",
                Utils.getManifestProperty("radio-traffic-show-leds", "false")));
        resetFiltering();
        startReceiveThread();
    }

    private byte getDSN() {
        return macDSN++;
    }

    /**
     * Reset the MAC attributes
     *
     */
    private void resetAttributes() {
        macDSN = (byte) (random.nextInt() & 0xFF);
        rxOnWhenIdle = false;
    }

    /**
     * Setup whether to filter packets based on a whitelist and
     * if so parse the whitelist. Uses the system properties:
     * radio.filter & radio.whitelist. The whitelist is a comma
     * separated list of radio addresses, where only the low part
     * of the addresses need to be specified, 
     * e.g. 1234 = 0014.4F01.0000.1234
     */
    public void resetFiltering() {
        filterPackets = false;
        if ("true".equalsIgnoreCase(Utils.getSystemProperty("radio.filter",
                                    Utils.getManifestProperty("radio-filter", "false")))) {
            String addrList = Utils.getSystemProperty("radio.whitelist",
                                    Utils.getManifestProperty("radio-whitelist", null));
                filterWhitelist = true;
            if (addrList == null || addrList.length() < 1) {
                filterWhitelist = false;
                addrList = Utils.getSystemProperty("radio.blacklist",
                                    Utils.getManifestProperty("radio-blacklist", null));
            }
            // comma separated list of LSBs: 0117, 29e2, 51.047A
            if (addrList != null && addrList.trim().length() > 1) {
                System.out.println("*** Radio will " + (filterWhitelist ? "only handle" : "ignore") + " packets received from: ");
                String addresses[] = Utils.split(addrList, ',');
                filterList = new long[addresses.length];
                for (int i = 0; i < addresses.length; i++) {
                    String addr = addresses[i].trim();
                    try {
                        filterList[i] = IEEEAddress.toLong("0014.4F01.0000.0000".substring(0, 19 - addr.length()) + addr);
                        System.out.println("***    " + IEEEAddress.toDottedHex(filterList[i]));
                        filterPackets = true;
                    } catch (IllegalArgumentException ex) {
                        System.out.println("Error: radio.whitelist badly formed: " + addr);
                        filterList[i] = 0;
                    }
                }
            } else {
                filterPackets = false;
            }
        }
    }
    
    /**
     * Make the dispatcher thread a daemon.
     * @param dispatcherThread
     */
    protected abstract void setAsDaemonThread(Thread dispatcherThread);

    /**
     * @author Syntropy
     *
     * The ReceiveThread class loops around reading packets from the physical layer
     * and queuing them for despatch to our clients. It copies their contents into
     * RadioPackets supplied by our clients and manages a pool locally so that we
     * minimise our memory allocations.
     */
    private class ReceiveThread extends Thread {

        public ReceiveThread() {
            super("MAC ReceiveThread");
        }

        public void run() {
            if (filterPackets) {            // call different routines so that
                receiveWithFilter();        // if we are not filtering packets
            } else {                        // we do not need an extra "if" in
                receiveAll();               // the inner loop of the radio code
            }
        }
        
        private void receiveAll() {  // make sure any changes get mirrored in receiveWithFilter()
            while (true) {
                RadioPacket recvPacket = RadioPacket.getDataPacket();
                try {
                    
                    dataIndication(recvPacket);
                    try {
                        recvPacket.decodeFrameControl();
                        if (recvPacket.isData()) {
                            if (awaitingAck) {
//                                Utils.log("Received data packet when awaiting ACK");
//                                Utils.log("Size = " + recvPacket.getLength());
//                                Utils.log(Utils.stringify(recvPacket.buffer));
                            }
                            validateDestAddr(recvPacket);
//				Utils.log("rx dsn =" + recvPacket.getDataSequenceNumber() + " " + System.currentTimeMillis() + " " + Thread.currentThread().getPriority());
                            if (recvPacket.getDestinationAddress() == extendedAddress || isRxQueueUnderLowerLimit()) {
                                rxDataQueue().put(recvPacket);
                            }
                            if (isRxQueueOverUpperLimit()) {
                                disableRx();
                            }
                        } else if (recvPacket.isAck()) {
                            synchronized (ackMonitor) {
                                if (awaitingAck) {
                                    if (recvPacket.getDataSequenceNumber() == ackDSN) {
                                        lastAck = recvPacket;
                                        awaitingAck = false;
                                        ackMonitor.notify();
                                    } else {
                                        wrongAck++;
                                    }
                                } else {
//                                    Utils.log("Discarding an ack with dsn " + recvPacket.getDataSequenceNumber());
                                }
                            }
                        } else {
                            resetRx();
                            rxError++;
                            dump();
                            // this was a throw of a new MACException, but as long as we catch it below why bother
                            System.err.println("RX error: Unknown packet type received: frame type =" + Integer.toHexString(recvPacket.getFrameControl()));
                        }
                    } catch (IllegalStateException badlyFormattedPacketException) {
                        System.err.println("RX error: " + badlyFormattedPacketException.getMessage());
                        rxError++;
                    } catch (MACException e) {
                        System.err.println("RX error: " + e.getMessage());
                        rxError++;
                    }
                } catch (Throwable e) {
                    System.err.println("RX thread error: " + e.getMessage());
                    rxError++;
                }
            }
        }

        /**
         * Same as receiveAll() except before queuing a received packet first check
         * that it was sent by a SPOT whose address is on our whitelist.
         */
        private void receiveWithFilter() {
            while (true) {
                RadioPacket recvPacket = RadioPacket.getDataPacket();
                try {
                    dataIndication(recvPacket);
                    try {
                        recvPacket.decodeFrameControl();
                        if (recvPacket.isData()) {
                            if (awaitingAck) {
//				Utils.log("Received data packet when awaiting ACK");
                            }
                            validateDestAddr(recvPacket);
//				Utils.log("rx dsn =" + recvPacket.getDataSequenceNumber() + " " + System.currentTimeMillis() + " " + Thread.currentThread().getPriority());
                            if (recvPacket.getDestinationAddress() == extendedAddress || isRxQueueUnderLowerLimit()) {
                                if (filterPackets) {
                                    long sourceAddr = recvPacket.getSourceAddress();
                                    boolean match = false;
                                    for (int i = 0; i < filterList.length; i++) {
                                        if (filterList[i] == sourceAddr) {
                                            match = true;
                                            break;
                                        }
                                    }
                                    if (!(filterWhitelist ^ match)) {
                                        rxDataQueue().put(recvPacket);
                                    }
                                } else {
                                    rxDataQueue().put(recvPacket);
                                }
                            }
                            if (isRxQueueOverUpperLimit()) {
                                disableRx();
                            }
                        } else if (recvPacket.isAck()) {
                            synchronized (ackMonitor) {
                                if (awaitingAck) {
                                    if (recvPacket.getDataSequenceNumber() == ackDSN) {
                                        lastAck = recvPacket;
                                        awaitingAck = false;
                                        ackMonitor.notify();
                                    } else {
                                        wrongAck++;
                                    }
                                } else {
//                                  Utils.log("Discarding an ack with dsn " + recvPacket.getDataSequenceNumber());
                                }
                            }
                        } else {
                            resetRx();
                            rxError++;
                            dump();
                            // this was a throw of a new MACException, but as long as we catch it below why bother
                            System.err.println("RX error: Unknown packet type received: frame type =" + Integer.toHexString(recvPacket.getFrameControl()));
                        }
                    } catch (IllegalStateException badlyFormattedPacketException) {
                        System.err.println("RX error: " + badlyFormattedPacketException.getMessage());
                        rxError++;
                    } catch (MACException e) {
                        System.err.println("RX error: " + e.getMessage());
                        rxError++;
                    }
                } catch (Throwable e) {
                    System.err.println("RX thread error: " + e.getMessage());
                    rxError++;
                }
            }
        }
    }

    public int getAckQueueJunk() {
        return 0;
    }

    public int getChannelAccessFailure() {
        return channelAccessFailure;
    }

    public int getNoAck() {
        return noAck;
    }

    public int getWrongAck() {
        return wrongAck;
    }

	public int getRxError() {
		return rxError;
	}

    public int getNullPacketAfterAckWait() {
        return nullPacketAfterAckWait;
    }

    public void resetErrorCounters() {
        nullPacketAfterAckWait = 0;
		channelAccessFailure = 0;
        noAck = 0;
        wrongAck = 0;
        rxError = 0;
	}


    private RadioPacket waitForAckPacket(byte myDSN) {
        synchronized (ackMonitor) {
            if (lastAck != null) {
                throw new SpotFatalException("ACK already there when about to wait for it");
            }
            ackDSN = myDSN;
            awaitingAck = true;
            try {
                ackMonitor.wait(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            awaitingAck = false;
        }
        RadioPacket result = lastAck;
        lastAck = null;
        return result;
    }

    protected abstract void disableRx();

    protected abstract void resetRx();

    protected void rxOnWhenIdleChanged() throws PHY_InvalidParameterException {
        if (isRxOnDesired() && !isRxQueueOverUpperLimit()) {
            enableRx();
        } else {
            disableRx();
        }
    }

    protected void conditionallyDisableRx() {
        //Utils.log("[conditionallyDisableRx] called...");
        if (!isRxOnDesired() || isRxQueueOverUpperLimit()) {
            //Utils.log("[conditionallyDisableRx] turning off rx...");
            disableRx();
        }
    }
}
