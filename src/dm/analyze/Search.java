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

package dm.analyze;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;

import dm.util.Strings;

public class Search {
	private String plain;
	private String hex;
	private String base64;
	private String base64URL;

	public Search() {

	}

	public Search(String plain) {
		setPlain(plain);
	}

	public String getPlain() {
		return plain;
	}

	public void setPlain(String plain) {
		setHex(Strings.stringToHex(plain));
	}

	public String getHex() {
		return hex;
	}

	public void setHex(String hex) {
		this.hex = hex;
		this.plain = Strings.hexToString(hex);
		this.base64 = Strings.stringToHex(Base64.encodeBase64String(Strings
				.hexToBytes(hex)));
		this.base64URL = Strings.stringToHex(Base64
				.encodeBase64String(Strings.hexToBytes(hex))
				.replace("+", "%2B").replace("/", "%2F").replace("=", "%3D"));
	}

	public String getBase64() {
		return base64;
	}

	public String getBase64URL() {
		return base64URL;
	}

	public Set<String> getEncoding() {
		Set<String> encodings = new HashSet<String>();
		encodings.add(base64);
		encodings.add(base64URL);
		encodings.add(hex);
		encodings.add(plain);
		return encodings;
	}

	@Override
	public int hashCode() {
		return this.hex.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return this.hex.equals(((Search) obj).getHex());
	}

}
