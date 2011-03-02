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

import com.sun.spot.util.Utils;
import java.util.Date;

/**
 *
 * @author Pete St. Pierre
 */
public class LowPanStats {
    // Time these stats were snapshotted/cloned
    private long timestamp;    // Protocol stats
    /**
     * number of SPOT protocol handlers registered
     */
    protected int protocolCount;
    /**
     * number of non-SPOT protocol family handlers registered
     */
    protected int protocolFamilyCount;
    /**
     * number of times we looked for a protocol handler, but failed
     */
    protected int protocolHandlerMissing;
    // Sent packet stats
    /**
     * Number of datagrams sent
     */
    protected int unicastsSent;
    /**
     * number of datagrams that required fragmentation
     */
    protected int unicastsFragmented;
    /**
     * non-mesh packets sent
     */
    protected int nonMeshPacketsSent;
    /**
     * mesh packets sent
     */
    protected int meshPacketsSent;
    /**
     * number of packets sent
     **/
    protected int packetsSent;
    /**
     * number of broadcasts sent
     **/
    protected int broadcastsSent;
    /**
     * broadcasts received
     */
    protected int broadcastsReceived;
    /**
     * number of mesh broadcasts sent
     **/
    protected int meshBroadcastsSent;
    /**
     * broadcasts received
     */
    protected int meshBroadcastsReceived;
    /**
     * number of mesh broadcasts that required fragmentation (we don't fragment local broadcasts)
     */
    protected int broadcastsFragmented;
    /**
     * number of packets forwarded through this node
     */
    protected int packetsForwarded;
    /**
     * number of broadcast packets forwarded
     */
    protected int meshBroadcastsForwarded;
    /**
     * number of packets intentionally dropped because TTL expired
     */
    protected int ttlExpired;
    /**
     * number of packets intentionally dropped because they didn't meet broadcast seqNo
     * requirements
     */
    protected int droppedBroadcasts;
    /**
     * number of packets intentionally dropped because we sent them
     */
    protected int broadcastsQueueFull;    // Receive statistics for local node
    /**
     * total mesh packets received
     */
    protected int meshPacketsReceived;
    /**
     * packets received without a mesh routing header (single hop);
     */
    protected int nonMeshPacketsReceived;
    /**
     * number of packets reassembled
     */
    protected int datagramsReassembled;
    /**
     * number of packets we couldn't reassemble
     */
    protected int reassemblyExpired;
    /** 
     * number of packet fragments we received
     */
    protected int fragmentsReceived;
    /**
     * full datagrams received
     */
    protected int unicastsReceived;

    /** Creates a new instance of LowPanStats */
    public LowPanStats() {
        timestamp = 0;
        unicastsSent = 0;
        unicastsFragmented = 0;
        unicastsReceived = 0;
        broadcastsSent = 0;
        broadcastsReceived = 0;
        meshBroadcastsSent = 0;
        meshBroadcastsReceived = 0;
        broadcastsFragmented = 0;
        meshBroadcastsForwarded = 0;
        packetsSent = 0;
        packetsForwarded = 0;
        meshPacketsReceived = 0;
        meshPacketsSent = 0;
        nonMeshPacketsReceived = 0;
        nonMeshPacketsSent = 0;
        reassemblyExpired = 0;
        ttlExpired = 0;
        datagramsReassembled = 0;
        fragmentsReceived = 0;
        broadcastsQueueFull = 0;
        droppedBroadcasts = 0;
        protocolCount = 0;
        protocolFamilyCount = 0;
        protocolHandlerMissing = 0;
    }

