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
/*
 * Copyright (C) 2009  Daniel van den Akker	(daniel.vandenakker@ua.ac.be)
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 *
 * TinySPOTComm
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 only, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package com.sun.spot.peripheral.radio;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.sun.spot.peripheral.SpotFatalException;
import com.sun.spot.util.CRC;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Utils;


/**
 * Represents an I802.15.4 radio packet.<br><br>
 * <p/>
 * Because the standard allows variable length and format packets according
 * to the purpose and data content of the packet, this class's apparent length
 * and contents varies also. For more details about the packet formats, see the first
 * reference below.<br><br>
 * <p/>
 * To obtain RadioPackets, call one of {@link #getAckPacket()}, {@link #getBroadcastPacket(int)},
 * {@link #getDataPacket(int)}.
 * <p/>
 * Once a packet has been received from the physical radio, clients - such as the MAC layer -
 * should call {@link #decodeFrameControl()} to decode the MAC header. Until this is done,
 * accessors for addresses, PAN id and other information will not return correct values.
 * <p/>
 * { @see <a href="http://standards.ieee.org/getieee802/index.html">http://standards.ieee.org/getieee802/index.html</a> }
 * <p/>
 * Changed by Daniel van den Akker 10/2008 :
 * -extended support for 16-bit adressing
 * -added automatic 64 <-> 16bit address conversions for compatibilty with the rest of the SunSPOT stack
 */
public class RadioPacket {

    /**
     * @author Daniel van den Akker
     * Selects 16-bit addressing
     */
    public static final int ADDR_16 = 0;
    /**
     * @author Daniel van den Akker
     * Selects 64-bit addressing
     */
    public static final int ADDR_64 = 1;


    /**
     * The total number of bytes that may be sent in a single packet, including the MAC header
     */
    public static final int MAX_DATA_LENGTH = 125;
    private static final int BUFFER_SIZE = MAX_DATA_LENGTH + 1 /* for length byte */ + 2 /* for FCS */;

    private static final int MAX_HEADER_LENGTH = 23; // inter-pan with 64 bit addresses

    /**
     * The maximum number of bytes of data that will fit into this packet after allowing
     * for the MAC layer header.
     */
    public static final int MIN_PAYLOAD_LENGTH = MAX_DATA_LENGTH - MAX_HEADER_LENGTH;

    // Offsets applicable to all packet types
    private static final int LENGTH_OFFSET = 0;
    private static final int FCF_OFFSET = 1;
    private static final int DSN_OFFSET = 3;

    // See discussion under getLinkQuality
    private static final int LINK_QUALITY_STEPS = 255;
    private static final int CORRELATION_LOW = 50;
    private static final int CORRELATION_HIGH = 110;

    private static final int FRAME_TYPE = 7;
    private static final int FRAME_TYPE_DATA = 1;
    private static final int FRAME_TYPE_ACK = 2;
    static final int ACK_REQUEST = 1 << 5;
    private static final int INTRA_PAN = 1 << 6;
    private static final int DST_ADDR_BITS = 3 << 10;
    private static final int DST_ADDR_NONE = 0;
    private static final int DST_ADDR_16 = 2 << 10;
    private static final int DST_ADDR_64 = 3 << 10;
    private static final int SRC_ADDR_BITS = 3 << 14;
    private static final int SRC_ADDR_NONE = 0;
    private static final int SRC_ADDR_16 = 2 << 14;
    private static final int SRC_ADDR_64 = 3 << 14;


    /*
     private static int[] crc_table = new int[256];
     make_crc_table();
     */

    // Offsets whose location varies according to packet type
    private int destinationPanOffset;
    private int destinationAddressOffset;
    private int sourceAddressOffset;
    private int payloadOffset;

    private boolean dsnOK;

    byte[] buffer;
    int rssi;
    int corr;
    long timestamp;

    /**
     * Answer a radio packet preformatted for sending data.
     *
     * @param address_mode the address mode used: must be one of RadioPacket.ADDR_16 or RadioPacket.ADDR_64
     *                     the selected address mode is used for both sender and receiver address
     *                     Added by Daniel van den Akker: 10/2008
     * @return -- the RadioPacket.
     */
    public static RadioPacket getDataPacket(int address_mode) {
        RadioPacket result = new RadioPacket();
        result.initAsData(address_mode);
        return result;
    }

