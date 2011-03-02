/*
 * Copyright 2007-2009 Sun Microsystems, Inc. All Rights Reserved.
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

/**
 * LowPanPacket.java
 *
 * @author Pete St. Pierre
 *         <p/>
 *         Created on January 23, 2007, 2:54 PM
 *         <p/>
 *         The LowPanPacket class is used to parse incoming RadioPackets to handle the flexible stacked
 *         header format defined in http://www.ietf.org/internet-drafts/draft-ietf-6lowpan-format-10.txt
 *         <p/>
 *         Headers may contain:
 *         a mesh header
 *         a broadcast header
 *         a fragmentation header
 *         The draft says they must be in this order, but we want to be flexible if possible.  Not all
 *         headers are required.
 */
public class LowPanPacket {
    /**
     * bitmask used to extract the dispatch field
     */
    protected static final byte DISPATCH_MASK = 0x3f;
    /**
     * mask used to test if this is a lowpan packet
     */

    private static final byte F_BIT = 0x10;
    private static final byte O_BIT = 0x20;
    private static final byte HOPSLEFT_BITS = 0x0f;
    private static final byte FRAG_FIRST = (byte) 0xc0;
    private static final byte FRAG_INTERIOR = (byte) 0xe0;
    private static final byte FRAG_LAST = (byte) 0xe0;
    /**
     * Indicates this LowPanPacket will use a RadioPacket of the type 'Data'
     */
    protected static final int DATA_PACKET = 1;
    /**
     * Indicates this LowPanPacket will use a RadioPacket of the type 'Broadcast'
     */
    protected static final int BROADCAST_PACKET = 2;


    // Interesting items about the packet when we are reading it
    private RadioPacket rp;   // a reference to the full radio packet
    private boolean meshed; // True if there is a mesh header
    private int meshIndex; // Index of start of Mesh header
    private boolean extendedHops; // 0xf hops means byte that follows is the hop count byte
    private int hopsLeftIndex; // place in packet of hopsLeft
    private int origAddrIndex; // packet index for originator address
    private int destAddrIndex; // packet index for destination address
    private int destLen;    // number of bytes in Final Destination Address 2=16bit, 8=64bit
    private int origLen;    // number of bytes in Final Destination Address 2=16bit, 8=64bit
    private boolean bCast;  // true if there is a LOWPAN_BC0 header
    private int bCastIndex; // Index of broadcast value
    private boolean fragged; // true if packet is fragmented
    private int fragIndex;  // start of fragmentation bits
    private int fragTagIndex;  // Index to start of Fragmentation Tag bits
    private int fragSizeIndex;
    private int fragOffsetIndex;   // Type of fragmented packet
    private boolean extendedProtocol; // True if using extended protocol number (non IPv6)
    private int protocolDispatchIndex;  // Index of the "protocol family" byte
    private int protocolIndex;      // Higher level protocol (follows DISPATCH_ESC)
    private int lppPayloadOffset;      // index at which first non-header data byte starts
    private int lppPayloadSize;       // Total size of remaining payload

    // House keeping variables

    private int parseIndex;  // current point within the packet where we are parsing;

    /**
     * Create a LowPanPacket of either DATA_PACKET or BROADCAST_PACKET
     *
     * @param type the type of underlying RadioPacket to use either a
     *             DATA_PACKET or BROADCAST_PACKET
     */
    public LowPanPacket(int type) {

        meshed = false;
        meshIndex = hopsLeftIndex = 0;
        extendedHops = false;
        destLen = origLen = 8;
        origAddrIndex = destAddrIndex = 0;
        bCast = false;
        bCastIndex = 0;
        fragged = false;
        fragTagIndex = fragSizeIndex = fragOffsetIndex = 0;
        protocolIndex = lppPayloadOffset = lppPayloadSize = 0;

        parseIndex = 0;
        switch (type) {
            case DATA_PACKET:
                rp = RadioPacket.getDataPacket();
                break;
            case BROADCAST_PACKET:
                rp = RadioPacket.getBroadcastPacket();
                break;
            default:
        }

    }