    /** Creates a new instance of LowPanStats */
    public LowPanStats(byte b[]) {
        int index = 0;
        timestamp = Utils.readLittleEndLong(b, index);
        index += 8;
        protocolCount = Utils.readLittleEndInt(b, index);
        index += 4;
        protocolFamilyCount = Utils.readLittleEndInt(b, index);
        index += 4;
        protocolHandlerMissing = Utils.readLittleEndInt(b, index);
        index += 4;
        unicastsSent = Utils.readLittleEndInt(b, index);
        index += 4;
        unicastsFragmented = Utils.readLittleEndInt(b, index);
        index += 4;
        nonMeshPacketsSent = Utils.readLittleEndInt(b, index);
        index += 4;
        meshPacketsSent = Utils.readLittleEndInt(b, index);
        index += 4;
        packetsSent = Utils.readLittleEndInt(b, index);
        index += 4;
        broadcastsSent = Utils.readLittleEndInt(b, index);
        index += 4;
        broadcastsReceived = Utils.readLittleEndInt(b, index);
        index += 4;
        meshBroadcastsSent = Utils.readLittleEndInt(b, index);
        index += 4;
        meshBroadcastsReceived = Utils.readLittleEndInt(b, index);
        index += 4;
        broadcastsFragmented = Utils.readLittleEndInt(b, index);
        index += 4;
        packetsForwarded = Utils.readLittleEndInt(b, index);
        index += 4;
        meshBroadcastsForwarded = Utils.readLittleEndInt(b, index);
        index += 4;
        ttlExpired = Utils.readLittleEndInt(b, index);
        index += 4;
        droppedBroadcasts = Utils.readLittleEndInt(b, index);
        index += 4;
        broadcastsQueueFull = Utils.readLittleEndInt(b, index);
        index += 4;
        meshPacketsReceived = Utils.readLittleEndInt(b, index);
        index += 4;
        nonMeshPacketsReceived = Utils.readLittleEndInt(b, index);
        index += 4;
        datagramsReassembled = Utils.readLittleEndInt(b, index);
        index += 4;
        reassemblyExpired = Utils.readLittleEndInt(b, index);
        index += 4;
        fragmentsReceived = Utils.readLittleEndInt(b, index);
        index += 4;
        unicastsReceived = Utils.readLittleEndInt(b, index);
        index += 4;
    }

    /**
     * Return the number of unicast packets that have been sent
     * @return number of unicasts sent from this node
     */
    public int getUnicastsSent() {
        return unicastsSent;
    }

    /**
     * Returns the number of unicast packets that required fragmentation.
     * 
     * @return number of times a unicast packet was too large to be delivered as a single packet
     */
    public int getUnicastsFragmented() {
        return unicastsFragmented;
    }

    /**
     * Returns the number of Unicast Packets received.  This includes both single packet unicasts and the number of 
     * datagrams created from multiple fragments.
     * 
     * @return the total number of unicast packets that have been received.
     */
    public int getUnicastsReceived() {
        return unicastsReceived;
    }

    /**
     * Returns the number of broadcast datagrams that have been generated from this node.
     * 
     * @return	the number of broadcast datagrams we have generated
     */
    public int getBroadcastsSent() {
        return broadcastsSent;
    }

    /**
     * Returns the number of broadcast datagrams that were generated from this node that required fragmentation.
     * 
     * @return the number of fragemented broadcast datagrams generated
     */
    public int getBroadcastsFragmented() {
        return broadcastsFragmented;
    }

    /**
     * Returns the number of broadcast datagrams received by this node.  This includes both single packet
     * broadcasts as well as broadcast datagrams successfully reassembled.
     * 
     * @return number of broadcast datagrams received
     */
    public int getBroadcastsReceived() {
        return broadcastsReceived;
    }

    /**
     * Returns the total number of packets sent from this node.  This is a combination of both single packet datagrams as well 
     * as the number of fragements generated from large datagrams.
     * 
     * @return total number of packets sent from this node
     */
    public int getPacketsSent() {
        return packetsSent;
    }

    /**
     * Returns the total number of packets received.  This includes both mesh and non-mesh packets.  Single hop packets that 
     * include a mesh header (ie. for broadcast sequence number) are considered mesh packets.
     * 
     * @return	total number of packets received by the lowpan layer
     */
    public int getPacketsReceived() {
        return nonMeshPacketsReceived + meshPacketsReceived;
    }

    /**
     * Returns the number of packets forwarded by the LowPan layer.  This includes all packets forward, regarless
     * of whether they were unicast, broadcast, full datagrams or fragments
     * 
     * @return	number of packets forwarded by this node
     */
    public int getPacketsForwarded() {
        return packetsForwarded;
    }

    /**
     * Returns the number of broadcast datagrams forwarded by this node.
     * 
     * @return number of broadcast datagrams forwarded by this node
     */
    public int getBroadcastsForwarded() {
        return meshBroadcastsForwarded;
    }

    /**
     * Returns the number of mesh packets received.  Single hop packets that 
     * include a mesh header (ie. for broadcast sequence number) are considered mesh packets.
     * 
     * @return number of mesh packets received
     */
    public int getMeshPacketsReceived() {
        return meshPacketsReceived;
    }

    /**
     * Return the number of mesh packets generated from this node.  Includes fragmented packets as well
     * as mesh broadcasts.
     * 
     * @return number of mesh packets sent
     */
    public int getMeshPacketsSent() {
        return meshPacketsSent;
    }

    /**
     * Return the number of non-mesh packets received by this node.  These packets had neither mesh or fragmentation headers
     * in the packet.
     * 
     * @return  number of non-mesh packets received by this node
     */
    public int getNonMeshPacketsReceived() {
        return nonMeshPacketsReceived;
    }

