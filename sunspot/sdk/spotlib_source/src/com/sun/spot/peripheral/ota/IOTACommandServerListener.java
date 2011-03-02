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

package com.sun.spot.peripheral.ota;


/**
 * This interface must be supported by any object that wishes to receive notifications from a 
 * {@link com.sun.spot.peripheral.ota.IOTACommandServer}.<br><br>
 * 
 * The purpose of this interface is to allow applications to take some action - eg suspend - while over-the-air
 * downloads are in progress.<br><br>
 * 
 * To register as a listener:<br><br>
 * 
 * <code>
 * 		...<br>
 * 		IOTACommandServer spotMon = Spot.getInstance().getOTACommandServer();<br>
 *		spotMon.addListener(this);<br>
 *		...<br>
 * </code>
 * 
 */
public interface IOTACommandServerListener {
	
	/**
	 *  Called by the OTACommandServer prior to beginning an over-the-air download
	 */
	public void preFlash();
	
	
	/**
	 * Called by the OTACommandServer at the end of an over-the-air download
	 */
	public void postFlash();

}
