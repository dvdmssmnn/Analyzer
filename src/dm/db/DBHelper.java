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

package dm.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.apache.log4j.Logger;

import dm.analyze.fs.Write;
import dm.data.Call;
import dm.data.Parameter;
import dm.util.Pair;

public class DBHelper {
	private static Logger log = Logger.getLogger(DBHelper.class);

	private static DBHelper instance = null;
	private static String dbPath = "tmp.sqlite";
	private static boolean initialized = false;

	private Connection connection = null;

	private Queue<Long> cacheQueue = new LinkedList<Long>();
	private Map<Long, Call> cache = new HashMap<Long, Call>();
	private static final int cacheSize = 1000;

	@Deprecated
	public synchronized static DBHelper getInstance() {
		if (instance == null) {
			instance = new DBHelper();
			try {
				instance.open();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return instance;
	}

	public static synchronized DBHelper getReadableConnections()
			throws SQLException {
		DBHelper connection = new DBHelper();
		connection.open();
		return connection;
	}

	public static void setDBPath(String path) {
		DBHelper.dbPath = path;
	}

	private void open() throws SQLException {
		if (DBHelper.dbPath == null) {
			log.error("Path to DB not set");
			return;
		}

		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			log.error("SQLite JDBC class not found");
			return;
		}

		connection = DriverManager.getConnection(String.format(
				"jdbc:sqlite:%s", DBHelper.dbPath));
		if (!initialized) {
			// Check if we can retrieve something from the DB...
			ResultSet result = connection.prepareStatement(
					"PRAGMA INTEGRITY_CHECK;").executeQuery();

			while (result.next()) {
				String r = result.getString(1);
				if (!r.contains("ok")) {
					throw new SQLException("Malformed db");
				}
			}
		}

		try {
			connection.prepareStatement(
					"CREATE INDEX IF NOT EXISTS call_id_idx ON CallInfo(ID);")
					.execute();
			connection
					.prepareStatement(
							"CREATE INDEX IF NOT EXISTS return_call_id_idx ON Return(CallID);")
					.execute();
			connection
					.prepareStatement(
							"CREATE INDEX IF NOT EXISTS parameter_call_id_idx ON Parameter(CallID);")
					.execute();
			connection
					.prepareStatement(
							"CREATE INDEX IF NOT EXISTS call_method ON CallInfo(Method);")
					.execute();
			connection
					.prepareStatement(
							"CREATE INDEX IF NOT EXISTS call_class ON CallInfo(Class);")
					.execute();

			if (!initialized) {
				// Filesystem
				connection
						.prepareStatement(
								"CREATE VIEW IF NOT EXISTS FSOpen AS SELECT * FROM CompleteCall WHERE Class LIKE 'Dev' AND Method LIKE 'open';")
						.execute();

				connection
						.prepareStatement(
								"CREATE VIEW IF NOT EXISTS FSWrite AS SELECT * FROM CompleteCall WHERE Class LIKE 'Dev' AND Method LIKE 'write';")
						.execute();

				connection
						.prepareStatement(
								"CREATE VIEW IF NOT EXISTS FSClose AS SELECT * FROM CompleteCall WHERE Class LIKE 'Dev' AND Method LIKE 'close';")
						.execute();

				connection.prepareStatement(
						"CREATE VIEW IF NOT EXISTS FSOpenCloseIDs AS "
								+ "SELECT FSC1.ID AS CloseID, " + "( "
								+ "  IFNULL(MAX( " + "    ( "
								+ "      SELECT MAX(FSC2.ID) "
								+ "      FROM FSClose FSC2 "
								+ "      WHERE FSC2.ID < FSC1.ID AND "
								+ "        FSC2.Value = FSC1.Value "
								+ "    ), " + "    ( "
								+ "      SELECT MAX(FSO1.ID) "
								+ "      FROM FSOpen FSO1 "
								+ "      WHERE FSO1.ID < FSC1.ID AND "
								+ "        FSO1.ReturnValue = FSC1.Value "
								+ "    ) " + "  ) " + ", 0) " + ") AS OpenID "
								+ "FROM FSClose FSC1; ").execute();

				initialized = true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void close() {
		try {
			this.connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public boolean isOpen() {
		if (connection != null) {
			try {
				return !connection.isClosed();
			} catch (SQLException e) {
				log.error(e.getMessage());
			}
		}
		return false;
	}

	public Call getCall(long id) throws SQLException {

		Call call = cache.get(id);
		if (call != null) {
			return call;
		}

		PreparedStatement stmt = connection
				.prepareStatement("SELECT Call.ID, Call.CallerID, Call.Class, "
						+ "Call.Method, Call.Self, Call.ReturnType, Call.ReturnValue, Call.ReturnDescription,"
						+ "Parameter.Type, Parameter.Value, Parameter.Description, Parameter.Name "
						+ "FROM Call LEFT OUTER JOIN Parameter "
						+ "ON Call.ID = Parameter.CallID "
						+ "WHERE Call.ID LIKE ?" + "ORDER BY Parameter.Num;");
		stmt.setString(1, String.valueOf(id));

		ResultSet result = stmt.executeQuery();

		boolean hasCallInfo = false;

		String callerID = null;
		String clazz = null;
		String method = null;
		String self = null;
		String returnType = null;
		String returnValue = null;
		String returnDescription = null;
		List<Parameter> parameters = new ArrayList<Parameter>();

		while (result.next()) {
			if (!hasCallInfo) {
				callerID = result.getString(2);
				clazz = result.getString(3);
				method = result.getString(4);
				self = result.getString(5);
				returnType = result.getString(6);
				returnValue = result.getString(7);
				returnDescription = result.getString(8);

				hasCallInfo = true;
			}

			String paramType = result.getString(9);
			String paramValue = result.getString(10);
			String paramDescription = result.getString(11);
			String paramName = result.getString(12);
			if (paramType != null && paramValue != null) {
				parameters.add(new Parameter(paramType, paramValue,
						paramDescription, paramName));
			}
		}

		if (callerID != null) {
			call = new Call(id, Long.valueOf(callerID), clazz, method, self,
					returnType, returnValue, returnDescription, parameters);
		} else {
			log.debug(String.format("Couldn't find call with id %d", id));
		}

		if (cacheQueue.size() < cacheSize) {
			cache.put(id, call);
		} else {
			cache.remove(cacheQueue.poll());
			cacheQueue.add(id);
			cache.put(id, call);
		}

		return call;
	}

	public List<Call> findCall(Long id, String clazz, String method,
			String self, String type, String value, String description)
			throws SQLException {

		if (clazz == null) {
			clazz = "%";
		}
		if (method == null) {
			method = "%";
		}
		if (type == null) {
			type = "%";
		}
		if (value == null) {
			value = "%";
		}
		if (description == null) {
			description = "%";
		}
		if (self == null) {
			self = "%";
		}

		PreparedStatement stmt = connection
				.prepareStatement("SELECT CompleteCall.ID, CompleteCall.CallerID, CompleteCall.Class, CompleteCall.Method, CompleteCall.Self, CompleteCall.ReturnType, "
						+ "CompleteCall.ReturnValue, CompleteCall.ReturnDescription, CompleteCall.Type, CompleteCall.Value, CompleteCall.Description, CompleteCall.Name, "
						+ "(SELECT COUNT(*) FROM Parameter p2 WHERE p2.CallID = CompleteCall.ID) AS Rows "
						+ "FROM CompleteCall INNER JOIN "
						+ "(SELECT Call.ID "
						+ "FROM Call LEFT OUTER JOIN Parameter "
						+ "ON Call.ID = Parameter.CallID "
						+ "WHERE Call.ID LIKE ? AND "
						+ "Call.Class LIKE ? AND "
						+ "Call.Method LIKE ? AND "
						+ "(Call.ReturnType LIKE ? OR Parameter.Type LIKE ?) AND "
						+ "(Call.ReturnValue LIKE ? OR Parameter.Value LIKE ?) AND "
						+ "(Call.ReturnDescription LIKE ? OR Parameter.Description LIKE ?) AND "
						+ "Call.Self LIKE ? GROUP BY Call.ID) as C ON CompleteCall.ID = C.ID "
						+ "ORDER BY CompleteCall.ID, CompleteCall.Num");

		if (id == null) {
			stmt.setString(1, "%");
		} else {
			stmt.setString(1, String.valueOf(id));
		}

		stmt.setString(2, clazz);
		stmt.setString(3, method);
		stmt.setString(4, type);
		stmt.setString(5, type);
		stmt.setString(6, value);
		stmt.setString(7, value);
		stmt.setString(8, description);
		stmt.setString(9, description);
		stmt.setString(10, self);

		ResultSet result = stmt.executeQuery();

		List<Call> calls = new ArrayList<Call>();

		while (result.next()) {
			int rows = result.getInt(13);

			boolean hasCallInfo = false;

			String callID = null;
			String callerID = null;
			String classname = null;
			String methodname = null;
			String selfRef = null;
			String returnType = null;
			String returnValue = null;
			String returnDescription = null;
			List<Parameter> parameters = new ArrayList<Parameter>();

			for (int i = 0; i < rows; ++i) {
				if (!hasCallInfo) {
					callID = result.getString(1);
					callerID = result.getString(2);
					classname = result.getString(3);
					methodname = result.getString(4);
					selfRef = result.getString(5);
					returnType = result.getString(6);
					returnValue = result.getString(7);
					returnDescription = result.getString(8);

					hasCallInfo = true;
				}

				String paramType = result.getString(9);
				String paramValue = result.getString(10);
				String paramDescription = result.getString(11);
				String paramName = result.getString(12);
				if (paramType != null && paramValue != null) {
					parameters.add(new Parameter(paramType, paramValue,
							paramDescription, paramName));
				}

				if (i != (rows - 1))
					result.next();
			}
			if (callID != null) {
				Call call = new Call(Long.valueOf(callID),
						Long.valueOf(callerID), classname, methodname, selfRef,
						returnType, returnValue, returnDescription, parameters);
				calls.add(call);
			}
		}

		while (result.next()) {
			String callID = result.getString(1);
			calls.add(getCall(Long.valueOf(callID)));
		}
		return calls;
	}

	public List<Call> getCalled(Long id) throws SQLException {
		PreparedStatement stmt = connection
				.prepareStatement("SELECT Call.ID FROM Call WHERE Call.CallerID LIKE ?;");

		stmt.setString(1, String.valueOf(id));

		ResultSet result = stmt.executeQuery();

		List<Call> calls = new ArrayList<Call>();
		while (result.next()) {
			String callID = result.getString(1);
			Call call = getCall(Long.valueOf(callID));
			if (call != null) {
				calls.add(call);
			}

		}
		return calls;
	}

	public List<Call> getCalled(Long id, String value) throws SQLException {
		PreparedStatement stmt = connection
				.prepareStatement("SELECT Call.ID "
						+ "FROM Call LEFT OUTER JOIN Parameter "
						+ "ON Call.ID = Parameter.CallID "
						+ "WHERE Call.CallerID LIKE ? AND (Call.ReturnValue LIKE ? OR Parameter.Value LIKE ?) "
						+ "GROUP BY Call.ID;");

		stmt.setString(1, String.valueOf(id));
		stmt.setString(2, value);
		stmt.setString(3, value);

		ResultSet result = stmt.executeQuery();

		List<Call> calls = new ArrayList<Call>();
		while (result.next()) {
			String callID = result.getString(1);
			Call call = getCall(Long.valueOf(callID));
			if (call != null) {
				calls.add(call);
			}

		}
		return calls;
	}

	public List<Call> getCallsWithParameter(String value) throws SQLException {
		PreparedStatement stmt = connection.prepareStatement("SELECT Call.ID "
				+ "FROM Call LEFT OUTER JOIN Parameter "
				+ "ON Call.ID = Parameter.CallID "
				+ "WHERE Parameter.Value LIKE ?;");

		stmt.setString(1, value);

		ResultSet result = stmt.executeQuery();

		List<Call> calls = new ArrayList<Call>();
		while (result.next()) {
			String callID = result.getString(1);
			Call call = getCall(Long.valueOf(callID));
			if (call != null) {
				calls.add(call);
			}

		}
		return calls;
	}

	public List<Call> getCallsWithParameter(String self, String value)
			throws SQLException {
		PreparedStatement stmt = connection.prepareStatement("SELECT Call.ID "
				+ "FROM Call LEFT OUTER JOIN Parameter "
				+ "ON Call.ID = Parameter.CallID "
				+ "WHERE Call.Self LIKE ? AND Parameter.Value LIKE ?;");

		stmt.setString(1, self);
		stmt.setString(2, value);

		ResultSet result = stmt.executeQuery();

		List<Call> calls = new ArrayList<Call>();
		while (result.next()) {
			String callID = result.getString(1);
			Call call = getCall(Long.valueOf(callID));
			if (call != null) {
				calls.add(call);
			}

		}
		return calls;
	}

	public List<Call> getCallsWithReturn(String value) throws SQLException {
		PreparedStatement stmt = connection.prepareStatement("SELECT Call.ID "
				+ "FROM Call LEFT OUTER JOIN Parameter "
				+ "ON Call.ID = Parameter.CallID "
				+ "WHERE Call.ReturnValue LIKE ?;");

		stmt.setString(1, value);

		ResultSet result = stmt.executeQuery();

		List<Call> calls = new ArrayList<Call>();
		while (result.next()) {
			String callID = result.getString(1);
			Call call = getCall(Long.valueOf(callID));
			if (call != null) {
				calls.add(call);
			}

		}
		return calls;
	}

	public List<Call> getCallsToObject(String object) throws SQLException {
		PreparedStatement stmt = connection.prepareStatement("SELECT Call.ID "
				+ "FROM Call " + "WHERE Call.Self LIKE ?;");

		stmt.setString(1, object);

		ResultSet result = stmt.executeQuery();

		List<Call> calls = new ArrayList<Call>();
		while (result.next()) {
			String callID = result.getString(1);
			Call call = getCall(Long.valueOf(callID));
			if (call != null) {
				calls.add(call);
			}

		}
		return calls;
	}

	public List<Pair<String, String>> getClassList() throws SQLException {
		List<Pair<String, String>> classes = new ArrayList<Pair<String, String>>();
		PreparedStatement stmt = connection
				.prepareStatement("SELECT Name, Super FROM Class;");

		ResultSet result = stmt.executeQuery();

		while (result.next()) {
			Pair<String, String> c = new Pair<String, String>(
					result.getString(1), result.getString(2));
			classes.add(c);
		}

		return classes;
	}

	// Tries to get the class name of the object
	// it uses the last call that was made to this reference before 'id'
	public String getClass(String value, long id) throws SQLException {
		PreparedStatement stmt = connection
				.prepareStatement("SELECT Class FROM Call "
						+ "WHERE Call.Self LIKE ? AND Call.ID < ? "
						+ "ORDER BY Call.ID DESC " + "LIMIT 1;");

		stmt.setString(1, value);
		stmt.setLong(2, id);

		ResultSet result = stmt.executeQuery();
		if (result.next()) {
			return result.getString(1);
		}
		return null;
	}

	/**
	 * Get all calls that were called by the 'self' object
	 * 
	 * @param self
	 * @param value
	 * @return
	 * @throws SQLException
	 */
	public List<Call> getCalled(String self, String value) throws SQLException {
		PreparedStatement stmt = connection
				.prepareStatement("SELECT * "
						+ "FROM Call INNER JOIN (SELECT Call2.ID AS SelfCallID FROM Call call2 WHERE call2.self LIKE ?) "
						+ "ON Call.CallerID = SelfCallID "
						+ "LEFT OUTER JOIN parameter ON Call.ID = Parameter.CallID "
						+ "WHERE Parameter.Value LIKE ? OR Call.ReturnValue LIKE ? "
						+ "GROUP BY Call.ID");

		if (value == null) {
			value = "%";
		}

		stmt.setString(1, self);
		stmt.setString(2, value);
		stmt.setString(3, value);

		ResultSet result = stmt.executeQuery();

		List<Call> calls = new ArrayList<Call>();
		while (result.next()) {
			String callID = result.getString(1);
			Call call = getCall(Long.valueOf(callID));
			if (call != null) {
				calls.add(call);
			}

		}
		return calls;
	}

	public List<Write> getFSWrites() throws SQLException {
		PreparedStatement stmt = connection.prepareStatement("SELECT  "
				+ "IFNULL( " + "  ( " + "    SELECT FSO1.Description "
				+ "    FROM FSOpen FSO1 " + "    WHERE FSO1.ID = FSOC1.OpenID "
				+ "  ) " + ", 'unknown') AS Path " + ", ( "
				+ "  SELECT REPLACE(GROUP_CONCAT(FSW1.Description), ',', '') "
				+ "  FROM FSWrite FSW1 "
				+ "  WHERE FSW1.ID > FSOC1.OpenID AND FSW1.ID < FSOC1.CloseID "
				+ ") AS Content " + "FROM FSOpenCloseIDs FSOC1 ");

		ResultSet result = stmt.executeQuery();

		List<Write> writes = new ArrayList<Write>();
		while (result.next()) {
			writes.add(new Write(result.getString(1), result.getString(2)));
		}

		return writes;
	}
}
