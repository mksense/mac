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

package com.sun.spot.peripheral;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

import com.sun.spot.dmamemory.DMAMemoryManager;
import com.sun.spot.dmamemory.IDMAMemoryManager;
import com.sun.spot.dmamemory.proxy.ProxyDMAMemoryManager;
import com.sun.spot.flashmanagement.FlashFileInputStream;
import com.sun.spot.flashmanagement.FlashFileOutputStream;
import com.sun.spot.flashmanagement.NorFlashSectorAllocator;
import com.sun.spot.interisolate.InterIsolateServer;
import com.sun.spot.io.j2me.memory.MemoryInputStream;
import com.sun.spot.io.j2me.remoteprinting.IRemotePrintManager;
import com.sun.spot.io.j2me.remoteprinting.RemotePrintManager;
import com.sun.spot.peripheral.driver.proxy.ProxyDriverRegistry;
import com.sun.spot.peripheral.external.ExternalBoard;
import com.sun.spot.peripheral.ota.IOTACommandServer;
import com.sun.spot.peripheral.ota.OTACommandServer;
import com.sun.spot.peripheral.proxy.ProxyAT91_PIO;
import com.sun.spot.peripheral.radio.I802_15_4_MAC;
import com.sun.spot.peripheral.radio.I802_15_4_PHY;
import com.sun.spot.peripheral.radio.IProprietaryRadio;
import com.sun.spot.peripheral.radio.IRadioPolicyManager;
import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.peripheral.radio.RadioPolicyManager;
import com.sun.spot.peripheral.radio.policy.proxy.ProxyRadioPolicyManager;
import com.sun.spot.resourcesharing.IResourceRegistry;
import com.sun.spot.resourcesharing.ProxyResourceRegistryMaster;
import com.sun.spot.resourcesharing.ResourceRegistryChild;
import com.sun.spot.resourcesharing.ResourceRegistryMaster;
import com.sun.spot.service.ServiceRegistry;
import com.sun.spot.service.SpotBlink;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Properties;
import com.sun.spot.util.Utils;
import com.sun.squawk.Address;
import com.sun.squawk.Isolate;
import com.sun.squawk.Unsafe;
import com.sun.squawk.VM;
import com.sun.squawk.peripheral.INorFlashSector;
import com.sun.squawk.util.Arrays;
import com.sun.squawk.vm.ChannelConstants;

/**
 * The class of the root object of the Spot base library.<br><br>
 * 
 * There is only one instance of Spot. To access it use:<br><br>
 * 
 * <code>
 * Spot.getInstance();
 * </code>
 * 
 * For details of public methods see {@link com.sun.spot.peripheral.ISpot}.
 * 
 */
public class Spot implements ISpot {

    private static final String SPOT_STARTUP_PREFIX = "spot-startup-";

    private static ISpot theSpot;
    private static boolean startupInitializationDone = false;
    private static boolean isMasterIsolate = false;

    private ILed greenLed;
    private ILed redLed;
    private IAT91_TC tc[] = new IAT91_TC[6];
    private Hashtable externalBoardMap;
    private ISpotPins spotPins;
    private IAT91_PIO pio[] = new IAT91_PIO[4];
    private IAT91_AIC aic;
    private IDriverRegistry driverRegistry;
    private ISpiMaster spi;
    private II2C i2c;
    private IAT91_PowerManager powerManager;
    private IFlashMemoryDevice flashMemory;
    private ILTC3455 ltc3455;
    private ISleepManager sleepManager;
    private ISecuredSiliconArea securedSiliconArea;
    private FiqInterruptDaemon fiqInterruptDaemon;
    private int hardwareType;
    private IUSBPowerDaemon usbPowerDaemon;
    private IPowerController powerController;
    private IRadioPolicyManager radioPolicyManager;
    private ResourceRegistryMaster masterResourceRegistry;
    private IResourceRegistry resourceRegistry;
    private IDMAMemoryManager dmaMemoryManager;

    /**
	 * Package visibility to support testing - ideally this would be private
	 */
	Spot() {
    }

