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

package dm.data;

import java.util.List;

public class Call {
	private long ID;
	private long callerID;

	private String clazz;
	private String method;
	private String self;
	private String returnType;
	private String returnValue;
	private String returnDescription;
	private List<Parameter> parameter;

	public Call(long iD, long callerID, String clazz, String method,
			String self, String returnType, String returnValue,
			String returnDescription, List<Parameter> parameter) {
		super();
		ID = iD;
		this.callerID = callerID;
		this.clazz = clazz;
		this.method = method;
		this.self = self;
		this.returnType = returnType;
		this.returnValue = returnValue;
		this.returnDescription = returnDescription;
		this.parameter = parameter;
	}

	public long getID() {
		return ID;
	}

	public long getCallerID() {
		return callerID;
	}

	public String getClazz() {
		return clazz;
	}

	public String getMethod() {
		return method;
	}

	public String getSelf() {
		return self;
	}

	public String getReturnType() {
		return returnType;
	}

	public String getReturnValue() {
		return returnValue;
	}

	public String getReturnDescription() {
		return returnDescription;
	}

	public List<Parameter> getParameter() {
		return parameter;
	}

	@Override
	public String toString() {
		return String.format("%s %s(%d: %d)\n\t%s", clazz, method, ID,
				callerID, parameter);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Call)) {
			return false;
		}
		Call other = (Call) obj;
		if (ID == other.getID()) {
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return (int) ID;
	}

	public boolean hasParameterValue(String parameterValue) {
		if (returnValue.equals(parameterValue)) {
			return true;
		}
		if (self.equals(parameterValue)) {
			return true;
		}
		for (Parameter p : parameter) {
			if (p.getValue().equals(parameterValue)) {
				return true;
			}
		}
		return false;
	}
}
