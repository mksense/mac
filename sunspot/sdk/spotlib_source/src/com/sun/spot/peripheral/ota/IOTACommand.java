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
import java.io.IOException;

/**
 * IOTACommand: a SPOT admin command. See {@link IOTACommandProcessorExtension} for details
 * of extending the default set of SPOT admin commands. A single IOTACommand instance may implement
 * one or more SPOT admin commands. 
 */
public interface IOTACommand {

	/**
	 * Attempt to process a command.
	 * @param command the command that was invoked
	 * @param params a {@link DataInputStream} from which the command's parameters can be read
	 * @param helper infrastructure-supplied source of command helper operations
	 * @return false if the SPOT admin command processor should closedown as a result of this command being run,
	 * otherwise true. Return true even when the command failed.
	 * @throws IOException
	 */
	boolean processCommand(String command, DataInputStream params, IOTACommandHelper helper) throws IOException;
	
	/**
	 * Answer the security level for a command. See {@link IOTACommandHelper#DEVICE_SECURITY_LEVEL}
	 * @param command the command  in question
	 * @return the security level
	 */
	int getSecurityLevelFor(String command);
}
