/*
 * Copyright 2007-2009 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.spot.peripheral.ota;

import com.sun.spot.service.ISpotRadioHelper;
import com.sun.spot.imp.MIDletDescriptor;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

import com.sun.spot.imp.MIDletSuiteDescriptor;
// import com.sun.spot.io.j2me.radiogram.Radiogram;
// import com.sun.spot.io.j2me.radiogram.RadiogramConnection;
import com.sun.spot.peripheral.IMultipleHopConnection;
import com.sun.spot.peripheral.IPowerController;
import com.sun.spot.peripheral.IRadioControl;
import com.sun.spot.peripheral.ISleepManager;
import com.sun.spot.peripheral.ITimeoutableConnection;
import com.sun.spot.peripheral.IUSBPowerDaemon;
import com.sun.spot.peripheral.Spot;
import com.sun.spot.peripheral.TimeoutException;
import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.peripheral.radio.routing.RouteInfo;
import com.sun.spot.service.ServiceRegistry;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Properties;
import com.sun.spot.util.Utils;
import com.sun.squawk.VM;
import com.sun.squawk.util.StringTokenizer;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import javax.microedition.io.DatagramConnection;

/**
 * Provides support for executing commands from Solarium (formerly called SpotWorld).
 * 
 * @author vgupta
 */
public class SpotWorldCommand implements ISpotAdminConstants, IOTACommand {
    private static final int MAJOR_VERSION     = 1;
    private static final int MINOR_VERSION     = 0;

    /**
     * Security levels: see OTACommandProcessor. Level 2 implies only the
     * SPOT owner can execute this command.
     */
    private static final int SECURITY_LEVEL_REQUIRE_OWNERSHIP = 2;
    private static final int SECURITY_LEVEL_ALLOW_ANYONE      = 4;

    private static int pingCounter = 1;

    /** Creates a new instance of SpotWorldCommand */
    public SpotWorldCommand() {
    }
    
    public int getSecurityLevelFor(String command) {
        if (    command.equals(GET_MEMORY_STATS_CMD) ||
                command.equals(GET_POWER_STATS_CMD) ||
                command.equals(GET_SLEEP_INFO_CMD) ||
                command.equals(GET_AVAILABLE_SUITES_CMD) ||
                command.equals(GET_SUITE_MANIFEST_CMD) ||
                command.equals(GET_APP_STATUS_CMD) ||
                command.equals(GET_ALL_APPS_STATUS_CMD) ||
                command.equals(REMOTE_GET_PHYS_NBRS_CMD) ||
                command.equals(GET_RADIO_INFO_CMD) ||
                command.equals(GET_ROUTE_CMD) ) {
            return SECURITY_LEVEL_ALLOW_ANYONE;
        }
        
        if (    command.equals(START_APP_CMD) ||
                command.equals(PAUSE_APP_CMD) ||
                command.equals(RESUME_APP_CMD) ||
                command.equals(STOP_APP_CMD) ||
                command.equals(START_REMOTE_PRINTING_CMD) ||
                command.equals(STOP_REMOTE_PRINTING_CMD) ||
                command.equals(SET_RADIO_INFO_CMD) ||
                command.equals(RECEIVE_APP_CMD) ||
                command.equals(MIGRATE_APP_CMD) ) {
            return SECURITY_LEVEL_REQUIRE_OWNERSHIP;
        }
        
        return SECURITY_LEVEL_REQUIRE_OWNERSHIP;
    }

