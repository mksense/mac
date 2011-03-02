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

package com.sun.spot.peripheral;

public interface IFiqInterruptDaemon {

	/**
	 * Replace the existing handler for power controller time alarms with a user-supplied handler.
	 * It is very unlikely that you will want to create a handler for time alarms.
	 * The default handler logs a message if spot.diagnostics is set.
	 * The previous handler is returned, so you can chain your new handler to that one.
	 * @param handler the new handler to use
	 * @return the previous handler
	 */
	IEventHandler setAlarmHandler(IEventHandler handler);

	/**
	 * Replace the existing handler for reset button presses with a user-supplied handler.
	 * The default handler calls VM.stopVM(0). The previous handler is returned, so you
	 * can chain your new handler to that one.
	 * @param handler the new handler to use
	 * @return the previous handler
	 */
	IEventHandler setButtonHandler(IEventHandler handler);

	/**
	 * Replace the existing handler for poweroff with a user-supplied handler.
	 * The power off event occurs when the user uses the reset button to turn off
	 * the SPOT. The handler has about 400ms to do work before the power goes away.
	 * The default handler does nothing.
	 * The previous handler is returned, so you can chain your new handler to that one.
	 * @param handler the new handler to use
	 * @return the previous handler
	 */
	IEventHandler setPowerOffHandler(IEventHandler handler);

	/**
	 * Replace the existing handler for low battery warnings with a user-supplied handler.
	 * The default handler logs a message if spot.diagnostics is set.
	 * The previous handler is returned, so you can chain your new handler to that one.
	 * @param handler the new handler to use
	 * @return the previous handler
	 */
	IEventHandler setLowBatteryHandler(IEventHandler handler);

	/**
	 * Replace the existing handler for external power applied events with a user-supplied handler.
	 * The default handler logs a message if spot.diagnostics is set.
	 * The previous handler is returned, so you can chain your new handler to that one.
	 * @param handler the new handler to use
	 * @return the previous handler
	 */
	IEventHandler setExternalPowerHandler(IEventHandler handler);

}
