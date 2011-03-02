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

package com.sun.spot.peripheral.test;

/**
 * Constants describing fields in radio packets sent during testing - not required
 * by normal user code.
 * 
 * @author syntropy
 */
public interface RadioTestPacketBits {
	// offsets into MAC payload
	static final int RP_BUF_SIZE_CHECK = 0; // matches MAC payload length - only used in PHY tests
	static final int RP_BUF_ACTIONS    = 1; // encoded actions - see below
	static final int RP_BUF_SEQ_NUMBER = 3;
	static final int RP_BUF_ECHO_COUNT = 4; // only used if ACTION_ECHO
	static final int RP_BUF_DATA       = 5; // offset of start of data
   
	static final int ACTION_LOG_DETAIL  = 1;	// output full packet to console
	static final int ACTION_RESET_STATS = 2;
	static final int ACTION_LOG         = 4;	// output summary of packet to console
	static final int ACTION_LOG_STATS   = 8;
	static final int ACTION_ECHO        = 16;	// echo RP_BUF_ECHO_COUNT packets containing [0]=0-based index; [1-n]=copy of rcvd data
	static final int ACTION_ECHO_HDR    = 32;   // echo the packet header received (21 bytes)
	static final int ACTION_SET_POWER   = 64;   // set the transmit power to the value of the first data byte
	static final int ACTION_ECHO_BROADCAST = 128; // as per ACTION_ECHO but broadcast it
	static final int ACTION_EXIT = 256; 		// exit the current slave
}
