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

import com.sun.spot.util.IEEEAddress;


/**
 *
 * @author pradip de pradip.de@sun.com
 */
public class NbrLinkInfo {
        
    private double gamma = ConfigLinkParams.GAMMA;
    /* Circular buffers for maintaining the average signal strength params for each slot */
    private double slotAvgLQ[];
    
    private long numOfPktsRecvdInSlot;//Num of pkts in the current slot
    
    /* Sum of the signal values of all packets in current slot */
    private long sumLQ;
    
   
    private double currSlotMaxLQ = 0.0;

    /* The normalized values of the History Time Window weighted Link Params that the Routing Layer *
     * should build its Cost Metric on                                                              *
     */
    private double currNormalizedLQ;

    private double nbrLQ = -1.0;   // How well our neighbor can hear our packets
    private double nbrOurLQ;       // How well we can hear our neighbor's packets
    private long nbrLastHeard;     // When did we last hear from this neighbor
    private long nbrLastLPREQ = 0; // When did we last send an LPREQ to this neighbor
    private long nbrLastLPREP = 0; // When did we last receive an LPREP from this neighbor

    private long nbrAddress;
    
    private NodeLifeAndLinkMonitor linkMonitor;
    
    private double currAvgLQ = 0;
    
    /** Creates a new instance of NbrLinLinkMonitor linkM, long address) {
     * numOfSlots = (int)ConfigLinkParakInfo */
    
    public NbrLinkInfo(NodeLifeAndLinkMonitor linkM, long address) {

        this.linkMonitor = linkM;
        
        numOfPktsRecvdInSlot = 0;
        sumLQ = 0;

        currNormalizedLQ = 0.93;   // Initialize to a typical value for links
        nbrAddress = address;
    }

    /**
     * Return the current link cost, which is the cost of the worst direction.
     *
     * @return current link cost
     */
    public double getCurrentLinkCost() {
        return (nbrLQ < 0) ? currNormalizedLQ : Math.min(currNormalizedLQ, nbrLQ);
    }
    
    public void updateSlotAvgLQ() {
        synchronized (this) {
            if (numOfPktsRecvdInSlot != 0) {
                currAvgLQ = ((double)sumLQ)/numOfPktsRecvdInSlot;
                currSlotMaxLQ = ConfigLinkParams.MAX_LQ;
            } else {
                currAvgLQ = 0.0;
                currSlotMaxLQ = 0.0;
            }
            sumLQ = 0;
            numOfPktsRecvdInSlot = 0;
        }
    }
    
    public void calcTimeWeightedLQ() {
        synchronized (this) {
            if (currAvgLQ > 0.0 ) {
                currNormalizedLQ = gamma * currNormalizedLQ + (1 - gamma)*(currAvgLQ/currSlotMaxLQ);
            } else {
                // if we haven't heard anything then assume nothing has changed
                // currNormalizedLQ = gamma * currNormalizedLQ + (1 - gamma)*ConfigLinkParams.LOW_NORM_LQI; //A low value for the normalized LQI
            }
            
        }
        //System.out.println("CurrNormLQ : " + currNormalizedLQ + "\n");
    }
    
    public long getNbrAddress() {
        return nbrAddress;
    }
    
    public long getNbrLastHeard() {
        return nbrLastHeard;
    }

    public void setNbrLastHeard(long when) {
        nbrLastHeard = when;
    }
    
    public long getNbrLastLQREQ() {
        return nbrLastLPREQ;
    }

    public void setNbrLastLQREQ(long when) {
        nbrLastLPREQ = when;
    }

    public long getNbrLastLQREP() {
        return nbrLastLPREP;
    }

    public void setNbrLastLQREP(long when) {
        nbrLastLPREP = when;
    }

    public double getNbrLQ() {
        return nbrLQ;
    }

    public void setNbrLQ(double cost) {
        nbrLQ = cost;
    }

    public double getOurNbrLQ() {
        return nbrOurLQ;
    }

    public void setOurNbrLQ(double cost) {
        nbrOurLQ = cost;
    }

    public long getNumOfPktsInSlot() {
        return numOfPktsRecvdInSlot;
    }
    
    public void setNumOfPktsInSlot(long numOfPktsInSlot) {
        this.numOfPktsRecvdInSlot = numOfPktsInSlot;
    }
    
    public void incNumOfPktsInSlot() {
        numOfPktsRecvdInSlot++;
    }

    public long getSumLQ() {
        return sumLQ;
    }
    
    public void setSumLQ(long sumLQ) {
        this.sumLQ = sumLQ;
    }
    
    public void incSumLQ(long sumLQ) {
        this.sumLQ += sumLQ;
    }
        
    public double getCurrNormalizedLQ() {
        return currNormalizedLQ;
    }
    
    //!!!! for testing purposes only
    public void setCurrNormalizedLQ(double lq) {
        currNormalizedLQ = lq;
    }
    
    
}
