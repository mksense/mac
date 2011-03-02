/*
 * Copyright 2007-2008 Sun Microsystems, Inc. All Rights Reserved.
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

/**
 * LowPanHeader.java
 *
 * @author Pete St. Pierre
 *         <p/>
 *         This class provides functionality for constructing a LowPanHeader.  Use this class to set
 *         all the parameters associated with a packet.  This object can be passed to
 *         LowPanPacket.writeHeaderAndPayload()
 */
public class LowPanHeader {
    // Useful information that needs to be set when a packet is being generated.
    // Flags above (meshed/bCast/fragged) do double duty to determine which of these is used
    private boolean meshed;
    private boolean fragged;
    private boolean bCast;
    private int hops;
    private int bCastSeqNo;
    private int fragType;
    private int fragTag;
    private int fragSize;
    private int fragOffset;
    private int protocolFamily;
    private int protocolNo;
    private int origLen;
    private int destLen;
    private long outgoingOriginatorAddress;
    private long outgoingDestinationAddress;

    /**
     * Marks a packet as unfragmented
     */
    protected static final byte UNFRAGMENTED = 0;
    /**
     * Marks a packet as the last Fragment
     */
    protected static final byte LAST_FRAGMENT = 1;
    /**
     * Marks a packet as a First Fragment
     */
    protected static final byte FIRST_FRAGMENT = 2;
    /**
     * Marks a packet as an Interior Fragment
     */
    protected static final byte INTERIOR_FRAGMENT = 3;


    /**
     * Dispatch headers are used to determine how to parse each piece of a LowPan
     * header.
     */
    /**
     * Denotes this packet contains part of an IPv6 format packet at the higher level
     */
    public static final byte DISPATCH_LOWPAN_IPV6 = (byte) 0x41;
    /**
     * Denotes this packet contains part of an IPv6 packet that uses header compression
     */
    public static final byte DISPATCH_LOWPAN_HC1 = (byte) 0x42;
    /**
     * Denotes this packet is a LowPan Mesh Broadcast packet
     */
    public static final byte DISPATCH_LOWPAN_BC0 = (byte) 0x50;
    /**
     * Denotes the following header field is not in the scope of the LowPan spec
     * We use this next byte for our SPOT protocol number
     */
    public static final byte DISPATCH_ESC = (byte) 0x7f;
    /**
     * Denotes the following header field is not in the scope of the LowPan spec
     * We use this next byte for our SPOT protocol number
     */
    public static final byte DISPATCH_SPOT = DISPATCH_ESC;

    /**
     * Denotes the protocol number is handled within the protocol family.
     * Should be used with DISPATCH_SPOT protocol families
     */
    public static final byte FAMILY_DEFINED_PROTO = -1;
    /**
     * Denotes what follows is a some part of a LowPan packet
     */
    protected static final byte DISPATCH_FRAG = (byte) 0xc0;
    /**
     * Denotes what follows is a LowPan Mesh header
     */
    protected static final byte DISPATCH_MESH = (byte) 0x80;

    /**
     * Max length of the protocol header
     */
    protected static final byte MAX_PROTOCOL_HEADER_LENGTH = 2;

    /**
     * Maximum size of an Unfragmenented payload
     */
    protected static final int MAX_UN_FRANG_BDC_MSG = 114;

    /**
     * Size of a Broadcast Header
     */
    protected static final byte BROADCAST_HEADER_LENGTH = 2;
    /**
     * Maximum size of a fragmentation header.  May be only 3 for a First Fragment
     */
    protected static final byte MAX_FRAGMENTATION_HEADER_LENGTH = 5;
    /**
     * Maximum size of a Mesh Header
     */
    protected static final byte MAX_MESH_HEADER_LENGTH = 17;

    /**
     * Unfragmented header is 17B Mesh, 2B Broadcast, 2B protocol
     */
    public static final byte MAX_UNFRAG_HEADER_LENGTH = 21;


    /**
     * Creates a new instance of LowPanHeader
     */
    public LowPanHeader() {
        hops = bCastSeqNo = fragType = fragSize = fragOffset = 0;
        outgoingOriginatorAddress = 0;
        outgoingDestinationAddress = 0;
        destLen = origLen = 8;
        hops = ILowPan.DEFAULT_HOPS;
    }

    /**
     * Calculates the number of bytes this LowPanHeader will take
     *
     * @return returns the length, in number of bytes, of the LowPanHeader
     */
    public int getLength() {
        int len = 0;  // Protocol Family & Number


        if (isMeshed()) len += (1 + destLen + origLen);
        len += (isExtendedHops()) ? 1 : 0;
        if (isBCast()) len += 2;
        if (isFragged()) {
            len += MAX_FRAGMENTATION_HEADER_LENGTH;

            if (isFirstFrag()) len -= 1;  // first fragments are 1 byte less (no offset byte)
        }
        len += (isExtendedProtocol()) ? 2 : 1;

        return len;
    }

    /**
     * Checks the header to see if this header belongs to a packet that is the
     * first fragment of a series.
     *
     * @return true if this header belongs to the first fragment of series
     */
    public boolean isFirstFrag() {
        return (fragType == FIRST_FRAGMENT);

    }

    /**
     * Defines whether this header uses the extended protocol field
     *
     * @return true if this is an extended protocol header
     */
    public boolean isExtendedProtocol() {
        return ((protocolFamily & DISPATCH_ESC) == DISPATCH_ESC);
    }

    /**
     * return the number of hops this header is set to take
     *
     * @return the number of hops for the associated packet
     */
    public int getOutgoingHops() {
        return hops;
    }

    /**
     * set the number of hops a packet associated with this header will make
     *
     * @param outgoingHops the number of hops to set in this header
     */
    public void setOutgoingHops(int outgoingHops) {
        this.hops = outgoingHops;
    }