    public boolean processCommand(String command, DataInputStream params, 
            IOTACommandHelper helper) throws IOException {
        boolean result = true;
        byte[] encodedResponse = null;
        String suiteId = null;
        int midletId = 0;
        String isolateId = null;
        String res = null;
        
        if (command.equals(GET_MEMORY_STATS_CMD)) {
            // Utils.log("getmemstats called");
            try {
                int fmem = 0;
                int tmem = 0;
                int idx = 0;
                
                encodedResponse = new byte[10];
                encodedResponse[idx++] = (byte) MAJOR_VERSION;
                encodedResponse[idx++] = (byte) MINOR_VERSION;
                fmem = (int) Runtime.getRuntime().freeMemory();
                Utils.writeBigEndInt(encodedResponse, idx, fmem);
                idx += 4;
                tmem = (int) Runtime.getRuntime().totalMemory();
                Utils.writeBigEndInt(encodedResponse, idx, tmem);
                idx += 4;
            } catch (Exception e) {
                System.err.println("getmemstats caught " + e);
                e.printStackTrace();
                helper.sendErrorDetails("Failed: " + "getmemstats caught " + e);
            }
                        
        } else if (command.equals(GET_POWER_STATS_CMD)) {
            // Utils.log("getpowerstats called");
            try {
                encodedResponse = createPowerStatsResponse();
            } catch (Exception e) {
                System.err.println("getpowerstats caught " + e);
                e.printStackTrace();
                encodedResponse = createResponse("Failed: " +
                        "getpowerstats caught " + e);
            }
            
        } else if (command.equals(GET_SLEEP_INFO_CMD)) {
            // Utils.log("getsleepinfo called");
            try {                
                // Create a response ...
                encodedResponse = createSleepInfoResponse();
            } catch (Exception e) {
                System.err.println("getsleepinfo caught " + e);
                e.printStackTrace();
                encodedResponse = createResponse("Failed: " +
                        "getsleepinfo caught " + e);
            }
            
        } else if (command.equals(GET_AVAILABLE_SUITES_CMD)) {
            // Utils.log("getavailablesuites called");
            try {
                // Create a response ...
                encodedResponse = createAvailableSuitesResponse();
            } catch (Exception e) {
                System.err.println("getavailablesuites caught " + e);
                e.printStackTrace();
                encodedResponse = createResponse("Failed: " +
                        "getavailablesuites caught " + e);
            }
            
        } else if (command.equals(GET_SUITE_MANIFEST_CMD)) {
            // Utils.log("getsuitemanifest called");
            try {
                suiteId = params.readUTF();
                // Utils.log("Executing GET_SUITE_MANIFEST_CMD with " +
                //        "suiteId: " + suiteId);
                
                // Create a response ...
                encodedResponse = createSuiteManifestResponse(suiteId);
            } catch (Exception e) {
                System.err.println("getsuitemanifest caught " + e);
                e.printStackTrace();
                encodedResponse = createResponse("Failed: " +
                        "getsuitemanifest caught " + e);
            }
            
        } else if (command.equals(START_APP_CMD)) {
            // Utils.log("startapp called");
            try {
                suiteId = params.readUTF();
                midletId = params.readInt();
//                Utils.log("Executing START_APP_CMD with " +
//                        "suiteId: " + suiteId + ", midletId: " + midletId);
                
                // Create a response ...
                res = IsolateManager.startApp(suiteId, midletId);
            } catch (Exception e) {
                System.err.println("startapp caught " + e);
                e.printStackTrace();
                res = "Failed: startapp caught " + e;
            }
            
            encodedResponse = createResponse(res);
            
        } else if (command.equals(PAUSE_APP_CMD)) {
            // Utils.log("pauseapp called");
            try {
                isolateId = params.readUTF();                
//                Utils.log("Executing PAUSE_APP_CMD with " +
//                        "isolateId: " + isolateId);                
                // Create a response ...
                res = IsolateManager.pauseApp(isolateId);
            } catch (Exception e) {
                System.err.println("pauseapp caught " + e);
                e.printStackTrace();
                res = "Failed: pauseapp caught " + e;
            }
            
            encodedResponse = createResponse(res);
            
        } else if (command.equals(RESUME_APP_CMD)) {
            // Utils.log("resumeapp called");
            try {
                isolateId = params.readUTF();                
                // Utils.log("Executing RESUME_APP_CMD with " +
                //        "isolateId: " + isolateId);
                
                // Create a response ...
                res = IsolateManager.resumeApp(isolateId);
            } catch (Exception e) {
                System.err.println("resumeapp caught " + e);
                e.printStackTrace();
                res = "Failed: resumeapp caught " + e;
            }
            
            encodedResponse = createResponse(res);
            
        } else if (command.equals(STOP_APP_CMD)) {
            // Utils.log("stopapp called");
            try {
                isolateId = params.readUTF();
                // Utils.log("Executing STOP_APP_CMD with " +
                // "isolateId: " + isolateId);
                
                // Create a response ...
                res = IsolateManager.stopApp(isolateId);
            } catch (Exception e) {
                System.err.println("stopapp caught " + e);
                e.printStackTrace();
                res = "Failed: stopapp caught " + e;
            }
            
            encodedResponse = createResponse(res);
            
        } else if (command.equals(GET_ALL_APPS_STATUS_CMD)) {
            // Utils.log("getallappsstatus called");
            try {
                // Create a response ...
                encodedResponse = createAllAppsStatusResponse(
                        IsolateManager.getAllAppsStatus());
            } catch (Exception e) {
                System.err.println("getallappsstatus caught " + e);
                e.printStackTrace();
                encodedResponse = createAllAppsStatusResponse(null);
            }
            
        } else if (command.equals(GET_APP_STATUS_CMD)) {
            // Utils.log("getappstatus called");
            try {
                isolateId = params.readUTF();
                // Utils.log("Executing GET_APP_STATUS with " +
                //        "isolateId: " + isolateId);
                
                // Create a response ...
                res = IsolateManager.getAppStatus(isolateId);
            } catch (Exception e) {
                System.err.println("getappstatus caught " + e);
                e.printStackTrace();
                res = "Failed: getappstatus caught " + e;
            }
            
            encodedResponse = createResponse(res);
            
        } else if (command.equals(START_REMOTE_PRINTING_CMD)) {
            // Utils.log("startremoteprinting called");
            try {
                isolateId = params.readUTF();
                String address = params.readUTF();
                // Utils.log("Executing START_REMOTE_PRINTING_CMD with " +
                //        "isolateId: " + isolateId +
                //        ", address: " + address);
                
                // Create a response ...
                res = IsolateManager.startRemotePrinting(isolateId, address);
            } catch (Exception e) {
                System.err.println("startremoteprinting caught " + e);
                e.printStackTrace();
                res = "Failed: startremoteprinting caught " + e;
            }
            
            encodedResponse = createResponse(res);
            
        } else if (command.equals(STOP_REMOTE_PRINTING_CMD)) {
            // Utils.log("startremoteprinting called");
            try {
                isolateId = params.readUTF();
                String address = params.readUTF();
                String port = params.readUTF();
                // Utils.log("Executing STOP_REMOTE_PRINTING_CMD with " +
                //        "isolateId: " + isolateId +
                //        ", address: " + address + 
                //        ", port: " + port);
                
                // Create a response ...
                res = IsolateManager.stopRemotePrinting(isolateId, address, port);
            } catch (Exception e) {
                System.err.println("stopremoteprinting caught " + e);
                e.printStackTrace();
                res = "Failed: stopremoteprinting caught " + e;
            }
            
            encodedResponse = createResponse(res);
            
        } else if (command.equals(MIGRATE_APP_CMD)) {
            // Utils.log("migrateapp called");
            try {
                isolateId = params.readUTF();
                String address = params.readUTF();
                String copyFlag = params.readUTF().toLowerCase();
//                Utils.log("Executing MIGRATE_APP_CMD with " +
//                        "isolateId: " + isolateId + ", address: " + 
//                        address + ", copyFlag: " + copyFlag);
                
                // Create a response ...
                res = IsolateManager.migrateApp(isolateId, address,
                        (copyFlag.startsWith("t") ? true : false));
            } catch (Exception e) {
                System.err.println("migrateapp caught " + e);
                e.printStackTrace();
                res = "Failed: migrateapp caught " + e;
            }
            
            encodedResponse = createResponse(res);
            
        } else if (command.equals(RECEIVE_APP_CMD)) {
            // Utils.log("receiveapp called");
            try {
                isolateId = params.readUTF();
                String address = params.readUTF();
//                Utils.log("Executing RECEIVE_APP_CMD with " +
//                        "isolateId: " + isolateId + ", address: " + 
//                        address);
                
                // Create a response ...
                res = IsolateManager.receiveApp(isolateId, address);
            } catch (Exception e) {
                System.err.println("receiveapp caught " + e);
                e.printStackTrace();
                res = "Failed: receiveapp caught " + e;
            }
            
            encodedResponse = createResponse(res);
            
        } else if (command.equals(GET_SPOT_PROPERTY_CMD)) {
            // Utils.log("getspotproperty called");
            try {
                String listOfKeys = params.readUTF();
                // Utils.log("Executing GET_SPOT_PROPERTY_CMD with " +
                //        "listOfKeys: " + listOfKeys);
                
                // Create a response ...
                encodedResponse = createSpotPropertyResponse(listOfKeys);
            } catch (Exception e) {
                System.err.println("getspotproperty caught " + e);
                e.printStackTrace();
                encodedResponse = createResponse("Failed: " +
                        "getspotproperty caught " + e);
            }
            
        } else if (command.equals(REMOTE_GET_PHYS_NBRS_CMD)) {
            try {
                encodedResponse = createRemotePingResponse();
            } catch (Exception e) {
                System.err.println("Error doing a remote Ping: " + e);
                encodedResponse = createResponse("Failed: remote Ping caught " + e);
            }

        } else if (command.equals(GET_ROUTE_CMD)) {
            try {
                long dest = params.readLong();

                encodedResponse = createGetRouteResponse(dest);
            } catch (Exception e) {
                System.err.println("Error doing get route: " + e);
                encodedResponse = createResponse("Failed: getRoute caught " + e);
            }

        } else if (command.equals(GET_RADIO_INFO_CMD)) {
            try {
                encodedResponse = createGetRadioInfoResponse();
            } catch (Exception e) {
                System.err.println("Error doing get radio info: " + e);
                encodedResponse = createResponse("Failed: getRadioInfo caught " + e);
            }

        } else {
            result = false; // command was not recognized
        }

        if (result) {
            helper.sendData(encodedResponse, 0, encodedResponse.length);
            helper.sendPrompt();
        }

        return result;
    }
    
