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

package com.sun.spot.peripheral.ota;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

import com.sun.spot.flashmanagement.FlashFile;
import com.sun.spot.flashmanagement.FlashFileOutputStream;
import com.sun.spot.io.j2me.remoteprinting.IRemotePrintManager;
import com.sun.spot.peripheral.IPowerController;
import com.sun.spot.peripheral.IRadioControl;
import com.sun.spot.peripheral.ISpot;
import com.sun.spot.peripheral.Spot;
import com.sun.spot.peripheral.SpotFatalException;
import com.sun.spot.peripheral.TimeoutException;
import com.sun.spot.peripheral.radio.RadioPolicy;
import com.sun.spot.util.CrcOutputStream;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Utils;
import com.sun.squawk.Isolate;
import com.sun.squawk.VM;
import com.sun.squawk.security.verifier.SignatureVerifierException;
import javax.microedition.io.Connection;

/**
 * This class monitors radiostream communications on port number 8 during an OTA
 * session, and responds to commands received. These commands allow flashing the
 * Spot's config page and/or applications remotely, retrieving the config page
 * contents, and restarting the Spot.
 */
class OTACommandProcessor extends Thread implements ISpotAdminConstants, IOTACommandProcessor, IOTACommandHelper, IOTACommandRepository {

	public static final int PORT = 8;

	private static final int COMMAND_OFFSET = 2;
	private static final int CRC_BLOCK_SIZE = 121; // bug 1131: if you set this to 122 then CRC stream blocks are 128 long and
												   // fit exactly into two USB frames - these are sent to the host but not
												   // passed to the application

	private Vector listeners = new Vector();
	private ISpot spot;
	private boolean suspended = false;
	private boolean isRemote = true;
	private long timeOfLastCommunication = 0;
    private long startTime = 0;
	private DataInputStream dataInputStream;
	private DataOutputStream dataOutputStream;
	private int flashedByteCount;
	private IEEEAddress hostAddress;
	private IRadioControl conn;
	private boolean closed = false;
	private Hashtable commands = new Hashtable();
	private Hashtable defaultCommands = new Hashtable();
	
