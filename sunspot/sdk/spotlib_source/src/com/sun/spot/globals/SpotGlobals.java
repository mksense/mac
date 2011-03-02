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

package com.sun.spot.globals;

import com.sun.squawk.VM;

/**
 * SpotGlobals provides access from the SPOT library to objects that are global across the whole VM; that is,
 * not limited to a single isolate. This facility should be used only from SPOT library code, not user applications.
 * <br><br>
 * All clients must wrap access to SpotGlobals in a synchronised block:
 * <code>
 * synchronized (SpotGlobals.getMutex()) {
 *   // manipulate SpotGlobals here ...
 * }
 * </code>
 */
public class SpotGlobals {
	
	public static Object getMutex() {
		return VM.getKeyedGlobalsMutex();
	}

	public static Object getGlobal(int key) {
		return VM.getKeyedGlobal(key);
	}

	public static void setGlobal(int key, Object value) {
		VM.putKeyedGlobal(key, value);
	}
	
}
