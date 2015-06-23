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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.apache.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dm.analyze.CipherSearch;
import dm.analyze.ISearchable;
import dm.analyze.Search;
import dm.analyze.cipher.SymmetricCipher;
import dm.analyze.hash.CCHash;
import dm.analyze.http.CFURLConnection;
import dm.analyze.warningns.Warning;
import dm.db.DBHelper;
import dm.util.Strings;

public class CheckAll {
	private static final Logger problemLogger = Logger.getLogger("report");

	public static void main(String[] args) throws FileNotFoundException {

		String dir = args[0];
		if (dir.endsWith("/")) {
			dir = dir.substring(0, dir.length() - 1);
		}
		DBHelper.setDBPath(String.format("%s/db.sqlite", dir));

		checkCipher();
		checkDigests();
		checkSensitive(dir);
		checkHTTP();
	}

	private static void checkCipher() {
		for (SymmetricCipher cipher : SymmetricCipher.getCiphers()) {
			problemLogger.info(String.format("########################"));
			Set<Warning> warnings = cipher.getWarnings();

			problemLogger.info(String.format("%s %d", cipher.getOperation()
					.toString(), cipher.getCallID()));
			if (warnings.size() == 0) {
				problemLogger.info("No warnings");
				continue;
			}
			problemLogger.info("Warnings:");

			for (Warning warning : warnings) {
				problemLogger.info(String.format("%s",
						warning.getValue(Warning.SHORT_INFO)));
			}

			problemLogger.info("\n");
		}

		problemLogger.info("///////////////////////////////////////////");
		problemLogger.info("////        Cipher checks done         ////");
		problemLogger.info("///////////////////////////////////////////");
	}

	private static void checkDigests() {
		for (CCHash h : CCHash.getHashes()) {
			if (h.getWarnings().size() != 0) {
				problemLogger.info(String.format("Call %d", h.getID()));
				for (Warning w : h.getWarnings()) {
					problemLogger.info(w.getValue(Warning.SHORT_INFO));
				}
			}
		}

		problemLogger.info("///////////////////////////////////////////");
		problemLogger.info("////        Digest checks done         ////");
		problemLogger.info("///////////////////////////////////////////");
	}

	private static void checkSensitive(String dir) throws FileNotFoundException {
		File f = new File(String.format("%s/data.json", dir));
		if (!f.exists()) {
			problemLogger.info("data.json not found");
			return;
		}

		String content = new Scanner(f).useDelimiter("\\Z").next();

		JsonArray data = new JsonParser().parse(content).getAsJsonArray();

		for (int i = 0; i < data.size(); ++i) {
			JsonObject object = (JsonObject) data.get(i);
			String type = object.get("type").getAsString();
			String search = null;
			String searchPlain = null;
			if (type.equals("plain")) {
				searchPlain = object.get("content").getAsString();
				search = Strings.stringToHex(searchPlain);
			}

			if (searchPlain != null) {
				problemLogger.info(String
						.format("Search for '%s'", searchPlain));
			} else {
				problemLogger.info(String.format("Search for '%s'", search));
			}

			Map<ISearchable, Search> found = ISearchable.find(search);
			for (ISearchable s : found.keySet()) {
				problemLogger.info(String.format("Found in: %s", s.toString()));
				if (found.get(s) instanceof CipherSearch) {
					SymmetricCipher cipher = ((CipherSearch) found.get(s))
							.getCipher();
					if (cipher.getWarnings().size() > 0) {
						problemLogger
								.info(String
										.format("Encrypted content with cipher %d has warnings",
												cipher.getCallID()));
					}
				}
			}
		}

		problemLogger.info("///////////////////////////////////////////");
		problemLogger.info("////     Sensitive information done    ////");
		problemLogger.info("///////////////////////////////////////////");
	}

	private static void checkHTTP() {
		for (CFURLConnection connection : CFURLConnection.getConnections()) {
			Set<Warning> warnings = connection.getWarnings();

			if (warnings.size() == 0) {
				continue;
			}
			problemLogger.info(String.format("URL %s", connection.getRequest()
					.getUrl()));
			for (Warning w : warnings) {
				problemLogger.info(w.getValue(Warning.SHORT_INFO));
			}
			problemLogger.info("########");
		}

		problemLogger.info("///////////////////////////////////////////");
		problemLogger.info("////     HTTP Authentication done      ////");
		problemLogger.info("///////////////////////////////////////////");
	}
}
