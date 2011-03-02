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

package com.sun.spot.util;

import java.util.Vector;

/**
 * Implements a fully synchronised FIFO queue of Objects. 
 */
public class Queue {
	private Vector v = new Vector();
	private boolean stopped = false;
	
	/**
	 * Answer whether the receiver is empty.
	 * 
	 * @return - whether the receiver is empty.
	 */
	public boolean isEmpty() {
		synchronized (v) {
			return v.isEmpty();
		}		
	}
	
	/**
	 * Answer the number of elements in the receiver.
	 * 
	 * @return -- the number of elements in the receiver.
	 */
	public int size() {
		synchronized (v) {
			return v.size();
		}		
	}
	
	/**
	 * Add an element to the receiver.
	 *  
	 * @param o -- the Object to add
	 */
	public void put(Object o) {
		synchronized (v) {
			v.addElement(o);
			v.notify();
		}
	}

	/**
	 * Answer an element from the receiver. If the receiver is empty, block
	 * until either an element becomes available, or for timeout milliseconds,
	 * or the queue is stopped. In the event of timeout, return null.
	 * Note that if a waiting thread is interrupted it will return (probably null).
	 * 
	 * @param timeout -- number of milliseconds to wait
	 * @return -- either the first element from the receiver or null after a timeout or if queue is stopped.
	 */
	public Object get(long timeout) {
		synchronized (v) {
			if (timeout > 0 && v.isEmpty() && !stopped) {
				try {
					v.wait(timeout);
				} catch (InterruptedException e) {
					// it's important to catch this exception because we don't notifyAll in
					// put(), so only one thread is unblocked and the thread must consume the
					// element even if it has been interrupted
				}
			}
			return pop();
		}		
	}

	/**
	 * Answer an element from the receiver. If the receiver is empty, block
	 * (possibly forever) until an element becomes available.
	 * Note that if a waiting thread is interrupted it will return (probably null).
	 * 
	 * @return -- the first element from the receiver, or null if queue is stopped
	 */
	public Object get() {
		synchronized (v) {
			while (v.isEmpty() && !stopped) {
				try {
					v.wait();
				} catch (InterruptedException e) {
					// it's important to catch this exception because we don't notifyAll in
					// put(), so only one thread is unblocked and the thread must consume the
					// element even if it has been interrupted
					break;
				}
			}
			return pop();
		}		
	}
	
	/**
	 * Release all waiters
	 */
	public void stop() {
		synchronized (v) {
			stopped = true;
			v.notifyAll();
		}		
	}
	
	/**
	 * Drop the contents of the queue.
	 */
	public void empty() {
		synchronized (v) {
			v.removeAllElements();
		}		
	}

	private Object pop() {
		Object result = null;
		if (!v.isEmpty()) {
			result = v.firstElement();
			v.removeElementAt(0);
		}
		return result;
	}
}