    /**
     * Parse a RadioPacket and use it to create a new instance of LowPanPacket that
     * represents this RadioPacket.  The packet must be a valid radio DATA packet.
     * Passing ACK packets will cause an IllegalStateException to be thrown.
     * This can be avoided by calling RadioPacket#isAck() to verify radio packet type.
     *
     * @param packet the RadioPacket to parse
     */
    public LowPanPacket(RadioPacket packet) {
        rp = packet;
        //System.out.println("MACPayloadOffset: " + rp.getPayloadOffset());
        //System.out.println("MACPayloadLen: " + rp.getMACPayloadLength());
        meshed = false;
        meshIndex = hopsLeftIndex = 0;
        destLen = origLen = 8;
        origAddrIndex = destAddrIndex = 0;
        bCast = false;
        bCastIndex = 0;
        fragged = false;
        fragTagIndex = fragSizeIndex = fragOffsetIndex = 0;
        extendedProtocol = false;
        protocolIndex = protocolDispatchIndex = lppPayloadOffset = lppPayloadSize = 0;
        parseIndex = 0;

        parse();
    }

    private void parse_fragment(byte dispatch) {
        fragged = true;
        fragIndex = fragSizeIndex = (short) (parseIndex - 1);  // Size starts in dispatch byte
        parseIndex++; // Skip the next byte (lower part of FragSize)
        fragTagIndex = parseIndex++;  // two bytes of tag start here
        parseIndex++; // Skip the next byte (lower part of FragTag)

        if ((dispatch & FRAG_INTERIOR) == FRAG_INTERIOR) {
            fragOffsetIndex = parseIndex++;
        }
//        System.out.println("[lowpan] Packet is fragmented");
//        System.out.println("SizeI:TagI:OffsetI - " + fragSizeIndex + ":" +
//                    fragTagIndex +":"+fragOffsetIndex);

        parse();
    }

    void parse_mesh(byte dispatch) {
        meshed = true;
        origLen = ((dispatch & O_BIT) == dispatch) ? 2 : 8;
        destLen = ((dispatch & F_BIT) == dispatch) ? 2 : 8;
        hopsLeftIndex = parseIndex - 1;
        if ((dispatch & HOPSLEFT_BITS) == 0xf) { // 0xf means hte next byte is the hop count
            extendedHops = true;
            parseIndex++;
        }
        //       System.out.println("origLen:" + origLen + " DestLen:" + destLen);
        origAddrIndex = parseIndex;
        parseIndex += origLen;

        destAddrIndex = parseIndex;
        parseIndex += destLen;
        //      System.out.println ("[lowpan] Recevied Mesh Header with hops : " + getHopsLeft());
        //      System.out.println ("\t origIndex " + origAddrIndex);
        //      System.out.println ("\t destIndex " + destAddrIndex);
        parse();
    }

    private void parse_broadcast(byte dispatch) {
        bCast = true;
        bCastIndex = parseIndex++;
//        System.out.println ("[lowpan] Recevied BCast Header with Seq number: " + getBCastSeqNo());
        parse();
    }

    private void parse_esc(byte dispatch) {
        extendedProtocol = true;
        protocolDispatchIndex = parseIndex - 1;
        protocolIndex = parseIndex++;
        //       System.out.println ("[lowpan] Extended Protocol Index: "+ protocolIndex);
        // Don't call parse again -- we must be done!
    }

    private void parse_family(byte dispatch) {
        extendedProtocol = false;
        protocolIndex = protocolDispatchIndex = parseIndex - 1;

        // Don't call parse again -- we must be done!
    }

