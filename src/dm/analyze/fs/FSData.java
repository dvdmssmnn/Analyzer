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

package dm.analyze.fs;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

import dm.analyze.IData;
import dm.analyze.ISearchable;
import dm.analyze.Search;
import dm.util.Strings;

public class FSData implements IData, ISearchable {

	private static Set<FSData> fsData;
	protected String path;
	protected StringBuffer dataBuffer = new StringBuffer();

	public static synchronized Set<FSData> getFSData() {
		if (fsData == null) {
			fsData = new HashSet<FSData>();

			fsData.addAll(Write.getFSData());
		}
		return fsData;
	}

	public void appendData(String hexData) {
		dataBuffer.append(hexData);
	}

	public FSData(String path) {
		this.path = path;
	}

	@Override
	public String getUTF8String() {
		if (dataBuffer.length() == 0) {
			return null;
		}
		BigInteger bigint = new BigInteger(dataBuffer.toString(), 16);
		return new String(bigint.toByteArray());
	}

	@Override
	public boolean containsData(String hexData) {
		return dataBuffer.toString().contains(hexData);
	}

	@Override
	public boolean containsUTF8String(String utf8) {
		return containsData(Strings.stringToHex(utf8));
	}

	@Override
	public String toString() {
		return String.format("Path: '%s'", this.path);
	}

	public String getPath() {
		return path;
	}

	@Override
	public boolean contains(Search search) {
		String data = this.dataBuffer.toString();
		if (data.contains(search.getPlain()) || data.contains(search.getHex())
				|| data.contains(search.getBase64())
				|| data.contains(search.getBase64URL())) {
			return true;
		}
		return false;
	}

	public static Set<FSData> search(Set<Search> searches) {
		Set<FSData> found = new HashSet<FSData>();
		for (FSData fs : getFSData()) {
			for (Search s : searches) {
				if (fs.contains(s)) {
					found.add(fs);
				}
			}
		}

		return found;
	}

}
