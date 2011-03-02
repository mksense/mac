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

package com.sun.spot.util;

import com.sun.spot.peripheral.radio.SpotNameLookup;

public class IEEEAddress {

    private static final char[] hexDigits = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};


    /**
     * The Prefix used to reconstruct the original address from the 16bit hash
     */
    public static final long HASH_PREFIX = 0x00144F0100000000l;

    public static final String MAC_PREFIX = "0014.4F01.0000."; 

    private long address;

    /**
     * Convert a numeric address into a dotted-hex string
     *
     * @param address the address to convert
     * @return the dotted-hex string
     */
    public static String toDottedHex(long address) {
        char[] c = new char[19];
        for (int i = 60, j = 0; i >= 0; i -= 4, j++) {
            int digit = (int) (address >> i) & 0xF;
            c[j] = hexDigits[digit];
            if ((i % 16 == 0) && (i != 0)) {
                c[++j] = '.';
            }
        }
        return new String(c);
    }

    /**
     * Convert a string representation of the address to a numeric value.
     * <p/>
     * On the host computer, the string is first looked up in the keys of the property file spot.names
     * in the user's home directory. If present, the corresponding value is used in place of the string parameter.
     * <p/>
     * Then, the string is first assumed to be dotted hex (that's four four-digit hex numbers separated
     * by full stops). If that conversion fails, the string is parsed as a decimal number.
     *
     * @param ieeeAddressString the string to convert
     * @return the numeric value of the address
     */
    public static long toLong(String ieeeAddressString) {
        String phyAddr = SpotNameLookup.logicalToPhysical(ieeeAddressString);
        long result = parseDottedHex(phyAddr);
        try {
            result = result == -1 ? Long.parseLong(phyAddr) : result;
        } catch (NumberFormatException e) {
            if (ieeeAddressString.equals(phyAddr)) {
                throw new IllegalArgumentException("'" + ieeeAddressString + "' is not a valid address");
            } else {
                throw new IllegalArgumentException("'" + ieeeAddressString + "' (" + phyAddr + ") is not a valid address");
            }
        }
        if (result == 0 || result == -1) {
            throw new IllegalArgumentException("Reserved address: " + ieeeAddressString + " cannot be used here");
        }
        return result;
    }

    public IEEEAddress(long address) {
        this.address = address;
    }

    public IEEEAddress(String ieeeAddressString) {
        long result = toLong(ieeeAddressString);
        this.address = result;
    }

    /**
     * Generate a dotted hex string from an IEEE address
     *
     * @return a dotted hex string
     */
    public String asDottedHex() {
        return toDottedHex(address);
    }

    public long asLong() {
        return address;
    }

    public String toString() {
        return asDottedHex();
    }

    public boolean equals(Object anObject) {
        return anObject instanceof IEEEAddress &&
                ((IEEEAddress) anObject).address == address;
    }

    public int hashCode() {
        return (int) address;
    }

    /*
      * Parses addresses specified as (at most four) strings of (at most 4 hex
      * digits) separated by dots. See Wikipedia entry for EUI-64.
      */

    private static long parseDottedHex(String dottedHexAddress) {
        long result = 0;
        dottedHexAddress += ".";

        if (dottedHexAddress.length() != 20) {
            return -1;
        }

        for (int fromIndex = 0; fromIndex < 20; fromIndex += 5) {
            String hexBlock = dottedHexAddress.substring(fromIndex, fromIndex + 4);
            if (dottedHexAddress.charAt(fromIndex + 4) != '.') {
                return -1;
            }
            try {
                result = (result << 16) + Long.parseLong(hexBlock, 16);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return result;
    }

    /**
     * This method translates a long, representing a 64bit IEEE address to it's 16bit representation
     *
     * @param orig the original 64 bit address
     * @return the 16bit representation of the address
     */
    public static final short To16Bit(long orig) {
        //Check for broadcast address: broadcasts may not be translated
        if (orig == 0xFFFF)
            return (short) 0xFFFF;
        else
            return DoTo16BitTranslation(orig);
    }

    /**
     * This method does the actual 64 -> 16 bit translation and is to be provided by the implementing subclass.
     *
     * @param orig - the original 16 bit address. This address may not be the broadcast address.
     * @return - the 64 bit representation of the 16 bit address
     */
    protected static short DoTo16BitTranslation(long orig) {
        return (short) orig;
    }

    /**
     * This method translates an IEEEAddress to it's 16bit representation
     *
     * @param orig the original 64 bit address
     * @return the 16bit representation of the address
     */
    public static final short To16Bit(IEEEAddress orig) {
        return To16Bit(orig.asLong());
    }

    /**
     * This method translates a String representation of a 64 IEEE address to it's 16bit representation
     *
     * @param orig the original 64 bit address
     * @return the 16bit representation of the address
     */
    public static final short To16Bit(String orig) {
        return To16Bit(new IEEEAddress(orig));
    }

    /**
     * This method converts a short, representing a 16bit address to it's 64bit counterpart.
     *
     * @param orig the original 16bit address
     * @return the 64 bit address
     */
    public static final long To64Bit(short orig) {
        //Check for broadcast address: broadcasts may not be translated
        //0xFFFF translates to -1 when casted to short
        //SunSPOT stack expects broadcasts to always be 16 bit
        if (orig == -1)
            return 0xFFFF;
        else
            return DoTo64BitTranslation(orig);
    }

    /**
     * This method does the actual 16 -> 64bit translation and is to be provided by the implementing subclass.
     *
     * @param orig - the original 16 bit address. This address may not be the broadcast address
     * @return - the 64 bit representation of the 16 bit address
     */
    protected static long DoTo64BitTranslation(short orig) {
        return HASH_PREFIX | (long) orig;
    }

    /**
     * This method converts an int, representing a 16bit address to it's 64bit counterpart.
     *
     * @param orig the original 16bit address
     * @return the 64 bit address
     */
    public static final long To64Bit(int orig) {
        return To64Bit((short) orig);
    }

    /**
     * This method converts a short, representing a 16bit address to
     * an IEEEAddress
     *
     * @param orig the original 16bit address
     * @return the 64 bit address
     */
    public static final IEEEAddress ToIEEEAddress(short orig) {
        return new IEEEAddress(To64Bit(orig));
    }

    /**
     * This method converts an int, representing a 16bit address to
     * an IEEEAddress
     *
     * @param orig the original 16bit address
     * @return the 64 bit address
     */
    public static final IEEEAddress ToIEEEAddress(int orig) {
        return ToIEEEAddress((short) orig);
    }
}
