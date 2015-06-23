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

package dm.analyze.cipher;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import dm.analyze.TraceFinder;
import dm.analyze.constants.Constant;
import dm.analyze.hash.CCHash;
import dm.analyze.hash.FoundInHashWarning;
import dm.analyze.keys.SymmetricKey;
import dm.analyze.random.SecureRandomBytes;
import dm.analyze.warningns.Warning;
import dm.data.Call;
import dm.data.Parameter;
import dm.data.Trace;
import dm.db.DBHelper;
import dm.util.Strings;

public class CCCryptorCreateWithMode extends SymmetricCipher {

	private static final Logger log = Logger
			.getLogger(CCCryptorCreateWithMode.class);

	private enum CCOperation {
		kCCEncrypt(0), kCCDecrypt(1);

		@SuppressWarnings("unused")
		private final int numVal;

		CCOperation(int numVal) {
			this.numVal = numVal;
		}
	}

	private enum CCMode {
		kCCModeECB(1), kCCModeCBC(2), kCCModeCFB(3), kCCModeCTR(4), kCCModeF8(5), kCCModeLRW(
				6), kCCModeOFB(7), kCCModeXTS(8), kCCModeRC4(9), kCCModeCFB8(10);
		private final int numVal;

		private CCMode(int numVal) {
			this.numVal = numVal;
		}
	}

	private enum CCAlgorithm {
		kCCAlgorithmAES(0), kCCAlgorithmDES(1), kCCAlgorithm3DES(2), kCCAlgorithmCAST(
				3), kCCAlgorithmRC4(4), kCCAlgorithmRC2(5), kCCAlgorithmBlowfish(
				6);

		@SuppressWarnings("unused")
		private final int numVal;

		private CCAlgorithm(int numVal) {
			this.numVal = numVal;
		}
	}

	private enum CCPadding {
		ccNoPadding(0), ccPKCS7Padding(1);

		@SuppressWarnings("unused")
		private final int numVal;

		private CCPadding(int numVal) {
			this.numVal = numVal;
		}
	}

	private enum CCModeOptions {
		kCCModeOptionCTR_LE(0x0001), kCCModeOptionCTR_BE(0x0002);

		@SuppressWarnings("unused")
		private final int numVal;

		private CCModeOptions(int numVal) {
			this.numVal = numVal;
		}
	}

	private static Set<CCCryptorCreateWithMode> ciphers = null;

	private int op;
	private int mode;
	private int alg;
	private int padding;
	private Parameter iv;
	private Parameter key;
	private String tweak;
	private int numRounds;
	private int options;
	private String cryptorRef;

	private Object lock = new Object();
	private boolean initialized = false;