	/**
	 * Entry point for admin mode (that is, locally connected mode).
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Spot.getInstance().getSleepManager().disableDeepSleep();
		conditionallyDirectOutputToUsart();
		OTACommandProcessor commandProcessor = new OTACommandProcessor(new IEEEAddress(0), Spot.getInstance().getPowerController(), Spot.getInstance(), null);		
		commandProcessor.isRemote = false;
		StreamConnection conn = (StreamConnection) Connector.open("serial://");
		IRadioControl rc = new IRadioControl() {
			public byte getLocalPort() {
				return 0;
			}
			public void setRadioPolicy(RadioPolicy selection) {
				// no op
			}
		};
		OutputStream rawOutputStream = conn.openOutputStream();
		InputStream rawInputStream = conn.openInputStream();
		rawOutputStream.write(("" + '\n').getBytes()); // bug 985 - send a USB packet that might get discarded on a Mac
		rawOutputStream.flush();
		rawOutputStream.write((commandProcessor.getBootloaderIdentificationString() + '\n').getBytes());
		rawOutputStream.flush();
		System.out.println("Sent identification string of " + commandProcessor.getBootloaderIdentificationString());
		
		CrcOutputStream crcOutputStream = new CrcOutputStream(rawOutputStream, rawInputStream, CRC_BLOCK_SIZE);
		commandProcessor.initialize(
				new DataInputStream(crcOutputStream.getInputStream()),
				new DataOutputStream(crcOutputStream),
				rc);
	}

	OTACommandProcessor(IEEEAddress hostAddress, IPowerController powerController, ISpot spot, IRemotePrintManager remotePrintManager) {
		super("OTACommandProcessor thread");
		this.hostAddress = hostAddress;
		this.spot = spot;
        startTime = System.currentTimeMillis();
		OTADefaultCommands defaultCommandsProcessor = new OTADefaultCommands(hostAddress, powerController, spot, remotePrintManager);
		defaultCommandsProcessor.configureCommands(this);
		Enumeration commandNames = commands.keys();
		while (commandNames.hasMoreElements()) {
			Object commandName = commandNames.nextElement();
			defaultCommands.put(commandName, commands.get(commandName));
		}
	}

	void initializeForTest(IRadioControl conn, DataOutputStream dataOutputStream) {
		this.conn = conn;
		this.dataOutputStream = dataOutputStream;
	}

	public void initialize(DataInputStream dataInputStream, DataOutputStream dataOutputStream, IRadioControl conn) throws IOException {
		Utils.log("[OTACommandProcessor] Starting session with " + hostAddress);
		configureExtensions();
		this.conn = conn;
		this.dataOutputStream = dataOutputStream;
		this.dataInputStream = dataInputStream;
		setPriority(Thread.MAX_PRIORITY - 1);
		start();
	}

	private void configureExtensions() {
		Enumeration keys = VM.getManifestPropertyNames();
		while (keys.hasMoreElements()) {
			String key = (String)keys.nextElement();
			if (key.startsWith(IOTACommandProcessorExtension.OTA_COMMAND_PROCESSOR_EXTENSION_PREFIX)) {
				String className = VM.getManifestProperty(key);
				try {
					IOTACommandProcessorExtension extension = (IOTACommandProcessorExtension)Class.forName(className).newInstance();
					extension.configureCommands(this);
					Utils.log("Added OTACommandProcessor extension " + className);
				} catch (ClassNotFoundException e) {
					throw new SpotFatalException("OTACommandProcessor extension " + key + " refers to missing class " + className);
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}

	}

	/**
	 * Should not normally be invoked from user code - call
	 * {@link #initialize()} instead.
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		try {
			sendPrompt();
			while (!closed) {
				byte[] cmd = null;
				try {
					cmd = getCommand();
				} catch (TimeoutException e) {
					// If the OTACommandServer called closedown, then that will have closed the dataInputStream
					// and generated a Timeout. If we aren't closed then this must
					// have been a genuine timeout.
					if (closed) {
						Utils.log("[OTACommandProcessor] Closing session at OTACommandServer request");
					} else {
						Utils.log("[OTACommandProcessor] Closing session as no recent command from host");
					}						
					break;
				}
				try {
					conn.setRadioPolicy(RadioPolicy.ON);
					if (cmd.length == 0) {
						throw new SpotFatalException("[OTACommandProcessor] Received a zero-length command");
					}
					if (!suspended) {
						timeOfLastCommunication  = System.currentTimeMillis();
						if (!processCommand(cmd)) {
							break;
                        }
					} else {
						sendErrorDetails("OTACommandServer is suspended");
					}
				} catch (SignatureVerifierException e) {
					Utils.log("[OTACommandProcessor] Got verification failure while processing command: ");					e.printStackTrace();
					sendErrorDetails(ERROR_COMMAND_VERIFICATION_FAILED, e.getMessage());
				} catch (IOException e) {
					Utils.log("[OTACommandProcessor] Got IOException while processing command: " + e.getMessage());
                    break;      // an IOException probably means a problem communicating with host,
                                // so don't compound matters by trying to report an error via radio
				} catch (Throwable e) {
					Utils.log("[OTACommandProcessor] Got exception while processing command: ");
					e.printStackTrace();
					sendErrorDetails("Error while processing command: " + e.getMessage());
                    break;
				} finally {
					conn.setRadioPolicy(RadioPolicy.AUTOMATIC);
				}
			}
		} catch (Throwable e1) {
			System.err.println("[OTACommandProcessor] Closing session as got exception communicating with host");
			e1.printStackTrace();
		} finally {
			closedown();
		}
	}

	public synchronized void closedown() {
        if (!closed) {
            closed = true;
            try {
                dataInputStream.close();
            } catch (Exception ei) {
                Utils.log("[OTACommandProcessor] Got an error closing session input: " + ei);
            }
            try {
                dataOutputStream.close();
            } catch (Exception eo) {
                Utils.log("[OTACommandProcessor] Got an error closing session output: " + eo);
            }
            try {
                if (conn instanceof Connection) {
                    ((Connection) conn).close();
                }
            } catch (Exception ec) {
                Utils.log("[OTACommandProcessor] Got an error closing session connection: " + ec);
            }
        }
	}

	public void sendErrorDetails(String msg) throws IOException {
		sendErrorDetails(ERROR_GENERAL, msg);
	}
	
	private void sendErrorDetails(int errorType, String msg) throws IOException {
		String fullMsg = BOOTLOADER_CMD_HEADER + ":E" + errorType + msg;
		Utils.log("[OTA] Sending error details: " + fullMsg);
		sendUTF(fullMsg, BOOTLOADER_CMD_HEADER + ">");
	}

	/**
	 * Attach a listener to be notified of the start and stop of flash
	 * operations.
	 * 
	 * @param sml --
	 *            the listener
	 */
	public void addListener(IOTACommandServerListener sml) {
		listeners.addElement(sml);
	}

