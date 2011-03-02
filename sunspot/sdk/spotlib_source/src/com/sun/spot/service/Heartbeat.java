/*
 * Copyright 2009 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.spot.service;

import com.sun.spot.peripheral.ILed;
import com.sun.spot.peripheral.Spot;
import com.sun.spot.util.Utils;
import com.sun.squawk.VM;

/**
 * A simple service that blinks the SPOT processor board green LED in a heartbeat pattern.
 *
 * When the green & red LEDs are used to signal events (e.g. radio packets
 * received & sent) the heartbeat thread can be used to periodically turn them off.
 *
 * Used by the basestation.
 * 
 * @author Ron Goldman
 */
public class Heartbeat implements IService, Runnable {

    /** Default period for heartbeat pattern in milliseconds */
    private static final long DEFAULT_HEARTBEAT_PERIOD = 10000;

    private ILed greenLed = Spot.getInstance().getGreenLed();
    private ILed redLed = Spot.getInstance().getRedLed();
    private int status = STOPPED;
    private long activityDecay;
    private long heartbeatPeriod;

    /**
     * Display basic heartbeat every 10 seconds.
     */
    public Heartbeat() {
        heartbeatPeriod = DEFAULT_HEARTBEAT_PERIOD;
        activityDecay = 0;
    }

    /**
     * Display basic heartbeat using specified period.
     *
     * @param period time in milliseconds for heartbeat pattern
     */
    public Heartbeat(long period) {
        if (period < 500) {
            throw new IllegalArgumentException("Heartbeat period must be greater than 500");
        }
        heartbeatPeriod = period;
        activityDecay = 0;
    }

    /**
     * Display basic heartbeat using specified period.
     *
     * Note: if the activityDecay interval between clearing the LEDs is less
     * than 3 seconds then the SPOT may not have enough time to enter deep sleep.
     *
     * @param period time in milliseconds for heartbeat pattern
     * @param activityDecayPeriod how often (in milliseconds) to clear activity LEDs
     *                      (zero means do not clear LEDs)
     */
    public Heartbeat(long period, long activityDecayPeriod) {
        if (period < 500) {
            throw new IllegalArgumentException("Heartbeat period must be greater than 500");
        }
        if (activityDecayPeriod != 0 && activityDecayPeriod < 500) {
            throw new IllegalArgumentException("Heartbeat activity decay period must be greater than 500");
        }
        heartbeatPeriod = period;
        activityDecay = activityDecayPeriod;
    }


    /**
     * Heartbeat display loop
     */
    public void run() {
        while (status == RUNNING) {
            if (activityDecay == 0) {
                Utils.sleep(heartbeatPeriod - 300);
            } else {
                long timeLeft = heartbeatPeriod - 300;
                while (timeLeft > 0) {
                    Utils.sleep(timeLeft > activityDecay ? activityDecay : timeLeft);
                    timeLeft -= activityDecay;
                    greenLed.setOff();
                    redLed.setOff();
                }
            }
            greenLed.setOn();
            Utils.sleep(100);
            greenLed.setOff();
            Utils.sleep(100);
            greenLed.setOn();
            Utils.sleep(100);
            greenLed.setOff();
        }
    }

    /**
     * Start the heartbeat service, if not already running.
     *
     * @return true if the service was successfully started
     */
    public boolean start() {
        if (status == STOPPED) {
            status = RUNNING;
            Thread th = new Thread(this, "Heartbeat thread");
            th.setPriority(Thread.MIN_PRIORITY);
            VM.setAsDaemonThread(th);
            th.start();
            return true;
        }
        return false;   // already running
    }

    /**
     * Stop the heartbeat service, if it is currently running.
     *
     * @return true if the service was successfully stopped
     */
    public boolean stop() {
        if (status == RUNNING) {
            status = STOPPED;
            return true;
        }
        return false;
    }

    /**
     * Same as calling stop().
     */
    public boolean pause() {
        return stop();
    }

    /**
     * Same as calling start().
     */
    public boolean resume() {
        return start();
    }

    /**
     * Return the current status of the heartbeat service.
     *
     * @return the current status of this service, e.g. STOPPED or RUNNING
     */
    public int getStatus() {
        return status;
    }

    /**
     * Return whether the heartbeat service is currently running.
     *
     * @return true if the heartbeat service is currently running
     */
    public boolean isRunning() {
        return status == RUNNING;
    }

    /**
     * Return the name of the heartbeat service.
     *
     * @return "Heartbeat" the name of this service
     */
    public String getServiceName() {
        return "Heartbeat";
    }

    /**
     * Assign a new name to this service.
     * Not settable for heartbeat service.
     *
     * @param who ignored
     */
    public void setServiceName(String who) {
    }

    /**
     * Return whether service is started automatically on reboot.
     *
     * @return false
     */
    public boolean getEnabled() {
        return false;
    }

    /**
     * Enable/disable whether service is started automatically.
     * Not settable for heartbeat service.
     *
     * @param enable ignored
     */
    public void setEnabled(boolean enable) {
    }

}
