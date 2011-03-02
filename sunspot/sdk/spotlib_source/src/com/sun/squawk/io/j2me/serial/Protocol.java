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

package com.sun.squawk.io.j2me.serial;

import com.sun.spot.peripheral.ISpot;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;

import javax.microedition.io.Connection;
import javax.microedition.io.StreamConnection;

import com.sun.spot.peripheral.SpotFatalException;
import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.resourcesharing.IResource;
import com.sun.spot.resourcesharing.IResourceHandle;
import com.sun.spot.resourcesharing.IResourceRegistry;
import com.sun.spot.resourcesharing.ResourceSharingException;
import com.sun.spot.resourcesharing.ResourceSharingScheme;
import com.sun.spot.resourcesharing.ResourceUnavailableException;
import com.sun.spot.resourcesharing.SimpleResource;
import com.sun.spot.util.Properties;
import com.sun.squawk.Address;
import com.sun.squawk.Unsafe;
import com.sun.squawk.VM;
import com.sun.squawk.io.ConnectionBase;
import com.sun.squawk.vm.ChannelConstants;

/**
 * serial.Protocol - provides read access to the serial/USB port for an eSPOT
 * <br><br>
 * This protocol understands only three URLs: "serial://usb", "serial://usart" and "serial://".
 * The first two refer to connections to the USB and USART respectively. The first is known as the
 * "default" connection and refers to a combination which will be the USB if the SPOT has
 * been connected to a host computer and enumerated, otherwise the USART. This "default" connection
 * is the one normally used for System.out and System.err traffic. By assigning System.out and 
 * System.err to a specific url, it is possible to use one connection for System.out and System.err
 * and the other for a specific application purpose.
 * <br><br>
 * Note that in the case where the URL is "serial://usart" it is possible to append parameters
 * to control the serial port settings. For example:
 * <br><br>
 * "serial://usart?baudrate=115200&databits=8&parity=even&stopbits=0"
 * <br><br>
 * Allowed values for parity are even, odd, mark and none. 
 */
public class Protocol extends ConnectionBase implements StreamConnection {
	
	private static final String URL_USB = "//usb";
	private static final String URL_USART = "//usart";
	public static final int USART0_BASE_ADDRESS = 	0xfffc0000;
	public static final int USART1_BASE_ADDRESS = 	0xfffc4000;
	public static final int US_BRGR = 				0x20 >> 2;
	public static final int US_MR = 				0x04 >> 2;

	// These have to match the values in syscalls-impl.h
	private static final int DEFAULT = 1;
	private static final int USB = 2;
	private static final int USART = 3;
	private static boolean resourcesInitialised;
	
	private static IResourceRegistry resourceRegistry;

	private int type;
	private IResourceHandle usart0ConfigHandle;
	
	private static IResourceRegistry getResourceRegistry() {
		if (resourceRegistry == null) {
			resourceRegistry = RadioFactory.getResourceRegistry();
		}
		return resourceRegistry;
	}
	
	/**
	 * This method is provided for test purposes only. It should not be used by
	 * normal applications.
	 * 
	 * @param resourceRegistry
	 */
	public static void setResourceRegistry(IResourceRegistry resourceRegistry) {
		resourcesInitialised = false;
		Protocol.resourceRegistry = resourceRegistry;
	}
	
