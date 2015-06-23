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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dm.analyze.ISearchable;
import dm.analyze.Search;
import dm.data.Call;
import dm.db.DBHelper;
import dm.util.NSDictionaryParser;
import dm.util.Pair;
import dm.util.Strings;

public class CFURLRequest implements ISearchable {

	public static final int CONTAINS_NONE = 0x0;
	public static final int CONTAINS_URL = 0x1 << 0;
	public static final int CONTAINS_HEADER = 0x1 << 1;
	public static final int CONTAINS_BODY = 0x1 << 2;

	private static final String AUTHORIZATION = "Authorization";

	private static Set<CFURLRequest> requests;

	protected String ref;
	protected String url;
	protected Map<String, String> headerFields = new HashMap<String, String>();
	protected StringBuffer bodyData = new StringBuffer();
	protected Pair<String, String> credentials;

	public static synchronized Set<CFURLRequest> getRequests() {
		if (requests == null) {
			requests = new HashSet<CFURLRequest>();

			requests.addAll(CFURLRequestCreateMutable.getRequests());
			requests.addAll(CFURLRequestCreateMutableCopy.getRequests());
		}
		return requests;
	}

	public CFURLRequest(String ref) {
		this.ref = ref;
		try {
			DBHelper connection = DBHelper.getReadableConnections();
			// Append readStream data
			List<Call> setBodyStreamCalls = connection.findCall(null, null,
					"CFURLRequestSetHTTPRequestBodyStream", null, null, ref,
					null);
			for (Call setBodyStreamCall : setBodyStreamCalls) {
				String streamRef = setBodyStreamCall.getParameter().get(1)
						.getValue();
				List<Call> readStreamCalls = connection.findCall(null, null,
						"CFReadStreamRead", null, null, streamRef, null);
				for (Call readStreamCall : readStreamCalls) {
					bodyData.append(readStreamCall.getParameter().get(1)
							.getDescription());
				}
			}

			// Append body data that was directly set
			List<Call> setBodyCalls = connection.findCall(null, null,
					"CFURLRequestSetHTTPRequestBody", null, null, ref, null);
			for (Call setBodyCall : setBodyCalls) {
				bodyData.append(setBodyCall.getParameter().get(1)
						.getDescription());
			}

			// Header
			List<Call> setMultipleHeaderCalls = connection.findCall(null, null,
					"CFURLRequestSetMultipleHTTPHeaderFields", null, null, ref,
					null);
			for (Call setMultipleHeaderCall : setMultipleHeaderCalls) {
				Map<String, String> header = NSDictionaryParser
						.parseDictionary(setMultipleHeaderCall.getParameter()
								.get(1).getDescription());
				this.headerFields.putAll(header);
			}

			List<Call> setHeaderFieldValueCalls = connection.findCall(null,
					null, "CFURLRequestSetHTTPHeaderFieldValue", null, null,
					ref, null);
			for (Call setHeaderFieldValue : setHeaderFieldValueCalls) {
				this.headerFields.put(setHeaderFieldValue.getParameter().get(1)
						.getDescription(), setHeaderFieldValue.getParameter()
						.get(2).getDescription());
			}

			// TODO: CFURLConnection and check credentials
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	protected void setURLWithCFURL(String cfURLRef) {
		if (cfURLRef == null) {
			try {
				DBHelper connection = DBHelper.getReadableConnections();
				List<Call> setURLCalls = connection.findCall(null, cfURLRef,
						"CFURLRequestSetURL", null, null, ref, null);
				if (setURLCalls.size() == 1) {
					setURLWithCFURL(setURLCalls.get(0).getParameter().get(1)
							.getDescription());
				} else {
					this.url = "";
				}
				connection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} else {
			int start = cfURLRef.indexOf("string = ") + 9;
			this.url = cfURLRef.substring(start, cfURLRef.indexOf(", ", start));
		}
	}

	/**
	 * Just checks if the given data is present somewhere in the message
	 * 
	 * @param hexData
	 * @return value that has CONTAINS_* flags set
	 */
	public int contains(String hexData) {
		int r = CONTAINS_NONE;
		String string = Strings.hexToString(hexData);
		if (this.url.contains(string)) {
			r |= CONTAINS_URL;
		}
		for (String headerKey : this.headerFields.keySet()) {
			// FIXME: should we really check the header key?
			if (headerKey.contains(string)
					|| (this.headerFields.get(headerKey) != null && this.headerFields
							.get(headerKey).contains(string))) {
				r |= CONTAINS_HEADER;
				break;
			}
		}
		if (this.bodyData.toString().contains(hexData)) {
			r |= CONTAINS_BODY;
		}
		return r;
	}

	public Map<String, String> getHeaderFields() {
		return new HashMap<String, String>(headerFields);
	}

	public String getRef() {
		return ref;
	}

	@Override
	public boolean contains(Search search) {
		Set<String> data = new HashSet<String>();

		data.add(this.bodyData.toString());
		data.add(this.url);
		data.add(this.headerFields.toString());

		for (String d : data) {
			for (String cmp : search.getEncoding()) {
				if (d.contains(cmp)) {
					return true;
				}
			}
		}
		return false;
	}

	public static Set<CFURLRequest> search(Set<Search> searches) {
		Set<CFURLRequest> found = new HashSet<CFURLRequest>();
		for (CFURLRequest request : CFURLRequest.getRequests()) {
			for (Search s : searches) {
				if (request.contains(s)) {
					found.add(request);
				}
			}
		}
		return found;
	}

	public String getUrl() {
		return url;
	}

	public void checkURLRequest() {
		// Check basic authentication
		if (this.headerFields.get(AUTHORIZATION) != null) {
			String auth = this.headerFields.get(AUTHORIZATION);
		}
	}

}
