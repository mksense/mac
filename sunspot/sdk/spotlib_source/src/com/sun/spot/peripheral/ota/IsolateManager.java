/*
 * Copyright 2007-2008 Sun Microsystems, Inc. All Rights Reserved.
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

import com.sun.spot.peripheral.Spot;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

import com.sun.spot.util.Properties;
import com.sun.squawk.Isolate;
import com.sun.squawk.VM;
import com.sun.squawk.util.StringTokenizer;

/* 
 * XXX all of these methods should be synchronized to avoid inconsistent
 * results, e.g. the list of isolates changing while getAllAppsStatus is
 * compiling its results.
 */

/**
 * Provides a way to start/pause/stop Isolates.
 * 
 * @author vgupta
 */
public class IsolateManager {
    private static final int SUCCEEDED = 1;
    private static final int FAILED = 2;
    private static final int NOT_IMPLEMENTED = 3;
    
    private static Vector remotePrintURLs = new Vector();
    
    /** Creates a new instance of IsolateManager */
    public IsolateManager() {
    }

    /*
     * Returns the status of the specified isolate.
     * @param iso Isolate whose status is requested
     * @return status of the isolate (one of "UNKNOWN", "ALIVE", "PAUSED",
     * "EXITED", "NEW" or "BEING_DEBUGGED");
     */
    public static String getIsolateStatus(Isolate iso) {
        String                     status = "UNKNOWN";
        if (iso.isAlive())         status = "ALIVE";
        if (iso.isHibernated())    status = "PAUSED";
        if (iso.isExited())        status = "EXITED";
        if (iso.isNew())           status = "NEW";
        if (iso.isBeingDebugged()) status = "BEING_DEBUGGED";
        
        return status;
    }
    
    /* 
     * Creates a globally unique Id for the given isolate.
     * @param iso Isolate for which a unique Id is requested
     * @return a globally unique Id for the specified isolate
     */
    public static String createUniversallyUniqueID(Isolate iso) {
        // return iso.getId()  + "@" + System.getProperty("IEEE_ADDRESS");
        String className = iso.getProperty("MIDletClassName");
        if (className == null) className = iso.getMainClassName();
        return createUniversallyUniqueID(iso, className);
    }

    /*
     * Creates a globally unique Id for the given isolate using
     * the specified class
     * @param iso Isolate for which a unique Id is requested
     * @param className main class name for the isolate
     * @return a globally unique Id for the specified isolate
     */
    public static String createUniversallyUniqueID(Isolate iso,
            String className){
        return className + "#" + iso.getId()  + "@" +
                System.getProperty("IEEE_ADDRESS");
    }
    
    /**
     * Looks up the isolate corresponding to the specified globally unique
     * isolate Id.
     * @param isoId globally unique isolate Id
     * @return corresponding isolate or null (if the Id was unrecognized)
     */
    public static Isolate getIsolate(String isoId) {
        Isolate[] isos = Isolate.getIsolates();
        String id = null;
        
        for (int i = 0; i < isos.length; i++) {
            id = isos[i].getProperty("id");
            if (id != null && id.equals(isoId)) {
                return isos[i];
            }
        }
        
        return null;
    }
    
    /**
     * Returns the class name corresponding to the specified MIDlet key, e.g.
     * @param suiteId Id of the suite in which to look for the given midlet number
     * @param midletKey MIDlet key in the manifest, e.g. "MIDlet-1"
     * @return class name for that MIDlet or a failure message
     */
    public static String midletKeyToClass(String suiteId, String midletKey) {
        String midletVal = null;
        String ret = null;
        Hashtable ht = null;
        
        try {
            ht = VM.getManifestPropertiesOfSuite(suiteId);
            midletVal = (String) ht.get(midletKey);
        } catch (Exception e) {
            ret = e.getMessage();
        } catch (Error e) {
            ret = e.getMessage();
        }
        
        if (ht == null) {
            return "Failed: Could not read manifest for suiteId " + suiteId +
                    ((ret == null) ? "": ("(" + ret + ")"));
        }

        // Next, extract the classname (last of the comma-separated fields)
        if (midletVal == null) {
            ret = "Failed: " + midletKey + " not found in suite manifest";
            return ret;
        }
        
        StringTokenizer stok = new StringTokenizer(midletVal, ",");
        int cnt = stok.countTokens();
        
        if (cnt > 1) {
            // skip all tokens but the last
            for (int i = 0; i < (cnt - 1); i++) {
                stok.nextToken();
            }
        }
        
        ret = stok.nextToken().trim();
        return ret;        
    }
    
