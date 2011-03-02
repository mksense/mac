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

package com.sun.spot.peripheral.radio.routing;

import com.sun.spot.peripheral.radio.routing.interfaces.Comparable;

/**
 * @author Allen Ajit George
 * @version 0.1
 */
public class SortedList {
    
    private LinkedListNode listHead;
    private LinkedListNode listTail;
    
    public SortedList() {
        listHead = null;
        listTail = null;
    }
    
    public void insertElement(Comparable newPayload) {
        if ((listHead == null) && (listTail == null)) {
            LinkedListNode nodeToInsert = new LinkedListNode(null, null, newPayload);
            listHead = nodeToInsert;
            listTail = nodeToInsert;
        } else {
            // Start at the list tail and work backwards
            LinkedListNode currentNode = listTail;
            while (currentNode != null) {
                if (currentNode.payload.compare(newPayload) < 0) {
                    break;
                }
                
                currentNode = currentNode.prevNode;
            }
            
            // We're here either because we're at the beginning of the list or 
            // because we found the right spot
            if (currentNode == null) {
                // Beginning
                LinkedListNode tempNode = listHead;
                listHead = new LinkedListNode(null, tempNode, newPayload);
                tempNode.prevNode = listHead;
            } else {
                if (currentNode == listTail) {
                    // Right at the end
                    LinkedListNode tempNode = listTail;
                    listTail = new LinkedListNode(tempNode, null, newPayload);
                    tempNode.nextNode = listTail;
                } else {
                    // Somewhere in the middle
                    LinkedListNode nodeToInsert = new LinkedListNode(currentNode, 
                            currentNode.nextNode, newPayload);
                    (currentNode.nextNode).prevNode = nodeToInsert;
                    currentNode.nextNode = nodeToInsert;
                }
            }
        }
    }
    
    public Comparable removeFirstElement() {
        if (listHead != null) {
            LinkedListNode removedNode = listHead;
            
            listHead = removedNode.nextNode;
            if (listHead != null) {
                listHead.prevNode = null;
            } else {
                listTail = null;
            }
            return removedNode.payload;
        } else {
            return null;
        }
    }
    
    public Comparable getFirstElement() {
        if (listHead != null) {
            return listHead.payload;
        }
        return null;
    }
    
    public Comparable removeElement(Comparable payloadToRemove) {
        LinkedListNode currentNode = listHead;
        Comparable returnedPayload = null;
        
        while (currentNode != null) {
            if (currentNode.payload == payloadToRemove) {
                break;
            }
            
            currentNode = currentNode.nextNode;
        }
        
        // We actually found a match...
        if (currentNode != null) {
            if ((currentNode == listHead) && (currentNode == listTail)) {
                // It's the only node in the list
                listHead = null;
                listTail = null;
            } else if (currentNode == listHead) {
                (currentNode.nextNode).prevNode = null;
                listHead = currentNode.nextNode;
            } else if (currentNode == listTail) {
                (currentNode.prevNode).nextNode = null;
                listTail = currentNode.prevNode;
            } else {
                (currentNode.prevNode).nextNode = currentNode.nextNode;
                (currentNode.nextNode).prevNode = currentNode.prevNode;
            }
            
            returnedPayload = currentNode.payload;
        }
        
        return returnedPayload;
    }
    
    private class LinkedListNode {
        public LinkedListNode(LinkedListNode prevNode, LinkedListNode nextNode, 
                Comparable payload) {
            this.prevNode = prevNode;
            this.nextNode = nextNode;
            this.payload = payload;
        }
        
        LinkedListNode nextNode = null;
        LinkedListNode prevNode = null;
        Comparable payload = null;
    }
}
