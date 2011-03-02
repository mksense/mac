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

package com.sun.spot.peripheral.driver.proxy;

import com.sun.spot.interisolate.ReplyEnvelope;
import com.sun.spot.interisolate.RequestEnvelope;
import com.sun.spot.interisolate.VoidReplyEnvelope;
import com.sun.spot.peripheral.IDriverRegistry;

class AddCommand extends RequestEnvelope {
	private String driverName;
	private String driverServerName; 

	public AddCommand(String driverName, String driverServerName) {
		this.driverName = driverName;
		this.driverServerName = driverServerName;
	}

	public ReplyEnvelope execute(Object context) throws Exception {
		IDriverRegistry theDriverRegistry = (IDriverRegistry) context;
		theDriverRegistry.add(new ProxyDriver(driverName, driverServerName));
		return new VoidReplyEnvelope(getUid());
	}

	public String getName() {
		return driverName;
	}

}