    /**
     * Answer a radio packet preformatted for sending data.
     * address mode used is 64 bits
     *
     * @return -- the RadioPacket.
     */
    public static RadioPacket getDataPacket() {
        return getDataPacket(ADDR_16);
    }

    /**
     * Answer a radio packet preformatted for sending ACKs.
     *
     * @return -- the RadioPacket.
     */
    public static RadioPacket getAckPacket() {
        RadioPacket result = new RadioPacket();
        result.initAsAck();
        return result;
    }

    /**
     * Answer a radio packet preformatted for broadcasting intra-PAN. Extra-PAN
     * broadcasting is not currently formatted.
     *
     * @param address_mode the address mode used: must be one of RadioPacket.ADDR_16 or RadioPacket.ADDR_64
     *                     the selected address mode is used for both sender and receiver address
     *                     Added by Daniel van den Akker: 10/2008
     * @return -- the RadioPacket.
     */
    public static RadioPacket getBroadcastPacket(int address_mode) {
        RadioPacket result = new RadioPacket();
        result.initAsBroadcast(address_mode);
        return result;
    }

    /**
     * Answer a radio packet preformatted for broadcasting intra-PAN. Extra-PAN
     * broadcasting is not currently formatted.
     *
     * @return -- the RadioPacket.
     */
    public static RadioPacket getBroadcastPacket() {
        return getBroadcastPacket(ADDR_16);
    }

    /**
     * Private to force use of the static methods
     */
    private RadioPacket() {
        this.buffer = new byte[BUFFER_SIZE];
    }

    private void initAsData(int address_mode) {
        //Added for TinySPOTComm project : allow 16-bit addressed data-frames
        if ((address_mode & ADDR_64) == ADDR_64) {
 //           System.out.println("Setting frame control to 64");
            setFrameControl(FRAME_TYPE_DATA | ACK_REQUEST | INTRA_PAN | DST_ADDR_64 | SRC_ADDR_64);
        } else {

   //         System.out.println("Setting frame control to 16");
            setFrameControl(FRAME_TYPE_DATA | ACK_REQUEST | INTRA_PAN | DST_ADDR_16 | SRC_ADDR_16);
        }

        setOffsets();
        setLength(payloadOffset - 1); // subtract one for length byte
    }

    private void setOffsets() {
        int frameControl = getFrameControl();
        if (isAck()) {
            destinationPanOffset = -1;
            destinationAddressOffset = -1;
            sourceAddressOffset = -1;
            payloadOffset = -1;
        } else if (isData()) {
            if ((frameControl & INTRA_PAN) == 0) {
                throw new IllegalStateException("Inter-pan communication not supported");
            }
            int addrMode = frameControl & DST_ADDR_BITS;
            switch (addrMode) {
                case DST_ADDR_NONE:
                    destinationPanOffset = -1;
                    destinationAddressOffset = -1;
                    sourceAddressOffset = 4;
                    break;
                case DST_ADDR_64:
                    destinationPanOffset = 4;
                    destinationAddressOffset = 6;
                    sourceAddressOffset = 14;
                    break;
                case DST_ADDR_16:
                    destinationPanOffset = 4;
                    destinationAddressOffset = 6;
                    sourceAddressOffset = 8;
                    break;
                default:
                    throw new IllegalStateException("Unsupported dest addr mode " + addrMode);
            }

            addrMode = frameControl & SRC_ADDR_BITS;
            switch (addrMode) {
                case SRC_ADDR_NONE:
                    payloadOffset = sourceAddressOffset;
                    sourceAddressOffset = -1;
                    break;
                case SRC_ADDR_64:
                    payloadOffset = sourceAddressOffset + 8;
                    break;
                case SRC_ADDR_16:
                    payloadOffset = sourceAddressOffset + 2;
                    break;
                default:
                    throw new IllegalStateException("Unsupported src addr mode " + addrMode);
            }
        }
    }

