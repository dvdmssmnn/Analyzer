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

package dm.analyze.warningns;

import java.util.HashMap;
import java.util.Map;

public class Warning implements IWarning {

	public static final String DATA = "data";
	public static final String TRACE = "trace";
	public static final String SHORT_INFO = "short_info";
	public static final String INFO = "info";

	private Map<String, Object> values = new HashMap<String, Object>();

	public Warning(String info) {
		this.setValue(INFO, info);
		this.setValue(SHORT_INFO, info);
	}

	public Warning(String info, String shortInfo) {
		this.setValue(INFO, info);
		this.setValue(SHORT_INFO, shortInfo);
	}

	@Override
	public Warning setValue(String key, Object value) {
		this.values.put(key, value);
		return this;
	}

	@Override
	public Object getValue(String key) {
		return this.values.get(key);
	}

	@Override
	public String toString() {
		return this.values.toString();
	}

	@Override
	public int hashCode() {
		return this.values.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		return this.values.equals(((Warning) obj).values);
	}

}
