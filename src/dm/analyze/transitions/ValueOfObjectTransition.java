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
import java.util.List;

import dm.data.Call;
import dm.data.ClassManager;
import dm.data.Parameter;
import dm.data.TraceCall;
import dm.db.DBHelper;

public class ValueOfObjectTransition implements ITransition {

	@Override
	public List<Transition> validTransition(TraceCall src, String value) {
		List<Transition> transitions = new ArrayList<Transition>();
		try {
			DBHelper connection = DBHelper.getReadableConnections();
			List<Call> calls = connection.getCallsToObject(value);
			for (Call call : calls) {
				if (src.getCall(call.getID()) == null) {
					for (Parameter p : call.getParameter()) {
						if (p.getValue().length() == 0
								|| p.getValue().equals("unable_to_parse")) {
							continue;
						}
						if (!p.getValue().equals("0x0")) {
							String classname = connection.getClass(
									p.getValue(), src.getID());
							if (classname == null
									|| ClassManager.getInstance().isKindOf(
											classname, "UIView")
									|| ClassManager.getInstance().isKindOf(
											classname, "UIEvent")
									|| ClassManager.getInstance().isKindOf(
											classname, "UIGestureRecognizer")
									|| ClassManager.getInstance().isKindOf(
											classname, "CALayer")) {
								continue;
							}
							transitions.add(new Transition(src, call, p
									.getValue(), "ValueOfObjectTransition"));
						}
					}
					// transitions.add(new Transition(src, call, call.getSelf(),
					// "ValueOfObjectTransition"));
				}
			}
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return transitions;
	}

}