    private void initAsBroadcast(int address_mode) {
        //Added for TinySPOTComm project : allow broadcast packets with 16-bit source addresses
        if ((address_mode & ADDR_64) == ADDR_64) {
            //System.out.println("Setting frame control to 64");
            setFrameControl(FRAME_TYPE_DATA | INTRA_PAN | DST_ADDR_16 | SRC_ADDR_64);

        } else {
            //System.out.println("BDC Setting frame control to 16");
            setFrameControl(FRAME_TYPE_DATA | INTRA_PAN | DST_ADDR_16 | SRC_ADDR_16);
        }

        setOffsets();
        setDestinationAddress(0xFFFF);
        setLength(payloadOffset - 1); // subtract one for length byte
    }

    private void initAsAck() {
        setFrameControl(FRAME_TYPE_ACK);
        setLength(3);
        setOffsets();
    }

    /**
     * Answer the destination address of this packet. Will throw an IllegalStateException
     * if this is an ACK packet and therefore has no destination. Will answer 0xFFFF (as per
     * the I802.15.4 spec) for a broadcast packet.
     *
     * @return -- the address
     */
    public long getDestinationAddress() {
        if (destinationAddressOffset == -1) {
            throw new IllegalStateException("Field not valid for this packet");
        }
        long result = 0;
        //Added for TinySPOTComm project : perform automatic 16 -> 64 bit conversion
        if ((getFrameControl() & DST_ADDR_BITS) == DST_ADDR_16) {
            result = IEEEAddress.To64Bit(getShortAt(destinationAddressOffset));
        } else {
            result = getLongAt(destinationAddressOffset);
        }
        return result;
    }

    /**
     * Set the destination address for the packet. If the packet can't have a destination,
     * for example an ACK packet, throw IllegalStateException.
     *
     * @param addr - the address to set. If addressing mode is 16 bit, the lower 2 bytes are used as address
     */
    public void setDestinationAddress(long addr) {
        if (destinationAddressOffset == -1) {
            throw new IllegalStateException("Field not valid for this packet");
        }
        if ((getFrameControl() & DST_ADDR_BITS) == DST_ADDR_16) {
            //Added for TinySPOTComm project : perform automatic 64 -> 16 bit conversion
     //     System.out.println("TinySPOTComm ");
            setShortAt(destinationAddressOffset, IEEEAddress.To16Bit(addr));
        } else {
            setLongAt(destinationAddressOffset, addr);
        }
    }

    /**
     * Answer the source address of this packet. Will throw an IllegalStateException
     * if this is an ACK packet and therefore has no source.
     *
     * @return -- the address
     */
    public long getSourceAddress() {
        if (sourceAddressOffset == -1) {
            throw new IllegalStateException("Field not valid for this packet");
        }
        //Added for TinySPOTComm project : perform automatic 16 -> 64 bit conversion
        long result = 0;
        if ((getFrameControl() & DST_ADDR_BITS) == DST_ADDR_16) {
            result = IEEEAddress.To64Bit(getShortAt(sourceAddressOffset));
        } else {
            result = getLongAt(sourceAddressOffset);
        }
        return result;
    }

    /**
     * Set the source address for the packet. If the packet can't have a source,
     * for example an ACK packet, throw IllegalStateException.
     *
     * @param addr - the address to set. If addressing mode is 16 bit, the lower 2 bytes are used as address
     */
    public void setSourceAddress(long addr) {

        if (sourceAddressOffset == -1) {
            throw new IllegalStateException("Field not valid for this packet");
        }
        //Added for TinySPOTComm project : perform automatic 64 -> 16 bit conversion
        if ((getFrameControl() & SRC_ADDR_BITS) == SRC_ADDR_16) {
       //     System.out.println("TinySPOTComm ");
            setShortAt(sourceAddressOffset, IEEEAddress.To16Bit(addr));
        } else {
            setLongAt(sourceAddressOffset, addr);
        }
    }

    /**
     * Answer the source pan ID of this packet. Throw an IllegalStateException
     * if the receiver is an ACK packet and cannot have a pan ID.
     *
     * @return -- the pan ID
     */
    public int getSourcePanID() {
        return getDestinationPanID();
    }

    /**
     * Answer the destination pan ID of this packet. Throw an IllegalStateException
     * if the receiver is an ACK packet and cannot have a pan ID.
     *
     * @return -- the pan ID
     */
    public int getDestinationPanID() {
        if (destinationPanOffset == -1) {
            throw new IllegalStateException("Field not valid for this packet");
        }
        return getShortAt(destinationPanOffset);
    }

