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

import com.sun.spot.util.Utils;
import com.sun.squawk.*;
import com.sun.squawk.vm.*;

class SleepManager extends Thread implements ISleepManager {

    /**
     *
     */
    private static final int MIN_VEXT = 4500;
    private static final long INITIAL_TEAR_DOWN_TIME = 700;
    private static final long INITIAL_SET_UP_TIME = 700;
	
    private static final long OVERHEAD_OF_RESUMING_DEEP_SLEEP_THREAD = 10;
    private static final long OVERHEAD_OF_GETTING_TO_GLOBAL_WAIT_FOR_EVENT = 3;
    private static final long OVERHEAD_OF_GETTING_TO_RESCHEDULENEXT_CALC = 2;
    private static final long OVERHEAD_OF_REBOOTING = 95; // time it takes the ARM9 to start and resume execution
    private static final long OVERHEAD_OF_RESUMING_APPLICATION = 5;
	
    private static final long MAXIMUM_SHALLOW_SLEEP_ALLOWANCE = 1; // just enough to ensure the sleep time the VM sees
    // is less than the min we set
    private static final long MIMIMUM_ACTUAL_ARM9_OFF_TIME = 2000; // to achieve energy saving
	
    private long tearDownTimeAllowed;
    private long setUpTimeAllowed;
    private int sleepCount;
    private boolean sleepEnabled;
    private boolean tracing;
    private boolean diagnosticMode = false;
    private DriverRegistry driverRegistry;
    private IUSBPowerDaemon usbPowerDaemon;
    private UnableToDeepSleepException deepSleepException;
    private Thread ensureDeepSleepThread;
    private SoakThread soakThread;
    private long startTime;
    private long totalDeepSleepTime = 0;
    private long totalSimulatedDeepSleep = 0;
    private IPowerController powerController;
    private int hardwareType;

    private static class SoakThread extends Thread {

        private boolean stopped = true;

        public SoakThread(String name) {
            super(name);
        }

        public synchronized void restart() {
            stopped = false;
            this.notify();
        }

        public void pause() {
            stopped = true;
        }

