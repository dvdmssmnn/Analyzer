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

import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;

import dm.analyze.transitions.Transition;
import dm.util.Pair;

public class Trace {
	private TraceCall root = null;
	private Set<Pair<TraceCall, Transition>> unmergedTransitions = new HashSet<Pair<TraceCall, Transition>>();

	public TraceCall getRoot() {
		return root;
	}

	public void setRoot(TraceCall root) {
		this.root = root;
	}

	public void addUnmergedTransition(TraceCall call, Transition transition) {
		for (Pair<TraceCall, Transition> unmerged : this.unmergedTransitions) {
			if (unmerged.getFirst().equals(call)) {
				return;
			}
		}
		unmergedTransitions.add(new Pair<TraceCall, Transition>(call,
				transition));
	}

	@Override
	public String toString() {

		StringWriter s = new StringWriter();

		if (unmergedTransitions.size() > 0) {
			for (Pair<TraceCall, Transition> unmerged : unmergedTransitions) {
				s.append(String.format("%s", unmerged.getSecond().toString()));
				s.append("\n");
				s.append(unmerged.getFirst().toString());
				s.append("\n");
				s.append("---\n");
			}
			s.append("\n");
		}

		return String.format("%s%s", s.toString(), root);
	}

	public void resolveUnmerged() {
		Set<Pair<TraceCall, Transition>> remainUnmerged = new HashSet<Pair<TraceCall, Transition>>();
		for (Pair<TraceCall, Transition> unmerged : unmergedTransitions) {
			try {
				root = TraceCall.merge(root, unmerged.getFirst());
			} catch (MergeException e) {
				TraceCall caller = root.getCall(unmerged.getFirst()
						.getCallerID());
				if (caller != null) {
					caller.addCalled(unmerged.getFirst());
				} else {
					remainUnmerged.add(unmerged);
				}
			}
		}
		if (remainUnmerged.size() == unmergedTransitions.size()) {
			return;
		}
		unmergedTransitions = remainUnmerged;
		resolveUnmerged();
	}
}