    /**
     * Set the destination pan ID of this packet. Throw an IllegalStateException
     * if the receiver is an ACK packet and cannot have a pan ID.
     *
     * @param id the pan id to set
     */
    public void setDestinationPanID(int id) {
        if (destinationPanOffset == -1) {
            throw new IllegalStateException("Field not valid for this packet");
        }
        setShortAt(destinationPanOffset, id);
    }

    /**
     * Answer the DSN (as defined by the I802.15.4 standard) of this packet.
     *
     * @return -- the DSN
     */
    public byte getDataSequenceNumber() {
        return buffer[DSN_OFFSET];
    }

    /**
     * Answer the frame control field (16 bits) of this packet.
     *
     * @return - the FCF
     */
    public int getFrameControl() {
        return getShortAt(FCF_OFFSET);
    }

    void setFrameControl(int fc) {
        setShortAt(FCF_OFFSET, fc);
    }

    /**
     * Get byte from MAC payload
     *
     * @param offset -- relative to bottom of MAC payload
     * @return -- the byte value
     */
    public byte getMACPayloadAt(int offset) {
        return buffer[offset + getPayloadOffset()];
    }

    /**
     * Set byte in MAC payload
     *
     * @param offset - relative to bottom of MAC payload
     * @param value  the value to set in the payload
     */
    public void setMACPayloadAt(int offset, byte value) {
        buffer[offset + getPayloadOffset()] = value;
    }

    /**
     * Set length of MACPayload
     *
     * @param macPayloadLength - max 104 bytes
     * @throws IOException
     */
    public void setMACPayloadLength(int macPayloadLength) {
        if (macPayloadLength > getMaxMacPayloadSize()) {
            throw new IllegalArgumentException("MAC payload size of " + macPayloadLength + " is too big");
        }
        setLength(macPayloadLength + getPayloadOffset() - 1); // allow one for length byte
    }

    /**
     * Link Quality Indication (LQI) is a characterization of the quality of a
     * received packet. Its value is computed from the CORR, correlation value. The
     * LQI ranges from 0 (bad) to 255 (good).
     *
     * @return linkQuality - range 0 to 0xFF
     */
    public int getLinkQuality() {
        /* Chipcon allow for calculating the LQI two ways.
           * The first is the RSSI, which is in the range -128 to 127. So
           * we could just add 128 to that.
           * The second is the correlation value, which Chipcon seem to favour.
           * Hence the formula below. In theory we should determine the constants
           * empirically, but for now, they're based on Chipcon's assertion that
           * the approximate range of likely values for corr is 50 to 110.
           *
           */
        return ((corr - CORRELATION_LOW) * LINK_QUALITY_STEPS) / (CORRELATION_HIGH - CORRELATION_LOW);
    }

    /**
     * CORR measures the average correlation value of the first 4 bytes of the packet
     * header. A correlation value of ~110 indicates a maximum quality packet while a
     * value of ~50 is typically the lowest quality packet detectable by the SPOT's
     * receiver.
     *
     * @return - correlation value
     * @see com.sun.spot.peripheral.radio.RadioPacket#getRssi()
     * @see com.sun.spot.peripheral.radio.RadioPacket#getLinkQuality()
     */
    public int getCorr() {
        return corr;
    }

    /**
     * RSSI (received signal strength indicator) measures the strength (power) of the
     * signal for the packet. It ranges from +60 (strong) to -60 (weak). To convert it
     * to decibels relative to 1 mW (= 0 dBm) subtract 45 from it, e.g. for an RSSI of
     * -20 the RF input power is approximately -65 dBm.
     *
     * @return - RSSI value
     * @see com.sun.spot.peripheral.radio.RadioPacket#getCorr()
     * @see com.sun.spot.peripheral.radio.RadioPacket#getLinkQuality()
     */
    public int getRssi() {
        return rssi;
    }

    void setMaxQuality() {
        rssi = 60;
        corr = 110;
    }

    /**
     * Answer whether the DSN received implies that packets were missed
     *
     * @return true if the sequence number is correct, false if packets were missed (incorrect sequence number)
     */
    public boolean isSeqOK() {
        return dsnOK;
    }

    public void setSeqOK(boolean dsnOK) {
        this.dsnOK = dsnOK;
    }

