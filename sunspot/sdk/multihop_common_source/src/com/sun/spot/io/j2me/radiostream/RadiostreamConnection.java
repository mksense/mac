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

package com.sun.spot.io.j2me.radiostream;

import javax.microedition.io.StreamConnection;

import com.sun.spot.peripheral.*;


/**
 * This interface defines the "radiostream" protocol.<br><br>
 * 
 * The radiostream protocol is a socket-like peer-to-peer protocol that provides reliable, buffered
 * stream-based IO between two devices.<br><br>
 *  
 * IMPORTANT This protocol is provided for test purposes and to allow creation of simple
 * demonstrations. It is NOT designed to be used as the base for higher level or more complex
 * protocols. If you want to create something more sophisticated write a new protocol that
 * calls the {@link com.sun.spot.peripheral.radio.RadioPacketDispatcher} or {@link com.sun.spot.peripheral.radio.LowPan}
 * directly. See {@link com.sun.spot.peripheral.radio.IProtocolManager} for more information.<br><br>
 * 
 * To open a connection do:<br><br>
 * 
 * <code>
 * ...<br>
 * StreamConnection conn = (StreamConnection) Connector.open("radiostream://destinationAddr:portNo");<br>
 * ...<br>
 * </code><br>
 * where destinationAddr is the 64 bit IEEE Address of the radio at the far end, and portNo is a
 * port number in the range 0 to 127 that identifies this particular connection. Note that 0 is not
 * a valid IEEE address in this implementation.<br>
 * <br>
 * To establish a bi-directional connection both ends must open connections specifying the same portNo
 * and corresponding IEEE addresses.
 * <br>
 * Port numbers between 0 and 31 are reserved for system services.  Use by applications may result in conflicts.
 * <br>
 * 
 * Once the connection has been opened, each end can obtain streams to use to send and receive data, eg:<br><br>
 * 
 * <code>
 * ...<br>
 * DataInputStream dis = conn.openDataInputStream();<br>
 * DataOutputStream dos = conn.openDataOutputStream();<br>
 * ...<br>
 * </code>
 */
public interface RadiostreamConnection extends ITimeoutableConnection, IRadioControl, StreamConnection {

}
