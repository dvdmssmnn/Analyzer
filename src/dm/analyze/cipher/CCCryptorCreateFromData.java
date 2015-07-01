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

package dm.analyze.cipher;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import dm.data.Call;
import dm.db.DBHelper;

public class CCCryptorCreateFromData extends CommonCryptor {

	private static final Logger log = Logger
			.getLogger(CCCryptorCreateFromData.class);

	private static Set<CCCryptorCreateFromData> ciphers = null;

	public static synchronized Set<CCCryptorCreateFromData> getCipher() {
		if (ciphers == null) {
			ciphers = new HashSet<CCCryptorCreateFromData>();
			try {
				DBHelper connection = DBHelper.getReadableConnections();
				List<Call> calls = connection.findCall(null, "CommonCrypto",
						"CCCryptorCreateFromData", null, null, null, null);
				log.debug(String.format("Found %d calls to 'CCCryptorCreate'",
						calls.size()));
				for (Call call : calls) {
					CCCryptorCreateFromData cipher = new CCCryptorCreateFromData(
							call);
					if (!cipher.isSSLCipher()) {
						ciphers.add(cipher);
					}

				}
				connection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			log.debug(String.format("Set contains %d ciphers", ciphers.size()));
		}
		return ciphers;
	}

	public CCCryptorCreateFromData(Call call) {
		super(
				call.getID(),
				call.getCallerID(),
				Integer.valueOf(call.getParameter().get(0).getValue()),
				Integer.valueOf(call.getParameter().get(1).getValue()),
				(Integer.valueOf(call.getParameter().get(2).getValue())
						.intValue() & kCCOptionECBMode) != 0 ? CCMode.kCCModeECB.numVal
						: CCMode.kCCModeCBC.numVal, call.getParameter().get(5),
				call.getParameter().get(3), call.getParameter().get(8)
						.getValue());
	}
}