    /**
     * Get length of MACPayload
     *
     * @return length - max 104 bytes
     */
    public int getMACPayloadLength() {
        return getLength() - getPayloadOffset() + 1; // add 1 because getLength doesn't include length byte
    }

    int getLength() {
        return buffer[LENGTH_OFFSET] & 0xFF;
    }

    void setLength(int l) {
        buffer[LENGTH_OFFSET] = (byte) l;
    }

    /*
      * Work out what kind of packet this received packet is and initialise its offsets appropriately
      */

    public void decodeFrameControl() {
        if (getLength() == 0) {
            throw new SpotFatalException("[RadioPacket] decodeFrameControl decoding a zero-length packet");
        }
        setOffsets();
    }

    /**
     * Make this packet an exact copy of another
     *
     * @param otherRP the packet to copy from
     */
    public void copyFrom(RadioPacket otherRP) {
        this.corr = otherRP.corr;
        this.rssi = otherRP.rssi;
        this.dsnOK = otherRP.dsnOK;
        this.timestamp = otherRP.timestamp;
        this.destinationPanOffset = otherRP.destinationPanOffset;
        this.destinationAddressOffset = otherRP.destinationAddressOffset;
        this.sourceAddressOffset = otherRP.sourceAddressOffset;
        this.payloadOffset = otherRP.payloadOffset;
        for (int i = 0; i <= otherRP.getLength(); i++) {
            this.buffer[i] = otherRP.buffer[i];
        }
    }

    public void writeOnto(DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeByte(corr);
        dataOutputStream.writeByte(rssi);
        dataOutputStream.writeLong(timestamp);
        dataOutputStream.write(buffer, 0, getLength() + 1);
    }

    public RadioPacket readFrom(DataInputStream dataInputStream) throws IOException {
        corr = dataInputStream.readByte();
        rssi = dataInputStream.readByte();
        timestamp = dataInputStream.readLong();
        dataInputStream.read(buffer, 0, 1);
        dataInputStream.read(buffer, 1, buffer[0]);
        decodeFrameControl();
        return this;
    }

    public RadioPacket readWithoutTimestampFrom(DataInputStream dataInputStream) throws IOException {
        corr = dataInputStream.readByte();
        rssi = dataInputStream.readByte();
        dataInputStream.read(buffer, 0, 1);
        dataInputStream.read(buffer, 1, buffer[0]);
        decodeFrameControl();
        return this;
    }

    public int writeOnto(byte[] outputBuffer, int startingOffset) {
        outputBuffer[startingOffset++] = (byte) corr;
        outputBuffer[startingOffset++] = (byte) rssi;
        Utils.writeBigEndLong(outputBuffer, startingOffset, timestamp);
        startingOffset += 8;
        System.arraycopy(buffer, 0, outputBuffer, startingOffset, getLength() + 1);
        return getLength() + 11; // 1 for corr, 1 for rssi, 8 for timestamp, 1 for length byte
    }

    public int writeWithoutTimestampOnto(byte[] outputBuffer, int startingOffset) {
        outputBuffer[startingOffset++] = (byte) corr;
        outputBuffer[startingOffset++] = (byte) rssi;
        System.arraycopy(buffer, 0, outputBuffer, startingOffset, getLength() + 1);
        return getLength() + 3; // 1 for corr, 1 for rssi, 1 for length byte
    }

    public RadioPacket readFrom(byte[] inputBuffer, int startingOffset) {
        corr = inputBuffer[startingOffset++];
        rssi = inputBuffer[startingOffset++];
        timestamp = Utils.readBigEndLong(inputBuffer, startingOffset);
        startingOffset += 8;
        System.arraycopy(inputBuffer, startingOffset, buffer, 0, 1);
        System.arraycopy(inputBuffer, startingOffset + 1, buffer, 1, buffer[0]);
        decodeFrameControl();
        return this;
    }

    /**
     * Answer whether this packet is an ACK packet (i.e. not data or broadcast)
     *
     * @return -- boolean
     */
    public boolean isAck() {
        return ((getFrameControl() & FRAME_TYPE) == FRAME_TYPE_ACK);
    }

