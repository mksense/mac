/*
 * Copyright 2007-2009 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.spot.service;

/**
 * Basic interface supported by all SPOT services so they can be started & stopped
 * from SPOT applications. The intent is that applications can get a long-lived
 * reference to a service and then use that reference to change the state of the
 * service, including stopping the service and later restarting it.
 *
 * The lifecycle for a service is:
 * <ol>
 *  <li> Create service using a constructor or factory method. May be done
 *  automatically by system code. (Service dependent)
 *  <li> If necessary initialize the service by calling methods unique to this
 *  service. (Service dependent)
 *  <li> Obtain a reference to the service. (Service dependent)
 *  <li> Start the service running via start() method. (All services)
 *  <li> Check the status of the service via getStatus() or isRunning(). (All services)
 *  <li> Initiate requests by calling methods unique to this service. (Service dependent)
 *  <li> Cause the service to suspend activity via pause(). (All services)
 *  <li> Cause the service to resume activity via resume(). (All services)
 *  <li> Stop the service via stop(), killing all threads and closing any open IO resources. (All services)
 *  <li> Restart the service running via start(). (All services)
 *  <li> repeat any of steps 4-9.
 * </ol>
 *
 * @author Ron Goldman
 */
public interface IService {

    /**
     * Service is currently stopped, i.e. not running.
     */
    public static final int STOPPED = 0;
    /**
     * Service is currently in the process of starting up.
     */
    public static final int STARTING = 1;
    /**
     * Service is currently running.
     */
    public static final int RUNNING = 2;
    /**
     * Service is currently paused.
     */
    public static final int PAUSED = 3;
    /**
     * Service is currently in the process of stopping.
     */
    public static final int STOPPING = 4;
    /**
     * Service is currently in the process of pausing.
     */
    public static final int PAUSING = 5;
    /**
     * Service is currently in the process of resuming.
     */
    public static final int RESUMING = 6;
    /**
     * Service is ready to be called. Used for services that do not run in their own threads.
     */
    public static final int READY = 7;

    /**
     * Start the service, and return whether successful.
     *
     * @return true if the service was successfully started
     */
    public boolean start();

    /**
     * Stop the service, and return whether successful.
     * Stops all running threads. Closes any open IO connections.
     *
     * @return true if the service was successfully stopped
     */
    public boolean stop();

    /**
     * Pause the service, and return whether successful.
     * Preserve any current state, but do not handle new requests.
     * Any running threads should block or sleep. 
     * Any open IO connections may be kept open.
     *
     * If there is no particular state associated with this service
     * then pause() can be implemented by calling stop().
     *
     * @return true if the service was successfully paused
     */
    public boolean pause();

    /**
     * Resume the service, and return whether successful.
     * Picks up from state when service was paused.
     *
     * If there was no particular state associated with this service
     * then resume() can be implemented by calling start().
     *
     * @return true if the service was successfully resumed
     */
    public boolean resume();

    /**
     * Return the current status of this service.
     *
     * @return the current status of this service, e.g. STOPPED, STARTING, RUNNING, PAUSED, STOPPING, etc.
     */
    public int getStatus();
    
    /**
     * Return whether the service is currently running.
     *
     * @return true if the service is currently running
     */
    public boolean isRunning();

    /**
     * Return the name of this service.
     *
     * @return the name of this service
     */
    public String getServiceName();

    /**
     * Assign a name to this service. For some fixed services this may not apply and
     * any new name will just be ignored.
     *
     * @param who the name for this service
     */
    public void setServiceName(String who);

    /**
     * Return whether service is started automatically on reboot.
     * This may not apply to some services and for those services it will always return false.
     *
     * @return true if the service is started automatically on reboot
     */
    public boolean getEnabled();

    /**
     * Enable/disable whether service is started automatically. 
     * This may not apply to some services and calls to setEnabled() may be ignored.
     *
     * @param enable true if the service should be started automatically on reboot
     */
    public void setEnabled(boolean enable);
}

