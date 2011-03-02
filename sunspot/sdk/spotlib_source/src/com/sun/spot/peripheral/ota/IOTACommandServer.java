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

package com.sun.spot.peripheral.ota;

import com.sun.spot.service.IService;
import java.util.Date;

/**
 * Defines constants used by Over-the-air command sessions.
 */
public interface IOTACommandServer extends IService {

	/**
	 * The default port on which to listen for hosts connecting
	 */
	public static final int DEFAULT_DATAGRAM_PORT = 8;
	
	/**
	 * The default port for stream command connections
	 */
	public static final int DEFAULT_STREAM_PORT = 8;

	/**
	 * The default datagram protocol to use
	 */
	public static final String DEFAULT_DATAGRAM_PROTOCOL = "radiogram";

	/**
	 * The default stream protocol to use
	 */
	public static final String DEFAULT_STREAM_PROTOCOL = "radiostream";

	/**
	 * Command that we recognise to start a new session
	 */
	public static final String START_OTA_SESSION_CMD = "START_OTA_SESSION_CMD";
	
	/**
	 * Command to respond information about the SPOT 
	 */
	public static final String HELLO_CMD = "HELLO_CMD";

	/**
	 * Version of the {@link #HELLO_CMD} that we support 
	 */
	public static final int HELLO_COMMAND_MAJOR_VERSION = 3;
        
	/**
	 * Starting with (major) version 2, we support major/minor version
	 * numbering. This lets us signal/detect changes that are backward
	 * compatible and those that aren't.
	 */
	public static final int HELLO_COMMAND_MINOR_VERSION = 1;

    public static final byte BASIC_HELLO_TYPE = 0;
    public static final byte PHYSICAL_NEIGHBORS_HELLO_TYPE  = 1;

    /**
     * SPOT device type
     */
    public static final byte SPOT_TYPE = 1;

    /**
     * eSPOT device subtype
     */
    public static final byte ESPOT_SUBTYPE = 1;

    /**
     * virtual eSPOT device subtype
     */
    public static final byte VIRTUAL_ESPOT_SUBTYPE = 2;


    /**
     * Host application device type
     */
    public static final byte HOST_APP_TYPE = 2;

    /**
     * generic host app subtype
     */
    public static final byte GENERIC_HOST_APP_SUBTYPE = 1;

    /**
     * shared basestation host app subtype
     */
    public static final byte SHARED_BASESTATION_SUBTYPE = 2;

    /**
	 * Major version of the eSPOT hardware to report in response to {@link #HELLO_CMD}
	 */
	int HARDWARE_MAJOR_REV_ESPOT = 0;
	
	
	/**
	 * Attach a listener to be notified of the start and stop of flash
	 * operations.
	 * 
	 * @param sml the listener
	 */
	void addListener(IOTACommandServerListener sml);

	/**
	 * Answer the IEEE address of the sender of the last command received.
	 * 
	 * @return -- the address
	 */
	String getBaseStationAddress();

	/**
	 * @return Returns true if the server has been suspended by software.
	 */
	boolean isSuspended();

	/**
	 * @param suspended Suspends or resumes the server (it is initially running).
	 */
	void setSuspended(boolean suspended);

	/**
	 * @return The time when the server last received a message from the host
	 */
	Date timeOfLastMessageFromHost();

    /**
     * Set the device subtype for the Hello command.
     *
     * @param subtype specify the type of SPOT this is
     */
    void setSubType(byte subtype);

    /**
     * Get the device subtype for the Hello command.
     *
     * @return the type of SPOT this is
     */
    byte getSubType();
    
}
