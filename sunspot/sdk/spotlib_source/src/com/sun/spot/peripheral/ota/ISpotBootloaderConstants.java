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

package com.sun.spot.peripheral.ota;

/**
 * ISpotBootloaderConstants
 *
 */
public interface ISpotBootloaderConstants {
	static final char PRE_PURPLE_FLASH_BOOTLOAD_CMD = 'D';
	static final char FLASH_CONFIG_CMD 				= 'E';
	static final char START_VM_CMD 					= 'F';

	static final char RESET_CMD 					= 'H';
	static final char GET_CONFIG_PAGE_CMD			= 'I';

	static final char RESYNC_CMD 					= 'L';
	static final char FLASH_MANUFACTURING_IMAGE_CMD = 'M';
	static final char SET_TIME_CMD 					= 'N';
	static final char READ_SECTOR_CMD				= 'O';
	
	static final char FLASH_BOOTLOAD_CMD			= 'P';
	
	static final char BOOTLOADER_SYNC 				= 's';
	static final char OLD_BOOTLOADER_SYNC 		    = 'S';
	
	static final char WRITE_SECTOR_CMD				= 'V';
	static final char NO_OPERATION_CMD				= 'X';
		
	static final String BOOTLOADER_CMD_HEADER = "*EL*";
	static final String ABORT_REQUEST = BOOTLOADER_CMD_HEADER + ":A";
	
	static final byte[] BOOTLOADER_CMD_PREFIX = new byte[] {'$', '@', '#', '%'};

}

