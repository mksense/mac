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

package com.sun.spot.imp;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import com.sun.spot.flashmanagement.FlashFile;
import com.sun.spot.flashmanagement.IFAT;
import com.sun.spot.flashmanagement.IFlashFileInfo;
import com.sun.spot.peripheral.ConfigPage;
import com.sun.spot.util.Utils;
import com.sun.squawk.VM;

/**
 * MIDletSuiteDescriptor: describes the contents of a MIDlet
 *
 */
public class MIDletSuiteDescriptor {
	
	private static final String MIDLET_PROP_PREFIX = "MIDlet-";

	private MIDletDescriptor[] midletDescriptors;
	private String uri;
	private String vendor;
	private String name;
	private String version;
	private long lastModified;
	private int length;
	private String sourcePath;
	
	/**
	 * @return an array of {@link MIDletSuiteDescriptor}s representing all the known MIDlets.
	 */
	public static MIDletSuiteDescriptor[] getAllInstances() {
		Vector tmpResult = new Vector();
		IFAT fat = FlashFile.getFAT();
		IFlashFileInfo[] fileInfos = fat.getFileInfos();
		for (int i = 0; i < fileInfos.length; i++) {
			if (fileInfos[i].getName().startsWith(ConfigPage.SPOT_SUITE_PROTOCOL_NAME + "://") &&
					!fileInfos[i].isObsolete() &&
					!fileInfos[i].getName().equals(ConfigPage.LIBRARY_URI)) {
				tmpResult.addElement(new MIDletSuiteDescriptor(fileInfos[i]));
			}
		}
		MIDletSuiteDescriptor[] result = new MIDletSuiteDescriptor[tmpResult.size()];
		tmpResult.copyInto(result);
		return result;
	}
	
	/**
	 * Construct a descriptor based on the information obtained from an {@link IFlashFileInfo}
	 * @param fileInfo
	 */
	public MIDletSuiteDescriptor(IFlashFileInfo fileInfo) {
		uri = fileInfo.getName();
		lastModified = fileInfo.lastModified();
		length = fileInfo.length();
		sourcePath = fileInfo.getComment();
		Vector tmpDescriptors = new Vector();
		Hashtable properties = null;
		try {
			properties = VM.getManifestPropertiesOfSuite(uri);
		} catch (Throwable e) {
			// Catching Throwable because ObjectMemoryLoader throws Error if it encounters 
			// a suite that was signed with the wrong key.
		}
		if (properties != null) {
			this.vendor = (String) properties.get("MIDlet-Vendor");
			this.name = (String) properties.get("MIDlet-Name");
			this.version = (String) properties.get("MIDlet-Version");
			Enumeration manifestProps = properties.keys();
			while (manifestProps.hasMoreElements()) {
				String propName = (String) manifestProps.nextElement();
				if (propName.startsWith(MIDLET_PROP_PREFIX)) {
					String n = propName.substring(MIDLET_PROP_PREFIX.length());
					try {
						int midletNum = Integer.parseInt(n);
						String[] descriptorParts = Utils.split((String)properties.get(propName), ',');
						MIDletDescriptor midletDescriptor = new MIDletDescriptor();
						midletDescriptor.number = midletNum;
						midletDescriptor.label = descriptorParts[0].trim();
						midletDescriptor.iconName = descriptorParts[1].trim();
						midletDescriptor.className = descriptorParts[2].trim();
						tmpDescriptors.addElement(midletDescriptor);
					} catch (NumberFormatException e) {}
				}
			}
		}
		midletDescriptors = new MIDletDescriptor[tmpDescriptors.size()];
		tmpDescriptors.copyInto(midletDescriptors);		
	}

	/**
	 * @return an array of {@link MIDletSuiteDescriptor}s for all the MIDlets in this MIDlet suite
	 */
	public MIDletDescriptor[] getMIDletDescriptors() {
		return midletDescriptors;
	}

	/**
	 * @return the uri for manipulating this MIDlet suite
	 */
	public String getURI() {
		return uri;
	}

	/**
	 * @return the vendor of this MIDlet suite
	 */
	public String getVendor() {
		return vendor;
	}

	/**
	 * @return the name of this MIDlet suite
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the version of this MIDlet suite
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * @return the lastModified date of this MIDlet suite
	 */
	public long getLastModified() {
		return lastModified;
	}

	/**
	 * @return Returns the length of this MIDlet suite.
	 */
	public int getLength() {
		return length;
	}

	/**
	 * @return Returns the sourcePath of this MIDlet suite.
	 */
	public String getSourcePath() {
		return sourcePath;
	}

}
