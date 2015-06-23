//Copyright (c) 2015, David Missmann
//All rights reserved.
//
//Redistribution and use in source and binary forms, with or without modification,
//are permitted provided that the following conditions are met:
//
//1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following
//disclaimer.
//
//2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
//disclaimer in the documentation and/or other materials provided with the distribution.
//
//THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
//INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
//DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
//SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
//SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
//WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
//OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package dm.analyze.random;

import java.util.HashSet;
import java.util.Set;

public class SecureRandomBytes {

	private static Set<SecureRandomBytes> bytes = null;

	private String hexData;
	private long callID;

	public synchronized static Set<SecureRandomBytes> getRandomBytes() {
		if (bytes == null) {
			bytes = new HashSet<SecureRandomBytes>();
			bytes.addAll(CCRandom.getRandomBytes());
			bytes.addAll(SecRandom.getRandomBytes());
		}
		return bytes;
	}

	public static SecureRandomBytes find(String hexData) {

		for (SecureRandomBytes bytes : getRandomBytes()) {
			if (bytes.getHexData().equals(hexData)) {
				return bytes;
			}
		}
		return null;
	}

	public SecureRandomBytes(String hexData, long callID) {
		this.hexData = hexData;
		this.callID = callID;
	}

	public String getHexData() {
		return hexData;
	}

	public long getCallID() {
		return callID;
	}
}
