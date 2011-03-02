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

import com.sun.spot.flashmanagement.FlashFileOutputStream;

/**
 * ISpotAdminConstants
 *
 */
public interface ISpotAdminConstants {
	public static final String FLASH_APP_CMD 			= "DEF-FA";
	public static final String FLASH_LIB_CMD 			= "DEF-FL";
	public static final String UNDEPLOY_CMD 			= "DEF-UD";

	public static final String START_VM_CMD 			= "DEF-SV";
	public static final String RESYNC_CMD 				= "DEF-RS";
	public static final String BOOTLOADER_CMD_ATTENTION = "DEF-AT";
	public static final String DELETE_PUBLIC_KEY_CMD	= "DEF-DP";
	public static final String CLOSEDOWN			    = "DEF-CL";
	public static final String BLINK_CMD				= "DEF-BL";

	public static final String GET_CONFIG_PAGE_CMD		= "DEF-GC";
	public static final String GET_CONFIG_PAGE_LEN_CMD	= "DEF-GCN";
	public static final String GET_SYSTEM_PROPERTIES    = "DEF-GS";
	public static final String GET_FILE_LIST_CMD		= "DEF-GL";
	public static final String GET_FILE_INFO_CMD		= "DEF-GF";

	public static final String SET_TIME_CMD 			= "DEF-ST";
	public static final String SET_SYSTEM_PROPERTIES    = "DEF-SS";
	public static final String SET_PUBLIC_KEY_CMD	    = "DEF-SP";
	public static final String SET_STARTUP_CMD	    	= "DEF-SU";

    public static final String GET_MEMORY_STATS_CMD     = "SW-GMS";
	public static final String GET_POWER_STATS_CMD      = "SW-GPS";
	public static final String GET_SLEEP_INFO_CMD       = "SW-GSI";
	public static final String GET_AVAILABLE_SUITES_CMD = "SW-GAS";
	public static final String GET_SUITE_MANIFEST_CMD   = "SW-GWM";
	public static final String START_APP_CMD            = "SW-SAP";
	public static final String PAUSE_APP_CMD            = "SW-PAP";
	public static final String RESUME_APP_CMD           = "SW-RAP";
	public static final String STOP_APP_CMD             = "SW-STA";
	public static final String GET_APP_STATUS_CMD       = "SW-APS";
	public static final String GET_ALL_APPS_STATUS_CMD  = "SW-AAP";
	public static final String START_REMOTE_PRINTING_CMD = "SW-SRP";
	public static final String STOP_REMOTE_PRINTING_CMD = "SW-STP";
	public static final String RECEIVE_APP_CMD          = "SW-REA";
	public static final String MIGRATE_APP_CMD          = "SW-MIA";
	public static final String GET_SPOT_PROPERTY_CMD    = "SW-GSP";
	public static final String REMOTE_GET_PHYS_NBRS_CMD = "SW-RPN";
	public static final String GET_RADIO_INFO_CMD       = "SW-GRI";
	public static final String SET_RADIO_INFO_CMD       = "SW-SRI";
	public static final String GET_ROUTE_CMD            = "SW-GRT";
	
	public static final String BOOTLOADER_CMD_HEADER = "*EL*";
	public static final String ABORT_REQUEST = BOOTLOADER_CMD_HEADER + ":A";
	
	// TODO It's critical for now that both these are multiples of FlashFileOutputStream.DEFAULT_BUFFER_SIZE so that the 
	// high level flow control doesn't conflict with that inside CrcOutputStream. This restriction can be removed
	// as and when the remote OTACommandProcessor is using a CrcOutputStream.
	public static final int REMOTE_FLASH_OPERATION_FLOW_CONTROL_QUANTUM = 8 * FlashFileOutputStream.DEFAULT_BUFFER_SIZE;
	public static final int ADMIN_FLASH_OPERATION_FLOW_CONTROL_QUANTUM = 8 * FlashFileOutputStream.DEFAULT_BUFFER_SIZE;
	
	public static final int ERROR_UNKNOWN_COMMAND = 0;
	public static final int ERROR_COMMAND_VERIFICATION_FAILED = 1;
	public static final int ERROR_GENERAL = 2;

	public static final int MASTER_ISOLATE_ECHO_PORT = 12;
	public static final int CHILD_ISOLATE_ECHO_PORT = 13;
	
	public static final String REMOTE_OTA_COMMAND_SERVER_IDENTIFICATION_STRING = "Remote Monitor";
	public static final String LOCAL_OTA_COMMAND_SERVER_IDENTIFICATION_STRING = "Local Monitor";
}

