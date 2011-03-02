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

import java.io.IOException;

import com.sun.spot.peripheral.ChannelBusyException;
import com.sun.spot.peripheral.NoAckException;
import com.sun.spot.peripheral.radio.mhrp.aodv.AODVManager;
import com.sun.spot.service.BasicService;
import com.sun.spot.service.ServiceRegistry;

import java.util.Hashtable;

import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Queue;

import java.util.Enumeration;
import java.util.Vector;
//import com.sun.spot.util.Debug;


/**
 * Implements {@link com.sun.spot.peripheral.radio.IRadioPacketDispatcher}
 * for the Spot environment.
 *
 * @author Pete St. Pierre, refactored from original LowPanPacketDispatcher by Jochen Furthmueller.
 */
public class RadioPacketDispatcher extends BasicService implements IRadioPacketDispatcher {
    private static final int MAX_PACKETS_QUEUED = 200;
    private ILowPan lowPan;
    private static IRadioPacketDispatcher theRPD;
    private IRadioPolicyManager rpm;
    private MACDescriptor[] macList;   // List of MACs we handle
    private Hashtable macTable;        // destination to MAC mapping
    private PacketQualityForwarderThread packetFwdThread;
    private Vector packetListener;
    private Queue packetQueue;

    /**
     * Return the singleton instance of RadioPacketDispatcher.
     *
     * @return the instance
     */
    public synchronized static IRadioPacketDispatcher getInstance() {
        if (theRPD == null) {
            theRPD = (RadioPacketDispatcher) ServiceRegistry.getInstance().lookup(RadioPacketDispatcher.class);
            if (theRPD == null) {
                theRPD = new RadioPacketDispatcher(RadioFactory.getI802_15_4_MACs(), RadioFactory.getRadioPolicyManager());
                RadioFactory.setProperty("IEEE_ADDRESS", IEEEAddress.toDottedHex(RadioFactory.getRadioPolicyManager().getIEEEAddress()));
                ServiceRegistry.getInstance().add((RadioPacketDispatcher) theRPD);
            }
        }
        return theRPD;
    }

    public String getServiceName() {
        return "RadioPacketDispatcher";
    }

    private RadioPacketDispatcher(I802_15_4_MAC[] macs, IRadioPolicyManager radioPolicyManager) {
        macTable = new Hashtable();
        macList = new MACDescriptor[macs.length];
        packetListener = new Vector();
        packetQueue = new Queue();
        this.rpm = radioPolicyManager;

        for (int i = 0; i < macs.length; i++) {
            macList[i] = new MACDescriptor(macs[i], macs[i].mlmeGet(I802_15_4_MAC.A_EXTENDED_ADDRESS));
        }
    }

    /**
     * Register to be notified with Link Quality information.
     *
     * @param packetListener the class that wants to be called back
     */
    public void registerPacketQualityListener(IPacketQualityListener packetListener) {
        addPacketQualityListener(packetListener);
    }

    /**
     * Undo a previous call of registerPacketListener()
     *
     * @param listener the class that wants to be deregistered
     */
    public void deregisterPacketQualityListener(IPacketQualityListener listener) {
        removePacketQualityListener(listener);
    }

    /**
     * Register to be notified with Link Quality information.
     *
     * @param packetListener the class that wants to be called back
     */
    public void addPacketQualityListener(IPacketQualityListener packetListener) {
        this.packetListener.addElement(packetListener);
        if (packetFwdThread == null) {
            packetFwdThread = new PacketQualityForwarderThread();
            packetFwdThread.start();
        }
    }

    /**
     * Undo a previous call of registerPacketListener()
     *
     * @param listener the class that wants to be deregistered
     */
    public void removePacketQualityListener(IPacketQualityListener listener) {
        this.packetListener.removeElement(listener);
        if (this.packetListener.isEmpty()) packetFwdThread = null;
    }

    /**
     * Broadcast a packet. Called from sendPacket if destination is a broadcast or unknown
     *
     * @param rp
     * @throws NoAckException
     * @throws ChannelBusyException
     */
    public void sendBroadcast(RadioPacket rp) throws NoAckException, ChannelBusyException {

        // Iterate over the list of Macs and send the packet on each
        for (int i = 0; i < macList.length; i++) {

            rp.setDestinationPanID(rpm.getPanId());
            rp.setSourceAddress(macList[i].getOurAddress());

            rp.setDestinationAddress(rp.getDestinationAddress());
            rp.setSourceAddress(rp.getSourceAddress());
            
            int result;
            result = macList[i].getMacDevice().mcpsDataRequest(rp);

            if (result != I802_15_4_MAC.SUCCESS) {
                if (result == I802_15_4_MAC.NO_ACK) {
                    throw new NoAckException("No ack");
                } else if (result == I802_15_4_MAC.CHANNEL_ACCESS_FAILURE) {
                    throw new ChannelBusyException("Channel busy");
                }
            }
        }
    }