    private byte[] createPowerStatsResponse() {
        IPowerController pctrl = Spot.getInstance().getPowerController();
        IUSBPowerDaemon usbpd = Spot.getInstance().getUsbPowerDaemon();
        String pctrlRev = pctrl.getRevision();
        byte[] pctrlRevbytes = pctrlRev.getBytes();
        
        // 2 bytes of version, 2 bytes of rev string length, pctrl 
        // rev string, followed by 11 short values followed by a long value
        // followed by a boolean followed by a short
        byte[] val = new byte[2 + 2 + pctrlRevbytes.length +
                11 * 2 + 8 + 1 + 2];
        int idx = 0;
        
        val[idx++] = (byte) MAJOR_VERSION;
        val[idx++] = (byte) (MINOR_VERSION + 2); // externallyPowered flag added, pctcharge added
        
        Utils.writeBigEndShort(val, idx, pctrlRevbytes.length);
        idx += 2;
        System.arraycopy(pctrlRevbytes, 0, val, idx, pctrlRevbytes.length);
        idx += pctrlRevbytes.length;
        Utils.writeBigEndShort(val, idx, (short) pctrl.getIcharge());
        idx += 2;
        Utils.writeBigEndShort(val, idx, (short) pctrl.getIdischarge());
        idx += 2;
        Utils.writeBigEndShort(val, idx, (short) pctrl.getIMax());
        idx += 2;
        Utils.writeBigEndShort(val, idx, (short) pctrl.getVbatt());
        idx += 2;
        Utils.writeBigEndShort(val, idx, (short) pctrl.getVcc());
        idx += 2;
        Utils.writeBigEndShort(val, idx, (short) pctrl.getVcore());
        idx += 2;
        Utils.writeBigEndShort(val, idx, (short) pctrl.getVext());
        idx += 2;
        Utils.writeBigEndShort(val, idx, (short) pctrl.getVusb());
        idx += 2;
        Utils.writeBigEndShort(val, idx, (short) pctrl.getPowerStatus());
        idx += 2;
        Utils.writeBigEndShort(val, idx, (short) pctrl.getStatus());
        idx += 2;
        Utils.writeBigEndShort(val, idx, (short) pctrl.getStartupTime());
        idx += 2;
        Utils.writeBigEndLong(val, idx, pctrl.getTime());
        idx += 8;
        // First added in version 1.1
        val[idx++] = (byte) (usbpd.isUsbPowered() ? 1 : 0);
        Utils.writeBigEndShort(val, idx, (short) pctrl.getBattery().getBatteryLevel());
        idx += 2;
        return val;
    }

