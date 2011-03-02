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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;

import com.sun.spot.flashmanagement.FlashFile;
import com.sun.spot.flashmanagement.IFAT;
import com.sun.spot.flashmanagement.IFlashFileInfo;
import com.sun.spot.imp.MIDletSuiteDescriptor;
import com.sun.spot.io.j2me.remoteprinting.IRemotePrintManager;
import com.sun.spot.peripheral.ConfigPage;
import com.sun.spot.peripheral.ILed;
import com.sun.spot.peripheral.IPowerController;
import com.sun.spot.peripheral.ISpot;
import com.sun.spot.service.IService;
import com.sun.spot.service.ISpotBlink;
import com.sun.spot.service.ServiceRegistry;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Properties;
import com.sun.spot.util.Utils;
import com.sun.squawk.Isolate;
import com.sun.squawk.VM;

class OTADefaultCommands extends Thread implements ISpotAdminConstants, IOTACommandProcessorExtension {

	/**
	 * 
	 * The security levels define which commands are verified using a digital
	 * signature and a timestamp:
	 * <p>
	 * 0 no command verification<br>
	 * 1 commands which change state (like run, or settime)<br>
	 * 2 flashapp. The application is always verified, but attackers could write
	 * arbitrary data to the flash. Note that the data itself is not verified
	 * before it is written, therefore attackers still could inject data (which
	 * later would fail verification), but they are not able to start the flash
	 * process.<br>
	 * 3 flashConfig. The received configpage is not written to flash until the
	 * config page and the application suite it points to are verified. This
	 * verification lasts about one second and is not interruptible by other
	 * threads, which could be used for DOS attacks. However, the security model
	 * doesn't consider DOS attacks, thus the security gain by signing
	 * flashconfig commands is probably low. <br>
	 * 4 commands which retrieve information (like getconfigpage).<br>
	 * 5 synchronize, attention.
	 * <p>
	 * The default security level is 2, which means that commands with a higher
	 * security level won't be verified.
	 * <p>
	 * ISignatureVerifier.MAX_CLOCK_SKEW defines how many milliseconds the
	 * timestamp may be different from the devices clock.
	 */

	private static final int SECURITY_LEVEL_STARTVM = 1;
	private static final int SECURITY_LEVEL_SETTIME = 1;
	private static final int SECURITY_LEVEL_SETSYSTEMPROPERTY = 1;
	private static final int SECURITY_LEVEL_GETSYSTEMPROPERTY = 4;
	private static final int SECURITY_LEVEL_GETCONFIGPAGE = 4;
	private static final int SECURITY_LEVEL_GETFILEINFO = 4;
	private static final int SECURITY_LEVEL_GETFILELIST = 4;
	private static final int SECURITY_LEVEL_FLASHAPP = 2;
	private static final int SECURITY_LEVEL_FLASHLIB = 2;
	private static final int SECURITY_LEVEL_UNDEPLOY = 2;
	private static final int SECURITY_LEVEL_ATTENTION = 5;
	private static final int SECURITY_LEVEL_DELETEPUBLICKEY = 2;
	private static final int SECURITY_LEVEL_SETPUBLICKEY = 2;
	private static final int SECURITY_LEVEL_CLOSEDOWN = 5;
	private static final int SECURITY_LEVEL_BLINK = 5;
	private static final int SECURITY_LEVEL_SET_STARTUP_CMD = 2;


	private ISpot spot;
	private IPowerController powerController;
	private IRemotePrintManager remotePrintManager;
	private IEEEAddress hostAddress;
	
	OTADefaultCommands(IEEEAddress hostAddress, IPowerController powerController, ISpot spot, IRemotePrintManager remotePrintManager) {
		this.hostAddress = hostAddress;
		this.powerController = powerController;
		this.spot = spot;
		this.remotePrintManager = remotePrintManager;
	}

	private void processBlinkCmd(DataInputStream params, IOTACommandHelper helper) throws IOException {
		int duration = params.readInt();
        IService s [] = ServiceRegistry.getInstance().lookupAll(ISpotBlink.class);
        for (int i = 0; i < s.length; i++) {
            ((ISpotBlink)s[i]).blink(duration);
        }
		helper.sendPrompt();
	}

