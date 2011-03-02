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

package com.sun.spot.peripheral;

import java.io.IOException;


public class Battery implements IBattery {
	
 
	private static final byte QUERY_PROGMEM_NEXT = 22;
	private static final byte LOCK_BATTERY_PROCESS = 27;
	private static final byte GET_BATTERY_INFO = 28;
	private static final byte SET_BATTERY_INFO = 29;
    private static final byte QUERY_PERSISTANT_WRITE = 30;
	private static final byte QUERY_RAW_TEMP = 4;

	private static final byte SIZEOF_BYTE = 1;
	private static final byte SIZEOF_SHORT = 2;
	private static final byte SIZEOF_INT = 4;
	private static final byte SIZEOF_LONG = 8;

	private static final byte AVAILABLE_CAPACITY_OFFSET = 0; // int
	private static final byte CAPACITY_OFFSET = 4; // int
	private static final byte SLEEPTIME_OFFSET = 8; // int
	private static final byte RUNTIME_OFFSET = 12; // int
	private static final byte CHARGETIME_OFFSET = 16; // int
	private static final byte CHARGECOUNT_OFFSET = 20; // short
	private static final byte STATE_OFFSET = 22; // byte
	private static final byte FLAGS_OFFSET = 23; // byte
	private static final byte SLEEP_DISCHARGE_OFFSET = 24; // short
	private static final byte RATED_CAPACITY_OFFSET = 26; // int
	private static final byte LOWBATTERY_CAPACITY_OFFSET = 30; // int
	private static final byte MODEL_OFFSET = 38; // char[13] with terminating null char

	private static final String BATTERY_MODEL_PROPERTY = "spot.battery.model";
	private static final String BATT720 = "LP523436B";
	private static final String BATT770 = "LP523436D";
	
	private static final byte TEMP_SENSOR_DETECTED = 1;
	private static final byte BATTERY_DETECTED = 2;
	private static final byte CYCLE_DETECTED = 4;
	
	private static final double MAHR_CONVERSION = 144000.0;
	private ISpiMaster spiMaster;
	private SpiPcs chipSelectPin;
	private byte[] snd = new byte[3];
	private byte[] rcv = new byte[3];
	

	/**
	 * Battery constructor for obtaining information about the SPOT rechargeable battery
	 * this can throw a RuntimeException if it is created with a pre-rev6 eSPOT or version earlier than PCTRL-1.99
	 * @param chipSelectPin chip select pin for power controller (PeripheralChipSelect.SPI_PCS_POWER_CONTROLLER)
	 * @param spiMaster SPI object (usually from Spot.getInstance().getSPI())
	 * @throws RuntimeException if pre-rev6 eSPOT, pctrl-1.98 or earlier firmware or invalid spot.battery.model property
	 */
	
    public Battery(SpiPcs chipSelectPin, ISpiMaster spiMaster) throws RuntimeException {
    	this.chipSelectPin = chipSelectPin;
   		this.spiMaster = spiMaster;
   		String revision = Spot.getInstance().getPowerController().getRevision();
      	int separator = revision.indexOf('.'); // find the decimal point
      	int i;
      	for (i = separator+1; i < revision.length() && revision.charAt(i) >= '0' && revision.charAt(i) <= '9'; i++);
		int minor = Integer.parseInt(revision.substring(separator + 1,i));
       	int major = Integer.parseInt(revision.substring(separator - 1, separator));
 	   	if ((major != 1) || (minor < 99)) throw new RuntimeException("Battery Class must use PCTRL-1.99 or greater");
 	   	if (Spot.getInstance().getHardwareType() < 5) throw new RuntimeException("Battery Class must use eSPOT hardware rev 5 or greater");
		String prop = System.getProperty(BATTERY_MODEL_PROPERTY);
		boolean changed = true;
		if (prop == null || prop.startsWith("720")) {
			Spot.getInstance().setPersistentProperty(BATTERY_MODEL_PROPERTY, BATT720);
			prop = BATT720;
		} else if (prop.startsWith("770")) {
			Spot.getInstance().setPersistentProperty(BATTERY_MODEL_PROPERTY, BATT770);
			prop = BATT770;
		} else changed = false;
		if (changed || !prop.equals(getModelNumber())) {
			if (prop.equals(BATT770)) {
				setBatteryInfo(BATT770, (int) 770, (int) 21);
			} else if (prop.equals(BATT720)) {
				setBatteryInfo(BATT720, (int) 720, (int) 16);
			} else throw new RuntimeException(BATTERY_MODEL_PROPERTY + " property \'" + prop + "\' is unknown.");
		}
    }
    