    private byte[] createSleepInfoResponse() {
        ISleepManager sm = Spot.getInstance().getSleepManager();

        byte[] val = new byte[
                2 + // version +
                4 + // deepsleepcnt
                5 * 8 + // five long values
                2 // two boolean values (one per byte)
                ];
        int idx = 0;
        
        val[idx++] = MAJOR_VERSION;
        val[idx++] = MINOR_VERSION;        
        Utils.writeBigEndInt(val, idx, sm.getDeepSleepCount());
        idx += 4;
        Utils.writeBigEndLong(val, idx, sm.getMaximumShallowSleepTime());
        idx += 8;
        Utils.writeBigEndLong(val, idx, sm.getMinimumDeepSleepTime());
        idx += 8;        
        Utils.writeBigEndLong(val, idx, sm.getTotalShallowSleepTime());
        idx += 8;
        Utils.writeBigEndLong(val, idx, sm.getTotalDeepSleepTime());
        idx += 8;        
        Utils.writeBigEndLong(val, idx, sm.getUpTime());
        idx += 8;
        val[idx++] = (byte) (sm.isDeepSleepEnabled() ? 1 : 2); 
        val[idx++] = (byte) (sm.isInDiagnosticMode() ? 1 : 2); 
        return val;
    }

// The old version ...    
//    private byte[] createAvailableSuitesResponse() throws IOException {
//    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
//    	DataOutputStream dos = new DataOutputStream(baos);
//    	MIDletSuiteDescriptor[] suites = MIDletSuiteDescriptor.getAllInstances();
//    	dos.writeByte(MAJOR_VERSION);
//    	dos.writeByte(MINOR_VERSION + 1);
//    	dos.writeShort(suites.length);
//    	for (int i = 0; i < suites.length; i++) {
//            dos.writeUTF(suites[i].getURI());
//            dos.writeUTF(suites[i].getSourcePath());
//            dos.writeInt(suites[i].getLength());
//            dos.writeLong(suites[i].getLastModified());
//        }
//        return baos.toByteArray();
//    }
    
