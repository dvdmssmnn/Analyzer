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

import java.util.HashSet;
import java.util.Set;

import dm.analyze.ISearchable;
import dm.analyze.Search;
import dm.util.Strings;

public class SecItem implements ISearchable {
	private static Set<SecItem> items;

	private long callID;
	protected String attributes;

	public static synchronized Set<SecItem> getItems() {
		if (items == null) {
			items = new HashSet<SecItem>();

			items.addAll(SecItemAdd.getItems());
			items.addAll(SecItemUpdate.getItems());
		}
		return items;
	}

	public SecItem(long callID, String attributes) {
		this.callID = callID;
		this.attributes = attributes;
	}

	public boolean contains(String hexData) {
		// Strings are in utf8 and data in hex, so we have to check for both
		String plainData = Strings.hexToString(hexData);

		if (this.attributes.contains(plainData)) {
			return true;
		}

		if (this.attributes.replace(" ", "").contains(hexData)) {
			return true;
		}

		return false;
	}

	@Override
	public boolean contains(Search search) {
		return this.contains(search.getHex());
	}

	public long getCallID() {
		return callID;
	}

	public void setCallID(long callID) {
		this.callID = callID;
	}

}