    /**
     * Send a packet. Is called by the low pan layer
     *
     * @param rp
     * @throws NoAckException
     * @throws ChannelBusyException
     */
    public void sendPacket(RadioPacket rp) throws NoAckException, ChannelBusyException {
        //   System.out.println("[RPD] Sending packet to: " + IEEEAddress.toDottedHex(rp.getDestinationAddress()));
        MACDescriptor macDesc = (MACDescriptor) macTable.get(new Long(rp.getDestinationAddress()));
        if (macDesc == null) {

            sendBroadcast(rp);
        } else {
            rp.setDestinationPanID(rpm.getPanId());
            rp.setSourceAddress(macDesc.getOurAddress());

//                System.out.println("sendPacket: sending packet from "
//                + IEEEAddress.toDottedHex(rp.getSourceAddress()) +" to "
//                + IEEEAddress.toDottedHex(rp.getDestinationAddress()));

            int result;
            result = macDesc.getMacDevice().mcpsDataRequest(rp);

            if (result != I802_15_4_MAC.SUCCESS) {
                if (result == I802_15_4_MAC.NO_ACK) {
                    throw new NoAckException("No ack");
                } else if (result == I802_15_4_MAC.CHANNEL_ACCESS_FAILURE) {
                    throw new ChannelBusyException("Channel busy");
                }
            }
        }
        if (!packetListener.isEmpty()) {
            if (packetQueue.size() < MAX_PACKETS_QUEUED) {
                packetQueue.put(rp); // If anyone is listening, queue the packet
            }
        }

    }

    /**
     * Called by LowPan to initialize the dispatcher.
     * <p/>
     * <strong>Note:</strong> This is only called after LowPan is completely
     * started up.  This method is there to prevent cycles in the initialization
     * process
     *
     * @param lowPanLayer reference to the fully started LowPan instance
     */
    public synchronized void initialize(ILowPan lowPanLayer) {
        this.lowPan = lowPanLayer;
        for (int i = 0; i < macList.length; i++) {
            DispatcherThread dispatcherThread = new DispatcherThread(macList[i]);
            RadioFactory.setAsDaemonThread(dispatcherThread);
            dispatcherThread.start();
        }
    }

    private class DispatcherThread extends Thread {
        MACDescriptor macDesc;

        DispatcherThread(MACDescriptor desc) {
            super("RadioPacketDispatcher:" + IEEEAddress.toDottedHex(desc.getOurAddress()));
            this.macDesc = desc;
        }

        public void run() {
            /**
             * Internally macLayer copies the contents of an internal radio packet
             * into the one that's supplied by this class
             */
            while (true) {
                try {
                    RadioPacket rp = RadioPacket.getDataPacket();
                    macDesc.getMacDevice().mcpsDataIndication(rp);

                    macTable.put(new Long(rp.getSourceAddress()), this.macDesc);
                    lowPan.receive(rp);
                    if (!packetListener.isEmpty()) {
                        if (packetQueue.size() < MAX_PACKETS_QUEUED) {
                            packetQueue.put(rp); // If anyone is listening, queue the packet
                        }
                    }
                } catch (Exception e) {
                    if (e instanceof NullPointerException) {
                        System.out.println("[RadioPacketDispatcher] " +
                                "Packet received while LowPan uninitialized");
                        e.printStackTrace();
                    } else if (e instanceof IOException) {
                        // Quietly ignore this as per old behavior
                    } else {
                        System.out.println("[RadioPacketDispatcher] \n" +
                                "Exception processing packet");
                        e.printStackTrace();
                    }

                }
            }
        }
    }

    private class MACDescriptor {
        private I802_15_4_MAC macDevice;
        private long ourAddress;

        public MACDescriptor(I802_15_4_MAC mac, long addr) {
            setMacDevice(mac);
            setOurAddress(addr);
        }

        public I802_15_4_MAC getMacDevice() {

            return macDevice;
        }

        public void setMacDevice(I802_15_4_MAC macDevice) {
            this.macDevice = macDevice;
        }

        public long getOurAddress() {
            return ourAddress;
        }

        public void setOurAddress(long ourAddress) {
            this.ourAddress = ourAddress;
        }

    }

    private class PacketQualityForwarderThread extends Thread {

        PacketQualityForwarderThread() {
            super("PacketQualityForwarder");
        }

        public void run() {
            while (true) {
                RadioPacket rp = null;
                while (rp == null) {
                    rp = (RadioPacket) packetQueue.get();
                }
                // send the packet
                if (!packetListener.isEmpty()) {
                    Enumeration en = packetListener.elements();
                    while (en.hasMoreElements()) {
                        ((IPacketQualityListener) en.nextElement()).notifyPacket(rp.getSourceAddress(),
                                rp.getDestinationAddress(), rp.getRssi(), rp.getCorr(), rp.getLinkQuality(),
                                rp.getLength());
                    }
                }
            }
        }
    }
}
