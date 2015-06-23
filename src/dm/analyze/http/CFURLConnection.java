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

import org.apache.log4j.Logger;

import dm.analyze.constants.Constant;
import dm.analyze.warningns.Warning;
import dm.data.Call;
import dm.db.DBHelper;
import dm.util.Strings;

public class CFURLConnection {

	private static final Logger log = Logger.getLogger(CFURLConnection.class);

	private static Set<CFURLConnection> connections;
	private String ref;
	private CFURLRequest request;
	private String username;
	private String password;
	private Set<Warning> warnings;

	public static synchronized Set<CFURLConnection> getConnections() {
		if (connections == null) {
			connections = new HashSet<CFURLConnection>();

			try {
				DBHelper connection = DBHelper.getReadableConnections();
				List<Call> createCalls = connection.findCall(null, null,
						"CFURLConnectionCreate", null, null, null, null);

				for (Call call : createCalls) {
					connections.add(new CFURLConnection(call.getReturnValue(),
							getRequestWithRef(call.getParameter().get(1)
									.getValue())));
				}

				List<Call> createWithPropertiesCalls = connection.findCall(
						null, null, "CFURLConnectionCreateWithProperties",
						null, null, null, null);
				for (Call call : createWithPropertiesCalls) {
					connections.add(new CFURLConnection(call.getReturnValue(),
							getRequestWithRef(call.getParameter().get(1)
									.getValue())));
				}
				connection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}

		}
		return connections;
	}

	private static CFURLRequest getRequestWithRef(String ref) {
		Set<CFURLRequest> requests = CFURLRequest.getRequests();
		for (CFURLRequest request : requests) {
			if (request.getRef().equals(ref)) {
				return request;
			}
		}
		return null;
	}

	public CFURLConnection(String ref, CFURLRequest request) {
		this.ref = ref;
		this.request = request;

		// Check if creds were set
		try {
			DBHelper connection = DBHelper.getReadableConnections();
			List<Call> useCredsCalls = connection.findCall(null, null,
					"CFURLConnectionUseCredential", null, null, this.ref, null);
			if (useCredsCalls.size() == 1) {
				List<Call> createCredentialCalls = connection.findCall(null,
						null, "CFURLCredentialCreate", null, null,
						useCredsCalls.get(0).getParameter().get(1).getValue(),
						null);

				if (createCredentialCalls.size() == 1) {
					this.username = createCredentialCalls.get(0).getParameter()
							.get(1).getDescription();
					this.password = createCredentialCalls.get(0).getParameter()
							.get(2).getDescription();
				} else {
					log.warn("There should be exactly one CreateCredentials call");
				}
			} else if (useCredsCalls.size() > 0) {
				// TODO: Maybe we should handle this case...
				throw new UnsupportedOperationException();
			}
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public synchronized void check() {
		if (this.warnings != null) {
			return;
		}
		this.warnings = new HashSet<Warning>();

		if (this.username != null
				&& Constant.find(Strings.stringToHex(this.username)) != null) {
			this.warnings.add(new Warning("Username is constants"));
		}
		if (this.password != null
				&& Constant.find(Strings.stringToHex(this.password)) != null) {
			this.warnings.add(new Warning("Password is constants"));
		}
	}

	public synchronized Set<Warning> getWarnings() {
		if (this.warnings == null) {
			this.check();
		}
		return this.warnings;
	}

	public CFURLRequest getRequest() {
		return request;
	}

}
