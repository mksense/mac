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

package com.sun.spot.networktools;

import java.io.IOException;

import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;

import com.sun.spot.io.j2me.radiogram.RadiogramConnection;
import com.sun.spot.peripheral.radio.LowPan;
import com.sun.spot.peripheral.radio.LowPanStats;
import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.peripheral.radio.RadioPolicy;
import com.sun.spot.peripheral.radio.routing.RouteInfo;
import com.sun.spot.peripheral.radio.routing.RouteTable;
import com.sun.spot.peripheral.radio.routing.interfaces.IRoutingManager;
import com.sun.spot.service.IService;
import com.sun.spot.util.IEEEAddress;
import java.util.Enumeration;

/**
 * A daemon that responds to network management requests
 */
public class NetManagementServer implements Runnable, IService {
    
    /**
     * the spot property that enables this server
     */
    public static final String systemProperty = "spot.mesh.management.enable";
    /**
     * default port number for this service
     */
    public static final int NET_MANAGEMENT_SERVER_PORT = 20;
    public static final int NET_MANAGEMENT_RECEIVE_PORT = 21;
    private static final String name = "NetManagementServer";
    private static final int ROUTE_CMD = 0x01;
    private static final int STATS_CMD = 0x02;
    private static final int CONFIG_CMD = 0x03;
    private static final int ROUTETABLE_CMD = 0x04;
    private static final int MAX_RETRIES = 3;
    private static RadiogramConnection reqConn;
    private RadiogramConnection respConn;
    private Thread mainThread;
    private int state;
    private static IService netMgr;
    
    /**
     * A server for creating and answering network management related requests
     */
    protected NetManagementServer() {
        // Check legacy property
        String meshOn = System.getProperty("spot.mesh.traceroute.enable");
        if (meshOn != null && "true".equalsIgnoreCase(meshOn)) {
            System.out.println("[notice] spot.mesh.traceroute.enable has been deprecated.  \n" +
                    "[notice] Setting new property " + NetManagementServer.systemProperty);
            this.setEnabled(true);
            if (!RadioFactory.isRunningOnHost()) {
                System.out.println("[notice] Turning off old property: spot.mesh.traceroute.enable");
                RadioFactory.setPersistentProperty("spot.mesh.traceroute.enable", "false");
            }
            RadioFactory.setProperty("spot.mesh.traceroute.enable", "false");
        }
        if (mainThread == null)
            mainThread = new Thread(this, name);
        RadioFactory.setAsDaemonThread(mainThread);
        state = IService.STOPPED;
    }
    /*
     * called if we're started from the manifest file
     */
    public static void main(String[] args) {
        IService netmgr = getNetManagementServer();
        if (netmgr.getEnabled() & !netmgr.isRunning()) {
            netmgr.start();
        }
    }
    /**
     * create and return the NetManagementServer singleton
     * @return a NetManagementServer singleton
     */
    public static synchronized IService getNetManagementServer() {
        if (netMgr == null)
            netMgr = new NetManagementServer();
        return netMgr;
    }
    
