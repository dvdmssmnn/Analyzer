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

import java.util.HashSet;
import java.util.Set;

import dm.analyze.CipherSearch;
import dm.analyze.Search;
import dm.analyze.warningns.Warning;

public abstract class SymmetricCipher {

	public enum Operation {
		Encrypt, Decrypt
	}

	private static Set<SymmetricCipher> ciphers = null;

	protected Set<Warning> warnings;
	protected StringBuffer plaintextBuffer = new StringBuffer();
	protected StringBuffer ciphertextBuffer = new StringBuffer();

	protected long callID;
	protected long callerID;

	public static synchronized Set<SymmetricCipher> getCiphers() {
		if (ciphers == null) {
			ciphers = new HashSet<SymmetricCipher>();

			// ciphers.addAll(CCCryptorCreate.getCipher());
			ciphers.addAll(CCCryptorCreateWithMode.getCipher());

			new Thread(new Runnable() {

				@Override
				public void run() {
					for (SymmetricCipher cipher : ciphers) {
						cipher.checkCipher();
					}
				}
			}).start();
		}

		return ciphers;
	}

	public static Set<SymmetricCipher> findWarnings(Set<SymmetricCipher> ciphers) {
		Set<SymmetricCipher> warningCiphers = new HashSet<SymmetricCipher>();
		for (SymmetricCipher cipher : getCiphers()) {
			if (cipher.getWarnings() != null) {
				warningCiphers.add(cipher);
			}
		}
		return warningCiphers;
	}

	public Set<Warning> getWarnings() {
		return this.warnings;
	}

	public void checkCipher() {
		this.warnings = new HashSet<Warning>();
	}

	public boolean containsPlaintext(String hexData) {
		return this.containsPlaintextData(hexData);
	}

	public boolean containsPlaintextData(String data) {
		return this.plaintextBuffer.toString().contains(data);
	}

	public String getPlaintextData() {
		return this.plaintextBuffer.toString();
	}

	public String getCipherTextData() {
		return this.ciphertextBuffer.toString();
	}

	public abstract Operation getOperation();

	public static Set<SymmetricCipher> getCiphersContaingPlaintext(
			String hexData, boolean encryptOnly) {
		Set<SymmetricCipher> ciphers = new HashSet<SymmetricCipher>();
		for (SymmetricCipher cipher : getCiphers()) {
			if ((encryptOnly && cipher.getOperation() == Operation.Encrypt && cipher
					.containsPlaintext(hexData))
					|| (!encryptOnly && cipher.containsPlaintext(hexData))) {
				ciphers.add(cipher);
			}
		}
		return ciphers;
	}

	public static Set<Search> getSearches(String hexData, boolean encryptOnly) {
		Search search = new Search();
		search.setHex(hexData);
		Set<Search> toSearch = new HashSet<Search>();
		toSearch.add(search);
		for (SymmetricCipher cipher : SymmetricCipher
				.getCiphersContaingPlaintext(search.getHex(), true)) {

			Search s = new CipherSearch(cipher);
			s.setHex(cipher.getCipherTextData());
			toSearch.add(s);

		}
		return toSearch;
	}

	public boolean isSSLCipher() {
		return false;
	}

	public long getCallID() {
		return callID;
	}

	public void setCallID(long callID) {
		this.callID = callID;
	}

	public long getCallerID() {
		return callerID;
	}

	public void setCallerID(long callerID) {
		this.callerID = callerID;
	}

}
