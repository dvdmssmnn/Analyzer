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
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import dm.analyze.transitions.Transition;
import dm.analyze.transitions.TransitionManager;
import dm.data.Call;
import dm.data.MergeException;
import dm.data.Trace;
import dm.data.TraceCall;
import dm.db.DBHelper;

public class TraceFinder {

	private static Logger log = Logger.getLogger(TraceFinder.class);

	public static TraceCall traceForward(long id, String parameterValue) {
		TraceCall call = null;
		try {
			DBHelper connection = DBHelper.getReadableConnections();
			call = new TraceCall(connection.getCall(id), parameterValue);
			// List<Call> called = DBHelper.getInstance().getCalled(id);
			List<Call> called = connection.getCalled(id, parameterValue);
			for (Call c : called) {
				if (c.hasParameterValue(parameterValue)) {
					call.addCalled(traceForward(c.getID(), parameterValue));
				}
			}
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return call;
	}

	public static TraceCall traceBackward(long id, String parameterValue) {
		TraceCall call = null;
		try {
			DBHelper connection = DBHelper.getReadableConnections();
			Call c = connection.getCall(id);
			if (c == null) {
				return null;
			}
			call = new TraceCall(c, parameterValue);
			if (call.hasParameterValue(parameterValue)) {
				TraceCall caller = traceBackward(call.getCallerID(),
						parameterValue);
				if (caller != null) {
					caller.addCalled(call);
				}
			}
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return call;
	}

	public static TraceCall traceReturn(long id, String returnValue) {
		TraceCall call = null;
		try {
			DBHelper connection = DBHelper.getReadableConnections();
			call = new TraceCall(connection.getCall(id), returnValue);
			// List<Call> called = DBHelper.getInstance().getCalled(id);
			List<Call> called = connection.getCalled(id, returnValue);
			for (Call c : called) {
				if (c.getReturnValue().equals(returnValue)) {
					call.addCalled(traceForward(c.getID(), returnValue));
				}
			}
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return call;
	}

	public static TraceCall trace(long id, String parameterValue) {
		// TraceCall fromStart = traceForward(id, parameterValue);
		TraceCall toRoot = traceBackward(id, parameterValue);

		TraceCall trace = toRoot.getRoot();

		trace = TraceCall.merge(trace,
				traceReturn(trace.getID(), parameterValue));

		return trace;
	}

	static int counter = 0;

	public static TraceCall completeTrace(Trace completeTrace, long id,
			String parameterValue, int maxDepth) {
		if (maxDepth == 0) {
			return null;
		}

		counter++;

		TraceCall trace = trace(id, parameterValue);
		if (completeTrace.getRoot() == null) {
			completeTrace.setRoot(trace);
		}

		if (maxDepth == 1) {
			return trace;
		}

		Set<Transition> transition = TransitionManager.getInstance()
				.findTransition(trace, parameterValue);
		log.debug(String.format("%d: %d %d", maxDepth, counter,
				transition.size()));

		for (Transition t : transition) {
			TraceCall call = completeTrace.getRoot()
					.getCall(t.getDst().getID());
			if (call != null && call.hasTracedValue(t.getValue())) {
				log.debug("Skip transition");
				continue;
			}
			TraceCall transitionTrace = completeTrace(completeTrace, t.getDst()
					.getID(), t.getValue(), maxDepth - 1);
			try {
				trace = TraceCall.merge(trace, transitionTrace);
				completeTrace.setRoot(trace);
			} catch (MergeException e) {
				completeTrace.addUnmergedTransition(transitionTrace, t);
			}
		}

		return trace;
	}

	public static TraceCall completeTrace(Trace completeTrace, long id,
			String parameterValue, int maxDepth,
			TransitionManager transitionManager) {
		if (maxDepth == 0) {
			return null;
		}

		counter++;

		TraceCall trace = trace(id, parameterValue);
		if (completeTrace.getRoot() == null) {
			completeTrace.setRoot(trace);
		}

		if (maxDepth == 1) {
			return trace;
		}

		Set<Transition> transition = transitionManager.findTransition(trace,
				parameterValue);
		log.debug(String.format("%d: %d %d", maxDepth, counter,
				transition.size()));

		for (Transition t : transition) {
			TraceCall call = completeTrace.getRoot()
					.getCall(t.getDst().getID());
			if (call != null && call.hasTracedValue(t.getValue())) {
				log.debug("Skip transition");
				continue;
			}
			TraceCall transitionTrace = completeTrace(completeTrace, t.getDst()
					.getID(), t.getValue(), maxDepth - 1, transitionManager);
			try {
				trace = TraceCall.merge(trace, transitionTrace);
				completeTrace.setRoot(trace);
			} catch (MergeException e) {
				completeTrace.addUnmergedTransition(transitionTrace, t);
			}
		}

		return trace;
	}
}
