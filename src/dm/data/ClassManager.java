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

package dm.data;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dm.db.DBHelper;
import dm.util.Pair;

public class ClassManager {
	private static ClassManager instance = null;

	private Map<String, String> classes = new HashMap();

	public static synchronized ClassManager getInstance() {
		if (instance == null) {
			instance = new ClassManager();
		}
		return instance;
	}

	private ClassManager() {
		try {
			DBHelper connection = DBHelper.getReadableConnections();
			List<Pair<String, String>> classList = connection.getClassList();

			for (Pair<String, String> c : classList) {
				if (c.getSecond() != null) {
					this.classes.put(c.getFirst(), c.getSecond());
				}
			}
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public boolean isKindOf(String clazz, String compare) {
		return compare(clazz, compare);
	}

	private boolean compare(String clazz, String compare) {
		if (clazz.equals(compare)) {
			return true;
		}
		String s = classes.get(clazz);
		if (s != null) {
			return compare(s, compare);
		}
		return false;
	}
}
