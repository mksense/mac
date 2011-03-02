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

package com.sun.spot.peripheral.basestation;

import java.io.IOException;

import com.sun.spot.peripheral.ILed;
import com.sun.spot.peripheral.Spot;
import com.sun.spot.peripheral.SpotFatalException;
import com.sun.spot.peripheral.radio.I802_15_4_MAC;
import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.peripheral.radio.SpotSerialPipe;
import com.sun.spot.service.Heartbeat;
import com.sun.spot.util.Queue;
import com.sun.spot.util.Utils;
import com.sun.squawk.VM;

public class MACProxyServer implements IResettableServer {

    private Queue commandQueue = new Queue();
    private SpotSerialPipe serialPipe;
    private byte[] inputBuffer = new byte[255];
    private MCPSDataIndicationCommand dataIndicationCommand = new MCPSDataIndicationCommand().with(RadioFactory.getI802_15_4_MAC());
    private MCPSDataRequestCommand dataRequestCommand = new MCPSDataRequestCommand();
    private ILed receiveLed = Spot.getInstance().getGreenLed();
    private ILed sendLed = Spot.getInstance().getRedLed();
    private Heartbeat heartbeat = new Heartbeat(10000, 3300);   // clear red/green LEDS every 3.3 seconds per 10 second heartbeat pattern

    public MACProxyServer(SpotSerialPipe serialPipe) {
        this.serialPipe = serialPipe;
    }

    public void run() {
        heartbeat.start();

        new MACProxyWorkerThread().start();
        new MACProxyWorkerThread().start();
        System.out.println("base station ready ...");
        VM.getCurrentIsolate().clearOut();
        VM.getCurrentIsolate().clearErr();
        VM.getCurrentIsolate().addErr("serial://usart");

        while (true) {
            try {
                commandQueue.put(receiveCommand());
            } catch (Throwable t) {
                System.err.println("[basestation] main thread failed with " + t);
                t.printStackTrace();
            }
        }
    }

    private ICommand receiveCommand() throws IOException {
        serialPipe.receive(inputBuffer);

        int uid = Utils.readBigEndInt(inputBuffer, 0);
        byte classIndicatorByte = inputBuffer[Utils.SIZE_OF_INT]; // space for uid

        ICommand command = commandFor(classIndicatorByte);
        command.setUID(uid);
        command.populateFrom(inputBuffer, Utils.SIZE_OF_INT + 1); // uid, plus 1 byte for the class indicator

        return command;
    }

    /**
     * Synchronize this method rather than SpotSerialPipe.send() for performance gain of around 20% (no, really)
     */
    private synchronized void serialPipeSend(byte[] outputBuffer, int length) {
        serialPipe.send(outputBuffer, length);
    }

    private ICommand commandFor(byte classIndicatorByte) {
        switch (classIndicatorByte) {
            case MACCommand.GetNullPacketAfterAckWaitCommand:  return new GetNullPacketAfterAckWaitCommand();
            case MACCommand.GetChannelAccessFailureCommand:    return new GetChannelAccessFailureCommand();
            case MACCommand.GetNoAckCommand:                   return new GetNoAckCommand();
            case MACCommand.GetRadioPropertyCommand:           return new GetRadioPropertyCommand();
            case MACCommand.GetWrongAckCommand:                return new GetWrongAckCommand();
            case MACCommand.MCPSDataIndicationCommand:         return dataIndicationCommand;
            case MACCommand.MCPSDataRequestCommand:            return dataRequestCommand;
            case MACCommand.MLMEGetCommand:                    return new MLMEGetCommand();
            case MACCommand.MLMEResetCommand:                  return new MLMEResetCommand();
            case MACCommand.MLMERxEnableCommand:               return new MLMERxEnableCommand();
            case MACCommand.MLMESetCommand:                    return new MLMESetCommand();
            case MACCommand.MLMEStartCommand:                  return new MLMEStartCommand();
            case MACCommand.SetPLMETransmitPowerCommand:       return new SetPLMETransmitPowerCommand();
            case MACCommand.ResetProxyCommand:                 return new ResetProxyCommand().with(this);
            case MACCommand.ExitCommand:                       return new ExitCommand();

            default:
                throw new SpotFatalException("Do not know a command indicated by " + classIndicatorByte);
        }
    }

    private final class MACProxyWorkerThread extends Thread {

        byte[] outputBuffer = new byte[255];
        private I802_15_4_MAC mac = RadioFactory.getI802_15_4_MAC();

        public MACProxyWorkerThread() {
        }

        public void run() {
            while (true) {
                try {
                    ICommand command = (ICommand) commandQueue.get();
                    if (command != null) {
                        Utils.writeBigEndInt(outputBuffer, SpotSerialPipe.PAYLOAD_OFFSET, command.getUID());
                        int dataLength = command.writeResultOnto(outputBuffer, SpotSerialPipe.PAYLOAD_OFFSET + Utils.SIZE_OF_INT, mac);
                        if (!resetting) {
                            serialPipeSend(outputBuffer, dataLength + Utils.SIZE_OF_INT); //for the command uid
                        }
                        if (command == dataRequestCommand) {
                            sendLed.setOn(!sendLed.isOn());
                        }
                        if (command == dataIndicationCommand) {
                            receiveLed.setOn(!receiveLed.isOn());
                        }
                    } else {
                        System.err.println("Ignoring null command");
                    }
                } catch (Throwable t) {
                    System.err.println("[basestation] worker thread failed with " + t);
                    t.printStackTrace();
                }
            }
        }
    }
    private boolean resetting = false;

    public void reset(I802_15_4_MAC mac) throws IOException {
        try {
            resetting = true;
            dataIndicationCommand.reset();
            serialPipe.reset();
            Utils.sleep(250); // allow worker threads to clean themselves up
        } finally {
            resetting = false;
        }
    }

}