    /**
     * The main routine used to initiate parsing of a RadioPacket
     */
    private void parse() {
        if (rp.getLength() == 0) {
            return;
        }
//        System.out.println("Check dispatch at: " + parseIndex);
        byte dispatch = rp.getMACPayloadAt(parseIndex++);
//        System.out.println("[lowpan] dispatch: " + Integer.toHexString(dispatch & 0xff));
        if ((dispatch & LowPanHeader.DISPATCH_FRAG) == LowPanHeader.DISPATCH_FRAG) {
            parse_fragment(dispatch);
        } else if ((dispatch & LowPanHeader.DISPATCH_MESH) == LowPanHeader.DISPATCH_MESH) {
            parse_mesh(dispatch);
        } else if (dispatch == LowPanHeader.DISPATCH_LOWPAN_BC0) {
            parse_broadcast(dispatch);
        } else if (dispatch == LowPanHeader.DISPATCH_ESC) {
            parse_esc(dispatch);
        } else {
            // I have no idea what this header is, must be an address family
            parse_family(dispatch);
        }
        lppPayloadOffset = rp.getPayloadOffset() + parseIndex;
        lppPayloadSize = rp.getMACPayloadLength() - getHeaderLength();

    }

    private void writeMeshHeader(LowPanHeader lph) {
        byte meshByte;
        if (lph.getOutgoingHops() > 14) {
            meshByte = (byte) ((LowPanHeader.DISPATCH_MESH & 0xf0) | 0x0f);  // init with two 64 bit addresses
            rp.setMACPayloadAt(parseIndex++, meshByte);  //initialize the first byte, hops follow
            rp.setMACPayloadAt(parseIndex++, (byte) (lph.getOutgoingHops() & 0xff));
        } else {
            meshByte = (byte) ((LowPanHeader.DISPATCH_MESH & 0xf0) | (lph.getOutgoingHops() & 0x0f));  // init with two 64 bit addresses
            // set hops left
            rp.setMACPayloadAt(parseIndex++, meshByte);  //initialize the top of the byte
        }
        // Set O/F Flags here when we support variable length addresses


        // write the originator address
        origAddrIndex = parseIndex;
        rp.setMACPayloadBigEndLongAt(origAddrIndex, lph.getOutgoingOriginatorAddress());
        parseIndex += 8;
        // write the destination address
        destAddrIndex = parseIndex;
        rp.setMACPayloadBigEndLongAt(destAddrIndex, lph.getOutgoingDestinationAddress());
        parseIndex += 8;

//        System.out.println("[lowpan send]Mesh Header: " + Integer.toHexString(meshByte & 0xff) + " from " +
//                new IEEEAddress(lph.getOutgoingOriginatorAddress()) + " to " +
//                new IEEEAddress(lph.getOutgoingDestinationAddress()));
//        System.out.println("Continuting header at byte: " +parseIndex);

    }

    private void writeBroadcastHeader(LowPanHeader lph) {

        rp.setMACPayloadAt(parseIndex++, LowPanHeader.DISPATCH_LOWPAN_BC0);
        bCastIndex = parseIndex;
        rp.setMACPayloadAt(parseIndex++, (byte) (lph.getOutgoingBCastSeqNo() & 0xff));

        // System.out.println ("[lowpan send]Broadcast Header, Sequence #: " + lph.getOutgoingBCastSeqNo());
    }

    // Fragmentation header has 4 bytes, 5 if it is a first fragment

    private void writeFragmentHeader(LowPanHeader lph) {
        byte b = 0x00;
        // Initialize top part of first byte
        switch (lph.getOutgoingFragType()) {
            case LowPanHeader.UNFRAGMENTED: {
                //there is no header if we're unfragmented, silly'
                return;
            }
            case LowPanHeader.FIRST_FRAGMENT: {
                b = FRAG_FIRST;
                break;
            }
            case LowPanHeader.LAST_FRAGMENT:
            case LowPanHeader.INTERIOR_FRAGMENT: {
                b = FRAG_INTERIOR;
                break;
            }
        }
//        System.out.println("Size:Tag:Offset:  " + lph.getOutgoingFragSize()+ " " +
//                lph.getOutgoingFragTag() + " " +
//                lph.getOutgoingFragOffset());
        // first 3 bits of the size in the last 3 bits of this byte
//        System.out.println("Type: " + Integer.toHexString(b));
        b |= (byte) ((lph.getOutgoingFragSize() >> 8) & 0x07);
        fragIndex = fragSizeIndex = parseIndex;
        rp.setMACPayloadAt(parseIndex++, b);

        // last 8 bytes of Size fit in 1 byte
        rp.setMACPayloadAt(parseIndex++, (byte) (lph.getOutgoingFragSize() & 0xff));

        // Now two bytes of tag info
        fragTagIndex = parseIndex;
        rp.setMACPayloadBigEndShortAt(parseIndex++, (short) (lph.getOutgoingFragTag() & 0xffff));
        parseIndex++; // skip lower byte we just wrote
        if (lph.getOutgoingFragType() != LowPanHeader.FIRST_FRAGMENT) {
            fragOffsetIndex = parseIndex;
            rp.setMACPayloadAt(parseIndex++, lph.getOutgoingFragOffset());
        }

//        System.out.println ("[lowpan send]Fragementation Header");
    }

