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

package dm.analyze.keys;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import dm.analyze.fs.FSData;
import dm.analyze.http.CFURLRequest;
import dm.analyze.warningns.Warning;

public abstract class SymmetricKey {

	private static final Logger log = Logger.getLogger(SymmetricKey.class);

	private static Set<SymmetricKey> keys = null;

	private String hexKey;
	private long callID;
	protected Set<Warning> warnings;

	public static synchronized Set<SymmetricKey> getKeys() {
		if (keys == null) {
			keys = new HashSet<>();

			keys.addAll(RandomSymmetricKey.getKeys());
			keys.addAll(CCDerivedKey.getKeys());
		}
		return keys;
	}

	public SymmetricKey(String hexKey, long callID) {
		this.hexKey = hexKey;
		this.callID = callID;
	}

	public void checkKey() {
		warnings = new HashSet<Warning>();

		for (FSData fsData : FSData.getFSData()) {
			if (fsData.containsData(hexKey)) {
				warnings.add(new Warning(String.format("Key written to '%s'",
						fsData.getPath()), "Key written to FS").setValue(
						Warning.DATA, hexKey));
			}
		}

		for (CFURLRequest request : CFURLRequest.getRequests()) {
			if (request.contains(hexKey) != CFURLRequest.CONTAINS_NONE) {
				warnings.add(new Warning(String.format(
						"Found in request to '%s'", request.getUrl()),
						"Key found in HTTP Request").setValue(Warning.DATA,
						hexKey));
			}
		}
	}

	public String getKey() {
		return this.hexKey;
	}

	public Set<Warning> getWarnings() {
		return warnings;
	}

}
