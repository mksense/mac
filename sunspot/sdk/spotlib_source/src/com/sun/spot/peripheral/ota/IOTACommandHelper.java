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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * IOTACommandHelper
 * 
 * Each {@link IOTACommand} is passed an instance of this interface at execution
 * to help it communicate with the remote SpotClient.
 * 
 * 
 *
 */
public interface IOTACommandHelper {
	/**
	 * When a SPOT's admin command processor is receiving commands from a remote
	 * source, sensitive commands are verified using the digital signature of the SDK 
	 * installation to which the SPOT is tied, and a timestamp to protect against 
	 * replay attacks. The sensitivity of commands is measured on a scale of 1 to 5,
	 * where lower numbers indicate more sensitive commands.<br />
	 * <br />
	 * Informally, this scale is used as follows for the built-in commands:
	 * <ul>
	 * <li>1 commands which change state (start VM and set time)</li>
	 * <li>2 commands which change the SPOT's future behaviour (flash app, flash lib, changes
	 * to the public key, and set startup command line params)</li>
	 * <li>3 not used</li>
	 * <li>4 commands that retrieve information (get system property, get config page,
	 * get file info)</li>
	 * <li>5 commands for communication and identification (attention, closedown and blink (note
	 * that this is "closedown" as in "stop talking to this remote command source" rather than closing
	 * down the SPOT itself))</li></ul><br />
	 * <br />
	 * {@link #DEVICE_SECURITY_LEVEL} defaults to {@value #DEVICE_SECURITY_LEVEL}, so that commands
	 * with a security level of {@value #DEVICE_SECURITY_LEVEL} or lower are verified. You can rebuild
	 * the library with different values to change the security level.<br />
	 * <br />
	 * See also ISignatureVerifier.MAX_CLOCK_SKEW which defines how many milliseconds the
	 * timestamp may be different from the SPOT's clock before a command fails verification.<br />
	 * <br />
	 * Further notes: when apps and libraries are flashed, the command is verified, but not the data. Thus
	 * attackers could write arbitrary data to the flash. Note that a a subsequent attempt to start such an
	 * application would fail verification. Such techniques could be used for for DOS attacks, which aren't 
	 * considered by the current security model. <br>
	 */
	static final int DEVICE_SECURITY_LEVEL = 2;
	
	/**
	 * The offset into a command string at which the command-defining character is found
	 */
	static final int COMMAND_OFFSET = 2;
	
	/**
	 * The offset into a command string at which the parameters start
	 */
	static final int PARAMETER_OFFSET = COMMAND_OFFSET + 1;
	
	/**
	 * Send a bootloader prompt to a remote SpotClient. Normally used to indicate
	 * that the current command has been successfully processed. Call {@link #sendErrorDetails(String)} if
	 * an error occurs.
	 * 
	 * @throws IOException
	 */
	void sendPrompt() throws IOException;

	/**
	 * Notify the host that an error has occurred. If you call this, do not also call {@link #sendPrompt()}.
	 * @param msg the error message for the host
	 * @throws IOException
	 */
	void sendErrorDetails(String msg) throws IOException;

	/**
	 * Answer a data output stream for sending information to the host.
	 * @return the {@link DataOutputStream}
	 */
	DataOutputStream getDataOutputStream();
	
	/**
	 * Answer a data input stream for getting information from the host.
	 * @return the {@link DataInputStream}
	 */
	DataInputStream getDataInputStream();

	/**
	 * Receive bulk data. See IAdminTarget.flashFile(Flashable, String, byte[])
	 * inside the Spot Client code for more details of the corresponding host-side functionality.
	 * 
	 * @param dataSize
	 * @param outputStream
	 * @throws IOException 
	 */
	void receiveFile(long dataSize, OutputStream outputStream) throws IOException;

	/**
	 * @return whether the SPOT is locally or remotely connected to its host
	 */
	boolean isRemote();
	
	/**
	 * Replace a suite file. This is a specialised helper method, only intended to be used in the implementation
	 * of the flash app and flash lib commands.
	 * @param params the params supplied with the {@link IOTACommand}
	 * @param filename the name of the FlashFile to replace
	 * @param virtualAddress the virtual address at which the FlashFile should be mapped after restart, or 0 if an
	 * unused virtual address should be allocated to the suite
	 * @throws IOException
	 */
	void replaceSuiteFile(DataInputStream params, String filename, int virtualAddress) throws IOException;

	/**
	 * Invoke the default behaviour for one of the built-in commands. Intended for add-in commands that have replaced
	 * one of the default commands to extend its behaviour and want to invoke the default behaviour at some point. 
	 * @param cmd the name of the command
	 * @param params the parameters that were supplied with the {@link IOTACommand}
	 * @throws IOException
	 */
	void doDefaultCommand(String cmd, DataInputStream params) throws IOException;

	/**
	 * @return the bootloader identification string that will be sent in response to synchronisation commands
	 */
	String getBootloaderIdentificationString();
	
	/**
	 * Send data to the host.
	 * 
	 * @param data byte array of data to send
	 * @param offset offset into data at which to start sending
	 * @param length number of bytes from data to send
	 * @throws IOException
	 * @deprecated use {@link #sendData(byte[], int, int)} instead
	 */
	void sendDataWithCRC(byte[] data, int offset, int length) throws IOException;

	/**
	 * Send data to the host. The data is preceded by a big-end int which is the number
	 * of bytes to follow.
	 * 
	 * @param data byte array of data to send
	 * @param offset offset into data at which to start sending
	 * @param length number of bytes from data to send
	 * @throws IOException
	 */
	void sendData(byte[] data, int offset, int length) throws IOException;
	
	/**
	 * Check whether a particular suite is in use
	 * @param suiteUri the URI of the suite to check
	 * @return true if there is an active isolate with this suite as its leaf
	 */
	boolean isSuiteInUse(String suiteUri);

	/**
	 * If there is a flash file with the given name, make it obsolete by renaming it and
	 * setting its obsolete flag.
	 * @param filename the name of the file to make obsolete
	 * @throws IOException
	 */
	void makeObsolete(String filename) throws IOException;
}