    /**
     * Answer whether this packet is a data packet (i.e. not an ack packet)
     *
     * @return -- boolean
     */
    public boolean isData() {
        return ((getFrameControl() & FRAME_TYPE) == FRAME_TYPE_DATA);
    }

    /**
     * Check whether this packet wants an acknowledgement
     *
     * @return -- true if ack required
     */
    public boolean ackRequest() {
        return ((getFrameControl() & ACK_REQUEST) != 0);
    }

    /**
     * @param macDSN
     */
    void setDSN(byte macDSN) {
        buffer[DSN_OFFSET] = macDSN;
    }

    /**
     * Answer the little end int value corresponding to four bytes at a given offset
     * within the MAC payload.
     *
     * @param macPayloadOffset -- the offset within the MAC payload
     * @return -- int
     */
    public int getMACPayloadIntAt(int macPayloadOffset) {
        return getIntAt(getPayloadOffset() + macPayloadOffset);
    }

    /**
     * Answer the big end int value corresponding to four bytes at a given offset
     * within the MAC payload.
     *
     * @param macPayloadOffset -- the offset within the MAC payload
     * @return -- int
     */
    public int getMACPayloadBigEndIntAt(int macPayloadOffset) {
        return Utils.readBigEndInt(buffer, getPayloadOffset() + macPayloadOffset);
    }

    private long getLongAt(int offset) {
        return Utils.readLittleEndLong(buffer, offset);
    }

    private int getIntAt(int offset) {
        return Utils.readLittleEndInt(buffer, offset);
    }

    /**
     * Answer the little end long value corresponding to eight bytes at a given offset
     * within the MAC payload.
     *
     * @param macPayloadOffset -- the offset within the MAC payload
     * @return -- long
     */
    public long getMACPayloadLongAt(int macPayloadOffset) {
        return getLongAt(getPayloadOffset() + macPayloadOffset);
    }

    /**
     * Answer the big end long value corresponding to eight bytes at a given offset
     * within the MAC payload.
     *
     * @param macPayloadOffset -- the offset within the MAC payload
     * @return -- long
     */
    public long getMACPayloadBigEndLongAt(int macPayloadOffset) {
        return Utils.readBigEndLong(buffer, getPayloadOffset() + macPayloadOffset);
    }

    private void setLongAt(int offset, long value) {
        Utils.writeLittleEndLong(buffer, offset, value);
    }

    /**
     * Fill the four bytes at a given offset within the MAC payload with the
     * little end int value provided.
     *
     * @param macPayloadOffset -- the offset within the MAC payload
     * @param value            -- the int value to store in four bytes
     */
    public void setMACPayloadIntAt(int macPayloadOffset, int value) {
        Utils.writeLittleEndInt(buffer, getPayloadOffset() + macPayloadOffset, value);
    }

    /**
     * Fill the four bytes at a given offset within the MAC payload with the
     * big end int value provided.
     *
     * @param macPayloadOffset -- the offset within the MAC payload
     * @param value            -- the int value to store in four bytes
     */
    public void setMACPayloadBigEndIntAt(int macPayloadOffset, int value) {
        Utils.writeBigEndInt(buffer, getPayloadOffset() + macPayloadOffset, value);
    }

    /**
     * Fill the eight bytes at a given offset within the MAC payload with the
     * little end long value provided.
     *
     * @param macPayloadOffset -- the offset within the MAC payload
     * @param value            -- the long value to store in eight bytes
     */
    public void setMACPayloadLongAt(int macPayloadOffset, long value) {
        setLongAt(getPayloadOffset() + macPayloadOffset, value);
    }

    private int getShortAt(int offset) {
        return Utils.readLittleEndShort(buffer, offset);
    }

    /**
     * Fill the eight bytes at a given offset within the MAC payload with the
     * big end long value provided.
     *
     * @param macPayloadOffset -- the offset within the MAC payload
     * @param value            -- the long value to store in eight bytes
     */
    public void setMACPayloadBigEndLongAt(int macPayloadOffset, long value) {
        Utils.writeBigEndLong(buffer, getPayloadOffset() + macPayloadOffset, value);
    }

    /**
     * Answer the little end short value corresponding to two bytes at a given offset
     * within the MAC payload.
     *
     * @param macPayloadOffset -- the offset within the MAC payload
     * @return -- short
     */
    public int getMACPayloadShortAt(int macPayloadOffset) {
        return getShortAt(getPayloadOffset() + macPayloadOffset);
    }