    // for fetches greater than one byte lock before a get/set
    private void lock() {
		snd[0] = LOCK_BATTERY_PROCESS;
		snd[1] = (byte) 1; // 1=locks, 0=unlock
		spiMaster.sendAndReceive(chipSelectPin, 2, snd, 0, 0, null);
	}

   // if lock is called before get/set, follow with unlock after get/set
    private void unlock() {
		snd[0] = LOCK_BATTERY_PROCESS;
		snd[1] = (byte) 0; // 1=locks, 0=unlock. 2=persistant write
		spiMaster.sendAndReceive(chipSelectPin, 2, snd, 0, 0, null);
	}
   // unlockPersistant used to write updated values to EEPROM
    private void unlockPersistant() {
		snd[0] = LOCK_BATTERY_PROCESS;
		snd[1] = (byte) 2; // 1=locks, 0=unlock. 2=persistant write
		spiMaster.sendAndReceive(chipSelectPin, 2, snd, 0, 0, null);
	}
    // get raw battery data which is bytes offset, and size in bytes
    // always returns a long and should be coerce to size
	private long get(byte offset, byte size) {
		snd[0] = GET_BATTERY_INFO; //GET_BATTERY_INFO, address->, Data<-
		snd[1] = (byte) (offset + size);
		int t = 0;
		for (byte i = 0; i < size; i++) {
			snd[1]--; //sendAndReceive(SpiPcs pcs, int txSize, byte[] tx, int rxOffset, int rxSize, byte[] rx) 
			spiMaster.sendAndReceive(chipSelectPin, 2, snd, 2, 1, rcv);
			t = (t << 8) | (rcv[0] & 0xFF);
		}
		return t;
	}

   // set raw battery data which is bytes offset, and size in bytes, and long value
	private void set(byte offset, byte size, long value) {
		snd[0] = SET_BATTERY_INFO; //SET_BATTERY_INFO, address->, Data->
		snd[1] = (byte) offset;
		for (byte i = 0; i < size; i++) {
			snd[2] = (byte) (value & 0xFF);
			spiMaster.sendAndReceive(chipSelectPin, 3, snd, 0, 0, null);
			value = value >> 8;
			snd[1]++;
		}
	}
	// set persistant battery info
	public void setBatteryInfo(String modelNumber, int rated, int lowbattery) {
		lock();
		set(RATED_CAPACITY_OFFSET, SIZEOF_INT, rated*144000);
		set(LOWBATTERY_CAPACITY_OFFSET, SIZEOF_INT, lowbattery*144000);
		for(int i = 0; i < modelNumber.length(); i++) {
			set((byte) (MODEL_OFFSET + i), SIZEOF_BYTE, modelNumber.charAt(i));
		}
		unlockPersistant();
	}

	// answer the battery model number
	public String getModelNumber() {
		StringBuffer s = new StringBuffer(13);
		char ch;
		byte addr = MODEL_OFFSET;
		byte endaddr = MODEL_OFFSET + 13;
		while ((addr < endaddr) && ((ch = (char) get(addr++, SIZEOF_BYTE)) != '\0')) s.append(ch);
		return s.toString();
	}
			
	public String rawBatteryData() {
		lock();	
		String model_number = getModelNumber();
		int available_capacity = (int) get(AVAILABLE_CAPACITY_OFFSET,SIZEOF_INT);
		int capacity = (int) get(CAPACITY_OFFSET,SIZEOF_INT);
		int sleeptime = (int) get(SLEEPTIME_OFFSET,SIZEOF_INT);
		int runtime = (int) get(RUNTIME_OFFSET,SIZEOF_INT);
		int chargetime = (int) get(CHARGETIME_OFFSET,SIZEOF_INT);
		short chargecount = (short) get(CHARGECOUNT_OFFSET,SIZEOF_SHORT);
		byte state = (byte) get(STATE_OFFSET,SIZEOF_BYTE);
		byte flags = (byte) get(FLAGS_OFFSET,SIZEOF_BYTE);
		short sleep_current = (short) get(SLEEP_DISCHARGE_OFFSET,SIZEOF_SHORT);
		int rated_capacity = (int) get(RATED_CAPACITY_OFFSET,SIZEOF_INT);
		int lowbattery_capacity = (int) get(LOWBATTERY_CAPACITY_OFFSET,SIZEOF_INT);
		unlock();
		StringBuffer s = new StringBuffer(50);
		s.append("Battery PN: "+model_number+"\n");
		s.append("Available Capacity: "+available_capacity+"\n");
		s.append("Maximum Capacity: "+capacity+"\n");
		s.append("Sleep Time: "+sleeptime+"\n");
		s.append("Run Time: "+runtime+"\n");
		s.append("Charge Time: "+chargetime+"\n");
		s.append("Charge Count: "+chargecount+"\n");
		s.append("Battery State: "+state+"\n");
		s.append("Battery Flags: "+flags+"\n");
		s.append("Sleep Current: "+sleep_current+"\n");
		s.append("Rated Capacity: "+rated_capacity+"\n");
		s.append("Low Battery Capacity: "+lowbattery_capacity);
		return s.toString();
	}
	
