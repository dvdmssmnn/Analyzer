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

package dm.analyze.constants;

import java.math.BigInteger;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dm.data.Call;
import dm.data.Parameter;
import dm.db.DBHelper;

public class ConstantString extends Constant {

	public static Set<Constant> getConstants() {
		Set<Constant> constants = new HashSet<Constant>();
		try {
			DBHelper connection = DBHelper.getReadableConnections();
			List<Call> constantParams = connection.findCall(null, null, null,
					null, "__NSCFConstantString", null, null);
			for (Call c : constantParams) {
				if (c.getReturnType().equals("__NSCFConstantString")) {
					constants.add(new ConstantString(c.getReturnDescription()));
				}

				for (Parameter p : c.getParameter()) {
					if (p.getType().equals("__NSCFConstantString")) {
						constants.add(new ConstantString(p.getDescription()));
					}
				}
			}
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return constants;
	}

	private String constant;
	private String hexValue;

	public ConstantString(String constant) {
		this.constant = constant;
		this.hexValue = String.format("%x",
				constant.length() > 0 ? new BigInteger(constant.getBytes())
						: null);
	}

	@Override
	public int hashCode() {
		return this.constant.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return this.constant.equals(((ConstantString) obj).getConstant());
	}

	public String getConstant() {
		return this.constant;
	}

	@Override
	public boolean equals(String hexValue) {
		return this.hexValue.equals(hexValue);
	}

	@Override
	public String getHexValue() {
		return this.hexValue;
	}

	@Override
	public String toString() {
		return String.format("%s (String)", this.constant);
	}
}