    private boolean isExtendedHops() {
        return (hops > 14);
    }

    /**
     * return the number of hops a mesh broadcast with this header will make
     *
     * @return the number of hops a mesh broadcast with this header will make
     */
    public int getOutgoingBCastSeqNo() {
        return (int) (bCastSeqNo & 0xff);
    }

    /**
     * Sets the number of hops a mesh broadcast with this header will make
     *
     * @param outgoingBCastSeqNo the number of hops a mesh broadcast with this header should make
     */
    public void setOutgoingBCastSeqNo(int outgoingBCastSeqNo) {
        this.bCastSeqNo = outgoingBCastSeqNo;
    }

    /**
     * return the current fragment type (UNFRAGMENTED, FIRST_FRAGMENT, INTERIOR_FRAGMENT, LAST_FRAGMENT)
     *
     * @return the type of fragment this header is
     */
    public int getOutgoingFragType() {
        return fragType;
    }

    /**
     * set the type of this fragment
     *
     * @param outgoingFragType the type of fragment, (UNFRAGMENTED, FIRST_FRAGMENT, INTERIOR_FRAGMENT, LAST_FRAGMENT)
     */
    public void setOutgoingFragType(int outgoingFragType) {
        this.fragType = outgoingFragType;
    }

    /**
     * return the datagram tag associated with this fragment
     *
     * @return the datagram tag for this header, if it is a fragment
     */
    public int getOutgoingFragTag() {
        return fragTag;
    }

    /**
     * set the datagram tag for this header
     *
     * @param outgoingFragTag the new datagram tag for this header
     */
    public void setOutgoingFragTag(int outgoingFragTag) {
        this.fragTag = outgoingFragTag;
    }

    /**
     * return the total size of the packet associated with this fragment
     *
     * @return the total size of the datagram that the fragment this header is associated with should be
     */
    public int getOutgoingFragSize() {
        return fragSize;
    }

    /**
     * sets the size field of this fragment header.  Reflects the total size of the datagram when reassembled
     *
     * @param outgoingFragSize the total size of the datagram when reassembled
     */
    public void setOutgoingFragSize(int outgoingFragSize) {
        this.fragSize = outgoingFragSize;
    }

    /**
     * get the fragmentation offset field of the header, based on its location in the orginal datagram
     *
     * @return the offset in the total datagram for which the packet assocated with this header belongs
     */
    public byte getOutgoingFragOffset() {
        return (byte) (fragOffset & 0xff);
    }

    /**
     * set the offset in the fragmentation header for this header, based on where the associated packet belongs in the reassembled packet
     *
     * @param outgoingFragOffset the place within the reassembled packet ththe packets associated with this
     *                           header belongs
     */
    public void setOutgoingFragOffset(int outgoingFragOffset) {
        this.fragOffset = outgoingFragOffset;
    }

    /**
     * get the originator from the mesh field of this header
     *
     * @return the originating address of the packet associated with this header
     */
    public long getOutgoingOriginatorAddress() {
        return outgoingOriginatorAddress;
    }

    /**
     * set the originator field of the mesh header
     *
     * @param outgoingOriginatorAddress the address of the originator of the packet associated with this mesh header
     */
    public void setOutgoingOriginatorAddress(long outgoingOriginatorAddress) {
        this.outgoingOriginatorAddress = outgoingOriginatorAddress;
    }

    /**
     * get the final destination from the mesh field of this header
     *
     * @return the final destination address of the packet associated with this header
     */
    public long getOutgoingDestinationAddress() {
        return outgoingDestinationAddress;
    }

    /**
     * set the final destination field of the mesh header
     *
     * @param outgoingDestinationAddress the address of the final destination of the packet associated with this mesh header
     */
    public void setOutgoingDestinationAddress(long outgoingDestinationAddress) {
        this.outgoingDestinationAddress = outgoingDestinationAddress;
    }

    /**
     * get the protocol number from this header
     *
     * @return the protocol number of the packet associated with this header
     */
    public byte getProtocolNo() {
        return (byte) (protocolNo & 0xff);
    }

    /**
     * check whether this header contains a mesh field
     *
     * @return true if there is a mesh field in this header
     */
    public boolean isMeshed() {
        return meshed;
    }

    /**
     * set the flag indicating there is a mesh field associated with this header
     *
     * @param meshed true if there is a mesh field to be set in this header
     */
    public void setMeshed(boolean meshed) {
        this.meshed = meshed;
    }

    /**
     * check whether this header contains a fragmentation field
     *
     * @return true if there is fragmentation information in this header
     */
    public boolean isFragged() {
        return fragged;
    }

    /**
     * set the flag indicating this header contains fragmentation information
     *
     * @param fragged true if this header contains fragmentation information
     */
    public void setFragged(boolean fragged) {
        this.fragged = fragged;
    }

    /**
     * check to see if this header contains mesh broadcast information
     *
     * @return true if this header contains mesh broadcast information
     */
    public boolean isBCast() {
        return bCast;
    }

    /**
     * sets this packet to contain mesh broadcast information
     *
     * @param bCast true if this packet is to contain mesh broadcast header information
     */
    public void setBCast(boolean bCast) {
        this.bCast = bCast;
    }

    public byte getProtocolFamily() {
        return (byte) (protocolFamily & 0xff);
    }

    /**
     * set the protocol information in this header
     *
     * @param protocolFamily the family this protocol information relates to (IPv6, SPOT, etc.)
     * @param protocolNo     the protocol number of the packet associated with this header
     */
    public void setProtocolInfo(byte protocolFamily, byte protocolNo) {
        this.protocolFamily = protocolFamily;
        this.protocolNo = protocolNo;


    }


}