	private void processSetStartupCmd(DataInputStream params, IOTACommandHelper helper) throws IOException {
		ConfigPage configPage = spot.getConfigPage();
		configPage.setStartup(params.readUTF(), params.readUTF(), params.readUTF());
		spot.flashConfigPage(configPage);
		helper.sendPrompt();
	}

	private void processSetPublicKeyCmd(DataInputStream params, IOTACommandHelper helper) throws IOException {
		byte[] newKey = new byte[params.readShort()];
		params.readFully(newKey);
		setPublicKey(newKey);
		helper.sendPrompt();
	}

	private void processDeletePublicKeyCmd(IOTACommandHelper helper) throws IOException {
		setPublicKey(new byte[0]);
		helper.sendPrompt();
	}

	private void processSetSystemPropertiesCmd(DataInputStream params, IOTACommandHelper helper) throws IOException {
		int dataSize = helper.getDataInputStream().readInt();
		ByteArrayOutputStream baos = new ByteArrayOutputStream(dataSize);
		helper.receiveFile(dataSize, baos);
		Properties newPersistentProperties = new Properties();
		newPersistentProperties.load(new ByteArrayInputStream(baos.toByteArray()));
		
		Properties oldPersistentProperties = spot.getPersistentProperties();
		
		// store new properties in flash
		spot.storeProperties(newPersistentProperties);
		
		// remove deleted properties from current isolate
		Enumeration oldKeys = oldPersistentProperties.keys();
		while (oldKeys.hasMoreElements()) {
			String oldKey = (String) oldKeys.nextElement();
			if (!newPersistentProperties.containsKey(oldKey)) {
				VM.setProperty(oldKey, null);
			}
		}
		
		// store new and modified properties in current isolate
		Enumeration newKeys = newPersistentProperties.keys();
		while (newKeys.hasMoreElements()) {
			String newKey = (String) newKeys.nextElement();
			VM.setProperty(newKey, newPersistentProperties.getProperty(newKey));
		}

		helper.sendPrompt();
	}

	private void processStartVMCmd() {
		VM.stopVM(0);
	}

	private void processFlashLibCmd(DataInputStream params, IOTACommandHelper helper) throws IOException {
		helper.replaceSuiteFile(params, ConfigPage.LIBRARY_URI, ConfigPage.LIBRARY_VIRTUAL_ADDRESS);
		MIDletSuiteDescriptor[] suites = MIDletSuiteDescriptor.getAllInstances();
		for (int i = 0; i < suites.length; i++) {
			FlashFile suiteFile = new FlashFile(suites[i].getURI());
			suiteFile.setObsolete(true);
			suiteFile.commit();
		}
		helper.sendPrompt();
	}

	private void processFlashAppCmd(DataInputStream params, IOTACommandHelper helper) throws IOException {
		String fileNameOnTarget = params.readUTF();
		FlashFile suiteFile = new FlashFile(fileNameOnTarget);
		String currentSuiteUri = Isolate.currentIsolate().getParentSuiteSourceURI();

		boolean isReplacingCurrentAppSuite = fileNameOnTarget.equals(currentSuiteUri);
		boolean isRunningLibrary = currentSuiteUri.equals(ConfigPage.LIBRARY_URI);
		boolean isLibrarySuiteObsolete = new FlashFile("obsolete:" + ConfigPage.LIBRARY_URI).exists();
		boolean isMasterAppSuiteObsolete = !isRunningLibrary && new FlashFile("obsolete:" + currentSuiteUri).exists();
		
		/*
		 *   ircas   irl    ilso   imaso      action
		 *   0       x       0       0        delete if exists and not in use, flash
		 *   0       0       0       1        error return "Attempt to flash child suite while update to master is pending"
		 *   0       1       0       1        cannot happen
		 *   0       x       1       x        error return "Attempt to flash application suite while update to library is pending"
		 *   1       0       x       0        obsolete master app, then flash
		 *   1       0       x       1        flash master app
		 *   1       1       x       x        cannot happen
		 *   
		 */
		
		if (!isReplacingCurrentAppSuite) {
			if (isLibrarySuiteObsolete) {
				helper.sendErrorDetails("Attempt to flash application suite while update to library is pending");
				return;				
			}
			if (isMasterAppSuiteObsolete) {
				helper.sendErrorDetails("Attempt to flash child suite while update to master is pending"); // because if we
					// allowed this we'd need to perform a remap of the MMU, which would cause us to start executing the
					// new bytecodes of the pending master app
				return;
			}
			if (suiteFile.exists()) {
				if (helper.isSuiteInUse(fileNameOnTarget)) {
					helper.sendErrorDetails("Attempt to replace child suite that is in use");
					return;
				}
				VM.unregisterSuite(fileNameOnTarget);
				suiteFile.delete();
			}
		}
		int virtualAddress;
		FlashFile currentFile = new FlashFile(fileNameOnTarget);
		if (currentFile.exists()) {
			virtualAddress = currentFile.getVirtualAddress();
		} else {
			virtualAddress = FlashFile.getUnusedVirtualAddress();
		}
		helper.sendPrompt();
		Utils.log("[OTA] Flashing suite: " + fileNameOnTarget);
		helper.replaceSuiteFile(params, fileNameOnTarget, virtualAddress);
		if (!isReplacingCurrentAppSuite) {
			Utils.log("[OTA] remapping virtual addresses...");
			suiteFile.map();
		}
		helper.sendPrompt();
	}