	static synchronized void initialiseResources() {
		if (!resourcesInitialised) {
			IResourceRegistry resourceRegistry = getResourceRegistry();

			IResource serialUSB = new SimpleResource("serial.usb");
			resourceRegistry.register(serialUSB.getResourceName(), serialUSB);

			IResource serialUSART = new SimpleResource("serial.usart");
			resourceRegistry.register(serialUSART.getResourceName(), serialUSART);
			
			IResource serialDEFAULT = new CompositeResource("serial.default", new String[] {"serial.usb", "serial.usart"});
			resourceRegistry.register(serialDEFAULT.getResourceName(), serialDEFAULT);
			
			IResource usart0Config = new SimpleResource("serial.usart0config");
			resourceRegistry.register(usart0Config.getResourceName(), usart0Config);
		
			resourcesInitialised = true;
		}
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.io.ConnectionBase#open(java.lang.String, java.lang.String, int, boolean)
	 */
	public Connection open(String protocol, String name, int mode, boolean timeouts) throws IOException {
		initialiseResources();
		if (name.equalsIgnoreCase(URL_USB)) {
			type = USB;
		} else {
			if (name.toLowerCase().startsWith(URL_USART)) {
				type = USART;
				if (!name.equalsIgnoreCase(URL_USART)) {
					try {
						// need to change line parameters, so grab an exclusive lock for a short while
						usart0ConfigHandle = getResourceRegistry().getResource("serial.usart0config", ResourceSharingScheme.EXCLUSIVE);
					} catch (ResourceUnavailableException e) {
						throw new IOException("Can't set usart0 parameters as device is in use.");
					}					
					try {
						handleUSARTParams(name.substring(URL_USART.length()));
						try {
							usart0ConfigHandle = getResourceRegistry().adjustLock(usart0ConfigHandle, ResourceSharingScheme.WRITE);
						} catch (ResourceUnavailableException e) {
							throw new SpotFatalException("Internal error downgrading lock on Usart0 config.");
						}
					} catch (IOException e) {
						getResourceRegistry().unlock(usart0ConfigHandle);
						throw e;
					} catch (RuntimeException e) {
						getResourceRegistry().unlock(usart0ConfigHandle);
						throw e;
					}
				} else {
					// not changing line parameters, so just a write lock
					usart0ConfigHandle = getWriteLockOnUsart0Config();
				}
			} else if (name.equals("//") || name.equals("")) {
				type = DEFAULT;
				usart0ConfigHandle = getWriteLockOnUsart0Config();
			} else {
				throw new IllegalArgumentException("Unrecognised URL in serial protocol: " + name);
			}
		}
		return this;
	}

	private static IResourceHandle getWriteLockOnUsart0Config() throws IOException {
		// Take a write lock on serial so no-one else can get an exclusive lock during this connection's lifetime
		// We do this as they won't be able to modify the USART config without an exclusive lock
		try {
			return getResourceRegistry().getResource("serial.usart0config", ResourceSharingScheme.WRITE);
		} catch (ResourceUnavailableException e) {
			throw new IOException("Usart0 is temporarily unavailable as another process is adjusting its configuration.");
		}
	}

	public void close() throws IOException {
		if (usart0ConfigHandle != null) {
			getResourceRegistry().unlock(usart0ConfigHandle);
			usart0ConfigHandle = null;
		}
	}

	private void handleUSARTParams(String string) throws IOException {
		// string should be "?key1=val2&key2=val2..."
		if (string.charAt(0) != '?') {
			throw new IllegalArgumentException("Unrecognised URL parameters in serial protocol: " + string);
		}
		Properties p = new Properties();
		p.load(new ByteArrayInputStream(string.substring(1).replace('&', '\n').getBytes()));
		Enumeration keys = p.propertyNames();
		
		int mode = Unsafe.getInt(Address.fromPrimitive(USART0_BASE_ADDRESS), US_MR);
		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			if (key.equalsIgnoreCase("baudrate")) {
				int requestedRate = Integer.parseInt(p.getProperty(key));
				int requiredClockDivisor = (2 * ISpot.MCLK_FREQUENCY / (16 * requestedRate) + 1) / 2;
				Unsafe.setInt(Address.fromPrimitive(USART0_BASE_ADDRESS), US_BRGR, requiredClockDivisor);
				Unsafe.setInt(Address.fromPrimitive(USART1_BASE_ADDRESS), US_BRGR, requiredClockDivisor);
			} else if (key.equalsIgnoreCase("databits")) {
				int requestedDatabits = Integer.parseInt(p.getProperty(key));
				mode &= ~(0x3 << 6);
				mode |= (requestedDatabits-5)<<6;
				
			} else if (key.equalsIgnoreCase("parity")) {
				mode &= ~(0x7 << 9);
				String parity = p.getProperty(key);
				if (parity.equalsIgnoreCase("none")) {
					mode |= 0x4 << 9;
				} else if (parity.equalsIgnoreCase("even")) {
					mode |= 0x0 << 9;
				} else if (parity.equalsIgnoreCase("odd")) {
					mode |= 0x1 << 9;
				} else if (parity.equalsIgnoreCase("space")) {
					mode |= 0x2 << 9;
				} else if (parity.equalsIgnoreCase("mark")) {
					mode |= 0x3 << 9;
				} else {
					throw new IllegalArgumentException("Unrecognised parity parameter: " + parity);
				}
				
			} else if (key.equalsIgnoreCase("stopbits")) {
				float stopbits = Float.parseFloat(p.getProperty(key));
				mode &= ~(0x3 << 12);
				mode |= (int)(stopbits * 2 - 2) << 12;
				
			} else {
				throw new IllegalArgumentException("Unrecognised URL parameter: " + key);
			}
		}
		Unsafe.setInt(Address.fromPrimitive(USART0_BASE_ADDRESS), US_MR, mode);
		Unsafe.setInt(Address.fromPrimitive(USART1_BASE_ADDRESS), US_MR, mode);
	}

