/*
 * Copyright 2006-2009 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.spot.peripheral.radio.mhrp.lqrp.linkParams;

import com.sun.spot.peripheral.IPowerController;
import com.sun.spot.peripheral.Spot;
import com.sun.spot.peripheral.radio.RadioFactory;
/**
 *
 * @author pradip de pradip.de@sun.com
 */
public class NodeLifetime {
    
    private static final double ENERGY_PER_BIT_TX = 4; //nJ value to transmit a bit
    private static final double ENERGY_PER_BIT_RX = 4; //nj value to receive a bit
    
    private double expectedNodeLifetime;
    
    
    private long disposableSlotRecvdByteCount;
    private long disposableSlotTransByteCount;
    
    private long slotBytesTransCount[];
    private long slotBytesRecvdCount[];
    
    /* begin and end indices for the Time Window Circular Buffer */
    private int firstIndex = 0;
    private int nextIndex = 0;//The next Index to write to at timer expiry
    
    private NodeLifeAndLinkMonitor linkMonitor;
    private long numOfBytesTransInSlot;
    private long numOfBytesRecvdInSlot;
    private int numOfSlots;
    private int windowSize;
    private long bytesTransWindowSum;
    private long bytesRecvdWindowSum;
    private double burnRate;
    private double currNormExpLifetime;
            
            
    NodeLifetime(NodeLifeAndLinkMonitor linkM) {
        numOfSlots = (int)ConfigLinkParams.NumOfSlots;
        this.linkMonitor = linkM;
        slotBytesTransCount = new long[numOfSlots];
        slotBytesRecvdCount = new long[numOfSlots];
        windowSize = 0;
        numOfBytesTransInSlot = 0;
        numOfBytesRecvdInSlot = 0;
        bytesTransWindowSum = 0;
        bytesRecvdWindowSum = 0;
        burnRate = 0;
        currNormExpLifetime = 1.0;
    }
    
    
    public void incBytesTransCount(int bytes) {
        numOfBytesTransInSlot += bytes;
    }
    
    
    public void incBytesRecvdCount(int bytes) {
        numOfBytesRecvdInSlot += bytes;
    }
    
    public double calcTransEnergyForBytes(long byteSize) {
        double energy = 0.0;
        energy = ENERGY_PER_BIT_TX * byteSize * 8;
        return energy;
    }
    
    public double calcRecvEnergyForBytes(long byteSize) {
        double energy = 0.0;
        energy = ENERGY_PER_BIT_RX * byteSize * 8;
        return energy;
    }
    
    
    public void updateBytesProcInSlot() {
        
        //If Window is already full then save
        if (windowSize >= numOfSlots) {
            disposableSlotTransByteCount = slotBytesTransCount[nextIndex];
            disposableSlotRecvdByteCount = slotBytesRecvdCount[nextIndex];
        }
        synchronized (this) {
            slotBytesTransCount[nextIndex] = numOfBytesTransInSlot;
            slotBytesRecvdCount[nextIndex] = numOfBytesRecvdInSlot;
            numOfBytesTransInSlot = 0;
            numOfBytesRecvdInSlot = 0;
        }
        nextIndex = (nextIndex + 1) % numOfSlots;
        if (nextIndex == firstIndex) {
            firstIndex = (firstIndex + 1) % numOfSlots;
        }
        
        if (windowSize <= numOfSlots) {//windowSize grows to a value 1 greater than numOfSlots
            windowSize++;
        }
    }
    
    
    public void calcNodeEnergyBurnRate() {
        int cnt;
        
        double smallWinGammaSum;
        double smallWinMaxLQSum;
        double smallWinMaxRSSISum;
        int i;
        
        //int firstIndex = linkMonitor.getFirstIndex();
        /* nextIndex was incremented after updateSlotAvgLQAndRSSI() */
        int endIndex = nextIndex - 1;
        
                
        double gamma = ConfigLinkParams.GAMMA;
        
        if (endIndex < 0) {
            endIndex = numOfSlots - 1;
        }
        
        if (windowSize == numOfSlots + 1) { //Maximum size reached for history data
            
            bytesTransWindowSum = slotBytesTransCount[endIndex] +
                    bytesTransWindowSum - disposableSlotTransByteCount;
            
            bytesRecvdWindowSum = slotBytesRecvdCount[endIndex] +
                    bytesRecvdWindowSum - disposableSlotRecvdByteCount;
            
            
            
            
        } else { //History data not grown to full window size

            bytesTransWindowSum = slotBytesTransCount[endIndex] + bytesTransWindowSum;
            bytesRecvdWindowSum = slotBytesRecvdCount[endIndex] + bytesRecvdWindowSum;
            burnRate = (calcTransEnergyForBytes(bytesTransWindowSum) +
                            calcRecvEnergyForBytes(bytesRecvdWindowSum)) /
                            ConfigLinkParams.TIME_WINDOW; //uJoules per sec
            double remainingBattFrac = 1.0;
            if (!RadioFactory.isRunningOnHost()) {
                IPowerController powerController = Spot.getInstance().getPowerController();
                int remainingBattVolt = powerController.getVbatt();
                double maxBattVolt = ConfigLinkParams.MAX_BATT_VOLT;
                remainingBattFrac = (double) remainingBattVolt / maxBattVolt;
            }
            synchronized (this) {
                currNormExpLifetime = remainingBattFrac * (1.0 - 1000*burnRate/(ConfigLinkParams.MAX_DATA_RATE*ENERGY_PER_BIT_RX));
                  //Multiplied by 1000 to correct dimensions
            }
        }
        
    }
    
   
    public double getNormNodeLifetime() {
        synchronized (this) {
           return currNormExpLifetime; 
        }
    }
}