    /**
     * Creates an isolate for the specified MIDlet and suite. 
     * @param suiteId Id of an already installed suite.
     * @param midletId MIDlet number to run
     * @return Returns a globally unique isolateId if successful or null in 
     * case of a failure.
     */
    public static String startApp(String suiteId, int midletId) {
        Isolate cur  = Isolate.currentIsolate();
        String ret = null;
        
        ret = midletKeyToClass(suiteId, "MIDlet-" + midletId);
        
        if (ret.startsWith("Failed: ")) {
            return ret;
        }
        
        String midletClassName = ret;
        // Check if the class name is valid
//        try {
//            Class.forName(midletClassName);
//        } catch (ClassNotFoundException e) {
//            ret = "Failed: Class " + midletClassName + " not found";
//            return ret;
//        }
        
        // TODO: make sure all of the proper arguments from the manifest for
        // this midlet are being passed in. Update: Talked to Derek, Sep 5, 
        // he thinks this should convey the resources and the manifest 
        // properties properly but we use a different constructor since this
        // one might be going away.
//        Isolate iso = new Isolate(
//                "com.sun.squawk.imp.MIDletMainWrapper",
//                new String[] {midletKey},
//                cur.getClassPath(),
//                cur.getParentSuiteSourceURI());
        Isolate iso = new Isolate(null, midletId, null, suiteId);
        if (iso == null) {
            return "Failed: Could not create isolate";            
        }
        
        iso.setProperty("MIDletClassName", midletClassName);
        String isoId = createUniversallyUniqueID(iso);
        iso.setProperty("id", isoId);
        
        /*
         * Add the Isolate to our collection.
         */
        iso.start();
        return isoId;
    }

        
    public static String pauseApp(String isolateId) {
        Isolate iso = getIsolate(isolateId);
        String ret = null;
        
        if (iso == null) {
            ret = "Failed: Isolate <" + isolateId + 
                    "> not found";
            return ret;
        }
        
        try {
            if (iso.isAlive()) iso.hibernate();
        } catch (Exception ex) {
            ex.printStackTrace();
        } 
        
        if (iso.isHibernated()) {
            ret = "Isolate <" + isolateId + "> paused";
        } else {
            ret = "Failed: Could not pause Isolate <" + isolateId + "> (" +
                    getIsolateStatus(iso) + ")";
        }
        
        return ret;
    }
    
