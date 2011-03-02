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

package com.sun.spot.peripheral;

import com.sun.squawk.VM;
import com.sun.squawk.vm.ChannelConstants;

class SecuredSiliconArea implements ISecuredSiliconArea {

	public byte[] read() {
		byte[] result = new byte[256];
		VM.execSyncIO(0, ChannelConstants.READ_SECURED_SILICON_AREA, 0, 0, 0, 0, 0, 0, result, null);
		return result;
	}

	public static final int SERIAL_NUMBER_FLASH_OFFSET = 0x80;

	public long readSerialNumber() {
		byte[] securedData = read();
		long result = 0;
		for (int i = 0; i < 8; i++) {
			result |= ((long)(securedData[i+SERIAL_NUMBER_FLASH_OFFSET] & 0xff)) << (i*8);
		}
		return result;
	}

}