        public void run() {
            while (true) {
                if (stopped) {
                    synchronized (this) {
                        try {
                            this.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    public SleepManager(DriverRegistry registry, IUSBPowerDaemon usbPowerDaemon, IPowerController powerController, int hardwareType) {
        super("SleepManager");
        this.driverRegistry = registry;
        this.usbPowerDaemon = usbPowerDaemon;
        this.powerController = powerController;
        this.hardwareType = hardwareType;
        this.sleepCount = 0;
        this.sleepEnabled = false;
        this.tracing = false;
        resetMinimumDeepSleepTime();

        soakThread = new SoakThread("Sleep manager soak thread");
        VM.setSystemThreadPriority(soakThread, VM.MAX_SYS_PRIORITY - 1);
        VM.setAsDaemonThread(soakThread);
        soakThread.start();

        VM.setSystemThreadPriority(this, VM.MAX_SYS_PRIORITY);
        VM.setAsDaemonThread(this);
        this.start();
    }

    public void run() {
        enableDeepSleep(true);
        startTime = System.currentTimeMillis();
        Isolate.LifecycleListener exitHook = new Isolate.LifecycleListener() {

            public void handleLifecycleListenerEvent(Isolate iso, int eventKind) {
                long upTime = getUpTime();
                long shallowTime = getTotalShallowSleepTime();
                long deepTime = getTotalDeepSleepTime();
                Utils.log("");
                Utils.log("SleepManager statistics:");
                double utilization = (upTime - (shallowTime + deepTime)) / (upTime * 1.0);
                Utils.log("CPU utilization     = " + ((int) Math.floor(utilization * 100 + 0.5)) + "%");
                Utils.log("Total deep sleep    = " + deepTime);
                Utils.log("Total shallow sleep = " + shallowTime);
                Utils.log("Total up time       = " + upTime);
            }
        };
        Isolate.currentIsolate().addLifecycleListener(exitHook, Isolate.SHUTDOWN_EVENT_MASK);
        while (true) {
            // tell the VM to block this thread until a sleep of at least the specified duration is required
            long targetMillisToWakeUp = VM.waitForDeepSleep(getThresholdForDeepSleepCheckInVM());
            soakThread.restart(); // soak thread mops up spare CPU so no other thread at or below Thread.MAX_PRIORITY get in
            try {
                deepSleep(targetMillisToWakeUp);
            } finally {
                soakThread.pause();
            }
        }
    }

    public int getDeepSleepCount() {
        return sleepCount;
    }

    public void enableDeepSleep() {
        enableDeepSleep(true);
    }

    public void enableDeepSleep(boolean b) {
        if (sleepEnabled != b) {
            sleepEnabled = b;
            VM.execSyncIO(ChannelConstants.SET_DEEP_SLEEP_ENABLED, sleepEnabled ? 1 : 0, 0, 0, 0, 0, 0, null, null);
        }
    }

    public void disableDeepSleep() {
        enableDeepSleep(false);
    }

    private long getThresholdForDeepSleepCheckInVM() {
        return tearDownTimeAllowed + setUpTimeAllowed + OVERHEAD_OF_RESUMING_DEEP_SLEEP_THREAD +
                OVERHEAD_OF_REBOOTING + OVERHEAD_OF_RESUMING_APPLICATION + OVERHEAD_OF_GETTING_TO_GLOBAL_WAIT_FOR_EVENT +
                MIMIMUM_ACTUAL_ARM9_OFF_TIME;
    }

    private long getWakeTimeForActualDeepSleep(long requestedApplicationRestartTime) {
        return requestedApplicationRestartTime - (setUpTimeAllowed + OVERHEAD_OF_REBOOTING + OVERHEAD_OF_RESUMING_APPLICATION);
    }

    public long getMinimumDeepSleepTime() {
        return getThresholdForDeepSleepCheckInVM() + OVERHEAD_OF_GETTING_TO_RESCHEDULENEXT_CALC;
    }

    public long getMaximumShallowSleepTime() {
        return getThresholdForDeepSleepCheckInVM() - MAXIMUM_SHALLOW_SLEEP_ALLOWANCE;
    }

    public boolean isDeepSleepEnabled() {
        return sleepEnabled;
    }

    public boolean isTracing() {
        return tracing;
    }

    public void setTracing(boolean tracing) {
        this.tracing = tracing;
        driverRegistry.setTracing(tracing);
    }

    private void deepSleep(long targetMillisToWakeUp) {
        if (tracing) {
            Utils.log("[SleepManager] Deep sleep called with wakeup " + targetMillisToWakeUp);
            Utils.log(" (current time=" + System.currentTimeMillis() + ")");
        }
        try {
            prepareToDeepSleep();
            primDeepSleep(targetMillisToWakeUp);
        } catch (UnableToDeepSleepException e) {
            if (tracing) {
                Utils.log("[SleepManager] " + e.getMessage());
            }
            if (ensureDeepSleepThread != null) {
                deepSleepException = e;
                ensureDeepSleepThread.interrupt();
            } else {
                primShallowSleep(targetMillisToWakeUp);
            }
        } catch (Throwable t) {
            if (tracing) {
                Utils.log("[SleepManager] " + t.getMessage());
                t.printStackTrace();
            }
            if (ensureDeepSleepThread != null) {
                deepSleepException = new UnableToDeepSleepException(t.getMessage());
                ensureDeepSleepThread.interrupt();
            } else {
                primShallowSleep(targetMillisToWakeUp);
            }
        }
    }

    private void primShallowSleep(long targetMillisToWakeUp) {
        int low  = (int) targetMillisToWakeUp;
        int high = (int) (targetMillisToWakeUp >> 32);
        VM.execSyncIO(ChannelConstants.SHALLOW_SLEEP, high, low, 0, 0, 0, 0, null, null);
    }

    private void primDeepSleep(long targetMillisToWakeUp) {
        String sleepTypeDescription = "not sleeping";
        long deepSleepStartTime = System.currentTimeMillis();
        if (deepSleepStartTime < targetMillisToWakeUp) {    // did tear down time exceed wake time?
            long deepSleepWakeUpTime = getWakeTimeForActualDeepSleep(targetMillisToWakeUp);
            if (deepSleepStartTime < deepSleepWakeUpTime) { // still enough time for deep sleep?
                int low = (int) (deepSleepWakeUpTime & 0xFFFFFFFF);
                int high = (int) (deepSleepWakeUpTime >> 32);

                int sleepType = diagnosticMode ? ChannelConstants.SHALLOW_SLEEP : ChannelConstants.DEEP_SLEEP;
                boolean isPoweredDeepSleep = false; // because of bug 1386 was: hardwareType > 6 ? false : usbPowerDaemon.isUsbPowered() || (powerController.getVext() > MIN_VEXT);
                sleepTypeDescription = diagnosticMode ? "simulated deep sleep" : (isPoweredDeepSleep ? "powered deep sleep" : "deep sleep");
                if (tracing) {
                    Utils.log("Spot: entering " + sleepTypeDescription);
                }
                VM.execSyncIO(sleepType, high, low, isPoweredDeepSleep ? 1 : 0, 0, 0, 0, null, null);
            }
        }

        long setUpStartTime = System.currentTimeMillis();
        long deepSleepTime = setUpStartTime - deepSleepStartTime;
        totalDeepSleepTime += deepSleepTime;
        if (diagnosticMode) {
            totalSimulatedDeepSleep += deepSleepTime;
        }

        if (tracing) {
            Utils.log("Returned from " + sleepTypeDescription + " at " + setUpStartTime);
        }
        driverRegistry.setUp();
        long setUpTime = System.currentTimeMillis() - setUpStartTime;
        if (setUpTime > setUpTimeAllowed) {
            setUpTimeAllowed = setUpTime + (setUpTime / 10);
        }

        if (tracing) {
            Utils.log("Set up drivers in " + setUpTime + "mSecs");
        }
        sleepCount++;
    }

    private void prepareToDeepSleep() throws UnableToDeepSleepException {
        if (!sleepEnabled) {
            throw new UnableToDeepSleepException("Cannot deep sleep while disabled");
        }

        if (!diagnosticMode) {
            int powerDaemonState = usbPowerDaemon.getCurrentState();
            if (powerDaemonState == USBPowerDaemon.STATE_ENUMERATED || powerDaemonState == USBPowerDaemon.STATE_AWAITING_ENUM) {
                throw new UnableToDeepSleepException("Cannot deep sleep with USB connected");
            }
        }

        long tearDownStartTime = System.currentTimeMillis();
        if (!driverRegistry.tearDown()) {
            throw new UnableToDeepSleepException("Driver " + driverRegistry.getLastVeto() + " vetoed deep sleep");
        }

        long tearDownTime = System.currentTimeMillis() - tearDownStartTime;
        if (tearDownTime > tearDownTimeAllowed) {
            tearDownTimeAllowed = tearDownTime + (tearDownTime / 10);
        }
        if (tracing) {
            Utils.log("Tore down drivers in " + tearDownTime + "mSecs");
        }
    }

    public void enableDiagnosticMode() {
        diagnosticMode = true;
        setTracing(true);
    }

    public void disableDiagnosticMode() {
        diagnosticMode = false;
        setTracing(false);
    }

    public boolean isInDiagnosticMode() {
        return diagnosticMode;
    }

    /* This is synchronized because if two application threads call this at the same time, the
     * ensureDeepSleepThread variable may get overwritten.
     */
    public synchronized void ensureDeepSleep(long sleepTime) throws UnableToDeepSleepException {
        if (!isDeepSleepEnabled()) {
            throw new UnableToDeepSleepException("Deep sleep is disabled");
        }
        if (sleepTime < getMinimumDeepSleepTime()) {
            throw new UnableToDeepSleepException("Time specified is less than minimum deep sleep time of " + getMinimumDeepSleepTime());
        }
        long timeBeforeAnotherThreadIsRunnable = VM.getTimeBeforeAnotherThreadIsRunnable();
        if (timeBeforeAnotherThreadIsRunnable < sleepTime) {
            throw new UnableToDeepSleepException("Another thread will be runnable within " + timeBeforeAnotherThreadIsRunnable + " millis");
        }
        ensureDeepSleepThread = Thread.currentThread();
        int count = getDeepSleepCount();
        Utils.sleep(sleepTime);
        ensureDeepSleepThread = null;
        if (deepSleepException != null) {
            UnableToDeepSleepException ex = deepSleepException;
            deepSleepException = null;
            throw ex;
        }
        if (getDeepSleepCount() == count) {
            throw new UnableToDeepSleepException("Unknown reason");
        }
    }

    public void resetMinimumDeepSleepTime() {
        tearDownTimeAllowed = INITIAL_TEAR_DOWN_TIME;
        setUpTimeAllowed = INITIAL_SET_UP_TIME;
        long minimumDeepSleepTime = getThresholdForDeepSleepCheckInVM();
        int lowParam = (int) minimumDeepSleepTime;
        int highParam = (int) (minimumDeepSleepTime >> 32);
        VM.execSyncIO(ChannelConstants.SET_MINIMUM_DEEP_SLEEP_TIME, highParam, lowParam, 0, 0, 0, 0, null, null);
    }

    public long getTotalShallowSleepTime() {
        long highResult = VM.execSyncIO(ChannelConstants.TOTAL_SHALLOW_SLEEP_TIME_MILLIS_HIGH, 0);
        long lowResult = VM.execSyncIO(ChannelConstants.TOTAL_SHALLOW_SLEEP_TIME_MILLIS_LOW, 0);
        return ((highResult << 32) | (lowResult & 0x00000000FFFFFFFFL)) - totalSimulatedDeepSleep;
    }

    public long getTotalDeepSleepTime() {
        return totalDeepSleepTime;
    }

    public long getUpTime() {
        return System.currentTimeMillis() - startTime;
    }
    
    /* This is used by the setTime() method in the PowerController */
    public void adjustStartTime(long delta) {
        startTime += delta;
    }
}