    /**
     * Return the number of non-mesh packets generated by this node.  These packets had neither mesh or fragmentation headers
     * in the packet.
     * 
     * @return  number of non-mesh packets generated by this node
     */
    public int getNonMeshPacketsSent() {
        return nonMeshPacketsSent;
    }

    /**
     * Returns the number of times reassembly of a datagram failed due to not receiving a complete set of fragements
     * within the alloted reassembly time.
     * 
     * @return  the number of times reassembly failed
     */
    public int getReassemblyExpired() {
        return reassemblyExpired;
    }

    /**
     * Returns the number of times we dropped a packet because the Time-To-Live field of the packet was decremented to zero.
     * 
     * @return  the number of times the TTL of a packet expired
     */
    public int getTTLExpired() {
        return ttlExpired;
    }

    /**
     * Returns the number of datagrams that were successfully reassembled by this node.
     * 
     * @return	the number of datagrams that arrived as a set of fragments
     */
    public int getDatagramsReassembled() {
        return datagramsReassembled;
    }

    /**
     * Returns the number of fragments received by this node.  The total reflects all fragements received, regardless of
     * whether the fragments resulted in successful reassembly of a datagram.
     * 
     * @return  the total number of fragments received by this node
     */
    public int getFragmentsReceived() {
        return fragmentsReceived;
    }

    /**
     * Returns the total number of protocol handlers that have been registered with the lowpan layer.  A normal value for this
     * is commonly 3, indicating that radiogram, radiostream and aodv are all registered.
     * 
     * @return  the number of protocol handlers currently registered with the LowPan Layer.
     */
    public int getProtocolCount() {
        return protocolCount;
    }

    /**
     * Returns the number of protocol families that are registered with the LowPan layer.  By default, this is 1 (the SPOT protocol
     * family).  Other modules that may register a protocol family may include IPv6, IPv6 HC1, etc.  RFC4944 discusses other 
     * protocol families that may register a dispatch byte with LowPan.
     * 
     * @return  the number of registered protocol families.
     */
    public int getProtocolFamilyCount() {
        return protocolFamilyCount;
    }

    /**
     * Returns the number of times we have received a packet for a protocol handler that is not registered.  This may occur when
     * IP packets have been sent to us at time we do not have an IP stack active.  Another example would be reception of AODV packets
     * when an alternate routing protocol is in use by this node.
     * 
     * @return  the number of times a packet was received for an unregistered protocol
     */
    public int getProtocolHandlerMissing() {
        return protocolHandlerMissing;
    }

    /**
     * Returns the number of times we dropped a broadcast packet.  These are usually duplicate sequence numbers (aka broadcasts we've
     * forwarded before.
     * 
     * @return	the number of times we refused to forward a broadcast from another node
     */
    public int getDroppedBroadcasts() {
        return droppedBroadcasts;
    }

    /**
     * Returns the number of times we've dropped a broadcast packet because we did not have the resources to queue the packet.  
     * This applies to forwarded packets.  Broadcast packets generated locally are sent immediately, not queued.
     * 
     * 
     * @return  the number of broadcast we've dropped due to insufficient resources
     */
    public int getDroppedQueuedBroadcasts() {
        return broadcastsQueueFull;
    }

    public LowPanStats clone() {
        LowPanStats newObj = new LowPanStats();
        newObj.timestamp = System.currentTimeMillis();
        newObj.unicastsSent = this.unicastsSent;
        newObj.unicastsFragmented = this.unicastsFragmented;
        newObj.unicastsReceived = this.unicastsReceived;
        newObj.broadcastsSent = this.broadcastsSent;
        newObj.broadcastsFragmented = this.broadcastsFragmented;
        newObj.broadcastsReceived = this.broadcastsReceived;
        newObj.packetsSent = this.packetsSent;
        newObj.packetsForwarded = this.packetsForwarded;
        newObj.meshBroadcastsSent = this.meshBroadcastsSent;
        newObj.meshBroadcastsReceived = this.meshBroadcastsReceived;
        newObj.meshBroadcastsForwarded = this.meshBroadcastsForwarded;
        newObj.meshPacketsReceived = this.meshPacketsReceived;
        newObj.meshPacketsSent = this.meshPacketsSent;
        newObj.nonMeshPacketsReceived = this.nonMeshPacketsReceived;
        newObj.nonMeshPacketsSent = this.nonMeshPacketsSent;
        newObj.reassemblyExpired = this.reassemblyExpired;
        newObj.ttlExpired = this.ttlExpired;
        newObj.droppedBroadcasts = this.droppedBroadcasts;
        newObj.broadcastsQueueFull = this.broadcastsQueueFull;
        newObj.datagramsReassembled = this.datagramsReassembled;
        newObj.fragmentsReceived = this.fragmentsReceived;
        newObj.protocolCount = this.protocolCount;
        newObj.protocolFamilyCount = this.protocolFamilyCount;
        newObj.protocolHandlerMissing = this.protocolHandlerMissing;

        return newObj;
    }

