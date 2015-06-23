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

import java.util.HashSet;
import java.util.Set;

import dm.analyze.TraceGenerator;
import dm.analyze.transitions.CopyTransition;
import dm.analyze.transitions.MethodTransition;
import dm.analyze.transitions.ReturnTransition;
import dm.analyze.transitions.Transition;
import dm.analyze.transitions.TransitionManager;
import dm.analyze.transitions.ValueOfObjectTransition;
import dm.data.TraceCall;
import dm.db.DBHelper;

public class AnalyzerOffline {

	public static void main(String[] args) {
		String filename = "tmp.sqlite";
		DBHelper.setDBPath(filename);

		TransitionManager.getInstance().addTransition(
				new MethodTransition("bytes"));

		TransitionManager.getInstance().addTransition(new CopyTransition());
		TransitionManager.getInstance().addTransition(
				new ValueOfObjectTransition());
		TransitionManager.getInstance().addTransition(new ReturnTransition());

		// TraceGenerator.getTraces(296831, 0);
		// TraceGenerator.getTraces("UITextField", "text", -1);
		Set<Transition> transitions = new HashSet<Transition>();
		Set<TraceCall> traces = TraceGenerator.getTraces("CommonCrypto",
				"CCCryptorCreate", 3, transitions);
		// TraceGenerator.getTraces(297146, "0x16257270", transitions);

	}

}
