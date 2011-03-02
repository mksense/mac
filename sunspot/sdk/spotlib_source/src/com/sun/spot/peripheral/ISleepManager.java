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
 * Interface to the low power sleep manager for the Sun SPOT
 * 
 * @author Syntropy
 *
 */
public interface ISleepManager {

	/**
	 * Get deep sleep count
	 * @return The number of times the SPOT has entered deep sleep
	 */
	int getDeepSleepCount();

	/**
	 * Enable deep sleep mode
	 *
	 */
	void enableDeepSleep();

	/**
	 * Conditionally enable/disable deep sleep mode
	 * @param b enable deep sleep if true, disable otherwise
	 */
	void enableDeepSleep(boolean b);
	
	/**
	 * Find out the minimum sleep time that will cause a deep sleep.
	 * Note that sleeps for periods shorter than this may still cause a deep
	 * sleep - see {@link #getMaximumShallowSleepTime()}. Note also that the value
	 * returned by this call is not a constant: the value can increase during execution
	 * if drivers take longer than expected to tear down or set up.
	 * @return The minimum sleep time to enter deep sleep
	 */
	long getMinimumDeepSleepTime();

	/**
	 * Find out the maximum sleep time that will cause a shallow sleep.
	 * Note that sleeps for periods longer than this may still cause a shallow
	 * sleep - see {@link #getMinimumDeepSleepTime()}
	 * @return The maximum sleep time to enter shallow sleep
	 */
	long getMaximumShallowSleepTime();

	/**
	 * Disable deep sleep mode
	 *
	 */
	void disableDeepSleep();
	
	/**
	 * In diagnostic mode, a SPOT will simulate deep sleep even when USB is connected.
	 * This facility enables device driver authors to debug their tearDown and setUp
	 * code. Enabling diagnostic mode also causes detailed trace output to the log.
	 */
	void enableDiagnosticMode();

	/**
	 * When diagnostic mode is disabled, a SPOT will not simulate deep sleep when USB 
	 * is connected. This is the most efficient set up for people other than device
	 * driver authors.
	 */
	void disableDiagnosticMode();

	/**
	 * @see #enableDiagnosticMode()
	 * 
	 * @return whether the sleep manager is in diagnostic mode.
	 */
	public boolean isInDiagnosticMode();

	/**
	 * @return whether deep sleep is currently enabled
	 */
	boolean isDeepSleepEnabled();
	
	/**
	 * Set whether to display detailed trace information during deep sleep tear
	 * down and setup.
	 * 
	 * @param tracingEnabled
	 */
	void setTracing(boolean tracingEnabled);

	/**
	 * If Thread.sleep would result in a deep sleep of the specified time,
	 * then do that sleep. Otherwise, throw an exception to indicate why not.
	 *  
	 * @param sleepTime time to sleep in millis
	 * @throws UnableToDeepSleepException
	 */
	void ensureDeepSleep(long sleepTime) throws UnableToDeepSleepException;

	/**
	 * After each deep sleep the minimum deep sleep time is adjusted to take
	 * account of how long the drivers took to tear down and set up. This method
	 * resets the minimum deep sleep time to the default.
	 */
	void resetMinimumDeepSleepTime();
	
    /**
     * Get the total time in milliseconds that the SPOT has spent in shallow sleep
     * since it was started.
     * @return the total shallow sleep time in millis
     */
    long getTotalShallowSleepTime();

    /**
     * Get the total time in milliseconds that the SPOT has spent in deep sleep
     * since it was started.
     * @return the total deep sleep time in millis
     */
    long getTotalDeepSleepTime();
    
    /**
     * Get the total time in milliseconds since the SPOT was started.
     * @return the total time the SPOT has been running
     */
    long getUpTime();

    /**
     * Adds the specified amount to the internal starttime variable
     * used in the computation of UpTime. This method is automatically
     * invoked when a user calls the setTime() method in PowerController.
     */
    public void adjustStartTime(long delta);
}
