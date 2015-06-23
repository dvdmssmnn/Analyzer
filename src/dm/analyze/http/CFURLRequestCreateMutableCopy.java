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

package dm.analyze.http;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dm.data.Call;
import dm.db.DBHelper;

public class CFURLRequestCreateMutableCopy extends CFURLRequest {

	private static Set<CFURLRequest> requests;

	public static synchronized Set<CFURLRequest> getRequests() {
		if (requests == null) {
			requests = new HashSet<CFURLRequest>();

			try {
				DBHelper connection = DBHelper.getReadableConnections();
				List<Call> calls = connection
						.findCall(null, null, "CFURLRequestCreateMutableCopy",
								null, null, null, null);
				for (Call call : calls) {
					requests.add(new CFURLRequestCreateMutableCopy(call));
				}
				connection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return requests;
	}

	public CFURLRequestCreateMutableCopy(Call call) {
		super(call.getReturnValue());
		String requestDescription = call.getParameter().get(1).getDescription();
		int start = requestDescription.indexOf("url = ") + 6;
		this.url = requestDescription.substring(start,
				requestDescription.indexOf(", ", start));
	}
}
