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
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Date;

import com.sun.spot.util.Utils;
import com.sun.squawk.VM;
import com.sun.squawk.security.verifier.SignatureVerifier;
import com.sun.squawk.security.verifier.SignatureVerifierException;

class SignatureVerifierWrapper implements ISignatureVerifier {
	private static final int EXPECTED_SIGNATURE_VERSION = 0;

	public void initialize(byte[] publicKeyBytes, int offset, int length) throws SignatureVerifierException {
		SignatureVerifier.initialize(publicKeyBytes, offset, length);
	}

	public void verify(byte[] buffer, int bufferOffset, int bufferLength, byte[] signature, int signatureOffset,
			int signatureLength) throws SignatureVerifierException {
		SignatureVerifier.verify(buffer, bufferOffset, bufferLength, signature, signatureOffset, signatureLength);
	}

	public void verifyWithTimestamp(byte[] data, int signatureOffset) throws SignatureVerifierException {
		try {
			int headerSize = 9;
			int startOfSignature = signatureOffset + headerSize;
			int signatureLength = data[startOfSignature + 1] + 2;

			int version;
			long timestamp;
			try {
				DataInputStream header = new DataInputStream(new ByteArrayInputStream(data, signatureOffset, headerSize));
				version = (header.readByte() & 0xff);
				if (version != EXPECTED_SIGNATURE_VERSION) {
					throw new SignatureVerifierException("Unsupported signature version. (" + version + ", expected: " + EXPECTED_SIGNATURE_VERSION + ")");
				}
				timestamp = header.readLong();
				header.close();
			} catch (IOException e) {
				throw new SignatureVerifierException(e.getMessage());
			}
			long currentTime = System.currentTimeMillis();
			if (VM.isVerbose()) {

				Utils.log("Verifying signed data:\n" + "\tSignature version: " + version + "\n"
						+ "\tTimestamp: " + timestamp + " (" + new Date(timestamp) + ")\n" + "\tcurrent Time: "
						+ currentTime + " (" + new Date(currentTime) + ")\n" + "\tmax skew: " + MAX_CLOCK_SKEW);
			}

			if (!(((timestamp < currentTime + MAX_CLOCK_SKEW) && ((timestamp > currentTime - MAX_CLOCK_SKEW))))) {
				throw new SignatureVerifierException("Timestamp not within max. clock skew. (local time=" + currentTime
						+ ", cmd timestamp=" + timestamp + ")");
			}
			verify(data, 0, startOfSignature, data, startOfSignature, signatureLength);
		} catch (RuntimeException e) {
			throw new SignatureVerifierException(e.getMessage());
		}
	}

}
