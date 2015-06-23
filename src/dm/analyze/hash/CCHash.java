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

package dm.analyze.hash;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import dm.analyze.warningns.Warning;
import dm.data.Call;
import dm.db.DBHelper;

public class CCHash {

	public enum HashType {
		MD2, MD4, MD5, SHA1, SHA224, SHA256, SHA384, SHA512
	}

	private static final Logger log = Logger.getLogger(CCHash.class);
	private static Set<CCHash> hashes;
	private static ExecutorService executor;

	protected long finishID;
	protected StringBuffer input;
	protected String output;
	private String ctx;
	private HashType type;
	private Set<Warning> warnings;

	public static synchronized Set<CCHash> getHashes() {
		if (hashes == null) {
			hashes = new HashSet<CCHash>();
			try {
				executor = Executors.newFixedThreadPool(10);
				DBHelper connection = DBHelper.getReadableConnections();
				List<Call> finalCalls = new ArrayList<Call>();

				finalCalls.addAll(connection.findCall(null, "CommonCrypto",
						"CC_MD2_Final", null, null, null, null));
				finalCalls.addAll(connection.findCall(null, "CommonCrypto",
						"CC_MD4_Final", null, null, null, null));
				finalCalls.addAll(connection.findCall(null, "CommonCrypto",
						"CC_MD5_Final", null, null, null, null));
				finalCalls.addAll(connection.findCall(null, "CommonCrypto",
						"CC_SHA1_Final", null, null, null, null));
				finalCalls.addAll(connection.findCall(null, "CommonCrypto",
						"CC_SHA224_Final", null, null, null, null));
				finalCalls.addAll(connection.findCall(null, "CommonCrypto",
						"CC_SHA256_Final", null, null, null, null));
				finalCalls.addAll(connection.findCall(null, "CommonCrypto",
						"CC_SHA384_Final", null, null, null, null));
				finalCalls.addAll(connection.findCall(null, "CommonCrypto",
						"CC_SHA512_Final", null, null, null, null));

				for (Call f : finalCalls) {
					hashes.add(new CCHash(f));
				}

				List<Call> oneShotCalls = new ArrayList<Call>();

				oneShotCalls.addAll(connection.findCall(null, "CommonCrypto",
						"CC_MD2", null, null, null, null));
				oneShotCalls.addAll(connection.findCall(null, "CommonCrypto",
						"CC_MD4", null, null, null, null));
				oneShotCalls.addAll(connection.findCall(null, "CommonCrypto",
						"CC_MD5", null, null, null, null));
				oneShotCalls.addAll(connection.findCall(null, "CommonCrypto",
						"CC_SHA1", null, null, null, null));
				oneShotCalls.addAll(connection.findCall(null, "CommonCrypto",
						"CC_SHA224", null, null, null, null));
				oneShotCalls.addAll(connection.findCall(null, "CommonCrypto",
						"CC_SHA256", null, null, null, null));
				oneShotCalls.addAll(connection.findCall(null, "CommonCrypto",
						"CC_SHA384", null, null, null, null));
				oneShotCalls.addAll(connection.findCall(null, "CommonCrypto",
						"CC_SHA512", null, null, null, null));

				for (Call oneShotCall : oneShotCalls) {
					hashes.add(new CCHash(Long.valueOf(oneShotCall.getID()),
							oneShotCall.getMethod(), oneShotCall.getParameter()
									.get(0).getDescription(), oneShotCall
									.getParameter().get(2).getDescription()));
				}

				connection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return hashes;
	}

	public CCHash(Call finalCall) {
		this.finishID = finalCall.getID();
		this.ctx = finalCall.getParameter().get(1).getValue();
		this.output = finalCall.getParameter().get(0).getDescription();
		String method = finalCall.getMethod();
		this.setType(method);

		// TODO: find corresponding input
		this.input = new StringBuffer();
		this.input.append("TODO");
	}

	public CCHash(long callID, String method, String input, String output) {
		this.finishID = callID;
		this.input = new StringBuffer();
		this.input.append(input);
		this.output = output;

		this.setType(method);
	}

	private void setType(String method) {
		if (method.contains("MD2")) {
			this.type = HashType.MD2;
		} else if (method.contains("MD4")) {
			this.type = HashType.MD4;
		} else if (method.contains("MD5")) {
			this.type = HashType.MD5;
		} else if (method.contains("SHA1")) {
			this.type = HashType.SHA1;
		} else if (method.contains("SHA224")) {
			this.type = HashType.SHA224;
		} else if (method.contains("SHA256")) {
			this.type = HashType.SHA256;
		} else if (method.contains("SHA384")) {
			this.type = HashType.SHA384;
		} else if (method.contains("SHA512")) {
			this.type = HashType.SHA512;
		}
	}

	private void findUpdateCalls() {
		this.input = new StringBuffer();

		try {
			DBHelper connection = DBHelper.getReadableConnections();

			List<Call> initCalls = new ArrayList<Call>();

			switch (this.type) {
			case MD2:
				initCalls.addAll(connection.findCall(null, "CommonCrypto",
						"CC_MD2_Init", null, null, this.ctx, null));
				break;
			case MD4:
				initCalls.addAll(connection.findCall(null, "CommonCrypto",
						"CC_MD4_Init", null, null, this.ctx, null));
				break;
			case MD5:
				initCalls.addAll(connection.findCall(null, "CommonCrypto",
						"CC_MD5_Init", null, null, this.ctx, null));
				break;
			case SHA1:
				initCalls.addAll(connection.findCall(null, "CommonCrypto",
						"CC_SHA1_Init", null, null, this.ctx, null));
				break;
			case SHA224:
			case SHA256:
				initCalls.addAll(connection.findCall(null, "CommonCrypto",
						"CC_SHA224_Init", null, null, this.ctx, null));
				initCalls.addAll(connection.findCall(null, "CommonCrypto",
						"CC_SHA256_Init", null, null, this.ctx, null));
				break;
			case SHA384:
			case SHA512:
				initCalls.addAll(connection.findCall(null, "CommonCrypto",
						"CC_SHA384_Init", null, null, this.ctx, null));
				initCalls.addAll(connection.findCall(null, "CommonCrypto",
						"CC_SHA512_Init", null, null, this.ctx, null));
				break;
			}

			long initID = 0;
			for (Call initCall : initCalls) {
				if (initCall.getID() < this.finishID
						&& initCall.getID() > initID) {
					initID = initCall.getID();
				}
			}

			List<Call> updateCalls = new ArrayList<Call>();

			switch (this.type) {
			case MD2:
				updateCalls.addAll(connection.findCall(null, "CommonCrypto",
						"CC_MD2_Update", null, null, this.ctx, null));
				break;
			case MD4:
				updateCalls.addAll(connection.findCall(null, "CommonCrypto",
						"CC_MD4_Update", null, null, this.ctx, null));
				break;
			case MD5:
				updateCalls.addAll(connection.findCall(null, "CommonCrypto",
						"CC_MD5_Update", null, null, this.ctx, null));
				break;
			case SHA1:
				updateCalls.addAll(connection.findCall(null, "CommonCrypto",
						"CC_SHA1_Update", null, null, this.ctx, null));
				break;
			case SHA224:
			case SHA256:
				updateCalls.addAll(connection.findCall(null, "CommonCrypto",
						"CC_SHA224_Update", null, null, this.ctx, null));
				updateCalls.addAll(connection.findCall(null, "CommonCrypto",
						"CC_SHA256_Update", null, null, this.ctx, null));
				Collections.sort(updateCalls, new Comparator<Call>() {

					@Override
					public int compare(Call o1, Call o2) {
						long id1 = o1.getID();
						long id2 = o2.getID();
						return id1 < id2 ? -1 : (id1 == id2 ? 0 : 1);
					}
				});
				break;
			case SHA384:
			case SHA512:
				updateCalls.addAll(connection.findCall(null, "CommonCrypto",
						"CC_SHA384_Update", null, null, this.ctx, null));
				updateCalls.addAll(connection.findCall(null, "CommonCrypto",
						"CC_SHA512_Update", null, null, this.ctx, null));
				Collections.sort(updateCalls, new Comparator<Call>() {

					@Override
					public int compare(Call o1, Call o2) {
						long id1 = o1.getID();
						long id2 = o2.getID();
						return id1 < id2 ? -1 : (id1 == id2 ? 0 : 1);
					}
				});
				break;
			}

			for (Call updateCall : updateCalls) {
				long updateID = updateCall.getID();
				if (updateID > initID && updateID < finishID) {
					this.input.append(updateCall.getParameter().get(1)
							.getDescription());
				}
			}

			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public synchronized String getInput() {
		if (this.input == null) {
			findUpdateCalls();
		}
		return input.toString();
	}

	public String getOutput() {
		return output;
	}

	public long getID() {
		return this.finishID;
	}

	public static CCHash find(String hexData) {
		for (CCHash hash : CCHash.getHashes()) {
			if (hash.getOutput().contains(hexData)) {
				return hash;
			} else if (hexData.contains(hash.getOutput())) {
				return hash;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		if (this.input == null) {
			return String.format("%d", this.finishID);
		}
		return String.format("%d %s", this.finishID, this.input.toString());
	}

	@Override
	public int hashCode() {
		return this.output.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return this.output.equals(((CCHash) obj).getOutput());
	}

	public void checkHash() {
		if (this.warnings != null) {
			return;
		}

		this.warnings = new HashSet<Warning>();

		Set<CCHash> hashes = getHashes();

		if (this.input == null || this.input.length() == 0) {
			this.warnings.add(new Warning("Empty digest input"));
		}
	}

	public Set<Warning> getWarnings() {
		if (this.warnings == null) {
			this.checkHash();
		}
		return this.warnings;
	}
}
