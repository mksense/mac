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

/**
 * @author Pradip De
 * @version 0.1
 */

package com.sun.spot.peripheral.radio.mhrp.lqrp.linkParams;

public class ConfigLinkParams {
    
    public static final long TIME_WINDOW = 300000; // was 5000;  //Time in milliseconds
    public static final long SLOT_SIZE   =  60000; // was 1000;  //Time in milliseconds
    public static final long NumOfSlots = TIME_WINDOW / SLOT_SIZE;
    public static final double GAMMA = 0.6; //History weight parameter
    public static final double SIGMA = 1.0; //weight distribution between Link Quality and Node Lifetime
    public static final int MAX_LQ = 255;
    public static final double LOW_NORM_LQI = 0.50; // was 0.01;   // not used
    public static final int MAX_DATA_RATE = 250 * 1000; //bits per sec
    public static final double THRESH_LOW_LQI = 0.50; // was 0.02; //Flag a link if quality below this value
    public static final double LOW_LQI_PENALTY_FACTOR = 0.8;//Factor to penalize a link if LQ < Low Threshold
    public static final double MAX_BATT_VOLT = 4500; //millivolts
}
