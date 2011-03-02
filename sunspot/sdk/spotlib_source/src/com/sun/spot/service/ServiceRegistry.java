/*
 * Copyright 2009 Sun Microsystems, Inc. All Rights Reserved.
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

import com.sun.spot.globals.SpotGlobals;
import java.util.Vector;

/**
 * Global service registry for all SPOT-related services.
 *
 * @author Ron Goldman
 */
public class ServiceRegistry {

    private static final int SPOT_GLOBAL_SERVICE_REGISTRY = 1001;

    private static ServiceRegistry instance = null;

    private Vector services = new Vector();

    /**
     * Get the globally unique service registry instance.
     *
     * @return the service registry instance
     */
    public static ServiceRegistry getInstance() {
        if (instance == null) {
            synchronized (SpotGlobals.getMutex()) {
                Object sr = SpotGlobals.getGlobal(SPOT_GLOBAL_SERVICE_REGISTRY);
                if (sr != null) {
                    instance = (ServiceRegistry) sr;
                } else {
                    instance = new ServiceRegistry();
                    SpotGlobals.setGlobal(SPOT_GLOBAL_SERVICE_REGISTRY, instance);
                }
            }
        }
        return instance;
    }

    private ServiceRegistry() {
    }

    /**
     * Add a new service to the registery.
     *
     * @param serviceInstance the new service instance to add
     */
    public void add(IService serviceInstance) {
        if (!services.contains(serviceInstance)) {
            services.addElement(serviceInstance);
        }
    }

    /**
     * Remove a service from the registery.
     *
     * @param serviceInstance the service instance to remove
     */
    public void remove(IService serviceInstance) {
        services.removeElement(serviceInstance);
    }

    private IService[] toArray(Vector v) {
        IService[] a = new IService[v.size()];
        for (int i = 0; i < v.size(); i++) {
            a[i] = (IService)v.elementAt(i);
        }
        return a;
    }

    /**
     * Lookup all matching services.
     *
     * @param serviceInterface the desired type of service
     * @return an array of all matching services
     */
    public IService[] lookupAll(Class serviceInterface) {
        Vector results = new Vector();
        for (int i = 0; i < services.size(); i++) {
            if (serviceInterface.isInstance(services.elementAt(i))) {
                results.addElement(services.elementAt(i));
            }
        }
        return toArray(results);
    }

    /**
     * Lookup a matching service. Returns the first service found that
     * implements serviceInterface. Subsequent calls may return another
     * service instance.
     *
     * @param serviceInterface the desired type of service
     * @return a matching service
     */
    public IService lookup(Class serviceInterface) {
        for (int i = 0; i < services.size(); i++) {
            IService s = (IService)services.elementAt(i);
            if (serviceInterface.isInstance(s)) {
                return s;
            }
        }
        return null;
    }

    /**
     * Lookup all matching services with a specified service name.
     *
     * @param serviceInterface the desired type of service
     * @param name the desired service name
     * @return an array of all matching services
     */
    public IService[] lookupAll(Class serviceInterface, String name) {
        Vector results = new Vector();
        for (int i = 0; i < services.size(); i++) {
            IService s = (IService)services.elementAt(i);
            if (serviceInterface.isInstance(s) && s.getServiceName().equals(name)) {
                results.addElement(s);
            }
        }
        return toArray(results);
    }

    /**
     * Lookup a matching service with a specified service name.
     * Returns the first service found that implements serviceInterface.
     * Subsequent calls may return another service instance.
     *
     * @param serviceInterface the desired type of service
     * @param name the desired service name
     * @return a matching service
     */
    public IService lookup(Class serviceInterface, String name) {
        for (int i = 0; i < services.size(); i++) {
            IService s = (IService)services.elementAt(i);
            if (serviceInterface.isInstance(s) && s.getServiceName().equals(name)) {
                return s;
            }
        }
        return null;
    }

}