    private void writeProtocolHeader(LowPanHeader lph) {
        if ((lph.getProtocolFamily() == LowPanHeader.DISPATCH_SPOT)) {
            rp.setMACPayloadAt(parseIndex++, LowPanHeader.DISPATCH_ESC);
            rp.setMACPayloadAt(parseIndex++, lph.getProtocolNo());
        } else {
            rp.setMACPayloadAt(parseIndex++, lph.getProtocolFamily());
        }
    }

    /**
     * Constructs a RadioPacket using a LowPanHeader and a data buffer.
     * Will only send the segment of the data buffer as bounded by start and end
     * indicies
     *
     * @param lph    the lowpan header for this packet
     * @param buffer data buffer to send
     * @param start  index of first byte to send from buffer
     * @param end    index of last byte to send from buffer
     */
    public void writeHeaderAndPayload(LowPanHeader lph, byte[] buffer,
                                      int start, int end) {
        // Reset the packet header
        parseIndex = 0;
        // Spec says headers, if present must be 1) Mesh, 2) Broadcast, 3) Fragment 4) Dispatch

        // Write mesh header if necessary
        if (lph.isMeshed()) {
            writeMeshHeader(lph);
        }

        // Write broadcast header
        if (lph.isBCast()) {
            writeBroadcastHeader(lph);
        }
        // Write Fragementation header
        if (lph.isFragged()) {
            writeFragmentHeader(lph);
        }
        // protocol header exists in all packets
        writeProtocolHeader(lph);

        int payloadStart = rp.getPayloadOffset() + lph.getLength();
        int dataLength = end - start;
        int payloadSize = dataLength + lph.getLength();

//        System.out.println("[lpp] Send Buffer len: " +buffer.length);
//        System.out.println ("[lpp] data len: " + dataLength + " payload Start: " +
//                payloadStart + " Size: " + payloadSize);
        if ((lph.getProtocolFamily() == LowPanHeader.DISPATCH_SPOT) && payloadSize < (LowPanHeader.MAX_UN_FRANG_BDC_MSG + 3)) {
            rp.setMACPayloadLength(payloadSize);
            System.arraycopy(buffer, start, rp.buffer, payloadStart, dataLength);
        } else if (payloadSize < (rp.MIN_PAYLOAD_LENGTH)) {
            // Copy payload into radio packet
            // Do we need to add the header length here?  We added it to PayloadSize above
            rp.setMACPayloadLength(payloadSize);
            System.arraycopy(buffer, start, rp.buffer, payloadStart, dataLength);

        } else {

            System.out.println("[lpp] Packet was too large to copy");
        }


    }

    /**
     * returns the number of hops left for this packet
     *
     * @return the number of hops remaining for this packet
     */
    public byte getHopsLeft() {
        byte hopsLeft = (byte) (rp.getMACPayloadAt(hopsLeftIndex) & HOPSLEFT_BITS);
//        System.out.println("Hops Left in PKT: " + (hopsLeft & HOPSLEFT_BITS));
        if (hopsLeft == HOPSLEFT_BITS)
            return (byte) (rp.getMACPayloadAt(hopsLeftIndex + 1));
        else
            return hopsLeft;

    }

