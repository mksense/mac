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

import java.io.IOException;

import com.sun.spot.peripheral.handler.NullEventHandler;
import com.sun.spot.peripheral.handler.StopVMEventHandler;
import com.sun.spot.util.Utils;
import com.sun.squawk.VM;


/**
 * The FiqInterruptDaemon gives access to the handlers used for various notifications
 * from the power controller. A handler that implements {@link IEventHandler} can be
 * supplied to handle a specific event, replacing the existing handler (all events
 * have a default handler). Note that the handler is called at MAX_SYS_PRIORITY.
 * Your code should reduce the priority as appropriate.
 */
public class FiqInterruptDaemon implements Runnable, IDriver, IFiqInterruptDaemon {
	private IPowerController powerController;
	private IEventHandler buttonHandler;
	private IEventHandler alarmHandler;
	private IEventHandler powerOffHandler;
	private IEventHandler lowBatteryHandler;
	private IEventHandler externalPowerHandler;
	private Object deepSleepThreadMonitor = new Object();
	private IAT91_AIC aic;
	private ISpotPins spotPins;
	
	public FiqInterruptDaemon(IPowerController powerController, IAT91_AIC aic, ISpotPins spotPins) {
		this.spotPins =spotPins;
		this.aic = aic;
		this.powerController = powerController;
		this.alarmHandler = new NullEventHandler("Alarm event ignored");
		this.buttonHandler = new StopVMEventHandler();
		this.powerOffHandler = new NullEventHandler("Power off");
		this.lowBatteryHandler = new NullEventHandler("Low battery");
		this.externalPowerHandler = new NullEventHandler("External power applied");
	}

	public void startThreads() {
		Thread thread = new Thread(this, "Fiq Interrupt Daemon");
		VM.setAsDaemonThread(thread);
		VM.setSystemThreadPriority(thread, VM.MAX_SYS_PRIORITY);
		thread.start();
		
		/*
		 * Create a separate thread that runs when we return from deep sleep. This thread
		 * is unblocked by the setUp method. On return from deep sleep we need to process
		 * any power controller status changes that happened while we were asleep. We can't
		 * afford to just call handlePowerControllerStatusChange from setUp because the
		 * processing of the handlers might take a while. Since this thread is at MAX_PRIORITY
		 * it will take precedence over most user threads once deep sleep set up is finished.
		 */
		Thread returnFromDeepSleepThread = new Thread(new Runnable() {
			public void run() {
				while (true) {
					Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
					synchronized (deepSleepThreadMonitor) {
						try {
							deepSleepThreadMonitor.wait();
						} catch (InterruptedException e) {
							// no-op
						}
						handlePowerControllerStatusChange();
					}
				}
		 	}}, 
		 	"FIQ interrupt handler return from deep sleep");
		VM.setAsDaemonThread(returnFromDeepSleepThread);
		returnFromDeepSleepThread.start();
	}

	public void run() {
		// Handle status change once without interrupt in case we got an interesting status bit passed in from the bootloader
		handlePowerControllerStatusChange();

		initFiq();
		Spot.getInstance().getDriverRegistry().add(this);
		while (true) {
			try {
				VM.waitForInterrupt(IAT91_Peripherals.FIQ_ID_MASK);
				handlePowerControllerStatusChange();
			} catch (IOException e) {
			}
			aic.enableIrq(IAT91_Peripherals.FIQ_ID_MASK);
			// reset priority in case the handler changed it
			VM.setSystemThreadPriority(Thread.currentThread(), VM.MAX_SYS_PRIORITY);
		}
	}

	private void handlePowerControllerStatusChange() {
		int status = powerController.getStatus();
//		Utils.log("[SpotLib] Handled power controller status of 0x" + Integer.toHexString(status));
		if ((status & IPowerController.COLD_BOOT_EVENT) != 0){
			powerOffHandler.signalEvent();
		}
		if ((status & IPowerController.ALARM_EVENT) != 0){
			alarmHandler.signalEvent();
		}
		if ((status & IPowerController.BUTTON_EVENT) != 0){
			buttonHandler.signalEvent();
		}
		if ((status & IPowerController.LOW_BATTERY_EVENT) != 0){
			lowBatteryHandler.signalEvent();
		}
		if ((status & IPowerController.EXTERNAL_POWER_EVENT) != 0){
			externalPowerHandler.signalEvent();
		}
	}

	/**
	 * Replace the existing handler for power controller time alarms with a user-supplied handler.
	 * It is very unlikely that you will want to create a handler for time alarms.
	 * The default handler logs a message if spot.diagnostics is set.
	 * The previous handler is returned, so you can chain your new handler to that one.
	 * @param handler the new handler to use
	 * @return the previous handler
	 */
	public IEventHandler setAlarmHandler(IEventHandler handler) {
		IEventHandler old = alarmHandler;
		alarmHandler = handler;
		return old;
	}

	/**
	 * Replace the existing handler for reset button presses with a user-supplied handler.
	 * The default handler calls VM.stopVM(0). The previous handler is returned, so you
	 * can chain your new handler to that one.
	 * @param handler the new handler to use
	 * @return the previous handler
	 */
	public IEventHandler setButtonHandler(IEventHandler handler) {
		IEventHandler old = buttonHandler;
		buttonHandler = handler;
		return old;
	}

	/**
	 * Replace the existing handler for poweroff with a user-supplied handler.
	 * The power off event occurs when the user uses the reset button to turn off
	 * the SPOT. The handler has about 400ms to do work before the power goes away.
	 * The default handler does nothing.
	 * The previous handler is returned, so you can chain your new handler to that one.
	 * @param handler the new handler to use
	 * @return the previous handler
	 */
	public IEventHandler setPowerOffHandler(IEventHandler handler) {
		IEventHandler old = powerOffHandler;
		powerOffHandler = handler;
		return old;
	}

	/**
	 * Replace the existing handler for low battery warnings with a user-supplied handler.
	 * The default handler logs a message if spot.diagnostics is set.
	 * The previous handler is returned, so you can chain your new handler to that one.
	 * @param handler the new handler to use
	 * @return the previous handler
	 */
	public IEventHandler setLowBatteryHandler(IEventHandler handler) {
		IEventHandler old = lowBatteryHandler;
		lowBatteryHandler = handler;
		return old;
	}
	
	/**
	 * Replace the existing handler for external power applied events with a user-supplied handler.
	 * The default handler logs a message if spot.diagnostics is set.
	 * The previous handler is returned, so you can chain your new handler to that one.
	 * @param handler the new handler to use
	 * @return the previous handler
	 */
	public IEventHandler setExternalPowerHandler(IEventHandler handler) {
		IEventHandler old = externalPowerHandler;
		externalPowerHandler = handler;
		return old;
	}

	public String getDriverName() {
		return "FIQ Interrupt Daemon";
	}

	public void setUp() {
		initFiq();
		synchronized (deepSleepThreadMonitor) {
			deepSleepThreadMonitor.notify();
		}
	}

	private void initFiq() {
		spotPins.getAttentionPin().claim();
		aic.configure(IAT91_Peripherals.FIQ_ID_MASK, 0, IAT91_AIC.SRCTYPE_EXT_LOW_LEVEL);
		aic.enableIrq(IAT91_Peripherals.FIQ_ID_MASK);
	}

	public void shutDown() {
		tearDown();
	}

	public boolean tearDown() {
		aic.disableIrq(IAT91_Peripherals.FIQ_ID_MASK);
		spotPins.getAttentionPin().release();
		return true;
	}
}
