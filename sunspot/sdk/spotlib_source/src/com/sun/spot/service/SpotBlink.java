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

/**
 * A simple service to blink the two LEDs on the SPOT main processor board.
 *
 * @author Ron Goldman
 */
public class SpotBlink extends BasicService implements ISpotBlink {

    private static final int CYCLE_TIME_MILLIS = 500;
    private static final int DEFAULT_DURATION = 5;

    public SpotBlink() {
    }

    /**
     * Do the default Blink action.
     */
    public void blink() {
        blink(DEFAULT_DURATION);
    }

    /**
     * Do the Blink action for a given time period.
     *
     * @param durationInSecs time to blink
     */
    public void blink(int durationInSecs) {
        final int blinkCount = durationInSecs;
        new Thread("SpotBlink") {
            public void run() {
                ILed redLED = Spot.getInstance().getRedLed();
                ILed greenLED = Spot.getInstance().getGreenLed();
                for (int i = 0; i < blinkCount; i++) {
                    redLED.setOn();
                    greenLED.setOn();
                    Utils.sleep(CYCLE_TIME_MILLIS / 2);
                    redLED.setOff();
                    greenLED.setOff();
                    Utils.sleep(CYCLE_TIME_MILLIS / 2);
                }
            }
        }.start();
    }

    public String getServiceName() {
        return "SPOT Blink service";
    }

}