	// return the available battery capacity in maHr
	public double getAvailableCapacity() {
		lock();
		int available_capacity = (int) get(AVAILABLE_CAPACITY_OFFSET,SIZEOF_INT);
		unlock();
		return available_capacity/MAHR_CONVERSION;
	}
	
	// get the calculated maximum capacity of the battery in maHr
	public double getMaximumCapacity() {
		lock();
		int capacity = (int) get(CAPACITY_OFFSET,SIZEOF_INT);
		unlock();
		return capacity/MAHR_CONVERSION;
	}
	
	public double getRatedCapacity() {
		lock();
		int rated_capacity = (int) get(RATED_CAPACITY_OFFSET,SIZEOF_INT);
		unlock();
		return rated_capacity/MAHR_CONVERSION;
	}
	// return the percentage charge of the battery level
	public int getBatteryLevel() {
		lock();
		int available_capacity = (int) get(AVAILABLE_CAPACITY_OFFSET,SIZEOF_INT);
		int capacity = (int) get(CAPACITY_OFFSET,SIZEOF_INT);
		unlock();
		return (int) ((available_capacity*100.0)/capacity);
	}
	// return the seconds the spot has been sleeping since last charge
	public long[] getTime() {
		long[] time = new long[3];
		lock();
		time[IBattery.SLEEPTIME] = get(SLEEPTIME_OFFSET,SIZEOF_INT) * 256;
		time[IBattery.RUNTIME] = get(RUNTIME_OFFSET,SIZEOF_INT) * 50;
		time[IBattery.CHARGETIME] = get(CHARGETIME_OFFSET,SIZEOF_INT) * 50;
		unlock();
		return time;
	}
	// return the times the spot has been fully charged
	public short getChargeCount() {
		lock();
		short chargecount = (short) get(CHARGECOUNT_OFFSET,SIZEOF_SHORT);
		unlock();
		return chargecount;
	}
	
	public int getSleepCurrent() {
		lock();
		short sleep_current = (short) get(SLEEP_DISCHARGE_OFFSET,SIZEOF_SHORT);
		unlock();
		return (int) (sleep_current * 25000)/65536;
	}

	public void setSleepCurrent(int microamps) {
		long adc = (microamps * 65536)/25000;
		lock();
		set(SLEEP_DISCHARGE_OFFSET, SIZEOF_SHORT, adc);
		unlock();
	}
	
				
	public byte getState() {
		return (byte) get(STATE_OFFSET,SIZEOF_BYTE);
	}

	public boolean hasTemperatureSensor() {
		return (get(FLAGS_OFFSET,SIZEOF_BYTE) & TEMP_SENSOR_DETECTED) != 0;
	}		

	public boolean hasBattery() {
		return (get(FLAGS_OFFSET,SIZEOF_BYTE) & BATTERY_DETECTED) != 0;
	}		

	public boolean calibrationCycleDetected() {
		return (get(FLAGS_OFFSET,SIZEOF_BYTE) & CYCLE_DETECTED) != 0;
	}		
	
	public String getStateAsString(int state) {
		switch (state) {
			case IBattery.NO_BATTERY:    return "no battery";    
			case IBattery.DEAD_BATTERY:  return "dead battery";  
			case IBattery.LOW_BATTERY:   return "low battery";   
			case IBattery.DISCHARGING:   return "discharging";   
			case IBattery.CHARGING:      return "charging";      
			case IBattery.EXT_POWERED: return "externally powered"; 
			case IBattery.OUT_OF_RANGE_TEMP: return "battery temperature out of range"; 
			default: return "";
		}
	}
	
	public void updatePersistantInfo() throws IOException {
        byte[] snd = new byte[2];
        byte[] rcv = new byte[2];
 
        long timeout = System.currentTimeMillis() + ((long) 10000);
        snd[0] = LOCK_BATTERY_PROCESS; // force command to write battery info
		snd[1] = (byte) 4;
		spiMaster.sendAndReceive(chipSelectPin, 2, snd, 0, 0, null);
        snd[0] = QUERY_PERSISTANT_WRITE; 
        snd[1] = (byte) 0;
        rcv[1] = (byte) 0;
        do {
			spiMaster.sendAndReceive(chipSelectPin, 2, snd, 0, 0, null);
         	if (System.currentTimeMillis() > timeout) throw new IOException("Power Controller not responding when sent persistant write");
		} while (rcv[1] != 0);
	}


} // end class
