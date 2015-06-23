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

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import dm.analyze.Search;
import dm.analyze.cipher.SymmetricCipher;
import dm.analyze.constants.Constant;
import dm.analyze.fs.FSData;
import dm.analyze.http.CFURLRequest;
import dm.analyze.warningns.Warning;
import dm.data.Call;
import dm.db.DBHelper;
import dm.util.Strings;

public class CCDerivedKey extends SymmetricKey {
	public static final Logger log = Logger.getLogger(CCDerivedKey.class);
	private static final int MIN_NO_ROUNDS = 1000;

	public static synchronized Set<SymmetricKey> getKeys() {
		Set<SymmetricKey> keys = new HashSet<SymmetricKey>();
		try {
			DBHelper connection = DBHelper.getReadableConnections();
			List<Call> pbkdfCalls = connection.findCall(null, "CommonCrypto",
					"CCKeyDerivationPBKDF", null, null, null, null);
			for (Call call : pbkdfCalls) {
				keys.add(new CCDerivedKey(call));
			}
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return keys;
	}

	private String password;
	private String hexPassword;
	private int rounds;

	public CCDerivedKey(Call call) {
		super(call.getParameter().get(7).getDescription(), call.getID());
		this.hexPassword = call.getParameter().get(1).getDescription();
		this.password = Strings.hexToString(hexPassword);
		this.rounds = Integer.valueOf(call.getParameter().get(6).getValue());
	}

	public void checkKey() {
		super.checkKey();
		Constant constantPassword = Constant.find(hexPassword);
		if (constantPassword != null) {
			log.warn("Key derived from constant");
			warnings.add(new Warning(String.format("%s",
					Strings.hexToString(constantPassword.getHexValue())),
					"Constant Password used").setValue(Warning.DATA,
					constantPassword.getHexValue()));
		}

		for (FSData fsData : FSData.getFSData()) {
			if (fsData.containsData(hexPassword)) {
				// TODO:
			}
		}

		for (CFURLRequest request : CFURLRequest.getRequests()) {
			if (request.contains(hexPassword) != CFURLRequest.CONTAINS_NONE) {
				// TODO:
			}
		}

		if (rounds < MIN_NO_ROUNDS) {
			warnings.add(new Warning(String.format(
					"Using only %d rounds for key derivation", this.rounds)));
		}

		Set<Search> searches = SymmetricCipher.getSearches(hexPassword, true);

		for (FSData fs : FSData.search(searches)) {
			warnings.add(new Warning(String.format("Found password in file %s",
					fs.getPath()), "Password written to FS"));
		}

		for (CFURLRequest request : CFURLRequest.search(searches)) {
			warnings.add(new Warning(String.format(
					"Found password in request to: %s", request.getUrl())));
		}
	}
}