	public static synchronized Set<CCCryptorCreateWithMode> getCipher() {
		if (ciphers == null) {
			ciphers = new HashSet<CCCryptorCreateWithMode>();
			try {
				DBHelper connection = DBHelper.getReadableConnections();
				List<Call> calls = connection.findCall(null, "CommonCrypto",
						"CCCryptorCreateWithMode", null, null, null, null);
				log.debug(String.format(
						"Found %d calls to 'CCCryptorCreateWithMode'",
						calls.size()));
				for (Call call : calls) {
					CCCryptorCreateWithMode cipher = new CCCryptorCreateWithMode(
							call);
					if (!cipher.isSSLCipher()) {
						ciphers.add(cipher);
					}

				}
				connection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			log.debug(String.format("Set contains %d ciphers", ciphers.size()));
		}
		return ciphers;
	}

	public CCCryptorCreateWithMode(Call call) {
		this.callID = call.getID();
		this.callerID = call.getCallerID();
		this.op = Integer.valueOf(call.getParameter().get(0).getValue());
		this.mode = Integer.valueOf(call.getParameter().get(1).getValue());
		this.alg = Integer.valueOf(call.getParameter().get(2).getValue());
		this.padding = Integer.valueOf(call.getParameter().get(3).getValue());
		this.iv = call.getParameter().get(4);
		this.key = call.getParameter().get(5);
		this.tweak = call.getParameter().get(7).getDescription();
		this.numRounds = Integer.valueOf(call.getParameter().get(9).getValue());
		this.options = Integer.valueOf(call.getParameter().get(10).getValue());
		this.cryptorRef = call.getParameter().get(11).getValue();

		new Thread(new Runnable() {

			@Override
			public void run() {
				synchronized (lock) {
					checkCryptorRef();
					setData();
					initialized = true;
					lock.notifyAll();
				}
			}
		}).start();

	}

	public synchronized void checkCipher() {
		this.waitForInit();
		if (this.warnings != null) {
			return;
		}
		log.debug(String.format("Check cipher with ID %d", this.callID));
		this.warnings = new HashSet<Warning>();

		if (this.op == CCOperation.kCCEncrypt.numVal) {
			if (this.mode == CCMode.kCCModeECB.numVal)
				this.warnings.add(new Warning("ECB mode used for encryption"));
			else if (this.iv.getDescription() == null
					|| this.iv.equals(Strings.repeat("00", getBlockSize()))) {
				this.warnings.add(new Warning("Null IV used"));
			} else if (SecureRandomBytes.find(this.iv.getDescription()) == null) {
				CCHash h = CCHash.find(this.iv.getDescription());
				if (h == null) {
					Trace t = new Trace();
					TraceFinder
							.completeTrace(t, callID, this.iv.getValue(), -1);
					this.warnings.add(new Warning("IV not random").setValue(
							Warning.TRACE, t).setValue(Warning.DATA,
							this.iv.getDescription()));
				} else {
					this.warnings.add(new FoundInHashWarning(
							"IV found in digest", h));
				}
			}
		}

		// Check key
		Constant constantKey = Constant.find(this.key.getDescription());
		if (constantKey != null) {
			this.warnings.add(new Warning(String.format("%s",
					Strings.hexToBytes(constantKey.getHexValue())),
					"Constant key used"));
		} else {
			SymmetricKey key = null;
			for (SymmetricKey k : SymmetricKey.getKeys()) {
				if (k.getKey().equals(this.key.getDescription())) {
					key = k;
					break;
				}
			}
			if (key == null) {
				CCHash h = CCHash.find(this.key.getDescription());
				if (h != null) {
					this.warnings.add(new FoundInHashWarning(String.format(
							"Key found in digest %d", h.getID()), h));
				} else {
					SymmetricCipher output = null;
					for (SymmetricCipher c : SymmetricCipher.getCiphers()) {
						if (c.getCipherTextData().contains(
								this.key.getDescription())
								|| c.getPlaintextData().contains(
										this.key.getDescription())) {
							output = c;
							break;
						}
					}
					if (output != null) {
						this.warnings.add(new Warning(String.format("%s",
								output.getPlaintextData()), String.format(
								"Found key in other cipher %d",
								output.getCallID())));
					} else {
						Trace t = new Trace();
						TraceFinder.completeTrace(t, callID,
								this.key.getValue(), -1);
						this.warnings.add(new Warning(
								String.format("Can't find key: %s",
										this.key.getDescription())).setValue(
								Warning.TRACE, t));
					}
				}
			} else {
				key.checkKey();
				this.warnings.addAll(key.getWarnings());
			}
		}
	}

	public String getCallDescription() {
		return "TODO";
	}

	public synchronized Set<Warning> getWarnings() {
		if (this.warnings == null) {
			this.checkCipher();
		}
		return this.warnings;
	}

	public void printWarnings() {

	}

	private int getBlockSize() {
		if (this.alg == CCAlgorithm.kCCAlgorithmAES.numVal) {
			return 16;
		}
		return 0;
	}

	private void checkCryptorRef() {
		try {
			DBHelper connection = DBHelper.getReadableConnections();
			Call call = connection.getCall(this.callID);
			Call caller = connection.getCall(call.getCallerID());
			if (caller != null && caller.getMethod().equals("CCCryptorCreate")) {
				caller = connection.getCall(caller.getCallerID());
				if (caller != null
						&& caller.getMethod().equals("CCCryptorCreateFromData")) {
					this.cryptorRef = caller.getParameter().get(8).getValue();
				}
			}
			connection.close();
		} catch (SQLException exception) {
			exception.printStackTrace();
		}
	}

	private void setData() {
		try {
			DBHelper connection = DBHelper.getReadableConnections();
			List<Call> finalCalls = connection.findCall(null, "CommonCrypto",
					"CCCryptorFinal", null, null, cryptorRef, null);
			Call finalCall = null;
			for (Call c : finalCalls) {
				if (c.getID() > this.callID) {
					finalCall = c;
					break;
				}
			}

			List<Call> updateCalls = connection.findCall(null, "CommonCrypto",
					"CCCryptorUpdate", null, null, cryptorRef, null);

			for (Call update : updateCalls) {
				if (update.getID() > this.callID
						&& (finalCall == null || update.getID() < finalCall
								.getID())) {
					if (this.op == CCOperation.kCCEncrypt.numVal) {
						plaintextBuffer.append(update.getParameter().get(1)
								.getDescription());
						ciphertextBuffer.append(update.getParameter().get(3)
								.getDescription());
					} else {
						plaintextBuffer.append(update.getParameter().get(3)
								.getDescription());
						ciphertextBuffer.append(update.getParameter().get(1)
								.getDescription());
					}
				}
			}

			if (finalCall != null) {
				if (this.op == CCOperation.kCCEncrypt.numVal) {
					ciphertextBuffer.append(finalCall.getParameter().get(1)
							.getDescription());
				} else {
					plaintextBuffer.append(finalCall.getParameter().get(1)
							.getDescription());
				}
			}

			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String toString() {
		this.waitForInit();
		StringBuffer buffer = new StringBuffer();
		buffer.append(String.format("%d ", callID));
		if (op == CCOperation.kCCDecrypt.numVal) {
			buffer.append("Decrypt: ");
		} else {
			buffer.append("Encrypt: ");
		}

		buffer.append(String.format("Plaintext: %s", this.getPlaintextData()));
		return buffer.toString();
	}

	@Override
	public boolean containsPlaintext(String string) {
		this.waitForInit();
		return super.containsPlaintext(string);
	}

	@Override
	public boolean containsPlaintextData(String data) {
		this.waitForInit();
		return super.containsPlaintextData(data);
	}

	@Override
	public String getPlaintextData() {
		this.waitForInit();
		return super.getPlaintextData();
	}

	@Override
	public String getCipherTextData() {
		this.waitForInit();
		return super.getCipherTextData();
	}

	private void waitForInit() {
		synchronized (lock) {
			try {
				while (!initialized) {
					lock.wait();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public Operation getOperation() {
		if (this.op == CCOperation.kCCDecrypt.numVal) {
			return Operation.Decrypt;
		}
		return Operation.Encrypt;
	}

	@Override
	public boolean isSSLCipher() {
		DBHelper connection;
		try {
			connection = DBHelper.getReadableConnections();
			Call create = connection.getCall(this.callerID);
			if (create.getClazz().equals("CommonCrypto")
					&& create.getMethod().equals("CCCryptorCreate")) {
				Call sslHandshake = connection.getCall(create.getCallerID());
				if (sslHandshake.getMethod().equals("SSLHandshake")) {
					return true;
				}
			}
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return super.isSSLCipher();
	}

	@Override
	public int hashCode() {
		return getPlaintextData().hashCode() + getCipherTextData().hashCode()
				+ mode;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof CCCryptorCreateWithMode)) {
			return false;
		}
		CCCryptorCreateWithMode other = (CCCryptorCreateWithMode) obj;
		if (!getPlaintextData().equals(other.getPlaintextData())
				|| !getCipherTextData().equals(other.getCipherTextData())) {
			return false;
		}
		return true;
	}

}
