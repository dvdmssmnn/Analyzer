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

package dm.analyze.transitions;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dm.data.Call;
import dm.data.Parameter;
import dm.data.TraceCall;
import dm.db.DBHelper;

public class TransitionManager {

	private static TransitionManager instance = null;

	private Set<ITransition> transitions = new HashSet<ITransition>();

	public synchronized static TransitionManager getInstance() {
		if (instance == null) {
			instance = new TransitionManager();
		}
		return instance;
	}

	public static TransitionManager defaultManager() {
		TransitionManager transitionManager = new TransitionManager();
		transitionManager.addTransition(new ITransition() {

			@Override
			public List<Transition> validTransition(TraceCall src, String value) {
				List<Transition> transitions = new ArrayList<Transition>();

				TraceCall r = src.getReturingCall(value);
				if (r != null) {

					try {
						DBHelper connection = DBHelper.getReadableConnections();
						List<Call> passed = connection.getCallsWithParameter(
								r.getSelf(), value);
						Call p2 = null;
						for (Call pass : passed) {
							if (pass.getID() < r.getID()) {
								p2 = pass;
							}
						}
						if (p2 != null) {
							transitions.add(new Transition(r, p2, value, ""));
						} else {
							transitions.add(new Transition(r, r, r.getSelf(),
									""));
							for (Parameter p : r.getParameter()) {
								if (!(p.getValue().contains("0x"))
										|| p.getValue().equals("0x0")) {
									continue;
								}
								transitions.add(new Transition(r, r, p
										.getValue(), ""));
							}
						}
						connection.close();
					} catch (SQLException e) {
						e.printStackTrace();
					}

				} else {
					try {
						DBHelper connection = DBHelper.getReadableConnections();
						List<Call> called = connection.getCalled(src.getSelf(),
								value);
						for (Call call : called) {
							if (call.getReturnValue().equals(value)
									&& src.getCall(call.getID()) == null) {
								transitions.add(new Transition(src, call,
										value, "other"));
							}
						}
						connection.close();

					} catch (SQLException e) {
						e.printStackTrace();
					}

				}

				return transitions;
			}
		});
		return transitionManager;
	}

	public TransitionManager() {

	}

	public void addTransition(ITransition transition) {
		this.transitions.add(transition);
	}

	public Set<Transition> findTransition(TraceCall src, String value) {
		Set<Transition> transitions = new HashSet<Transition>();
		for (ITransition transition : this.transitions) {
			transitions.addAll(transition.validTransition(src, value));

		}
		return transitions;
	}

}
