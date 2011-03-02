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

package com.sun.spot.peripheral.ota;


/**
 * IOTACommandProcessorExtension
 * 
 * Any class that wants to install OTA commands (either additional commands or replacements/extensions
 * for the default commands) must implement this interface.
 * 
 * The class must also have a no-arg public constructor, and should be referenced in the 
 * manifest file of the library add-in jar that it is part of, with a line like this:
 * 
 * {@value #OTA_COMMAND_PROCESSOR_EXTENSION_PREFIX}-my-library-addin: foo.bar.MyOTACommandProcessorExtension
 * 
 * where "{@value #OTA_COMMAND_PROCESSOR_EXTENSION_PREFIX}-" is a required prefix.
 *
 */

public interface IOTACommandProcessorExtension {
	/**
	 * The prefix that properties must have to identify classes that extend the SPOT admin command processor.
	 */
	static final String OTA_COMMAND_PROCESSOR_EXTENSION_PREFIX = "spot-ota-extension-";
	
	/**
	 * Call back invoked when command processor extensions are installed. The receiver should add and replace
	 * commands using {@link IOTACommandRepository#addCommand(String, IOTACommand)}
	 * 
	 * @param repository the repository to adjust
	 */
	void configureCommands(IOTACommandRepository repository);
}
