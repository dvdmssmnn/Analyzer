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

package dm.util;

import java.util.HashMap;
import java.util.Map;

public class NSDictionaryParser {
	public static Map<String, String> parseDictionary(String dictionary) {
		Map<String, String> map = new HashMap<String, String>();

		int currentIdx = 0;

		while (true) {
			int keyStart = dictionary.indexOf("\"", currentIdx);
			currentIdx = keyStart < 0 ? currentIdx : keyStart + 1;
			int keyEnd = dictionary.indexOf("\"", currentIdx);
			currentIdx = keyEnd < 0 ? currentIdx : keyEnd + 1;

			int valueStart = dictionary.indexOf("\"", currentIdx);
			currentIdx = valueStart < 0 ? currentIdx : valueStart + 1;
			int valueEnd = dictionary.indexOf("\"", currentIdx);
			currentIdx = valueEnd < 0 ? currentIdx : valueEnd + 1;

			if (valueEnd < 0) {
				break;
			}

			String value = dictionary.substring(keyStart + 1, keyEnd);
			String key = dictionary.substring(valueStart + 1, valueEnd);

			map.put(key, value);
		}

		return map;
	}
}
