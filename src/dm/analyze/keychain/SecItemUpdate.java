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

package dm.analyze.keychain;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dm.data.Call;
import dm.db.DBHelper;
import dm.util.Strings;

public class SecItemUpdate extends SecItem {
	private static Set<SecItem> items;

	private String query;

	public static synchronized Set<SecItem> getItems() {
		if (items == null) {
			items = new HashSet<SecItem>();

			try {
				DBHelper connection = DBHelper.getReadableConnections();

				List<Call> updateCalls = connection.findCall(null, "Security",
						"SecItemUpdate", null, null, null, null);

				for (Call updateCall : updateCalls) {
					items.add(new SecItemUpdate(updateCall.getID(), updateCall
							.getParameter().get(0).getDescription(), updateCall
							.getParameter().get(1).getDescription()));
				}

				connection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return items;
	}

	public SecItemUpdate(long callID, String query, String attributes) {
		super(callID, attributes);

		this.query = query;
	}

	@Override
	public boolean contains(String hexData) {
		if (super.contains(hexData)) {
			return true;
		}

		if (query.contains(Strings.hexToString(hexData))) {
			return true;
		}

		if (query.replace(" ", "").contains(hexData)) {
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return String.format("SecitemUpdate %d", getCallID());
	}

}