	private void processUndeployCmd(DataInputStream params, IOTACommandHelper helper) throws IOException {
		String suiteUri = params.readUTF();
		FlashFile suiteFile = new FlashFile(suiteUri);
		if (suiteFile.exists()) {
			if (helper.isSuiteInUse(suiteUri)) {
				if (suiteUri.equals(Isolate.currentIsolate().getParentSuiteSourceURI())) {
					helper.makeObsolete(suiteUri);
				} else {
					helper.sendErrorDetails("Attempt to undeploy suite that is in use");
					return;
				}
			} else {
				VM.unregisterSuite(suiteUri);
				suiteFile.delete();
			}
		} else {
			helper.sendErrorDetails("Attempt to undeploy unknown suite: " + suiteUri);
			return;			
		}
		helper.sendPrompt();
	}

	private void processResyncCmd(IOTACommandHelper helper) throws IOException {
		sendUTF(helper.getDataOutputStream(), helper.getBootloaderIdentificationString());
	}
	
	private void processGetFileInfoCmd(DataInputStream params, IOTACommandHelper helper) throws IOException {
		String filename = params.readUTF();
		FlashFile flashFile = new FlashFile(filename);
		if (flashFile.exists()) {
			helper.getDataOutputStream().writeBoolean(true);
			byte[] fileData = new OTAFlashFileInfo(flashFile).toByteArray();
			helper.sendData(fileData, 0, fileData.length);
		} else {
			helper.getDataOutputStream().writeBoolean(false);
		}
		helper.sendPrompt();
	}

	private void processGetFileListCmd(DataInputStream params, IOTACommandHelper helper) throws IOException {
		IFAT fat = FlashFile.getFAT();
		IFlashFileInfo[] fileInfos = fat.getFileInfos();
		DataOutputStream dos = helper.getDataOutputStream();
		dos.writeInt(fileInfos.length);
		for (int i = 0; i < fileInfos.length; i++) {
			dos.writeUTF(fileInfos[i].getName());
		}
		int[] freeSectorIndices = fat.getFreeSectorIndices();
		dos.writeInt(freeSectorIndices.length);
		for (int i = 0; i < freeSectorIndices.length; i++) {
			dos.writeInt(freeSectorIndices[i]);
		}
		helper.sendPrompt();
	}

	private void processGetConfigPageCmd(IOTACommandHelper helper, boolean len) throws IOException {
		ConfigPage configPage;
		configPage = spot.getConfigPage();
		byte[] data = configPage.asByteArray();
        if (len) {
            int i = data.length - 1;
            while (i > 0 && data[i] == 0) i--;
            i = i / 4;
            helper.getDataOutputStream().writeByte(i & 0xff);
            helper.getDataOutputStream().write(data, 0, (i + 1) * 4);
        } else {
            helper.getDataOutputStream().write(data);
        }
		helper.sendPrompt();
	}

	private void processAttentionCmd(IOTACommandHelper helper) {
		if (helper.isRemote()) {
			Utils.log("Redirecting output to " + hostAddress.asDottedHex());
			remotePrintManager.redirectOutputStreams(hostAddress.asDottedHex());
		}
	}