    /**
     * set the number of hops for this packet
     *
     * @param hopsLeft number of hops remaining
     */
    public void setHopsLeft(int hopsLeft) {
        byte save;
        if (extendedHops) {
            save = (byte) (rp.getMACPayloadAt(hopsLeftIndex) & 0xf0);
            rp.setMACPayloadAt(hopsLeftIndex, (byte) (save | 0xf));
            rp.setMACPayloadAt(hopsLeftIndex + 1, (byte) (hopsLeft & 0xff));
        } else {
            save = (byte) (rp.getMACPayloadAt(hopsLeftIndex) & 0xf0);
            rp.setMACPayloadAt(hopsLeftIndex, (byte) (save | hopsLeft));
        }
    }

    /**
     * returns the length in bytes of the destination address field
     * 2 bytes == 16-bit addressing
     * 8 bytes == 64-bit addressing
     *
     * @return number of bytes in the destination address
     */
    public int getDestLen() {
        return destLen;
    }

    /**
     * set the length in bytes of the destination address field
     * 2 bytes == 16-bit addressing
     * 8 bytes == 64-bit addressing
     *
     * @param destLen number of bytes in the destination address
     */
    public void setDestLen(int destLen) {
        this.destLen = destLen;
    }

    /**
     * return the length in bytes of the originator address field
     * 2 bytes == 16-bit addressing
     * 8 bytes == 64-bit addressing
     *
     * @return number of bytes in the originator address
     */
    public int getOrigLen() {
        return origLen;
    }

    /**
     * set the length in bytes of the destination address field
     * 2 bytes == 16-bit addressing
     * 8 bytes == 64-bit addressing
     *
     * @param origLen number of bytes in the originator address
     */
    public void setOrigLen(int origLen) {
        this.origLen = origLen;
    }

    /**
     * return the address of the originator field of the mesh header
     *
     * @return the originator address of this packet
     */
    public long getOriginatorAddress() {
        if (origLen == 2) {
            return rp.getMACPayloadBigEndShortAt(origAddrIndex);
        } else {
            return rp.getMACPayloadBigEndLongAt(origAddrIndex);
        }

    }

    /**
     * set the address of the originator field of the mesh header
     *
     * @param origAddr address of the originator of this packet
     */
    public void setOriginatorAddress(long origAddr) {
        if (origLen == 2) {
            rp.setMACPayloadBigEndShortAt(origAddrIndex, (short) (origAddr & 0xffff));
        } else {
            rp.setMACPayloadBigEndLongAt(origAddrIndex, origAddr);
        }
    }

    /**
     * return the final destination address of this packet
     *
     * @return the final destination of this mesh packet
     */
    public long getFDestinationAddress() {
        if (destLen == 2) {
            return rp.getMACPayloadBigEndShortAt(destAddrIndex);
        } else {
            return rp.getMACPayloadBigEndLongAt(destAddrIndex);
        }
    }

    /**
     * set the final destination for this meshed packet
     *
     * @param destAddr new final destination address for this meshed packet
     */
    public void setFDestinationAddress(long destAddr) {
        if (destLen == 2) {
            rp.setMACPayloadBigEndShortAt(destAddrIndex, (short) (destAddr & 0xffff));
        } else {
            rp.setMACPayloadBigEndLongAt(destAddrIndex, destAddr);
        }
    }


    /**
     * return the sequence number for this mesh broadcast
     *
     * @return the sequence number of this broadcast
     */
    public int getBCastSeqNo() {
        if (isBCast())
            return (int) (rp.getMACPayloadAt(bCastIndex) & 0xff);
        else
            return (int) 0;
    }

    /**
     * set the sequence number for this mesh broadcast
     *
     * @param bCastSeqNo the mesh broadcast sequence number
     */
    public void setBCastSeqNo(int bCastSeqNo) {
        rp.setMACPayloadAt(bCastIndex, (byte) (bCastSeqNo & 0xff));
    }

    /**
     * define the fragment type for this packet, as defined in the 6lowpan spec
     *
     * @param type set the fragment type bits in this header
     */
    public void setFragType(byte type) {
        byte save = (byte) (rp.getMACPayloadAt(fragIndex) & 0x07);       // save size bits  (low 3)
        save |= (type & 0xe0);// OR in the high 3 bits - two middle unused
        rp.setMACPayloadAt(fragTagIndex, save);
    }