    /**
     * Main entry point. This is called by Squawk prior to running any user code because
     * this class is specified using -isolateinit.
     * 
     * @param args arg[0] indicates whether the main or a child isolate is being started
     */
    public static void main(String[] args) {
    	// If this is the master isolate, then the PIOs will not be available until masterIsolateStartup()
    	// has executed, so before that point, be careful not to access the PIOs.
        isMasterIsolate = "true".equals(args[0]);

        startupInitializationDone = true;
        final Spot spot = (Spot) getInstance();
        spot.peekPlatform();
        spot.loadSystemProperties();

        // Don't waste time trying to log before loadSystemProperties has completed!
        // log("Spot initialization called (" + isMasterIsolate + ")");

        spot.determineIEEEAddress();

        if (isMasterIsolate) {
            spot.masterIsolateStartup();
        }
        Isolate isolate = Isolate.currentIsolate();
        isolate.removeOut("debug:");
        isolate.removeErr("debug:err");
        isolate.addOut("serial:");
        isolate.addErr("serial:");
    }

    private void masterIsolateStartup() {

    	for (int pioSelector=0; pioSelector<pio.length; pioSelector++) {
	    	pio[pioSelector] = new AT91_PIO(pioSelector,
	                getAT91_AIC(),
	                getAT91_PowerManager(),
	                getSpotPins().getPinsNotAvailableToPIO(pioSelector));
	        getDriverRegistry().add((IDriver) (pio[pioSelector]));
    	}
    	
        Utils.log("Allocating " + ConfigPage.DEFAULT_SECTOR_COUNT_FOR_RMS + " sectors for RMS");
        VM.getPeripheralRegistry().add(new NorFlashSectorAllocator());

        USBPowerDaemon usbPowerDaemon = new USBPowerDaemon(getLTC3455(), getSpotPins().getUSB_PWR_MON());
        getDriverRegistry().add(usbPowerDaemon);
        setUsbPowerDaemon(usbPowerDaemon);

        DriverRegistry masterIsolateVersionOfDriverRegistry = ((DriverRegistry) getDriverRegistry());
        setSleepManager(new SleepManager(masterIsolateVersionOfDriverRegistry, getUsbPowerDaemon(), getPowerController(), hardwareType));

        usbPowerDaemon.startThreads();

        ((FiqInterruptDaemon) getFiqInterruptDaemon()).startThreads();

        masterResourceRegistry = new ResourceRegistryMaster();

        InterIsolateServer.run(ProxyResourceRegistryMaster.RESOURCE_REGISTRY_SERVER, masterResourceRegistry);
        InterIsolateServer.run(ProxyDriverRegistry.DRIVER_REGISTRY_SERVER, masterIsolateVersionOfDriverRegistry);
        InterIsolateServer.run(ProxyRadioPolicyManager.RADIO_POLICY_SERVER, null);
        InterIsolateServer.run(ProxyDMAMemoryManager.DMA_MEMORY_SERVER, getDMAMemoryManager());
        InterIsolateServer.run(ProxyAT91_PIO.AT91_PIO_SERVER, this);

        ServiceRegistry.getInstance().add(new SpotBlink());

        if (Utils.isOptionSelected("spot.start.manifest.daemons", true)) {
            runThirdPartyStartups();

            // want to start OTA only after any network/radio stack has been initialized
            IOTACommandServer ota = OTACommandServer.getInstance();
            if (ota.getEnabled()) {
                ota.start();
            }
        } else {
            Utils.log("Not starting manifest daemons");
        }

        Utils.log(getPowerController().getRevision());
        getExternalBoardMap(); // to force display of version number(s)

        Thread.yield();

        updateSystemVersionProperties();
    }

