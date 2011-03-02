/*
 * Copyright 2005-2008 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.spot.io.j2me.socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import javax.microedition.io.DatagramConnection;
import javax.microedition.io.StreamConnection;

import com.sun.spot.peripheral.ITimeoutableConnection;
import com.sun.spot.peripheral.NoAckException;
import com.sun.spot.peripheral.TimeoutException;
import com.sun.spot.util.Utils;
import com.sun.squawk.Isolate;


/**
 * <p>This class provides the necessary implementation for a socket connection.</p> 
 * <p>A SocketProxy must be running on the desktop in order to properly establish a connection.</p>
 * 
 * <p>
 * The SocketConnection uses the following Manifest properties to establish the connection
 * {@value #SOCKET_PROXY_BASE_STATION_ADDRESS_MANIFEST_PROPERTY} : IEEE address of the base station where the SocketProxy is running (can also be set as an argument to the VM)
 * {@value #SOCKET_PROXY_BASE_STATION_PORT_MANIFEST_PROPERTY} : Radiogram port to connect to, where the SocketProxy is running. (can also be set as an argument to the VM)
 * </p> 
 * @author Martin Morissette
 */
public class SocketConnection {

    public static final String SOCKET_PROXY_BASE_STATION_ADDRESS_MANIFEST_PROPERTY = "com.sun.spot.io.j2me.socket.SocketConnection-BaseStationAddress";
    public static final String SOCKET_PROXY_BASE_STATION_PORT_MANIFEST_PROPERTY = "com.sun.spot.io.j2me.socket.SocketConnection-BaseStationPort";

    public static final String DEFAULT_BASE_STATION_PORT = "10";
    
    public static final int PACKET_TYPE_PORT_REQUEST = 0;
    public static final int PACKET_TYPE_PORT_RESPONSE = 1;
    
    private StreamConnection conn;
    private SocketOutputStream out = null;
    private SocketInputStream in = null;
    
    int opens=0;
    
    private boolean connected=false;

    /**
     * Create a SocketConnection object.
     * @param initializer Initializer string to send the proxy to init the connection.
     * @param timeouts set to true to use timeouts
     * @throws IOException when unable to establish the connection with the proxy 
     * @throws IllegalArgumentException when the property {@value #SOCKET_PROXY_BASE_STATION_ADDRESS_MANIFEST_PROPERTY} is not set in the Manifest file or passed as an argument to the VM. 
     */
    public SocketConnection(ProxyInitializer initializer, boolean timeouts) throws IllegalArgumentException, IOException {
        
        /* Determine Base Station address */
        String baseStationAddress = Utils.getManifestProperty(SOCKET_PROXY_BASE_STATION_ADDRESS_MANIFEST_PROPERTY, null);
        if (baseStationAddress == null) {
            // Try to get the property from the Isolates properties
            baseStationAddress = Isolate.currentIsolate().getProperty(SOCKET_PROXY_BASE_STATION_ADDRESS_MANIFEST_PROPERTY);
            if(baseStationAddress==null){
                baseStationAddress = "broadcast";
            }
        }
        baseStationAddress = baseStationAddress.trim();
        
        /* Determine port number */
        String baseStationPort = Utils.getManifestProperty(SOCKET_PROXY_BASE_STATION_PORT_MANIFEST_PROPERTY, null);
        if (baseStationPort == null) {
            // Try to get the property from the Isolates properties
            baseStationPort = Isolate.currentIsolate().getProperty(SOCKET_PROXY_BASE_STATION_PORT_MANIFEST_PROPERTY);
            if(baseStationPort==null){
                baseStationPort = DEFAULT_BASE_STATION_PORT;                
            }
        }
        baseStationPort = baseStationPort.trim();


        // Open init connection with SocketProxy
        int port;
        DatagramConnection initConnection=null;
        DatagramConnection initReceiveConnection=null;
        try{
            if(baseStationAddress.equals("broadcast")){
                initReceiveConnection = (DatagramConnection) Connector.open("radiogram://:" + baseStationPort, Connector.READ_WRITE, true);
                ((ITimeoutableConnection)initReceiveConnection).setTimeout(10000);
            }
            
            initConnection = (DatagramConnection) Connector.open("radiogram://" + baseStationAddress + ":" + baseStationPort, Connector.READ_WRITE, true);
            // We force timeouts here for the initial connection with the proxy so that we can determine if the proxy is reachable or not
            ((ITimeoutableConnection)initConnection).setTimeout(8000);
            Datagram datagram = initConnection.newDatagram(initConnection.getMaximumLength());

            datagram.writeByte(PACKET_TYPE_PORT_REQUEST);
            datagram.writeUTF(initializer.toString());
            
            initConnection.send(datagram);

            // Rely on receive connection timeout to eventually end this loop
            while (true) {
	            if(initReceiveConnection!=null){
	                datagram = initReceiveConnection.newDatagram(initReceiveConnection.getMaximumLength());
	                initReceiveConnection.receive(datagram);
	            }else{
	                initConnection.receive(datagram);                
	            }
	            if (datagram.readUnsignedByte() == PACKET_TYPE_PORT_RESPONSE) {
	            	break;
	            }
            }
            
            
            port = datagram.readInt();
            baseStationAddress = datagram.getAddress();
        }catch(TimeoutException te){
            throw new IOException("unable to establish connection with socket proxy at address " + baseStationAddress + " on port " + baseStationPort + " (timeout)");
        }catch (NoAckException nae){
            throw new IOException("unable to establish connection with socket proxy at address " + baseStationAddress + " on port " + baseStationPort + " (no ack)");
        }finally{

            if(initConnection!=null){
                initConnection.close();                
            }
            
            if(initReceiveConnection!=null){
                initReceiveConnection.close();
            }
            
        }
        
        
        conn = (StreamConnection) Connector.open("radiostream://" + baseStationAddress + ":" + port, Connector.READ_WRITE, timeouts);
        connected=true;
        opens++;
    }

