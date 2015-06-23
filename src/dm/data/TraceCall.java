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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

public class TraceCall extends Call {
	private static Logger log = Logger.getLogger(TraceCall.class);

	private List<TraceCall> called = new ArrayList<TraceCall>();
	private TraceCall root = null;
	private Set<String> tracedValues = new HashSet<String>();

	@Deprecated
	public TraceCall(Call call) {
		super(call.getID(), call.getCallerID(), call.getClazz(), call
				.getMethod(), call.getSelf(), call.getReturnType(), call
				.getReturnValue(), call.getReturnDescription(), call
				.getParameter());
		this.root = this;
	}

	public TraceCall(Call call, String parameterValue) {
		super(call.getID(), call.getCallerID(), call.getClazz(), call
				.getMethod(), call.getSelf(), call.getReturnType(), call
				.getReturnValue(), call.getReturnDescription(), call
				.getParameter());
		this.root = this;
		this.tracedValues.add(parameterValue);
	}

	public TraceCall(Call call, Set<String> tracedValues) {
		super(call.getID(), call.getCallerID(), call.getClazz(), call
				.getMethod(), call.getSelf(), call.getReturnType(), call
				.getReturnValue(), call.getReturnDescription(), call
				.getParameter());
		this.root = this;
		this.tracedValues = new HashSet<String>(tracedValues);
	}

	public TraceCall(TraceCall call, TraceCall root) {
		super(call.getID(), call.getCallerID(), call.getClazz(), call
				.getMethod(), call.getSelf(), call.getReturnType(), call
				.getReturnValue(), call.getReturnDescription(), call
				.getParameter());
		this.root = root;
		this.tracedValues = new HashSet<String>(call.tracedValues);
		for (TraceCall tc : call.getCalled()) {
			addCalled(new TraceCall(tc, root));
		}
	}

	public List<TraceCall> getCalled() {
		return called;
	}

	public TraceCall getRoot() {
		return root;
	}

	public void setRoot(TraceCall root) {
		this.root = root;
		for (TraceCall call : called) {
			call.setRoot(root);
		}
	}

	public void addCalled(TraceCall call) {
		called.add(call);
		call.setRoot(root);
		Collections.sort(called, new Comparator<TraceCall>() {

			@Override
			public int compare(TraceCall o1, TraceCall o2) {
				// TODO: check to keep the order of dummy calls (they have no
				// valid ID)
				// if (o1 instanceof TransitionTraceCall
				// || o2 instanceof TransitionTraceCall) {
				// return 0;
				// }
				long d = o1.getID() - o2.getID();
				return d < 0 ? -1 : (d > 0 ? 1 : 0);
			}
		});
	}

	public TraceCall copy(TraceCall root) {
		TraceCall copy = new TraceCall(this, tracedValues);
		if (root != null) {
			copy.setRoot(root);
		} else {
			copy.setRoot(copy);
		}
		for (TraceCall call : called) {
			copy.addCalled(call.copy(copy));
		}
		return copy;
	}

	public TraceCall copy() {
		return copy(null);
	}

	public TraceCall getCall(long id) {
		if (getID() == id) {
			return this;
		} else {
			for (TraceCall call : called) {
				TraceCall c = call.getCall(id);
				if (c != null) {
					return c;
				}
			}
		}
		return null;
	}

	public void removeCall(long id) {
		Set<TraceCall> toRemove = new HashSet<TraceCall>();
		for (TraceCall call : called) {
			if (call.getID() == id) {
				toRemove.add(call);
			} else {
				call.removeCall(id);
			}
		}
		called.removeAll(toRemove);
	}

	public void merge(TraceCall call) throws MergeException {
		if (getCall(call.getID()) == null) {
			if (getCall(call.getCallerID()) != null) {
				getCall(call.getCallerID()).addCalled(
						new TraceCall(call, (TraceCall) null));
				return;
			}
			throw new MergeException(this, call);
		}
		if (getID() == call.getID()) {
			tracedValues.addAll(call.tracedValues);
			for (TraceCall tc : call.getCalled()) {
				if (getCall(tc.getID()) == null) {
					TraceCall m = tc.copy();
					m.setRoot(getRoot());
					addCalled(m);
				} else {
					getCall(tc.getID()).merge(tc);
				}
			}
		} else {
			getCall(call.getID()).merge(call);
		}

	}

	@Override
	public String toString() {
		return toString(0);
	}

	public String toString(int level) {
		String s = "";
		for (int i = 0; i < level; ++i) {
			s += " ";
		}
		s += String
				.format("%d-%d(%d): %s %s (%s)", level, getID(), getCallerID(),
						getClazz(), getMethod(), tracedValues.toString());
		for (TraceCall call : called) {
			s += String.format("\n%s", call.toString(level + 1));
		}
		return s;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof TraceCall)) {
			return false;
		}
		TraceCall other = (TraceCall) obj;
		return toString().equals(other.toString());
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	public List<TraceCall> getAllCalls() {
		List<TraceCall> calls = new ArrayList<TraceCall>();
		addCalls(calls);
		return calls;
	}

	private void addCalls(List<TraceCall> calls) {
		calls.add(this);
		for (TraceCall call : called) {
			call.addCalls(calls);
		}
	}

	public static TraceCall merge(TraceCall a, TraceCall b)
			throws MergeException {
		if (a == null) {
			return b;
		} else if (b == null) {
			return a;
		}
		if (a.getID() < b.getID()) {
			a.merge(b.copy());
			return a;
		}
		b.merge(a.copy());
		return b;
	}

	public static TraceCall merge(List<TraceCall> calls) throws MergeException {
		if (calls.size() == 0) {
			return null;
		}
		TraceCall c = calls.get(0);
		for (int i = 1; i < calls.size(); ++i) {
			c = TraceCall.merge(c, calls.get(i));
		}
		return c;
	}

	public boolean hasTracedValue(String value) {
		return tracedValues.contains(value);
	}

	public TraceCall getReturingCall(String value) {
		if (!hasTracedValue(value)) {
			return null;
		}
		for (TraceCall call : called) {
			if (call.getReturnValue().equals(value)) {
				return call.getReturingCall(value);
			}
		}
		if (getReturnValue().equals(value)) {
			return this;
		}
		return null;
	}

	public List<TraceCall> findCalls(String clazz, String method) {
		List<TraceCall> calls = new ArrayList<TraceCall>();
		findCalls(calls, clazz, method);
		return calls;
	}

	private void findCalls(List<TraceCall> calls, String clazz, String method) {
		if (getClazz().equals(clazz) && getMethod().equals(method)) {
			calls.add(this);
		}

		for (TraceCall called : this.called) {
			called.findCalls(calls, clazz, method);
		}
	}
}