    private void runThirdPartyStartups() {
        Enumeration keys = VM.getManifestPropertyNames();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            if (key.startsWith(SPOT_STARTUP_PREFIX)) {
                String className = VM.getManifestProperty(key);
                try {
                    VM.invokeMain(className, new String[]{key.substring(SPOT_STARTUP_PREFIX.length())});
                    Utils.log("Called startup class " + className);
                } catch (ClassNotFoundException e) {
                    System.err.println("Warning: startup class " + className + " is missing.");
                }
            }
        }
    }

    private void loadSystemProperties() {
        Properties persistentProperties = getPersistentProperties();
        Enumeration keys = persistentProperties.keys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            VM.getCurrentIsolate().setProperty(key, (String) persistentProperties.get(key));
        }
        VM.getCurrentIsolate().setProperty("line.separator", "\n");
    }

    /**
     * Get the singleton instance of this class.
     * @return The singleton instance
     */
    public static synchronized ISpot getInstance() {
        if (theSpot == null) {
            if (!startupInitializationDone) {
                throw new SpotFatalException("Spot initialization code has not been run");
            }
            theSpot = new Spot();
        }
        return theSpot;
    }

    public synchronized IFiqInterruptDaemon getFiqInterruptDaemon() {
        if (fiqInterruptDaemon == null) {
            fiqInterruptDaemon = new FiqInterruptDaemon(getPowerController(), getAT91_AIC(), getSpotPins());
        }
        return fiqInterruptDaemon;
    }

    public synchronized ILed getGreenLed() {
        assertIsMaster();
        if (greenLed == null) {
            greenLed = new Led(getSpotPins().getLocalGreenLEDPin(), false);
            getDriverRegistry().add((IDriver) greenLed);
        }
        return greenLed;
    }

    public synchronized ILed getRedLed() {
        assertIsMaster();
        if (redLed == null) {
            redLed = new Led(getSpotPins().getLocalRedLEDPin(), false);
            getDriverRegistry().add((IDriver) redLed);
        }
        return redLed;
    }

    public synchronized ISpotPins getSpotPins() {
        if (spotPins == null) {
            spotPins = new SpotPins(getHardwareType(), this);
        }
        return spotPins;
    }

    public IAT91_PIO getAT91_PIO(int pioSelector) {
    	if (!isMasterIsolate) { // PIOs for master isolate are initialised in masterIsolateStartup
	        synchronized (pio) {
	            if (pio[pioSelector] == null) {
                    pio[pioSelector] = new ProxyAT91_PIO(pioSelector);
	            }
	        }
        }
    	return pio[pioSelector];
    }

    /**
     * Return the AT91_PowerManager.
     * @return the AT91_PowerManager
     */
    public synchronized IAT91_PowerManager getAT91_PowerManager() {
        if (powerManager == null) {
            powerManager = new AT91_PowerManager();
            if (isMasterIsolate()) {
                getDriverRegistry().add(powerManager);
            }
        }
        return powerManager;
    }

    public synchronized IRadioPolicyManager getRadioPolicyManager() {
        if (radioPolicyManager == null) {
            if (isMasterIsolate()) {
                radioPolicyManager = new RadioPolicyManager(
                        RadioFactory.getI802_15_4_MAC(),
                        Utils.getSystemProperty("radio.channel",              // use system property if set
                            Utils.getManifestProperty("DefaultChannelNumber", // else manifest property
                                IProprietaryRadio.DEFAULT_CHANNEL)),          // else default
                        (short) Utils.getSystemProperty("radio.pan.id",
                            Utils.getManifestProperty("DefaultPanId",
                                IRadioPolicyManager.DEFAULT_PAN_ID)),
                        Utils.getSystemProperty("radio.transmit.power",
                            Utils.getManifestProperty("DefaultTransmitPower",
                                IProprietaryRadio.DEFAULT_TRANSMIT_POWER)));
                getDriverRegistry().add((IDriver) radioPolicyManager);
            } else {
                radioPolicyManager = new ProxyRadioPolicyManager();
            }
        }
        return radioPolicyManager;
    }

    public synchronized IDMAMemoryManager getDMAMemoryManager() {
        if (dmaMemoryManager == null) {
            if (isMasterIsolate()) {
                dmaMemoryManager = new DMAMemoryManager();
            } else {
                dmaMemoryManager = new ProxyDMAMemoryManager();
            }
        }
        return dmaMemoryManager;
    }

    /**
     * Return the LTC3455 power regulator used by the Spot.
     * @return the LTC3455
     */
    public synchronized ILTC3455 getLTC3455() {
        assertIsMaster();
        if (ltc3455 == null) {
        	if (getHardwareType() > 6) {
        		ltc3455 = new LTC3455ControlledViaPowerController(getPowerController());
        	} else {
        		LTC3455ControlledViaPIO ltc3455ControlledViaPIO = new LTC3455ControlledViaPIO(getSpotPins());
        		getDriverRegistry().add(ltc3455ControlledViaPIO);
				ltc3455 = ltc3455ControlledViaPIO;
        	}
        }
        return ltc3455;
    }

    /* (non-Javadoc)
     * @see com.sun.squawk.peripheral.spot.ISpot#getAT91_AIC()
     */
    public synchronized IAT91_AIC getAT91_AIC() {
        if (aic == null) {
            aic = new AT91_AIC();
            if (isMasterIsolate()) {
                getDriverRegistry().add(aic);
            }
        }
        return aic;
    }

    /* (non-Javadoc)
     * @see com.sun.squawk.peripheral.spot.ISpot#getI802_15_4_PHY()
     */
    public I802_15_4_PHY getI802_15_4_PHY() {
        assertIsMaster();
        return RadioFactory.getI802_15_4_PHY();
    }

    /* (non-Javadoc)
     * @see com.sun.squawk.peripheral.spot.ISpot#getIProprietaryRadio()
     */
    public IProprietaryRadio getIProprietaryRadio() {
        assertIsMaster();
        return RadioFactory.getIProprietaryRadio();
    }

    /* (non-Javadoc)
     * @see com.sun.squawk.peripheral.spot.ISpot#getI802_15_4_MAC()
     */
    public I802_15_4_MAC[] getI802_15_4_MACs() {
        assertIsMaster();
        return new I802_15_4_MAC[]{ RadioFactory.getI802_15_4_MAC() };
    }

    /* (non-Javadoc)
     * @see com.sun.squawk.peripheral.spot.ISpot#getSPI()
     */
    public synchronized ISpiMaster getSPI() {
        if (spi == null) {
            spi = new SpiMaster();
        }
        return spi;
    }

    /* (non-Javadoc)
     * @see com.sun.squawk.peripheral.spot.ISpot#getI2C()
	 */
    public synchronized II2C getI2C() {
        if (i2c == null) {
            i2c = new AT91_I2C();
        }
        return i2c;
    }


    public synchronized IDriverRegistry getDriverRegistry() {
        if (driverRegistry == null) {
            if (isMasterIsolate) {
                driverRegistry = new DriverRegistry();
            } else {
                driverRegistry = new ProxyDriverRegistry();
            }
        }
        return driverRegistry;
    }

    /* (non-Javadoc)
     * @see com.sun.squawk.peripheral.spot.ISpot#getFlashMemoryDevice()
     */
    public synchronized IFlashMemoryDevice getFlashMemoryDevice() {
        if (flashMemory == null) {
        	if (getHardwareType() <= 6) {
        		flashMemory = new S29PL_Flash(S29PL_Flash.S29PL032J);
        	} else {
        		flashMemory = new S29PL_Flash(S29PL_Flash.S29PL064J);
        	}
        }
        return flashMemory;
    }

    public int getHardwareType() {
        return hardwareType;
    }

    /**
     * Get access to an AT91 Timer-Counter.
     * @param index The index of the required TC in the range 0-5
     * @return The AT91 TC
     */
    public synchronized IAT91_TC getAT91_TC(int index) {
        if (tc[index] == null) {
            tc[index] = new AT91_TC(index, getAT91_AIC(), getAT91_PowerManager(), getSpotPins());
            getDriverRegistry().add(tc[index]);
        }
        return tc[index];
    }

    public IUSBPowerDaemon getUsbPowerDaemon() {
        assertIsMaster();
        return usbPowerDaemon;
    }

    public synchronized ISecuredSiliconArea getSecuredSiliconArea() {
        if (securedSiliconArea == null) {
            securedSiliconArea = new SecuredSiliconArea();
        }
        return securedSiliconArea;
    }

    /* (non-Javadoc)
     * @see com.sun.squawk.peripheral.spot.ISpot#getConfigPage()
     */
    public ConfigPage getConfigPage() {
        int configPageAddr = ConfigPage.CONFIG_PAGE_ADDRESS;
        byte[] data;
        try {
            StreamConnection mem = (StreamConnection) Connector.open("memory://" + configPageAddr);
            MemoryInputStream mis = (MemoryInputStream) mem.openInputStream();
            data = new byte[1024];
            mis.read(data);
            Utils.writeLittleEndLong(data, ConfigPage.SERIAL_NUMBER_OFFSET, getSecuredSiliconArea().readSerialNumber());
            return new ConfigPage(data);
        } catch (IOException e) {
            throw new SpotFatalException(e.getMessage());
        }
    }

    public byte[] getPublicKey() {
        byte[] result = new byte[256];
        int numberOfBytesRead = VM.execSyncIO(ChannelConstants.GET_PUBLIC_KEY, result.length, 0, 0, 0, 0, 0, result, null);
        result = Arrays.copy(result, 0, numberOfBytesRead, 0, numberOfBytesRead);
        return result;
    }

    public void flashConfigPage(ConfigPage configPage) {
        assertIsMaster();
        byte[] data = configPage.asByteArray();
        int configPageAddr = ConfigPage.CONFIG_PAGE_ADDRESS;
        IFlashMemoryDevice flash = getFlashMemoryDevice();
        INorFlashSector sector = new NorFlashSector(flash, flash.getSectorContainingAddress(configPageAddr), INorFlashSector.SYSTEM_PURPOSED);
        sector.erase();
        sector.setBytes(0, data, 0, 1024);
    }

    /* (non-Javadoc)
     * @see com.sun.squawk.peripheral.spot.ISpot#getExternalBoardMap()
     */
    public synchronized Hashtable getExternalBoardMap() {
        if (externalBoardMap == null) {
            externalBoardMap = new Hashtable();
            // Try to read the properties using each board select in turn. If this fails assume the board is missing or uninitialized.
            registerExternalBoard(PeripheralChipSelect.SPI_PCS_BD_SEL1);
            registerExternalBoard(PeripheralChipSelect.SPI_PCS_BD_SEL2);
        }
        return externalBoardMap;
    }

    private void registerExternalBoard(PeripheralChipSelect peripheralChipSelect) {
        ExternalBoard externalBoard = new ExternalBoard(peripheralChipSelect);
        if (externalBoard.isInstalled()) {
            try {
                Properties properties = externalBoard.getProperties();
                externalBoardMap.put(peripheralChipSelect, properties);
                String boardName = (properties.containsKey(ExternalBoard.ID_PROPERTY_NAME))
                        ? properties.get(ExternalBoard.ID_PROPERTY_NAME).toString() : "Unknown board";
                Utils.log(boardName + " on " + peripheralChipSelect);
                setPersistentProperty("spot.external." + peripheralChipSelect.getPcsIndex() + ".part.id", boardName);
            } catch (RuntimeException e) {
                System.err.println("[ExternalBoard] Runtime exception reading properties of board on " +
                        peripheralChipSelect + ": " + e);
            }
        } else {
            removeAllPersistentPropertiesStartingWith("spot.external." + peripheralChipSelect.getPcsIndex());
        }
    }

    public synchronized void resetExternalBoardMap() {
        externalBoardMap = null;
    }

    public ISleepManager getSleepManager() {
        assertIsMaster();
        return sleepManager;
    }

    public synchronized IPowerController getPowerController() {
        if (powerController == null) {
            powerController = new PowerController(getSPI(), PeripheralChipSelect.SPI_PCS_POWER_CONTROLLER);
        }
        return powerController;
    }

    public boolean isMasterIsolate() {
        return isMasterIsolate;
    }

    public String getPersistentProperty(String key) {
        Properties properties = getPersistentProperties();
        return properties.getProperty(key);
    }

    public synchronized Properties getPersistentProperties() {
        Properties properties = new Properties();
        try {
            BoundedInputStream bis = new BoundedInputStream(new FlashFileInputStream(new NorFlashSector(
                    getFlashMemoryDevice(),
                    ConfigPage.SYSTEM_PROPERTIES_SECTOR,
                    INorFlashSector.SYSTEM_PURPOSED)));
            int len = bis.available();
            if (0 < len && len < (0x2000 - 4)) {         // small sector size = 8K
                properties.load(bis);
            }
        } catch (Exception e) {
            System.err.println("Error reading persistent system properties: " + e);
        }
        return properties;
    }

    public synchronized void setPersistentProperty(String key, String value) {
        try {
            Properties currentProps = getPersistentProperties();
            if (value == null) {
                currentProps.remove(key);
            } else {
                if (value.equals(currentProps.getProperty(key))) {
                    return;
                }
                currentProps.setProperty(key, value);
            }
            VM.getCurrentIsolate().setProperty(key, value);
            storeProperties(currentProps);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void setPersistentProperties(Properties props) {
        try {
            Properties currentProps = getPersistentProperties();
            Enumeration keys = props.propertyNames();
            while (keys.hasMoreElements()) {
                String key = (String) keys.nextElement();
                currentProps.setProperty(key, props.getProperty(key));
                VM.getCurrentIsolate().setProperty(key, props.getProperty(key));
            }
            storeProperties(currentProps);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void storeProperties(Properties props) throws IOException {
        // remember that opening the flash output stream erases the sector(s), so do the reading first
        BoundedOutputStream bos = new BoundedOutputStream(
                new FlashFileOutputStream(new NorFlashSector(
                getFlashMemoryDevice(),
                ConfigPage.SYSTEM_PROPERTIES_SECTOR,
                INorFlashSector.SYSTEM_PURPOSED)));
        props.store(bos, null);
        bos.close();
    }

    private void peekPlatform() {
        int pioState = Unsafe.getInt(Address.fromPrimitive(AbstractAT91_PIO.BASE_ADDRESS[SpotPins.BD_REV0.pio]), AbstractAT91_PIO.PIO_PDSR);
        // assume all three pins on the same PIO
        hardwareType = ((pioState & SpotPins.BD_REV0.pin) == 0 ? 1 : 0) + 
        				((pioState & SpotPins.BD_REV1.pin) == 0 ? 2 : 0) +
        				((pioState & SpotPins.BD_REV2.pin) == 0 ? 4 : 0) +
        				4;
        Utils.log("Detected hardware type " + hardwareType);
    }

    private void setSleepManager(SleepManager manager) {
        sleepManager = manager;
    }

    private void setUsbPowerDaemon(IUSBPowerDaemon usbPowerDaemon) {
        this.usbPowerDaemon = usbPowerDaemon;
    }

    private void assertIsMaster() {
        if (!(isMasterIsolate)) {
            throw new SpotFatalException("Only the master isolate can access Spot resources");
        }
    }

    private void determineIEEEAddress() {
        if (System.getProperty("IEEE_ADDRESS") == null) {
            VM.getCurrentIsolate().setProperty("IEEE_ADDRESS", IEEEAddress.toDottedHex(getSecuredSiliconArea().readSerialNumber()));
        }
    }

    public boolean isRunningOnHost() {
        return false;
    }

    public IOTACommandServer getOTACommandServer() throws IOException {
        // assertIsMaster();  // OTA is now in ServiceRegistry
        return OTACommandServer.getInstance();
    }

    public void setProperty(String key, String value) {
        VM.getCurrentIsolate().setProperty(key, value);
    }

    public int getSystemTicks() {
        return AT91_TC.getSystemTicks();
    }

    public synchronized IResourceRegistry getResourceRegistry() {
        if (resourceRegistry == null) {
            if (isMasterIsolate()) {
                resourceRegistry = new ResourceRegistryChild(Isolate.currentIsolate().getId(), masterResourceRegistry);
            } else {
                resourceRegistry = new ResourceRegistryChild(Isolate.currentIsolate().getId(), new ProxyResourceRegistryMaster());
            }
        }
        return resourceRegistry;
    }

    public IRemotePrintManager getRemotePrintManager() {
        return RemotePrintManager.getInstance();
    }

    private void updateSystemVersionProperties() {
        String propVal = getPowerController().getRevision();
        String propKey = "spot.powercontroller.firmware.version";
        if (!propVal.equals(System.getProperty(propKey))) {
            setPersistentProperty(propKey, propVal);
        }
        propVal = "" + getHardwareType();
        propKey = "spot.hardware.rev";
        if (!propVal.equals(System.getProperty(propKey))) {
            setPersistentProperty(propKey, propVal);
        }
    }

    private void removeAllPersistentPropertiesStartingWith(String prefix) {
        Properties persistentProperties = getPersistentProperties();
        Enumeration e = persistentProperties.keys();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            if (key.startsWith(prefix)) {
                setPersistentProperty(key, null);
            }
        }
    }

    /**
     * FOR TEST PURPOSES ONLY
     */
    public static void setInstance(ISpot theSpot) {
        Spot.theSpot = theSpot;
    }
}
