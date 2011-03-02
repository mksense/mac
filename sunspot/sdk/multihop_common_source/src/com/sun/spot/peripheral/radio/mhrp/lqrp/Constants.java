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

package com.sun.spot.peripheral.radio.mhrp.lqrp;

/**
 * @author Allen Ajit George, modifications by Pradip De and Pete St. Pierre
 * @version 0.1
 */
public class Constants {
  /*
   * General constants
   *
   * All times given in milliseconds
   */
    //    public static final int NET_DIAMETER = 20;
    public static final int NET_DIAMETER = 15;
    public static final int NUM_LONG_BYTES = 8;
    public static final int INVALID_SEQUENCE_NUMBER = -1;
    public static final int UNKNOWN_SEQUENCE_NUMBER = 0;
    public static final int FIRST_VALID_SEQUENCE_NUMBER = 1;
    public static final byte LQRP_PROTOCOL_NUMBER = 110;
    public static final long INVALID_NEXT_HOP = -1;
    public static final long QUEUE_TIMEOUT = 5;   //not used
    public static final long MAX_SEQUENCE_NUMBER = 0x0fffffff;
    
  /*
   * RREQ constants
   */
    public static final int RREQ_RETRIES = 3;
    public static final int RREQ_RATELIMIT = 10; // not used
    public static final int RERR_RATELIMIT = 10; // not used
    
    
  /*
   * Timeouts
   *
   * Given in milliseconds
   */
    private static final long AVERAGE_RANDOM_BACKOFF = 200; // 200 is actually about 75%
    public static final long NODE_TRAVERSAL_TIME = 30;
    public static final long NET_TRAVERSAL_TIME = 
            2 * NODE_TRAVERSAL_TIME * NET_DIAMETER; //=900
    public static final long PATH_DISCOVERY_TIME =  // for RREQ timeout and cleaner sleep
            (AVERAGE_RANDOM_BACKOFF * NET_DIAMETER * 2) + NET_TRAVERSAL_TIME; //= 6000 + 900
    public static final long ACTIVE_ROUTE_TIMEOUT = 30000; // for Route timeout
           
    public static final long DELETE_PERIOD = 2500; //  routing table cleaner
    public static final long REQUEST_GRACE_PERIOD =  1 * 60000; // in increments of 60 seconds
    public static final long NEXT_HOP_WAIT = 2 * NODE_TRAVERSAL_TIME; // = 60   not used
    public static final long BLACKLIST_TIMEOUT = RREQ_RETRIES * NET_TRAVERSAL_TIME; // = 2700 not used
    public static final long EXPIRY_TIME_DELTA = 5;  // used by table cleaners
    
    /*
     * HELLO constants
     */
    public static final int ALLOWED_HELLO_LOSS = 2; // not used
    public static final long HELLO_INTERVAL = ACTIVE_ROUTE_TIMEOUT; // not used
  /*
   * Message type constants
   */
    public static final byte RREQ_TYPE = 0x01;
    public static final byte RREP_TYPE = 0x02;
    public static final byte RERR_TYPE = 0x03;

    public static final byte LQREQ_TYPE = 0x41;
    public static final byte LQREP_TYPE = 0x42;

  /*
   * Sleep times
   */
    public static final long REQUEST_TABLE_CLEANER_SLEEP_TIME = 1500; 
    public static final long ROUTING_TABLE_CLEANER_SLEEP_TIME = 2000;
                
}
