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

import com.sun.squawk.security.verifier.SignatureVerifierException;

interface ISignatureVerifier {
	/**
	 * The maximum time in milliseconds that the host and SPOT clocks can deviate by before 
	 * commands fail verification
	 */
	long MAX_CLOCK_SKEW = 3*60*1000;
	
	void initialize(byte[] publicKeyBytes, int offset, int length) throws SignatureVerifierException;
	
	void verify(byte[] buffer, int bufferOffset, int bufferLength, byte[] signature, int signatureOffset,
			int signatureLength) throws SignatureVerifierException;
		
	/**
	 * verifyWithTimestamp Verifies a buffer created by a SigningOutputStream considering the included
	 * timestamp. The buffer is successfully verified if it was signed with the private key belonging
	 * to the public key stored in the devices keystore, and the timestamp is within the maximum clock
	 * skew.<br><br>
	 *
	 * The maximum clock skew is MAX_CLOCK_SKEW. <br><br>
	 *
	 * @param data 
     * @param signatureOffset 
     * @throws SignatureVerifierException if the verification fails
     */
	void verifyWithTimestamp(byte[] data, int signatureOffset) throws SignatureVerifierException;
	
	
}