    private static Datagram initRequest(long dest) {
        RadioFactory.setProperty("spot.log.connections", "false");
        try {
            reqConn = (RadiogramConnection)Connector.open("radiogram://" + dest + ":" +
                    NET_MANAGEMENT_SERVER_PORT);
            reqConn.setTimeout(10000);
            Datagram dg = reqConn.newDatagram(reqConn.getMaximumLength());
            return dg;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private Datagram initResponse(String dest) {
        try {
            respConn = (RadiogramConnection)Connector.open("radiogram://" + dest + ":" +
                    NET_MANAGEMENT_RECEIVE_PORT);
            Datagram dg = respConn.newDatagram(respConn.getMaximumLength());
            return dg;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private static void cleanupRequest() {
        if (reqConn != null) {
            try {
                reqConn.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void cleanupResponse() {
        if (respConn != null) {
            try {
                respConn.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private static Datagram makeRequest(Datagram datagram) {
        RadiogramConnection respConn=null;
        Datagram dg = null;
        if (reqConn != null) {
            for (int i=0; i< MAX_RETRIES; i++) {
                try {
                    respConn = (RadiogramConnection)Connector.open("radiogram://" + datagram.getAddress() + ":" +
                            NET_MANAGEMENT_RECEIVE_PORT);
                    respConn.setTimeout(8000);
                    dg = respConn.newDatagram(respConn.getMaximumLength());
                    reqConn.send(datagram);
                    
                    respConn.receive(dg);
                    
                } catch (IOException e) {
                    dg=null;
                } finally {
                    try {
                        if (respConn != null)  respConn.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return dg;
    }
    
    
    /**
     * retrieve route information to a destination from a remote node
     * @param src the node to be queried for information
     * @param dst the address of the route destination of interest
     * @return a string value that contains the routing information and number of hops
     */
    public static String requestRoute(long src, long dst) {
        String result=null;
        Datagram dg = initRequest(src);
        if (dg != null) {
            dg.reset();
            try {
                dg.writeByte(ROUTE_CMD);
                dg.writeLong(dst);
                dg = makeRequest(dg);
                if (dg != null) {
                    result = dg.readUTF();
                } else result = null;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                cleanupRequest();
            }
        } else
            return null;
        
        return result;
    }
    
    private void doTraceRoute(Datagram datagram, Datagram response) {
        IRoutingManager routingManager = LowPan.getInstance().getRoutingManager();
        try {
            long requestedAddress = datagram.readLong();
            RouteInfo routeInfo = routingManager.getRouteInfo(requestedAddress);
            
            response.reset();
            response.writeUTF(routeInfo.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * Retrieve Route Table from a remote node
     * @param target address of the target node
     * @return A RouteTable object with a snapshot of the route info from the node
     */
    public static RouteTable requestRouteTable(long target) {
        RouteTable rt = null;
        Datagram dg = initRequest(target);
        if (dg != null) {
            dg.reset();
            try {
                dg.writeByte(ROUTETABLE_CMD);
                dg = makeRequest(dg);
                if (dg != null) {
                    // parse result
                    int size = dg.readInt(); // number of route entries
                    // Each entry is two longs and a short
                    rt = new RouteTable();
                    for (int i=0; i<size; i++) {
                        rt.addEntry(new RouteInfo(dg.readLong(), dg.readLong(), dg.readShort()));
                    }
                } else rt = null;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                cleanupRequest();
            }
        } else
            return null;
        return rt;
    }
    
    private void doRouteTable(Datagram datagram, Datagram response) {
        // Right now, we just dump the whole object
        response.reset();
        RouteTable rt = LowPan.getInstance().getRoutingManager().getRoutingTable();
        Enumeration en = rt.getAllEntries();
        try {
            response.writeInt(rt.getSize());
            while (en.hasMoreElements()) {
                RouteInfo ri = (RouteInfo)en.nextElement();
                response.writeLong(ri.destination);
                response.writeLong(ri.nextHop);
                response.writeShort(ri.hopCount);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * Retrieve LowPan statistics from a remote node
     * @param target address of the target node
     * @return A LowPanStats object with a snapshot of the statistics from the node
     */
    public static LowPanStats requestStats(long target) {
        LowPanStats lps=null;
        Datagram dg = initRequest(target);
        if (dg != null) {
            dg.reset();
            try {
                dg.writeByte(STATS_CMD);
                dg = makeRequest(dg);
                if (dg != null) {
                    // parse result
                    int size = dg.readInt();
                    byte b[] = new byte[size];
                    for (int i=0; i<size; i++) {
                        b[i] = dg.readByte();
                    }
                    lps = new LowPanStats(b);
                } else lps = null;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                cleanupRequest();
            }
            
        } else
            return null;
        
        return lps;
    }
    
    private void doStats(Datagram datagram, Datagram response) {
        // Right now, we just dump the whole object
        response.reset();
        LowPanStats lps = ((LowPan)LowPan.getInstance()).getStatistics();
        byte b[] = lps.toByteArray();
        try {
            response.writeInt(b.length);
            for (int i=0; i<b.length; i++)
                response.writeByte(b[i]);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void doConfig(Datagram datagram, Datagram response) {
        // Clearly -- not implemented yet.
        
    }
    
    /**
     * main execution thread of this server
     */
    public void run() {
        System.out.println("[NetManagementServer] starting on port " + NET_MANAGEMENT_SERVER_PORT);
        try {
            RadiogramConnection conn = (RadiogramConnection)Connector.open("radiogram://:" +
                    NET_MANAGEMENT_SERVER_PORT);
            conn.setRadioPolicy(RadioPolicy.AUTOMATIC);
            Datagram datagram = conn.newDatagram(conn.getMaximumLength());
            
            while (state != IService.STOPPING) {
                conn.receive(datagram);
                boolean valid = true;
                Datagram response = initResponse(datagram.getAddress());
                // First byte is the command.  we assume the rest of the packet can be read by the
                // targeted method.
                byte cmd = datagram.readByte();
                switch (cmd) {
                    case ROUTE_CMD:
                        doTraceRoute(datagram, response);
                        break;
                        
                    case STATS_CMD:
                        doStats(datagram, response);
                        break;
                        
                    case CONFIG_CMD:
                        doConfig(datagram, response);
                        break;
                        
                    case ROUTETABLE_CMD:
                        doRouteTable(datagram, response);
                        break;
                        
                    default:
                        valid = false;
                        break;
                }
                // Each method should reset the datagram and create the return message.  We send the return
                // message here
                if (valid) {
                    try {
                        respConn.send(response);
                        cleanupResponse();
                    } catch (IOException e) {
                        System.err.println("[NetManagementServer] failed to send reply to " + new IEEEAddress(response.getAddress()));
                    }
                }
            }
            // Must be stopping.  Clean up and exit
            conn.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        state = IService.STOPPED;
        System.out.println("[NetManagementServer] exiting");
    }
    
    /**
     * Assign a name to this service. For some fixed services this may not apply and
     * any new name will just be ignored.
     *
     * @param who the name for this service
     */
    public void setServiceName(String who) {
    }
    
    /**
     * Enable/disable whether service is started automatically.
     * This may not apply to some services and calls to setEnabled() may be ignored.
     *
     * @param enable true if the service should be started automatically on reboot
     */
    public void setEnabled(boolean enable) {
        if (!RadioFactory.isRunningOnHost()) {
            RadioFactory.setPersistentProperty(systemProperty, String.valueOf(enable));
        }
        RadioFactory.setProperty(systemProperty, String.valueOf(enable));
    }
    
    /**
     * Stop the service, and return whether successful.
     * Stops all running threads. Closes any open IO connections.
     *
     * @return true if the service was successfully stopped
     */
    public boolean stop() {
        state = IService.STOPPING;
        return true;
    }
    
    /**
     * Start the service, and return whether successful.
     *
     * @return true if the service was successfully started
     */
    public boolean start() {
        state = IService.STARTING;
        if (mainThread != null) {
            mainThread.start();
        }
        if (mainThread != null) {
            state = IService.RUNNING;
        } else
            state = IService.STOPPED;
        
        return (state == IService.RUNNING);
    }
    
    /**
     * Resume the service, and return whether successful.
     * Picks up from state when service was paused.
     *
     * If there was no particular state associated with this service
     * then resume() can be implemented by calling start().
     *
     * @return true if the service was successfully resumed
     */
    public boolean resume() {
        return start();
    }
    
    /**
     * Pause the service, and return whether successful.
     * Preserve any current state, but do not handle new requests.
     * Any running threads should block or sleep.
     * Any open IO connections may be kept open.
     *
     * If there is no particular state associated with this service
     * then pause() can be implemented by calling stop().
     *
     * @return true if the service was successfully paused
     */
    public boolean pause() {
        return stop();
    }
    
    /**
     * Return whether the service is currently running.
     *
     * @return true if the service is currently running
     */
    public boolean isRunning() {
        return (state == IService.RUNNING);
    }
    
    /**
     * Return the current status of this service.
     *
     * @return the current status of this service, e.g. STOPPED, STARTING, RUNNING, PAUSED, STOPPING, etc.
     */
    public int getStatus() {
        return state;
    }
    
    /**
     * Return the name of this service.
     *
     * @return the name of this service
     */
    public String getName() {
        return getServiceName();
    }
    /**
     * return the name of the NetManagementServer
     * @return the name of this service
     */
    public String getServiceName() {
        return name;
    }
    
    /**
     * Return whether service is started automatically on reboot.
     * This may not apply to some services and for those services it will always return false.
     *
     * @return true if the service is started automatically on reboot
     */
    public boolean getEnabled() {
        String meshOn = System.getProperty(systemProperty);
        return (meshOn != null && "true".equalsIgnoreCase(meshOn));
    }
}