    /**
     * Get a SocketInputStream object associated to this conneciton.
     * @return a SocketInputStream object associated to this conneciton. 
     * @throws IOException
     */
    public SocketInputStream getInputStream() throws IOException {
        if (in == null) {
            opens++;
            in = new SocketInputStream(conn.openInputStream());
        }
        return in;
    }

    /**
     * Get a SocketOutputStream object associated to this conneciton.
     * @return a SocketOutputStream object associated to this conneciton.
     * @throws IOException
     */
    public SocketOutputStream getOutputStream() throws IOException {
        if (out == null) {
            opens++;
            out = new SocketOutputStream(conn.openOutputStream());
        }
        return out;
    }
    
    /**
     * Disconnect and close the SocketConnection. 
     * @throws IOException
     */
    private void disconnect() throws IOException {
        if (connected) {
            
            if(in!=null){
                in.closeConnection();
                in = null;                
            }
            
            if(out!=null){
                out.closeConnection();
                out = null;                
            }

            conn.close();
            conn = null;
            connected = false;
        }
    }

    /**
     * Disconnect and close the SocketConnection.
     * @throws IOException
     */
    public void close() throws IOException {
        if(connected){
            if (--opens == 0){
                disconnect();    
            }
        }
    }

    /**
     * Socket specific input stream.
     * @author Martin Morissette
     */
    private class SocketInputStream extends SocketProtocolInputStream {

        private boolean opened=false;
        
        public SocketInputStream(InputStream in) {
            super(in);
            opened = true;
        }
        
        public int read() throws IOException {
            if(!opened){
                throw new IOException("inputstream is closed");
            }            
            return super.read();
        }
        
        public void close() throws IOException {
            if(opened){
//                super.close();
                opened=false;
                if (--opens == 0){
                    disconnect();    
                }
            }
        }
        
        private void closeConnection() throws IOException{
            super.close();
        }
               
    }
    
    /**
     * Socket specific output stream.
     * @author Martin Morissette
     *
     */
    private class SocketOutputStream extends SocketProtocolOutputStream {
        private boolean opened=false;
        
        public SocketOutputStream(OutputStream out) {
            super(out);
            opened=true;
        }
        
        public void write(int data) throws IOException {
            if(!opened){
                throw new IOException("outputstream is closed");
            }
            super.write(data);
        }
        
        public void flush() throws IOException {
            if(!opened){
                throw new IOException("outputstream is closed");
            }
            super.flush();
        }
        
        public void close() throws IOException {
            if(opened){
//                super.close();
                opened=false;
                if (--opens == 0){
                    disconnect();    
                }
            }
        }
        
        private void closeConnection() throws IOException{
            super.close();
        }
        
    }

}
