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

package dm.analyze.httpstream;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import dm.data.Call;
import dm.db.DBHelper;

public class CFHTTPMessage {
	private static final Logger log = Logger.getLogger(CFHTTPMessage.class);

	protected String cfHTTPMessageRef;
	protected StringBuffer content = new StringBuffer();

	private static Set<CFHTTPMessage> messages = null;
	private File f;
	private BufferedWriter writer;

	public static synchronized Set<CFHTTPMessage> getHTTPMessages() {
		if (messages == null) {
			messages = new HashSet<CFHTTPMessage>();

			messages.addAll(CFHTTPMessageCreateEmpty.getHTTPMessages());
		}
		return messages;
	}

	public CFHTTPMessage(String CFHTTPMessageRef) {
		this.cfHTTPMessageRef = CFHTTPMessageRef;
		this.buildMessage();
	}

	protected void buildMessage() {
		try {
			DBHelper connection = DBHelper.getReadableConnections();
			List<Call> appendBytesCalls = connection.findCall(null,
					"CFNetwork", "CFHTTPMessageAppendBytes", null, null,
					this.cfHTTPMessageRef, null);
			for (Call call : appendBytesCalls) {
				content.append(call.getParameter().get(1).getDescription());
				Logger.getLogger(CFHTTPMessage.class).debug(
						String.format("%d bytes", call.getParameter().get(1)
								.getDescription().length()));
				log.debug(String.format("%d length", content.length()));
			}
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		try {
			writer.write(content.toString());
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
