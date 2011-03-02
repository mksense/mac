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

package com.sun.spot.peripheral.radio;

public class RadioPolicy {
	
	public static final RadioPolicy ON        = new RadioPolicy("ON");
	public static final RadioPolicy OFF       = new RadioPolicy("OFF");
	public static final RadioPolicy AUTOMATIC = new RadioPolicy("AUTO");
	private String name;
	
	private RadioPolicy(String name) {
		this.name = name;
	}
	
	public String toString() {
		return "Radio policy of " + name;
	}

	/**
	 * Return the instance that matches the argument. We need this to allow selections to be
	 * passed between isolates
	 * @param selection the selection to be matched
	 * @return the matching selection
	 */
	public static RadioPolicy policyMatching(RadioPolicy selection) {
		if (selection.name.equals(ON.name)) {
			return ON;
		} else if (selection.name.equals(OFF.name)) {
			return OFF;
		} else {
			return AUTOMATIC;
		}
	}

}