	/**
	 * 
	 * @param cmd
	 * @return false if the command was closedown, otherwise true
	 * @throws IOException
	 * @throws SignatureVerifierException 
	 */
	boolean processCommand(byte[] cmd) throws IOException, SignatureVerifierException {
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(cmd));
		dis.skip(2);
		String commandName = dis.readUTF();
		IOTACommand command = (IOTACommand)commands.get(commandName);
		if (command == null) {
			sendErrorDetails(ERROR_UNKNOWN_COMMAND, "Unsupported command " + commandName);
			return true;
		}
		Utils.log("Processing command " + commandName);
		verifyCommand(command.getSecurityLevelFor(commandName), cmd);
		return command.processCommand(commandName, dis, this);
	}

	public String getBootloaderIdentificationString() {
		String bootloaderTimestamp = Utils.getManifestProperty("BootloaderTimestamp", "Unknown");
		return BOOTLOADER_CMD_HEADER + ":S " +
			(isRemote ? REMOTE_OTA_COMMAND_SERVER_IDENTIFICATION_STRING : LOCAL_OTA_COMMAND_SERVER_IDENTIFICATION_STRING)
			+ " (" + bootloaderTimestamp + ") ";
	}

	public void sendPrompt() throws IOException {
		sendUTF(BOOTLOADER_CMD_HEADER + ">");
	}

	public void replaceSuiteFile(DataInputStream params, String filename, int virtualAddress) throws IOException {
		String slotDescriptor = params.readUTF();
		FlashFile newFile = new FlashFile("new:" + filename);
		if (newFile.exists()) {
			newFile.delete();
		}
		dataOutputStream.writeInt(virtualAddress);
		dataOutputStream.flush();
		int size = dataInputStream.readInt();
		if (!newFile.createNewFile(size+1)) throw new IOException("File " + newFile.getName() + " already exists");
		newFile.setComment(slotDescriptor);
		Utils.log("[OTA] Using virtual address 0x" + Integer.toHexString(virtualAddress) + " for " + filename);
		notifyListenersOfFlashStart();
		FlashFileOutputStream flashFileOutputStream = new FlashFileOutputStream(newFile);
		try {
			receiveFile(size, flashFileOutputStream);
			flashFileOutputStream.write(0xFF); // append an 0xFF to allow space for suite verified flag
			flashFileOutputStream.close();
			makeObsolete(filename);
			newFile.setVirtualAddress(virtualAddress);
			newFile.renameTo(new FlashFile(filename)); // rename also forces write of virtual address
		} finally {
			notifyListenersOfFlashEnd();
		}
	}

	public void makeObsolete(String filename) throws IOException {
		FlashFile currentFile = new FlashFile(filename);
		if (currentFile.exists()) {
			FlashFile obsoleteFlashFile = new FlashFile(("obsolete:" + filename));
			if (obsoleteFlashFile.exists()) {
				// we already have an obsolete version of this file, so the currentFile is
				// not in use and can be safely deleted
				currentFile.delete();
			} else {
				// the currentFile is live, so we can't delete it - mark it obsolete instead
				currentFile.setObsolete(true);
				currentFile.renameTo(obsoleteFlashFile);
			}
		}
	}

	private void notifyListenersOfFlashStart() {
		Enumeration theEnum = listeners.elements();
		while (theEnum.hasMoreElements()) {
			IOTACommandServerListener element = (IOTACommandServerListener) theEnum.nextElement();
			element.preFlash();
		}
	}

	private void notifyListenersOfFlashEnd() {
		Enumeration theEnum = listeners.elements();
		while (theEnum.hasMoreElements()) {
			IOTACommandServerListener element = (IOTACommandServerListener) theEnum.nextElement();
			element.postFlash();
		}
	}

	public void doDefaultCommand(String commandName, DataInputStream params) throws IOException {
		IOTACommand command = (IOTACommand)defaultCommands.get(commandName);
		if (command == null) {
			throw new SpotFatalException("Unknown default command " + commandName);
		}
		Utils.log("Processing default command " + commandName);
		command.processCommand(commandName, params, this);
	}
	
	private void sendUTF(String stringToSend) throws IOException {
		dataOutputStream.writeUTF(stringToSend);
		dataOutputStream.flush();
	}

	private void sendUTF(String stringToSend1, String stringToSend2) throws IOException {
		dataOutputStream.writeUTF(stringToSend1);
		dataOutputStream.writeUTF(stringToSend2);
		dataOutputStream.flush();
	}

	private byte[] getCommand() throws IOException {
        int len = 0;
        while (len == 0) {
            len = dataInputStream.readInt();
        }
        if (isRemote) {
            if (len < 0 || len > 10000) {
                Utils.log("[OTACommandProcessor] getCommand: Bad command length = " + len);
                throw new IOException("Bad command length");
            }
        }
		byte[] command = new byte[len];
		dataInputStream.readFully(command);
		return command;
	}

	private static void conditionallyDirectOutputToUsart() {
		Isolate.currentIsolate().clearOut();
		Isolate.currentIsolate().clearErr();
		if (Utils.isOptionSelected(ISpot.PROPERTY_SPOT_DIAGNOSTICS, false) && Spot.getInstance().getUsbPowerDaemon().isUsbEnumerated()) {
			// diagnostics are on and the USB appears to be in use
			Isolate.currentIsolate().addOut("serial://usart");
			Isolate.currentIsolate().addErr("serial://usart");
			Utils.log("Output now redirected to USART");
		}
	}

	private void verifyCommand(int securityLevel, byte[] cmd) throws SignatureVerifierException, IOException {
		if (VM.isVerbose()) {
			Utils.log("Command security level: " + securityLevel + ". Current security level " + DEVICE_SECURITY_LEVEL);
		}
		if (isRemote && securityLevel <= DEVICE_SECURITY_LEVEL) {
			// Only verify command if public key is set.
			if (spot.getPublicKey().length > 0) {
				// the length in the command + the length of the length field
				// is the offset of the signature in the command.
				int length = (cmd[1] & 0xff) + ((cmd[0] & 0xff)<<8) + COMMAND_OFFSET;
				getSignatureVerifier().verifyWithTimestamp(cmd, length);
			}
		}
		sendPrompt();
	}

	ISignatureVerifier getSignatureVerifier() throws SignatureVerifierException {
		ISignatureVerifier signatureVerifier = new SignatureVerifierWrapper();
		signatureVerifier.initialize(spot.getPublicKey(), 0, spot.getPublicKey().length);
		return signatureVerifier;
	}

	/**
	 * @return Returns true if the server has been suspended by software.
	 */
	public boolean isSuspended() {
		return suspended;
	}

	/**
	 * @param suspended Suspends or resumes the server (it is initially running).
	 */
	public void setSuspended(boolean suspended) {
		this.suspended = suspended;
	}
	
	/**
	 * @return The time when the server last received a message from the host
	 */
	public Date timeOfLastMessageFromHost() {
		return new Date(timeOfLastCommunication);
	}

    /**
     * @return the time that this OTA session was started
     */
	public long getStartTime() {
		return startTime;
	}

	public IEEEAddress getBasestationAddress() {
		return hostAddress;
	}

	public void addCommand(String commandName, IOTACommand command) {
		commands.put(commandName, command);
	}
	
	public DataOutputStream getDataOutputStream() {
		return dataOutputStream;
	}

	public DataInputStream getDataInputStream() {
		return dataInputStream;
	}

	public void receiveFile(long dataSize, OutputStream flashOutputStream) throws IOException {
		flashedByteCount = 0;
		try {
			boolean isUsbEnumerated = spot.getUsbPowerDaemon().isUsbEnumerated();
			byte[] buffer = new byte[FlashFileOutputStream.DEFAULT_BUFFER_SIZE];
			while (flashedByteCount < dataSize) {
				int bytesToWriteThisLoop = Math.min(buffer.length, (int) dataSize - flashedByteCount);
				dataInputStream.readFully(buffer, 0, bytesToWriteThisLoop);
				if (!isUsbEnumerated) {
					Utils.sleep(20); // when using a USART connection, allow time for the stream traffic to quiesce
									 // TODO find out why this is necessary - bug 1133
				}
                if (isRemote) {
                    if (dataInputStream.available() > 0) {
                        throw new IOException("Unexpected input while receiving file!");
                    }
                    dataOutputStream.writeUTF("ok");
                    dataOutputStream.flush();
                }
				flashOutputStream.write(buffer, 0, bytesToWriteThisLoop);
				flashedByteCount += bytesToWriteThisLoop;
			}
		} catch (TimeoutException e) {
			throw new RuntimeException("Timeout failure during flashing operation");
		}
	}

	public boolean isRemote() {
		return isRemote;
	}

	/**
	 * @deprecated
	 */
	public void sendDataWithCRC(byte[] data, int offset, int length) throws IOException {
		Utils.putDataWithCRC(dataOutputStream, data, offset, length);
	}

	public void sendData(byte[] data, int offset, int length) throws IOException {
		dataOutputStream.writeInt(length);
		if (length > 0) {
			dataOutputStream.write(data, offset, length);
		}
		dataOutputStream.flush();
	}

	/**
	 * For test only
	 */
	void setDataInputStream(DataInputStream dataInputStream) {
		this.dataInputStream = dataInputStream;
	}

	public boolean isSuiteInUse(String suiteUri) {
		System.gc(); // Try to ensure that all exited isolates go away
		Isolate[] allIsolates = Isolate.getIsolates();
		for (int i = 0; i < allIsolates.length; i++) {
			if (allIsolates[i].getParentSuiteSourceURI().equals(suiteUri)) {
				return true;
			}
		}
		return false;
	}
}