    /**
     * retrieve the fragment type bits from the fragmentation header
     *
     * @return the fragmentation type bits of this header
     */
    public byte getFragType() {
        byte fragTag = (byte) (rp.getMACPayloadAt(fragIndex) & 0xe0);
        return fragTag;
    }

    /**
     * set the datagram tag for this fragment
     *
     * @param tag the datagram tag to which this fragment belongs
     */
    public void setFragTag(short tag) {
        rp.setMACPayloadBigEndShortAt(fragTagIndex, tag);
    }

    /**
     * retrieve the datagram tag for this fragment
     *
     * @return the datagram tag
     */
    public short getFragTag() {
        return (short) (rp.getMACPayloadBigEndShortAt(fragTagIndex) & 0xffff);
    }

    /**
     * set the total size of the datagram to which this fragment belongs
     *
     * @param size total size of the original datagram
     */
    public void setFragSize(short size) {
        byte save = (byte) (rp.getMACPayloadAt(fragSizeIndex) & 0xf8);  // save type bits  (high 3)
        save |= ((size >> 8) & 0x07);
        rp.setMACPayloadAt(fragSizeIndex, save);
        // last 8 bits of size
        rp.setMACPayloadAt(fragSizeIndex + 1, (byte) (size & 0xff));
    }

    /**
     * retrieve the total size of the original datagram of which this fragment is part
     *
     * @return total size of original datagram
     */
    public short getFragSize() {
        int fragSize = 0;
        int val = (rp.getMACPayloadAt(fragSizeIndex) & 0xff);
        fragSize = (val & 0x7) << 8;
        fragSize |= (rp.getMACPayloadAt(fragSizeIndex + 1) & 0xff);
        //System.out.println("Frag Size: " + fragSize);
        return (short) (fragSize & 0xffff);
    }

    /**
     * retrieve the offset of this fragment as related to the original datagram
     * offsets are in multiples of 8 bytes
     *
     * @return offset of this fragment
     */
    public byte getFragOff() {
        int fragOff = (rp.getMACPayloadAt(fragOffsetIndex) & 0xff);
        //System.out.println("FragOff: " + fragOff);
        return (byte) (fragOff & 0xff);
    }

    /**
     * set the offset of this fragment as related to the original datagram
     * offsets are in multiples of 8 bytes
     *
     * @param fragOff offset within the original datagram
     */
    public void setFragOff(byte fragOff) {
        rp.setMACPayloadAt(fragOffsetIndex, fragOff);
    }

    /**
     * determine whether this is a leading fragment of a packet
     *
     * @return true if the header indicates this is a first fragment of a set
     */
    public boolean isFirstFrag() {
        byte fragTag = (byte) (rp.getMACPayloadAt(fragIndex) & 0xe0);
        return (fragTag == FRAG_FIRST);
    }

    /**
     * retrieve the protocol family number from this packet
     *
     * @return the 8 bit protocol family number
     */
    public byte getProtocolFamily() {
        byte pf = rp.getMACPayloadAt(protocolDispatchIndex);
        return pf;
    }

    /**
     * sets the protocol family number for this packet
     *
     * @param protocolFamily the 8-bit protocol family number
     */
    public void setProtocolFamily(byte protocolFamily) {
        rp.setMACPayloadAt(protocolDispatchIndex, protocolFamily);
    }

    /**
     * retrieve the protocol number from this packet
     *
     * @return the 8 bit protocol number
     */
    public byte getProtocol() {
        byte protocol = rp.getMACPayloadAt(protocolIndex);
        return protocol;
    }

    /**
     * sets the protocol number for this packet
     *
     * @param protocol the 8-bit protocol number
     */
    public void setProtocol(byte protocol) {
        rp.setMACPayloadAt(protocolIndex, protocol);
    }

    /**
     * sets the protocol number , within a specific family, for this packet
     *
     * @param family   the protocol family
     * @param protocol the 8-bit protocol number
     */
    public void setProtocol(byte family, byte protocol) {
        setProtocolFamily(family);
        setProtocol(protocol);
    }