    /**
     * Answer the big end short value corresponding to two bytes at a given offset
     * within the MAC payload.
     *
     * @param macPayloadOffset -- the offset within the MAC payload
     * @return -- short
     */
    public int getMACPayloadBigEndShortAt(int macPayloadOffset) {
        return Utils.readBigEndShort(buffer, getPayloadOffset() + macPayloadOffset);
    }

    /**
     * Fill the two bytes at a given offset within the MAC payload with the
     * short value provided.
     *
     * @param macPayloadOffset -- the offset within the MAC payload
     * @param value            -- the short value to store in two bytes
     */
    public void setMACPayloadShortAt(int macPayloadOffset, int value) {
        setShortAt(getPayloadOffset() + macPayloadOffset, value);
    }

    private void setShortAt(int offset, int value) {
        Utils.writeLittleEndShort(buffer, offset, value);
    }

    /**
     * Fill the two bytes at a given offset within the MAC payload with the
     * big end short value provided.
     *
     * @param macPayloadOffset -- the offset within the MAC payload
     * @param value            -- the short value to store in two bytes
     */
    public void setMACPayloadBigEndShortAt(int macPayloadOffset, int value) {
        Utils.writeBigEndShort(buffer, getPayloadOffset() + macPayloadOffset, value);
    }

    private void setFCS(short s) {
        setShortAt(getLength() + 1, s);
    }

    /**
     * Answer the frame check sequence for this radio packet.
     *
     * @return -- short value
     */
    public short getFCS() {
        return (short) getShortAt(getLength() + 1);
    }

    /**
     * Calculate a frame check sequence for this packet -- not normally required
     * for the CC2420 radio. This exists here to support MAC operation over devices
     * that don't calculate the FCS in hardware (for example, the serial line)
     */
    public void calculateAndSetFCS() {
        short crc = CRC.crc(buffer, 1, getLength());
        setFCS(crc);
    }

    /**
     * Answer whether this packet was received with a correct FCS
     *
     * @return -- boolean
     */
    public boolean isFCSValid() {
        short crc = CRC.crc(buffer, 1, getLength());
        return crc == getFCS();
    }


    /**
     * Answer the size of the payload in this packet.
     *
     * @return -- number of bytes
     */
    public int getMaxMacPayloadSize() {
        return MAX_DATA_LENGTH - getPayloadOffset() + 1; // add 1 because MAX_DATA_LENGTH doesn't include length byte
    }

    int getPayloadOffset() {
        if (payloadOffset == -1) {
            throw new IllegalStateException("This packet cannot have a payload");
        }
        return payloadOffset;
    }

    void dump() {
        byte[] tempBuff = new byte[buffer[LENGTH_OFFSET] + 1];
        System.arraycopy(buffer, 0, tempBuff, 0, tempBuff.length);
        Utils.log(Utils.stringify(tempBuff));
        Utils.log("src addr: " + Long.toString(getSourceAddress(), 16) + " [offset=" + sourceAddressOffset + "]");
        Utils.log("dest addr: " +
                (destinationAddressOffset == -1 ?
                        "(none)" :
                        Long.toString(getDestinationAddress(), 16) + " [offset=" + destinationAddressOffset + "]"));
        Utils.log("");
    }

    /* (non-Javadoc)
      * @see java.lang.Object#toString()
      */

    public String toString() {
        StringBuffer result = new StringBuffer("RP: ");
        if (isAck()) result.append("ack ");
        if (isData()) result.append("dat ");
        result.append("seq ");
        result.append(getDataSequenceNumber());
        result.append(" (");
        result.append(getLength());
        result.append("bytes) from ");
        if (!isAck()) {
            result.append(IEEEAddress.toDottedHex(getSourceAddress()));
            result.append(" to ");
            result.append(IEEEAddress.toDottedHex(getDestinationAddress()));
            result.append(" [");
            for (int i = 0; i < getMACPayloadLength(); i++) {
                result.append((i == 0 ? "" : " "));
                result.append(Integer.toHexString((0xFF & getMACPayloadAt(i))));
            }
            result.append("]");
        }
        return result.toString();
    }

    public long getTimestamp() {
        return timestamp;
    }
}