    public byte[] toByteArray() {
        byte b[] = new byte[104];
        int index = 0;
        Utils.writeLittleEndLong(b, index, timestamp);
        index += 8;
        Utils.writeLittleEndInt(b, index, protocolCount);
        index += 4;
        Utils.writeLittleEndInt(b, index, protocolFamilyCount);
        index += 4;
        Utils.writeLittleEndInt(b, index, protocolHandlerMissing);
        index += 4;
        Utils.writeLittleEndInt(b, index, unicastsSent);
        index += 4;
        Utils.writeLittleEndInt(b, index, unicastsFragmented);
        index += 4;
        Utils.writeLittleEndInt(b, index, nonMeshPacketsSent);
        index += 4;
        Utils.writeLittleEndInt(b, index, meshPacketsSent);
        index += 4;
        Utils.writeLittleEndInt(b, index, packetsSent);
        index += 4;
        Utils.writeLittleEndInt(b, index, broadcastsSent);
        index += 4;
        Utils.writeLittleEndInt(b, index, broadcastsReceived);
        index += 4;
        Utils.writeLittleEndInt(b, index, meshBroadcastsSent);
        index += 4;
        Utils.writeLittleEndInt(b, index, meshBroadcastsReceived);
        index += 4;
        Utils.writeLittleEndInt(b, index, broadcastsFragmented);
        index += 4;
        Utils.writeLittleEndInt(b, index, packetsForwarded);
        index += 4;
        Utils.writeLittleEndInt(b, index, meshBroadcastsForwarded);
        index += 4;
        Utils.writeLittleEndInt(b, index, ttlExpired);
        index += 4;
        Utils.writeLittleEndInt(b, index, droppedBroadcasts);
        index += 4;
        Utils.writeLittleEndInt(b, index, broadcastsQueueFull);
        index += 4;
        Utils.writeLittleEndInt(b, index, meshPacketsReceived);
        index += 4;
        Utils.writeLittleEndInt(b, index, nonMeshPacketsReceived);
        index += 4;
        Utils.writeLittleEndInt(b, index, datagramsReassembled);
        index += 4;
        Utils.writeLittleEndInt(b, index, reassemblyExpired);
        index += 4;
        Utils.writeLittleEndInt(b, index, fragmentsReceived);
        index += 4;
        Utils.writeLittleEndInt(b, index, unicastsReceived);
        index += 4;

        return b;
    }

    public String toString() {
        String s = "Timestamp: " + new Date(timestamp).toString() + "\n";
        s += "unicastsSent: " + unicastsSent + "\n";
        s += "unicastsFragmented: " + unicastsFragmented + "\n";
        s += "unicastsReceived: " + unicastsReceived + "\n";

        s += "broadcastsSent: " + broadcastsSent + "\n";
        s += "broadcastsFragmented: " + broadcastsFragmented + "\n";
        s += "broadcastsReceived: " + broadcastsReceived + "\n";

        s += "meshBroadcastsSent: " + meshBroadcastsSent + "\n";
        s += "meshBroadcastsReceived: " + meshBroadcastsReceived + "\n";
        s += "meshBroadcastsForwarded: " + meshBroadcastsForwarded + "\n";


        s += "packetsSent: " + packetsSent + "\n";
        s += "packetsForwarded: " + packetsForwarded + "\n";

        s += "nonMeshPacketsSent: " + nonMeshPacketsSent + "\n";
        s += "nonMeshPacketsReceived: " + nonMeshPacketsReceived + "\n";
        s += "meshPacketsSent: " + meshPacketsSent + "\n";
        s += "meshPacketsReceived: " + meshPacketsReceived + "\n";

        s += "reassemblyExpired: " + reassemblyExpired + "\n";
        s += "datagramsReassembled: " + datagramsReassembled + "\n";
        s += "fragmentsRecevied: " + fragmentsReceived + "\n";

        s += "ttlExpired: " + ttlExpired + "\n";
        s += "broadcastQueueFull: " + broadcastsQueueFull + "\n";
        s += "droppedBroadcasts: " + droppedBroadcasts + "\n";

        s += "protocolCount: " + protocolCount + "\n";
        s += "protocolfamilyCount: " + protocolFamilyCount + "\n";
        s += "protocolHandlerMissing: " + protocolHandlerMissing + "\n";
        return s;
    }
}
