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

/**
 * This interface represents any Connection between a pair of Spots that 
 * can timeout on receiving data.
 */
public interface ITimeoutableConnection {

	/**
	 * Set the timeout for receiving information on this connection
	 * 
	 * @param time - Timeout period in milliseconds. Set this to -1 to turn off timeouts (infinite wait),
	 *   or to 0 or more to wait that long for data before throwing a TimeoutException.
	 */
	void setTimeout(long time);
	/**
	 * Get the timeout for receiving information on this connection
	 * 
	 * @return - Timeout period in milliseconds. Answers -1 for infinite wait.
	 */
	long getTimeout();
}