    /**
     * determine if this packet has a mesh header
     *
     * @return true if a mesh header is present
     */
    public boolean isMeshed() {
        return meshed;
    }

    /**
     * define this packet to have a mesh header
     *
     * @param meshed true if there will be a mesh header in this packet
     */
    public void setMeshed(boolean meshed) {
        this.meshed = meshed;
    }

    /**
     * determine if there is a LowPan broadcast header
     *
     * @return true if a LowPan broadcast header is present
     */
    public boolean isBCast() {
        return bCast;
    }

    /**
     * define this packet to have a LowPan broadcast header
     *
     * @param bCast true if this packet will have a LowPan broadcast header
     */
    public void setBCast(boolean bCast) {
        this.bCast = bCast;
    }

    /**
     * determine whether this packet has a fragmentation header
     *
     * @return true if there is a fragmentation header
     */
    public boolean isFragged() {
        return fragged;
    }

    /**
     * define this packet to have a fragmentation header
     *
     * @param fragged true if there will be a fragmentation header
     */
    public void setFragged(boolean fragged) {
        this.fragged = fragged;
    }

    /**
     * return the source address of the actual radio packet
     *
     * @return Radio Packet source address
     */
    public long getRPSourceAddress() {
        return rp.getSourceAddress();
    }

    /**
     * return a representation of this LowPanPacket in an actual RadioPacket
     *
     * @return a RadioPacket suitable for sending via the RadioPacketDispatcher
     */
    public RadioPacket getRadioPacket() {
        return rp;
    }

    /**
     * set the destination of the underlying radio packet
     *
     * @param addr the destination address
     */
    public void setRPDestinationAddress(long addr) {
        rp.setDestinationAddress(addr);
    }

    /**
     * set the source address for this radio packet
     * this may be over written by the packet dispatcher.  It is used predominantly for testing
     *
     * @param addr source address for this radio packet
     */
    public void setRPSourceAddress(long addr) {
        rp.setSourceAddress(addr);
    }

    /**
     * return the actual size of the LowPanPacket payload
     *
     * @return number of bytes in this LowPanPacket that are payload (non-header)
     */
    public int getPayloadSize() {
        return lppPayloadSize;
    }

    /**
     * set the payload size in this LowPanPacket
     *
     * @param payloadSize number of bytes that are payload
     */
    public void setPayloadSize(int payloadSize) {
        this.lppPayloadSize = payloadSize;
    }

    /**
     * return the length of the LowPanPacket header
     *
     * @return actual length of the LowPan header
     */
    public int getHeaderLength() {
        int len = 0;

        if (isMeshed()) len += 1 + destLen + origLen;
        if (isBCast()) len += 2;
        if (isFragged()) {
            len += LowPanHeader.MAX_FRAGMENTATION_HEADER_LENGTH;
            if (isFirstFrag()) len -= 1;
        }
        len += (isExtendedProtocol()) ? 2 : 1;
        return len;
    }

    /**
     * determine if this packet uses the extended protocol field
     *
     * @return true if we are using SPOT protocol numbers
     */
    public boolean isExtendedProtocol() {
        return extendedProtocol;
    }

    /**
     * define this packet to use the extended protocol byte
     *
     * @param extendedProtocol true if this is a SPOT protocol number
     */
    public void setExtendedProtocol(boolean extendedProtocol) {
        this.extendedProtocol = extendedProtocol;
    }

    /**
     * return the starting offset of the payload section of the packet
     *
     * @return starting offset for data within this packet
     */
    public int getLppPayloadOffset() {
        return lppPayloadOffset;
    }

    /**
     * create a string representation of this Object
     *
     * @return this object as a string
     */
    public String toString() {
        StringBuffer sb = new StringBuffer(150);
        sb.append("LPP orig: ");
        sb.append(IEEEAddress.toDottedHex(getOriginatorAddress()));
        sb.append(" fdest: ");
        sb.append(IEEEAddress.toDottedHex(getFDestinationAddress()));
        sb.append(" hopsLeft: ");
        sb.append(getHopsLeft());
        sb.append(" ");
        sb.append(rp.toString());
        return sb.toString();
    }
}