	private void processSetTimeCmd(DataInputStream params, IOTACommandHelper helper) throws IOException {
		long newTime = params.readLong();
		powerController.setTime(newTime);
		helper.sendPrompt();
	}

	private void setPublicKey(byte[] newKey) {
		ConfigPage configPage = spot.getConfigPage();
		configPage.setPublicKey(newKey);
		spot.flashConfigPage(configPage);
	}

	private void processGetSystemPropertiesCmd(IOTACommandHelper helper) throws IOException {
		Properties p = spot.getPersistentProperties();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		p.store(baos, "System properties on SPOT");
		helper.sendData(baos.toByteArray(), 0, baos.size());
		helper.sendPrompt();
	}

	private void sendUTF(DataOutputStream dataOutputStream, String stringToSend) throws IOException {
		dataOutputStream.writeUTF(stringToSend);
		dataOutputStream.flush();
	}

	public void configureCommands(IOTACommandRepository repository) {
		repository.addCommand(SET_TIME_CMD, new ExtensionWrapper(SECURITY_LEVEL_SETTIME) {
			public void processCommand(DataInputStream params, IOTACommandHelper helper) throws IOException { processSetTimeCmd(params, helper); }
		});
		repository.addCommand(GET_SYSTEM_PROPERTIES, new ExtensionWrapper(SECURITY_LEVEL_GETSYSTEMPROPERTY) {
			public void processCommand(DataInputStream params, IOTACommandHelper helper) throws IOException { processGetSystemPropertiesCmd(helper); }
		});
		repository.addCommand(SET_SYSTEM_PROPERTIES, new ExtensionWrapper(SECURITY_LEVEL_SETSYSTEMPROPERTY) {
			public void processCommand(DataInputStream params, IOTACommandHelper helper) throws IOException { processSetSystemPropertiesCmd(params, helper); }
		});
		repository.addCommand(BLINK_CMD, new ExtensionWrapper(SECURITY_LEVEL_BLINK) {
			public void processCommand(DataInputStream params, IOTACommandHelper helper) throws IOException { processBlinkCmd(params, helper); }
		});
		repository.addCommand(GET_CONFIG_PAGE_CMD, new ExtensionWrapper(SECURITY_LEVEL_GETCONFIGPAGE) {
			public void processCommand(DataInputStream params, IOTACommandHelper helper) throws IOException { processGetConfigPageCmd(helper, false); }
		});
		repository.addCommand(GET_CONFIG_PAGE_LEN_CMD, new ExtensionWrapper(SECURITY_LEVEL_GETCONFIGPAGE) {
			public void processCommand(DataInputStream params, IOTACommandHelper helper) throws IOException { processGetConfigPageCmd(helper, true); }
		});
		repository.addCommand(GET_FILE_INFO_CMD, new ExtensionWrapper(SECURITY_LEVEL_GETFILEINFO) {
			public void processCommand(DataInputStream params, IOTACommandHelper helper) throws IOException { processGetFileInfoCmd(params, helper); }
		});
		repository.addCommand(GET_FILE_LIST_CMD, new ExtensionWrapper(SECURITY_LEVEL_GETFILELIST) {
			public void processCommand(DataInputStream params, IOTACommandHelper helper) throws IOException { processGetFileListCmd(params, helper); }
		});
		repository.addCommand(FLASH_APP_CMD, new ExtensionWrapper(SECURITY_LEVEL_FLASHAPP) {
			public void processCommand(DataInputStream params, IOTACommandHelper helper) throws IOException { processFlashAppCmd(params, helper); }
		});
		repository.addCommand(FLASH_LIB_CMD, new ExtensionWrapper(SECURITY_LEVEL_FLASHLIB) {
			public void processCommand(DataInputStream params, IOTACommandHelper helper) throws IOException { processFlashLibCmd(params, helper); }
		});
		repository.addCommand(UNDEPLOY_CMD, new ExtensionWrapper(SECURITY_LEVEL_UNDEPLOY) {
			public void processCommand(DataInputStream params, IOTACommandHelper helper) throws IOException { processUndeployCmd(params, helper); }
		});
		repository.addCommand(BOOTLOADER_CMD_ATTENTION, new ExtensionWrapper(SECURITY_LEVEL_ATTENTION) {
			public void processCommand(DataInputStream params, IOTACommandHelper helper) throws IOException { processAttentionCmd(helper); }
		});
		repository.addCommand(DELETE_PUBLIC_KEY_CMD, new ExtensionWrapper(SECURITY_LEVEL_DELETEPUBLICKEY) {
			public void processCommand(DataInputStream params, IOTACommandHelper helper) throws IOException { processDeletePublicKeyCmd(helper); }
		});
		repository.addCommand(SET_PUBLIC_KEY_CMD, new ExtensionWrapper(SECURITY_LEVEL_SETPUBLICKEY) {
			public void processCommand(DataInputStream params, IOTACommandHelper helper) throws IOException { processSetPublicKeyCmd(params, helper); }
		});
		repository.addCommand(SET_STARTUP_CMD, new ExtensionWrapper(SECURITY_LEVEL_SET_STARTUP_CMD) {
			public void processCommand(DataInputStream params, IOTACommandHelper helper) throws IOException { processSetStartupCmd(params, helper); }
		});
		repository.addCommand(START_VM_CMD, new ExtensionWrapper(SECURITY_LEVEL_STARTVM) {
			public void processCommand(DataInputStream params, IOTACommandHelper helper) throws IOException { processStartVMCmd(); }
		});
		repository.addCommand(RESYNC_CMD, new ExtensionWrapper(SECURITY_LEVEL_ATTENTION) {
			public void processCommand(DataInputStream params, IOTACommandHelper helper) throws IOException { processResyncCmd(helper); }
		});

		repository.addCommand(CLOSEDOWN, new ExtensionWrapper(SECURITY_LEVEL_CLOSEDOWN) {
			public void processCommand(DataInputStream params, IOTACommandHelper helper) throws IOException { /* no-op */ }
			protected boolean shouldClosedown() { return true; }
		});

		IOTACommand swcmd = new SpotWorldCommand();

		repository.addCommand(SpotWorldCommand.GET_MEMORY_STATS_CMD, swcmd);
		repository.addCommand(SpotWorldCommand.GET_POWER_STATS_CMD, swcmd);
		repository.addCommand(SpotWorldCommand.GET_SLEEP_INFO_CMD, swcmd);
		repository.addCommand(SpotWorldCommand.GET_AVAILABLE_SUITES_CMD, swcmd);
		repository.addCommand(SpotWorldCommand.GET_SUITE_MANIFEST_CMD, swcmd);
		repository.addCommand(SpotWorldCommand.START_APP_CMD, swcmd);
		repository.addCommand(SpotWorldCommand.PAUSE_APP_CMD, swcmd);
		repository.addCommand(SpotWorldCommand.RESUME_APP_CMD, swcmd);
		repository.addCommand(SpotWorldCommand.STOP_APP_CMD, swcmd);
		repository.addCommand(SpotWorldCommand.GET_APP_STATUS_CMD, swcmd);
		repository.addCommand(SpotWorldCommand.GET_ALL_APPS_STATUS_CMD, swcmd);
		repository.addCommand(SpotWorldCommand.START_REMOTE_PRINTING_CMD, swcmd);
		repository.addCommand(SpotWorldCommand.STOP_REMOTE_PRINTING_CMD, swcmd);
		repository.addCommand(SpotWorldCommand.MIGRATE_APP_CMD, swcmd);
		repository.addCommand(SpotWorldCommand.RECEIVE_APP_CMD, swcmd);
		repository.addCommand(SpotWorldCommand.GET_SPOT_PROPERTY_CMD, swcmd);
		repository.addCommand(SpotWorldCommand.REMOTE_GET_PHYS_NBRS_CMD, swcmd);
		repository.addCommand(SpotWorldCommand.GET_RADIO_INFO_CMD, swcmd);
		repository.addCommand(SpotWorldCommand.GET_ROUTE_CMD, swcmd);
	}

	private abstract class ExtensionWrapper implements IOTACommand {
		private int level;
		
		public ExtensionWrapper(int level) {
			this.level = level;
		}

		public int getSecurityLevelFor(String command) {
			return level;
		}
		
		public boolean processCommand(String command, DataInputStream params, IOTACommandHelper helper) throws IOException {
			processCommand(params, helper);
			return !shouldClosedown();
		}


		protected boolean shouldClosedown() {
			return false;
		}
		
		protected abstract void processCommand(DataInputStream params, IOTACommandHelper helper) throws IOException;
	}
}