    private byte[] createAvailableSuitesResponse() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
    	DataOutputStream dos = new DataOutputStream(baos);
    	MIDletSuiteDescriptor[] suites = MIDletSuiteDescriptor.getAllInstances();
    	dos.writeByte(MAJOR_VERSION + 1);
    	dos.writeByte(MINOR_VERSION);
    	dos.writeShort(suites.length);
    	for (int i = 0; i < suites.length; i++) {
            dos.writeUTF(suites[i].getURI());
            dos.writeUTF(suites[i].getSourcePath());
            dos.writeInt(suites[i].getLength());
            dos.writeLong(suites[i].getLastModified());
            MIDletDescriptor[] mds = suites[i].getMIDletDescriptors();
            dos.writeInt(mds.length);
            for (int j = (mds.length - 1); j >= 0; j--){
                dos.writeInt(mds[j].getNumber());
                dos.writeUTF(mds[j].getClassName());
            }
        }
        return baos.toByteArray();
    }
    
    // Creates a byte array containing the length (2 bytes), version (2 bytes)
    // and a null terminated string
    private byte[] createResponse(String str) {
        if (str == null) str = "";
        byte[] strbytes = str.getBytes();
        byte[] val = new byte[2 + 2 + strbytes.length];
        int idx = 0;
        
        val[idx++] = MAJOR_VERSION;
        val[idx++] = MINOR_VERSION;
        
        Utils.writeBigEndShort(val, idx, (short) strbytes.length);
        idx += 2;
        System.arraycopy(strbytes, 0, val, idx, strbytes.length);
        idx += strbytes.length;

        return val;
    }

    private byte[] createSuiteManifestResponse(String suiteId) throws Exception {
    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
    	DataOutputStream dos = new DataOutputStream(baos);
    	Hashtable manifestPropertiesOfSuite;
        try {
            manifestPropertiesOfSuite = VM.getManifestPropertiesOfSuite(suiteId);
        } catch (Error e1) {
            throw new Exception("Could not read manifest for " + suiteId +
                    ". " + e1.getMessage());
        }
        dos.writeByte(MAJOR_VERSION);
        dos.writeByte(MINOR_VERSION);
        dos.writeShort(manifestPropertiesOfSuite.size());
        Enumeration manifestKeys = manifestPropertiesOfSuite.keys();
        while (manifestKeys.hasMoreElements()) {
            String key = (String) manifestKeys.nextElement();
            dos.writeUTF(key);
            dos.writeUTF((String) manifestPropertiesOfSuite.get(key));
        }
        
        return baos.toByteArray();
    }

    private byte[] createAllAppsStatusResponse(Properties prop) {
        int idx = 4; // ver: 2, count: 2
        int cnt = 0;
        String key = null;
        String value = null;
        
        if (prop != null) {
            for (Enumeration e = prop.propertyNames();
            e.hasMoreElements();) {
                key = (String) e.nextElement();
                if (key == null) continue;
                value = prop.getProperty(key);
                if (value != null) {
                    cnt++;
                    idx += 2 + key.length() + 2 + value.length();
                }
            }
        }
        
        byte[] val = new byte[idx];
        idx = 0;
        
        val[idx++] = (byte) MAJOR_VERSION;
        val[idx++] = (byte) MINOR_VERSION;
        Utils.writeBigEndShort(val, idx, (short) cnt);
        idx += 2;
        
        if (prop != null) {
            for (Enumeration e = prop.propertyNames();
            e.hasMoreElements();) {
                key = (String) e.nextElement();
                if (key == null) continue;
                value = prop.getProperty(key);
                if (value != null) {
                    byte[] keybytes = key.getBytes();
                    byte[] valuebytes = value.getBytes();
                    
                    Utils.writeBigEndShort(val, idx, (short) keybytes.length);
                    idx += 2;
                    System.arraycopy(keybytes, 0, val, idx, keybytes.length);
                    idx += keybytes.length;
                    
                    Utils.writeBigEndShort(val, idx, (short) valuebytes.length);
                    idx += 2;
                    System.arraycopy(valuebytes, 0, val, idx, valuebytes.length);
                    idx += valuebytes.length;  
                }
            }
        }
        
        return val;
    }    

    private byte[] createSpotPropertyResponse(String listOfKeys) 
    throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        Properties p = Spot.getInstance().getPersistentProperties();
        dos.writeByte(MAJOR_VERSION);
        dos.writeByte(MINOR_VERSION);
        StringTokenizer stok = new StringTokenizer(listOfKeys, ",");
        int cnt = stok.countTokens();
        String key = null;
        String value = null;
        dos.writeShort(cnt);
        for (int i = 0; i < cnt; i++) {
            key = stok.nextToken();
            value = p.getProperty(key);
            if (value == null) {
                // Look for special keys identifying 'virtual' properties
                if (key.equalsIgnoreCase("spot.temperature")) {
                    value = "25";
                } else if (key.equalsIgnoreCase("spot.lightsensor")) {
                    value = "450";
                } else {
                    value = "";
                }                   
            }
            dos.writeUTF(key);
            dos.writeUTF(value);
        }
        
        return baos.toByteArray();
    }

    public static class PingReply {
        public long addr;
        public byte linkQualityReq;
        public byte linkQualityReply;
        public byte linkStrengthReq;
        public byte linkStrengthReply;

        public PingReply(long addr, byte linkQualityReq, byte linkQualityReply, byte linkStrengthReq, byte linkStrengthReply) {
            this.addr = addr;
            this.linkQualityReq = linkQualityReq;
            this.linkQualityReply = linkQualityReply;
            this.linkStrengthReq = linkStrengthReq;
            this.linkStrengthReply = linkStrengthReply;
        }
    }

    public static Vector doRemoteGetPhysicalNeighbors() {
        ISpotRadioHelper radioManager = (ISpotRadioHelper)(ServiceRegistry.getInstance().lookup(ISpotRadioHelper.class));
        Vector results = new Vector();
		DatagramConnection inConn = null;
        DatagramConnection outConn = null;
        
		try {
            inConn = (DatagramConnection) Connector.open(radioManager.getDatagramConnectionProtocol() + "://");
            byte port = ((IRadioControl) inConn).getLocalPort();
            ((ITimeoutableConnection) inConn).setTimeout(1000);
            Datagram incoming = inConn.newDatagram(128);
            outConn = (DatagramConnection) Connector.open(radioManager.getDatagramConnectionProtocol() + "://broadcast:" +
                    OTACommandServer.DEFAULT_DATAGRAM_PORT);
            ((IMultipleHopConnection) outConn).setMaxBroadcastHops(1);
            Datagram outgoing = outConn.newDatagram(64);
            outgoing.writeUTF(IOTACommandServer.HELLO_CMD);
            outgoing.writeByte(IOTACommandServer.PHYSICAL_NEIGHBORS_HELLO_TYPE);
            outgoing.writeInt(pingCounter++);
            outgoing.writeByte(port);
            outConn.send(outgoing);
            // System.out.println("Sending remote ping....")
nextPing:
            while (true) {
				try {
					inConn.receive(incoming);
                    // System.out.println("   received ping reply from " + incoming.getAddressAsLong());
                    int majorVersion = incoming.readByte();
                    int minorVersion = incoming.readByte();
                    if (incoming.readByte() == IOTACommandServer.PHYSICAL_NEIGHBORS_HELLO_TYPE &&
                            (pingCounter - 1) == incoming.readInt()) {
                        PingReply pr = new PingReply(IEEEAddress.toLong(incoming.getAddress()),
                                                     incoming.readByte(),
                                               (byte)radioManager.getLinkQuality(incoming),
                                (majorVersion > 3 || (majorVersion == 3 && minorVersion > 0)) ? incoming.readByte() : -1,
                                               (byte)radioManager.getLinkStrength(incoming));
                        for (int i = 0; i < results.size(); i++) {
                            if (((PingReply)results.elementAt(i)).addr ==
                                    IEEEAddress.toLong(incoming.getAddress())) {
                                continue nextPing;
                            }
                        }
                        results.addElement(pr);
                    }
				} catch (TimeoutException e) {
					break;
				}
			}
        } catch (IOException ex) {
		} finally {
            try {
                if (inConn  != null) inConn.close();
                if (outConn != null) outConn.close();
            } catch (IOException ex) {
                // ignore any error closing connections
            }
		}
        return results;
    }

    private byte[] createRemotePingResponse() throws Exception {
        ISpotRadioHelper radioManager = (ISpotRadioHelper)(ServiceRegistry.getInstance().lookup(ISpotRadioHelper.class));
        if (radioManager == null || !radioManager.getDatagramConnectionProtocol().startsWith("radiogram")) {
            byte[] res = {(byte) MAJOR_VERSION, (byte) MINOR_VERSION, 0, 0 };
            return res;
        }
        Vector results = doRemoteGetPhysicalNeighbors();
        byte[] result = new byte[results.size() * (8 + 4) + 2 + 2];
        int idx = 0;
        result[idx++] = (byte) MAJOR_VERSION;
        result[idx++] = (byte) MINOR_VERSION;
        Utils.writeBigEndShort(result, idx, results.size());
        idx += 2;
        for (int i = 0; i < results.size(); i++) {
            PingReply pr = (PingReply)results.elementAt(i);
            Utils.writeBigEndLong(result, idx, pr.addr);
            idx += 8;
            result[idx++] = pr.linkQualityReq;
            result[idx++] = pr.linkQualityReply;
            result[idx++] = pr.linkStrengthReq;
            result[idx++] = pr.linkStrengthReply;
        }
		return result;
    }

    private byte[] createGetRadioInfoResponse() throws Exception {
        int power = RadioFactory.getRadioPolicyManager().getOutputPower();
        boolean filter = Utils.isOptionSelected("radio.filter", false);
        String whiteList = System.getProperty("radio.whitelist");
        String blackList = System.getProperty("radio.blacklist");
        byte[] whitebytes = whiteList == null ? new byte[0] : whiteList.getBytes();
        byte[] blackbytes = blackList == null ? new byte[0] : blackList.getBytes();
        String name = "unknown";
        boolean mutable = false;
        ISpotRadioHelper radioManager = (ISpotRadioHelper)(ServiceRegistry.getInstance().lookup(ISpotRadioHelper.class));
        if (radioManager != null) {
            name = radioManager.getRoutingManagerName();
            mutable = radioManager.isMutableRoutingManager();
        }
        byte[] strbytes = name.getBytes();
        byte[] result = new byte[2 + 1 + 1 + 2 + whitebytes.length + 2 + blackbytes.length + 2 + strbytes.length + 1];
        int idx = 0;
        result[idx++] = (byte) MAJOR_VERSION;
        result[idx++] = (byte) MINOR_VERSION;
        result[idx++] = (byte) (power & 0xff);
        result[idx++] = (byte) (filter ? 1 : 0);

        Utils.writeBigEndShort(result, idx, whitebytes.length);
        idx += 2;
        System.arraycopy(whitebytes, 0, result, idx, whitebytes.length);
        idx += whitebytes.length;
        Utils.writeBigEndShort(result, idx, blackbytes.length);
        idx += 2;
        System.arraycopy(blackbytes, 0, result, idx, blackbytes.length);
        idx += blackbytes.length;

        Utils.writeBigEndShort(result, idx, strbytes.length);
        idx += 2;
        System.arraycopy(strbytes, 0, result, idx, strbytes.length);
        idx += strbytes.length;
        result[idx++] = (byte) (mutable ? 1 : 0);
		return result;
    }

    private byte[] createGetRouteResponse(long dest) throws Exception {
        byte[] noRoute = {(byte) MAJOR_VERSION, (byte) MINOR_VERSION, 0, 0};
        ISpotRadioHelper radioManager = (ISpotRadioHelper)(ServiceRegistry.getInstance().lookup(ISpotRadioHelper.class));
        if (radioManager == null) {
            return noRoute;
        }
        RouteInfo[] ri = (dest == -1) ? radioManager.getRouteInfo() : radioManager.getRouteInfo(dest);
        if (ri == null || ri.length == 0) {
            return noRoute;
        }
        byte[] result = new byte[2 + 2 + ri.length * (8 + 8 + 2)];
        int idx = 0;
        result[idx++] = (byte) MAJOR_VERSION;
        result[idx++] = (byte) MINOR_VERSION;
        Utils.writeBigEndShort(result, idx, ri.length);
        idx += 2;
        for (int i = 0; i < ri.length; i++) {
            Utils.writeBigEndLong(result, idx, ri[i].destination);
            idx += 8;
            Utils.writeBigEndLong(result, idx, ri[i].nextHop);
            idx += 8;
            Utils.writeBigEndShort(result, idx, ri[i].hopCount);
            idx += 2;
        }
		return result;
    }

}