    public static String resumeApp(String isolateId) {
        Isolate iso = getIsolate(isolateId);
        String ret = null;
        
        if (iso == null) {
            ret = "Failed: Isolate <" + isolateId + 
                    "> not found";
            return ret;
        }
        
        try {
            if (iso.isHibernated()) iso.unhibernate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if (iso.isHibernated()) {
            ret = "Failed: Could not resume Isolate <" + isolateId + "> (" +
                    getIsolateStatus(iso) + ")";
        } else {
            ret = "Isolate <" + isolateId + "> resumed";
        }

        return ret;       
    }
        
    public static String stopApp(String isolateId) {
        Isolate iso = getIsolate(isolateId);
        String ret = null;
        
        if (iso == null) {
            ret = "Failed: Isolate <" + isolateId + 
                    "> not found";
            return ret;
        }
        
        try {
            if (!iso.isExited()) {
                // this next line is disturbing, 
                // as seems it should not be necessary
                if (iso.isHibernated()) iso.unhibernate();
                iso.exit(0);
                iso.join();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if (iso.isExited()) {
            ret = "Isolate <" + isolateId + "> stopped";
        } else {
            ret = "Failed: Could not stop Isolate <" + isolateId + "> (" +
                    getIsolateStatus(iso) + ")";
        }
        
        return ret; 
    }
    
    public static String getAppStatus(String isolateId) {
        Isolate iso = getIsolate(isolateId);
        String ret = null;
        
        if (iso == null) {
            ret = "Failed: Isolate <" + isolateId +
                    "> not found";
            return ret;
        }
        
        return (getIsolateStatus(iso));
    }
    
    public static Properties getAllAppsStatus() {
        Properties ret = null;
        Isolate[] isos = Isolate.getIsolates();
        String key = null;
        
        if ((isos == null) || (isos.length == 0))
            return null;
        
        ret = new Properties();
        for (int i = 0; i < isos.length; i++) {
            if (isos[i] != null)  {
                // We ignore the master isolate
                key = isos[i].getProperty("id");
                if (key != null) {
                    ret.setProperty(key, getIsolateStatus(isos[i]));
                } else {
                    // deal with the master isolate
                    if (isos[i].getMainClassName().
                            equals("com.sun.squawk.imp.MIDletMainWrapper")) {
                        String[] args = isos[i].getMainClassArguments();
                        // System.out.println("Mainclass args: ");
                        // for (int j = 0; j < args.length; j++) {
                        //     System.out.println("args[" + j + "]=" + args[j]);
                        // }
                        // Midlet key is the first argument
                        String cmdLineParams = null;
                        String masterSuite = "spotsuite://app";
                        // Get the master suite Id from VM args
                        try {
                            cmdLineParams = Spot.getInstance().
                                    getConfigPage().getCmdLineParams();
                        } catch (Exception ex) {                            
                        }
                        // CmdLineParams looks like "-spotsuite://<name> ...""
                        if (cmdLineParams != null) {
                            int idx = cmdLineParams.indexOf(" ");
                            if (idx > 0)
                                masterSuite = cmdLineParams.substring(1, idx);
                        }
                        String className = midletKeyToClass(masterSuite, 
                                args[0]);
                        if (!className.startsWith("Failed: ")) {
                            key = createUniversallyUniqueID(isos[i], className);
                            ret.setProperty(key, getIsolateStatus(isos[i]));
                        }
                    }
                }
            }
        }
        
        return ret;
    }
    
    public static String startRemotePrinting(String isolateId, String addr) {
        String ret = null;
        String url = null;
        int port = 0;
        Isolate iso = null;
        
        if (isolateId.equals("masterIsolate")) {
            iso = Isolate.currentIsolate();
        } else {
            iso = getIsolate(isolateId);
        }
                        
        if (iso == null) {
            ret = "Failed: Isolate <" + isolateId + 
                    "> not found";
            return ret;
        }
        
        // Synchronized so that another acces will not be allowed 
        // until the remotePrintURLs is updated.
        synchronized (remotePrintURLs) {
            port = getAvailablePortHackVersion(addr);
            url = "remoteprint://" + addr + ":" + port;
            remotePrintURLs.addElement(url);
        }

//        System.out.println("Opening remote print connection for isolate: " + 
//                isolateId + " URL: " + url);
        
        iso.addOut(url);
        iso.addErr(url);
        
        return ("" + port);
        
//        /* For an unknown reason, forking the addOut() and addErr() calls in a seperate thread
//         * stops an error from happening in the case of the currentIsolate and a shared basestation */
//        final String redirURL = redir;
//        final Isolate targetIso = iso;
//        Runnable r = new Runnable(){
//            public void run() {
//                targetIso.addOut(redirURL);
//                targetIso.addErr(redirURL);
//            }
//        };
//        (new Thread(r)).start();
        
    }
    
    /* This should not be necessay if we closed the connections as they 
     * become freed up, in which case we would search through and reuse 
     * these previously closed ports. See getAvailablePort(String addr)
     * In this version we find the biggest value of the ports used so far, 
     * and add one.
     */
    public static int getAvailablePortHackVersion(String addr){
        int biggest = 89;
        String addrI;
        String portI;
        for (int i = 0; i < remotePrintURLs.size(); i++){
            URL url = new URL((String) remotePrintURLs.elementAt(i));
            addrI = url.getAddress();
            portI = url.getPort();
//            System.out.println("~~~~~~~~~~~~~ CONSIDERING URL " + 
//                    portI + " @ " + addrI);
            if (addrI.equals(addr)) {
                int p = Integer.parseInt(portI);
                if (p > biggest) biggest = p;
            }
        }
        int result = biggest + 1;
//        System.out.println("~~~~~~~~~~~~~ ============== USING PORT = " + 
//                result);
        return result;
    }
    
    public static String stopRemotePrinting(String isolateId, 
            String address, String port) {
        String ret = "";
        String url = "remoteprint://" + address + ":" + port;
        Isolate iso = null;
        
        if (isolateId.equals("masterIsolate")) {
            iso = Isolate.currentIsolate();
        } else {
            iso = getIsolate(isolateId);
        }
        
        if (iso == null) {
            ret = "Failed: Isolate <" + isolateId + 
                    "> not found";
            return ret;
        }

        boolean found = false;
        synchronized (remotePrintURLs) {
            Object[] urls = vectorToArray(remotePrintURLs);
            for (int i = 0; !found && (i < urls.length); i++) {
                if (url.equalsIgnoreCase((String) urls[i])) {
                    remotePrintURLs.removeElement((String) urls[i]);
                    found = true;
                    iso.removeErr((String) urls[i]);
                    iso.removeOut((String) urls[i]);
                }
            }
        }
        
        if (!found) {
            ret = "Failed: could not locate URL <" + url + "> to remove.";
//            System.err.println(ret);       
        } else {
            ret = "Printing stopped for isolate <" + isolateId + "> on " + url;
        }

        return ret;
        
//        String[] errs = iso.listErr();
//        boolean found = false;
//        for (int i = 0; i < errs.length; i++) {
//            URL url = new URL(errs[i]);
//            if (url.getAddress().equals(address) && 
//                    url.getPort().equals(port)) {
//                iso.removeErr(errs[i]);
//                found = true;
//            }
//        }
//        
//        if (! found) {
//            System.out.println("...could not find remote printing " +
//                    "URL in list of errs for address " + address +
//                    " and port " + port);
//        }
//        
//        String[] outs = iso.listOut();
//        found = false;
//        for (int i = 0; i < outs.length; i++) {
//            URL url = new URL(outs[i]);
//            if(url.getAddress().equals(address) && url.getPort().equals(port)){
//                try {
//                    iso.removeOut(outs[i]);
//                } catch (Exception ex ){
//                    //Do nothing...I think this is a bug.
//                }
//                found = true;
//            }
//        }
//        if (! found) System.out.println("...could not find remote printing " +
//                "URL in list of outs for address " + address + 
//                " and port " + port);
        
    }
    
    public static  Object[] vectorToArray(Vector v){
        Object[] r = new Object[v.size()];
        for (int i = 0; i < r.length; i++) {
            r[i] = v.elementAt(i);
        }
        return r;
    }
    
    /**
     * Moves the state associated with the specified isolate to remote
     * address. If the mkCopy flag is true, the specified isolate stays
     * alive after this method call. Otherwise, it is stopped.
     */
    public static String migrateApp(final String isolateId, final String destination, 
            final boolean mkCopy) {
        Isolate iso = getIsolate(isolateId);
        String ret = null;
        System.out.println("Migrate app called for " + isolateId + ", " +
                destination + ", " + mkCopy);
        if (iso == null) {
            ret = "Failed: Isolate <" + isolateId + "> not found";
            return ret;
        }

        // XXX Deal with an already hibernated app ...
        if (!iso.isAlive()) {
            ret = "Failed: Isolate <" + isolateId + "> not active";
            return ret;
        }
        
        if (mkCopy) {
            // XXX TODO: fix this to return an isoId different from 
            // what the isolate already has ...
            String isoId = createUniversallyUniqueID(iso);
            iso.setProperty("id", isoId);
        }
        
        try {
            iso.hibernate();
        } catch (Exception ex) {
        }
        
        if (!iso.isHibernated()) {
            ret = "Failed: Isolate <" + isolateId + 
                    "> could not be hibernated";
            return ret;
        }
        
        final Isolate isoToMigrate = iso;
        System.out.println("App hibernated");
        Runnable r = new Runnable() {
            public void run() {
                /* Actually move the Isolate  */
                String url = "radiostream://" + destination + ":" +  "61";
                StreamConnection conn = null;
                DataOutputStream dos = null;
                String err = null;
                try {
                    Thread.yield();
                    conn = (StreamConnection) Connector.open(url);
                    System.out.println("Connection opened");
                    dos = conn.openDataOutputStream();
                    dos.writeUTF("Here comes an Isolate");
                    dos.flush();
                    isoToMigrate.save(dos, destination);
                    dos.flush();
                    System.out.println("app sent");
                } catch (Exception e) {
                    err = "Failed: Caught " + e.getMessage() +
                            " while migrating isolate " + isoToMigrate;
                } finally {
                    if (conn != null) {
                        try {
                            dos.close();
                            conn.close();
                        } catch (Exception ex) {
                            System.err.println("Caught " + ex.getMessage() +
                                    " while closing connections in migrateApp");
                        }
                    }
                }
                if (err != null) System.err.println(err);;
                
                try {
                    isoToMigrate.unhibernate();
                    System.out.println("App unhibernated");
                    if (!mkCopy) {
                        isoToMigrate.exit(0);
                        isoToMigrate.join();
                    } else {
                        isoToMigrate.setProperty("id", isolateId);
                    }
                } catch (Exception ex) {
                    err = "Failed: Caught " + ex.getMessage() +
                            "while changing state of isolate <" + 
                            isoToMigrate + ">";
                    System.err.println(err);;
                }
            }
        };
        
                
        (new Thread(r)).start();
        // Thread.yield(); //Try to get the above thread to the reedUTF() hang AASAP.
        System.out.println("migrationapp thread started"); 

        return "Started thread to migrate " + (mkCopy ? "a copy of ": "") +
                "isolate <" + isolateId + "> to " + destination;
    }
    
    public static String receiveApp(final String isoId, final String source) {
        Runnable r = new Runnable(){
            public void run() {
                String url = "radiostream://" + source + ":" +  "61";
                Isolate iso = null;
                StreamConnection conn = null;
                DataInputStream dis = null;
                String ret = null;
                try {
                    conn = (StreamConnection) Connector.open(url);
                    System.out.println("++++++++++++++ opened url " + url +
                            " to recieve isolate <" + isoId + ">");
                    dis = conn.openDataInputStream();
                    String greetingHeader = dis.readUTF();
                    System.out.println("++++++++++ Got this from an " +
                            "isolate stream: " + greetingHeader );
                    iso = Isolate.load(dis, source);
                } catch (Exception ex) {
                    ret = "Failed: Caught " + ex.getMessage() +
                            " while receiving isolate <" + isoId + ">";
                } finally {
                    if (conn != null) {
                        try {
                            dis.close();
                            conn.close();
                        } catch (Exception ex) {
                            System.err.println("Caught " + ex.getMessage() +
                                    " while closing connections in receiveApp");
                        }
                    }
                }
                
                if (ret != null) System.err.println(ret);
                
                if (iso == null) {
                    System.err.println("Failed: Received null isolate <" + 
                            isoId + ">");
                }
                
                try {
                    iso.unhibernate();
                    System.out.println("app unhibernated");
                } catch (Exception ex) {
                }
                if (!iso.isAlive()) {
                    System.err.println("Failed: Could not unhibernate " +
                            "isolate <" + isoId + ">");
                }
                
                System.out.println("Receiveapp thread finished!");
            }
        };
        
        (new Thread(r)).start();
        Thread.yield(); //Try to get the above thread to the reedUTF() hang AASAP.
        System.out.println("receiveapp thread started");        
        return "Receiveapp thread started for isolate <" + isoId + "> from " +
                source;
    }
}