	public InputStream openInputStream() throws IOException {
		return new SerialInputStream();
    }
	
	private String getResourceName() {
		switch (type) {
			case DEFAULT:
				return "serial.default";
			case USB:
				return "serial.usb";
			case USART:
				return "serial.usart";
			default:
				throw new SpotFatalException("Unexpected serial device type " + type);
		}
	}

	public OutputStream openOutputStream() throws IOException {
		return new SerialOutputStream();
	}

	private static final class CompositeResource implements IResource, IResourceHandle {
		private String[] parts;
		private String name;
		private IResourceHandle[] handles;

		private CompositeResource(String name, String[] parts) {
			this.name = name;
			this.parts = parts;
		}

		public IResourceHandle getHandle(ResourceSharingScheme scheme, boolean isLockedInADifferentIsolate) throws ResourceSharingException, ResourceUnavailableException {
			if (scheme != ResourceSharingScheme.EXCLUSIVE) {
				throw new ResourceSharingException("Composite resources only support EXCLUSIVE locking");
			}
			handles = new IResourceHandle[parts.length];
			for (int i = 0; i < parts.length; i++) {
				try {
					handles[i] = getResourceRegistry().getResource(parts[i], scheme);
				} catch (ResourceSharingException e) {
					freeHandles();
					throw e;
				} catch (ResourceUnavailableException e) {
					freeHandles();
					throw e;
				}
			}
			return this;
		}

		public IResourceHandle lockAdjusted(IResourceHandle handle, ResourceSharingScheme oldScheme, ResourceSharingScheme newScheme) throws ResourceSharingException, ResourceUnavailableException {
			throw new ResourceSharingException("CompositeResource doesn't support lock adjustment");
		}

		private void freeHandles() {
			for (int i = 0; i < handles.length; i++) {
				IResourceHandle handle = handles[i];
				if (handle != null) {
					getResourceRegistry().unlock(handle);
				}
			}
		}

		public String getResourceName() {
			return name;
		}

		public void unlocked(IResourceHandle handle) {
			freeHandles();
		}

	}
	
	private class SerialInputStream extends InputStream {

		private byte [] resultArray = new byte[1];
		private int[] numberOfCharactersReadArray = new int[1];
		private IResourceHandle resourceHandle;
		private IResourceHandle usart0ConfigHandle;
		
		SerialInputStream() throws IOException {
			try {
				resourceHandle = getResourceRegistry().getResource(getResourceName(), ResourceSharingScheme.EXCLUSIVE);
			} catch (ResourceUnavailableException e) {
				throw new IOException("Serial input stream already in use");
			}
			if (type != USB) {
				try {
					usart0ConfigHandle = getWriteLockOnUsart0Config();
				} catch (IOException e) {
					getResourceRegistry().unlock(resourceHandle);
					throw e;
				}
			}
		}

		public int read() throws IOException {
			int chars = read(resultArray, 0, 1);
			
			if (chars != 1) {
				throw new IOException("Error reading one character:"+chars);
			}
			return resultArray[0] & 0xFF;
		}
		
		public int read(byte b[], int off, int len) throws IOException {
			VM.execIO(ChannelConstants.GET_SERIAL_CHARS,0,off,len,type,0,0,0,numberOfCharactersReadArray,b);
			return numberOfCharactersReadArray[0];
		}
		
		public int available() throws IOException {
			return VM.execSyncIO(ChannelConstants.AVAILABLE_SERIAL_CHARS, type,0,0,0,0,0,null,null);
		}

		public void close() throws IOException {
			super.close();
			if (usart0ConfigHandle != null) {
				getResourceRegistry().unlock(usart0ConfigHandle);
			}
			getResourceRegistry().unlock(resourceHandle);
		}
	}

	private class SerialOutputStream extends OutputStream {
		private IResourceHandle usart0ConfigHandle;
		byte[] oneByteArray = new byte[1];
		
		public SerialOutputStream() throws IOException {
			if (type != USB) {
				usart0ConfigHandle = getWriteLockOnUsart0Config();
			}
		}
		
		public void write(int b) throws IOException {
			oneByteArray[0]= (byte)b;
			write(oneByteArray, 0, 1);
		}
		
		public void write(byte b[], int off, int len) throws IOException {
			VM.execSyncIO(ChannelConstants.WRITE_SERIAL_CHARS, off, len, type,0,0,0,b,null);
		}
		
		public void close() throws IOException {
			super.close();
			if (usart0ConfigHandle != null) {
				getResourceRegistry().unlock(usart0ConfigHandle);
			}
		}
	}
}

