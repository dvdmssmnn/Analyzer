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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import dm.analyze.transitions.Transition;
import dm.analyze.transitions.TransitionManager;
import dm.data.Call;
import dm.data.Parameter;
import dm.data.TraceCall;
import dm.db.DBHelper;

public class TraceGenerator {
	private static Logger log = Logger.getLogger(TraceGenerator.class);

	public static interface ICriterion<E> {
		boolean fulfillsCriterion(E element);
	}

	public static Set<TraceCall> getTraces(String clazz, String method,
			int param, Set<Transition> transitions) {
		List<Call> callsSrc = null;
		try {
			DBHelper connection = DBHelper.getReadableConnections();
			callsSrc = connection.findCall(null, clazz, method, null, null,
					null, null);
			Set<TraceCall> traces = new HashSet<TraceCall>();
			for (Call call : callsSrc) {
				traces.addAll(getTraces(call.getID(), param, transitions));
			}
			log.debug(traces.toString());
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		if (callsSrc == null) {
			return null;
		}

		return null;
	}

	public static Set<TraceCall> getTraces(long id, int param,
			Set<Transition> transitions) {
		Call startCall;
		try {
			DBHelper connection = DBHelper.getReadableConnections();
			startCall = connection.getCall(id);
			final String value = param < 0 ? startCall.getReturnValue()
					: startCall.getParameter().get(param).getValue();
			connection.close();
			return getTraces(id, value, transitions);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Set<TraceCall> getTraces(long id, String value,
			Set<Transition> transitions) {
		try {
			DBHelper connection = DBHelper.getReadableConnections();
			Call startCall = connection.getCall(id);
			log.debug(String.format("Get Traces: %s %s %s",
					startCall.getClazz(), startCall.getMethod(), value));
			TraceCall startTraceCall = new TraceCall(startCall);

			Set<TraceCall> tracesFromStart = new HashSet<TraceCall>();
			tracesFromStart.add(startTraceCall);
			traceForward(startTraceCall, new ICriterion<Call>() {

				@Override
				public boolean fulfillsCriterion(Call element) {
					if (element.getReturnValue().equals(value)) {
						return true;
					}
					for (Parameter p : element.getParameter()) {
						if (p.getValue().equals(value)) {
							return true;
						}
					}
					return false;
				}
			});

			TraceCall backtrace = new TraceCall(startCall);
			traceBackward(backtrace, new ICriterion<Call>() {

				@Override
				public boolean fulfillsCriterion(Call element) {
					if (element.getSelf().equals(value)) {
						return true;
					}
					if (element.getReturnValue().equals(value)) {
						return true;
					}
					for (Parameter p : element.getParameter()) {
						if (p.getValue().equals(value)) {
							return true;
						}
					}
					return false;
				}
			});
			backtrace = backtrace.getRoot();

			Call root = connection.getCall(backtrace.getRoot().getCallerID());
			if (root != null) {
				TraceCall backtraceRoot = new TraceCall(root);
				backtraceRoot.addCalled(backtrace);
				backtrace.setRoot(backtraceRoot);
				backtrace = backtraceRoot;
			}

			for (TraceCall traceCall : tracesFromStart) {
				TraceCall backtraceCopy = backtrace.getRoot().copy();
				TraceCall backtraceTop = backtraceCopy.getCall(traceCall
						.getCallerID());
				if (backtraceTop == null) {
					log.warn("Cannot find top of backtrace that matches the trace");
					continue;
				}
				backtraceCopy.removeCall(traceCall.getID());
				backtraceTop.addCalled(traceCall);
				traceCall.setRoot(backtraceCopy);
			}

			Set<TraceCall> tracesFromRoot = new HashSet<TraceCall>();
			TraceCall tracesFromRootStart = new TraceCall(backtrace);
			tracesFromRoot.add(tracesFromRootStart);
			traceForward(tracesFromRootStart, new ICriterion<Call>() {

				@Override
				public boolean fulfillsCriterion(Call element) {
					for (TraceCall call : tracesFromStart) {
						if (call.getRoot().getCall(element.getID()) != null) {
							return false;
						}
					}
					if (element.getReturnValue().equals(value)) {
						return true;
					}
					for (Parameter p : element.getParameter()) {
						if (p.getValue().equals(value)) {
							return true;
						}
					}
					return false;
				}
			});

			Set<TraceCall> traces = new HashSet<TraceCall>();
			for (TraceCall traceCall1 : tracesFromStart) {
				for (TraceCall traceCall2 : tracesFromRoot) {
					TraceCall t = traceCall1.getRoot().copy(null);
					t.merge(traceCall2.getRoot());
					t.toString();
					traces.add(t);
				}
			}

			List<TraceCall> toRemove = new ArrayList<TraceCall>();
			List<TraceCall> toAdd = new ArrayList<TraceCall>();

			log.debug(traces);

			for (TraceCall trace : traces) {
				// for (TraceCall call : trace.getAllCalls()) {
				TraceCall call = trace.getRoot();

				List<TraceCall> newTraces = findTransitions(call, value,
						transitions);

				if (newTraces.size() > 0) {
					toRemove.add(trace);
					for (TraceCall n : newTraces) {
						call = TraceCall.merge(call, n);
					}
					toAdd.add(call);
				}

				// if (newTraces.size() == 1) {
				// TraceCall n = newTraces.get(0);
				// n = TraceCall.merge(n, trace);
				// toRemove.add(trace);
				// toAdd.add(n.getRoot());
				// } else if (newTraces.size() > 0) {
				// toRemove.add(trace);
				// for (TraceCall newTrace : newTraces) {
				// // newTrace = newTrace.getCall(call.getID());
				// newTrace = TraceCall.merge(newTrace, trace);
				// newTrace = newTrace.getRoot();
				// toAdd.add(newTrace.getRoot());
				//
				// }
				// }
				// }
			}

			traces.removeAll(toRemove);
			traces.addAll(toAdd);

			connection.close();
			return traces;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static void traceForward(TraceCall currentTrace,
			ICriterion<Call> criterion) {

		try {
			DBHelper connection = DBHelper.getReadableConnections();
			List<Call> called = connection.getCalled(currentTrace.getID());
			List<Call> filtered = new ArrayList<Call>();
			for (Call call : called) {
				if (criterion.fulfillsCriterion(call)) {
					filtered.add(call);
				}
			}

			for (int i = 0; i < filtered.size(); ++i) {
				Call call = filtered.get(i);
				TraceCall traceCall = new TraceCall(call);
				// if (i == (filtered.size() - 1)) {
				traceCall.setRoot(currentTrace.getRoot());
				traceCall.getRoot().toString();
				currentTrace.addCalled(traceCall);
				traceForward(traceCall, criterion);
				// } else {
				// TraceCall currentTraceCopy = currentTrace.getRoot()
				// .copy(null).getCall(currentTrace.getID());
				// traceCall.setRoot(currentTraceCopy.getRoot());
				// currentTraceCopy.addCalled(traceCall);
				// traces.add(currentTraceCopy.getRoot());
				// traceForward(traces, traceCall, criterion);
				// }
			}
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	private static void traceBackward(TraceCall trace,
			ICriterion<Call> criterion) {
		try {
			DBHelper connection = DBHelper.getReadableConnections();
			Call calledBy = connection.getCall(trace.getCallerID());
			if (calledBy == null || !criterion.fulfillsCriterion(calledBy)) {
				return;
			}
			TraceCall traceCall = new TraceCall(calledBy);
			traceCall.addCalled(trace);
			trace.setRoot(traceCall);
			traceBackward(traceCall, criterion);
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private static List<TraceCall> findTransitions(TraceCall call,
			String value, Set<Transition> transitions) {
		log.debug(String.format("Find transitions from %s %s with value %s",
				call.getClazz(), call.getMethod(), value));
		List<TraceCall> traces = new ArrayList<TraceCall>();
		for (Transition transition : TransitionManager.getInstance()
				.findTransition(call, value)) {
			if (!transitions.contains(transition)) {
				log.debug(String.format("Follow transition %s",
						transition.toString()));
				transitions.add(transition);
				Set<TraceCall> newTraces = getTraces(transition.getDst()
						.getID(), transition.getValue(), transitions);
				traces.addAll(newTraces);
			}
		}
		return traces;
	}
}
