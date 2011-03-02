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

package com.sun.spot.peripheral.radio;

import com.sun.spot.io.j2me.radiogram.Radiogram;
import com.sun.spot.peripheral.NoRouteException;
import com.sun.spot.peripheral.radio.routing.RouteInfo;

import com.sun.spot.peripheral.radio.routing.RouteTable;
import com.sun.spot.peripheral.radio.routing.interfaces.IRoutingManager;
import com.sun.spot.peripheral.radio.routing.interfaces.RouteEventClient;
import com.sun.spot.service.BasicService;
import com.sun.spot.service.ISpotRadioHelper;
import com.sun.spot.service.ServiceRegistry;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Utils;
import java.util.Enumeration;
import java.util.Vector;
import javax.microedition.io.Datagram;

/**
 * A facade so OTA can access low-level radio features.
 *
 * @author Ron
 */
public class LowPanSpotRadioHelper extends BasicService implements ISpotRadioHelper, RouteEventClient {

    /**
     * Static method to register the LowPan radio stack with the base OTA code.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        Utils.log("[LowPan] radio stack initialization...");
        LowPanSpotRadioHelper rm = new LowPanSpotRadioHelper();
        ServiceRegistry.getInstance().add(rm);
    }

    public String getServiceName() {
        return "LowPan Radio Stack service";
    }

    /**
     * Tell others the Datagram protocol name for use with GCF.
     *
     * @return Datagram protocol name for use with GCF
     */
    public String getDatagramConnectionProtocol() {
        return "radiogram";
    }

    /**
     * Tell others the Stream protocol name for use with GCF.
     *
     * @return Stream protocol name for use with GCF
     */
    public String getStreamConnectionProtocol() {
        return "radiostream";
    }

    /**
     * Returns the link quality for the received datagram as a combination
     * of RSSI and CORR.
     * 
     * Values should range from 0 (unreadable) to 100 (perfect).
     * If link quality does not apply to this connection return -1.
     *
     * @param dg Datagram
     * @return link quality or -1 if cannot compute link quality
     */
    public int getLinkQuality(Datagram dg) {
        if (dg instanceof Radiogram) {
            int corr = ((Radiogram)dg).getCorr();
            if (corr == 0) {
                return -1;
            }
            if (corr < 51) {            // for CC2420 lowest CORR value is 50?
                corr = 51;
            } else if (corr > 100) {    // for CC2420 highest CORR value is 110
                corr = 100;
            }
            int corrLQI = (corr - 50) * 2; // maps to [1, 100]
            return corrLQI;                      // maps to [1, 100]
        } else {
            return -1;
        }
    }

    /**
     * Returns the link strength for the received datagram.
     * Values should range from 0 (weak) to 100 (strong).
     * If transport layer does not have a link strength metric then
     * return -1.
     *
     * @param dg Datagram
     * @return link strength or -1
     */
    public int getLinkStrength(Datagram dg) {
        if (dg instanceof Radiogram) {
            int rssi = ((Radiogram)dg).getRssi();
            if (rssi < -49) {           // for SPOT CC2420 RSSI ranges from -50 to 30
                rssi = -49;
            } else if (rssi > 50) {
                rssi = 50;
            }
            return (rssi + 50);                      // maps to [1, 100]
        } else {
            return -1;
        }
    }

    /**
     * Return the time that a Datagram packet was actually received/sent.
     *
     * @param dg the Datagram to check
     * @return time that the Datagram packet was actually received/sent

     */
    public long getTimestamp(Datagram dg) {
        long result = 0;
        if (dg instanceof Radiogram) {
            result = ((Radiogram)dg).getTimestamp();
        }
        if (result == 0) {
            result = System.currentTimeMillis();
        }
        return result;
    }

    /**
     * Tell if datagram was sent as a broadcast message.
     *
     * @param dg the Datagram to check
     * @return true if the Datagram had been broadcast
     */
    public boolean isBroadcast(Datagram dg) {
        if (dg instanceof Radiogram) {
            return ((Radiogram)dg).isBroadcast();
        } else {
            return false;
        }
    }

    /**
     * Return the name of the current Routing Manager.
     *
     * @return the name of the current Routing Manager
     */
    public String getRoutingManagerName() {
        return LowPan.getInstance().getRoutingManager().getServiceName();
    }

    /**
     * Return information on the current route(s) to the specified destination address.
     *
     * @param address the specified destination address
     * @return the current route(s) to the specified destination address
     */
    public RouteInfo[] getRouteInfo(long address) {
        IRoutingManager rm = LowPan.getInstance().getRoutingManager();
        RouteInfo info = rm.getRouteInfo(address);
        if (info != null && info.nextHop != com.sun.spot.peripheral.radio.mhrp.aodv.Constants.INVALID_NEXT_HOP) {
            RouteInfo[] results = { info };
            return results;
        } else {
            try {
                Vector v = new Vector();
                rm.findRoute(address, this, v);
                synchronized (v) {
                    v.wait(5000);
                }
                if (!v.isEmpty()) {
                    RouteInfo[] results = { (RouteInfo)v.firstElement() };
                    return results;
                }
            } catch (InterruptedException ex) {
                System.out.println("Timeout trying to find route to " + IEEEAddress.toDottedHex(address));
            } catch (NoRouteException ex) {
                System.out.println("NoRouteException trying to find route to " + IEEEAddress.toDottedHex(address) + " " + ex.getMessage());
            }
            return null;
        }
    }

    /**
     * Callback for findRouteInfo().
     * 
     * @param info
     * @param uniqueKey
     */
    public void routeFound(RouteInfo info, Object uniqueKey) {
        ((Vector)uniqueKey).addElement(info);
        synchronized (uniqueKey) {
            uniqueKey.notify();
        }
    }

    /**
     * Return an array of the current route info to all known addresses.
     *
     * @return the current route info to all known addresses
     */
    public RouteInfo[] getRouteInfo() {
        RouteTable table = LowPan.getInstance().getRoutingManager().getRoutingTable();
        if (table.getSize() > 0) {
            RouteInfo[] results = new RouteInfo[table.getSize()];
            Enumeration e = table.getAllEntries();
            int i = 0;
            while (e.hasMoreElements()) {
                results[i++] = (RouteInfo)e.nextElement();
            }
            return results;
        } else {
            return null;
        }
    }

    /**
     * Check whether this Routing Manager allows writing to the Routing Table.
     *
     * @return true if Routing Table is mutable
     */
    public boolean isMutableRoutingManager() {
        return false;
    }

    // to be determined later:
    //    methods to set the route table

    /*

    void setRoute(long destination, long nextHop, int hopCount);

    void setRoutingTable(String table);

    void clearRoutingTable();

    void saveRoutingTable();

     */

}
