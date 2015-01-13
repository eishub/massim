package massim.eismassim;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.Vector;

/**
 * Class to be used for some statistic purpose, e.g. the time between requests
 * and actions or the total number of actions of a distinct type
 * 
 * @author tobi
 *
 */
public class Statistic {

	private static final String scriptURL = "http://multiagentcontest.org/stats/stats.php";

	private HashMap<String, Long> requests;

	private Vector<Long> allIntervals;
	private HashMap<String, Vector<Long>> connectionIntervals;

	private HashMap<String, Integer> actionCount;
	private HashMap<String, HashMap<String, Integer>> singleAgentActionCount;
	private HashMap<String, HashMap<String, Integer>> failedActions;

	private HashMap<String, Integer> simStartPercepts;
	private HashMap<String, Integer> simEndPercepts;
	private HashMap<String, Integer> requestActionPercepts;
	private HashMap<String, Integer> byePercepts;

	private HashMap<String, Vector<String>> achievements;
	private HashMap<String, Vector<Integer>> zoneScores;
	private HashMap<String, Vector<Integer>> zonesScores;

	private boolean toFile = false;
	private boolean toShell = false;
	private boolean submit = true;
	private boolean testing = false;

	private boolean concluded = false;
	private File file;
	private FileWriter writer;
	private String ls = System.getProperty("line.separator");

	public static final String TEST_FLAG = "[TESTSTATS]";

	public Statistic() {

		this.requests = new HashMap<String, Long>();

		this.connectionIntervals = new HashMap<String, Vector<Long>>();
		this.allIntervals = new Vector<Long>();

		this.actionCount = new HashMap<String, Integer>();
		this.singleAgentActionCount = new HashMap<String, HashMap<String, Integer>>();
		this.failedActions = new HashMap<String, HashMap<String, Integer>>();

		this.simStartPercepts = new HashMap<String, Integer>();
		this.simEndPercepts = new HashMap<String, Integer>();
		this.requestActionPercepts = new HashMap<String, Integer>();
		this.byePercepts = new HashMap<String, Integer>();

		this.achievements = new HashMap<String, Vector<String>>();
		this.zoneScores = new HashMap<String, Vector<Integer>>();
		this.zonesScores = new HashMap<String, Vector<Integer>>();
	}

	/**
	 * Print the gathered data to a file and/or to the shell and/or submit them
	 */
	public synchronized void onSimulationEnd() {

		if (!concluded) { // prevent this from being called multiple times
			concluded = true;

			// start a new thread so to not block everything for a long time
			new SubmitThread(this).start();

		}
	}

	private String appendTeamResults(String connection, Vector<String> achs,
			Vector<Integer> zScores) {

		TreeSet<String> achset = new TreeSet<String>();
		achset.addAll(achs);

		String teamResults = ls + "Results for the team of " + connection + ":"
				+ ls;
		String str = "";
		for (String ach : achset) {
			str += ach + ", ";
		}
		if (!str.equals("")) {
			str = str.substring(0, str.length() - 2);
		}
		teamResults += "Achievements:" + ls + str + ls;

		str = "";
		for (Integer zs : zScores) {
			str += zs + ", ";
		}
		if (!str.equals("")) {
			str = str.substring(0, str.length() - 2);
		}
		teamResults += "ZonesScores:" + ls + str + ls;

		return teamResults;
	}

	/**
	 * method writes to statistic output file
	 */
	private void logToFile(String datum) {

		if (this.file == null) {
			String filename = "stats-"
					+ Calendar.getInstance().getTime().toString() + ".txt";
			this.file = new File(filename);
		}
		File f = this.file;
		if (!f.exists()) {
			try {
				f.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		try {
			FileWriter out = getFileWriter();
			out.append(datum + ls);
			out.flush();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void submitResults(String results) {

		try {
			URL url = new URL(scriptURL);

			URLConnection urlConn = url.openConnection();
			urlConn.setDoInput(true);
			urlConn.setDoOutput(true);
			urlConn.setUseCaches(false);
			urlConn.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded");

			DataOutputStream output = new DataOutputStream(
					urlConn.getOutputStream());

			String ls = System.getProperty("line.separator");

			String osInfo = "Operating System:" + ls
					+ System.getProperty("os.name") + ls + "Version: "
					+ System.getProperty("os.version") + ls + "Architecture: "
					+ System.getProperty("os.arch") + ls + ls;

			String content;
			if (testing) {
				content = "stats=[beginstats]" + ls + TEST_FLAG + ls + osInfo
						+ results + ls + "[endstats]";
			} else {
				content = "stats=[beginstats]" + ls + osInfo + results + ls
						+ "[endstats]";
			}

			output.write(content.getBytes());
			output.flush();
			output.close();

			// for some reason, this seems to be necessary
			InputStream in = urlConn.getInputStream();
			int c = 0;
			// StringBuffer incoming = new StringBuffer();
			while (c >= 0) {
				c = in.read();
				// incoming.append((char) c);
			}
			// System.out.println(incoming.toString());

		} catch (Exception e) {
			// e.printStackTrace();
		}
	}

	public void setLogToFile() {
		this.toFile = true;
	}

	public void setLogToShell() {
		this.toShell = true;
	}

	public void disableSend() {
		this.submit = false;
	}

	/**
	 * this method will guarantee, that results submitted via php will contain a
	 * special flag that marks them as testing-results, so they can be sorted
	 * out
	 */
	public void enableTestMode() {
		this.testing = true;
	}

	public void submitRequest(String name, int actionID) {

		long timestamp = System.currentTimeMillis();
		this.requests.put(name + actionID, timestamp);
	}

	public void submitAction(String name, int actionID, String type,
			long timestamp) {

		String key = name + actionID;
		long interval = -1;
		if (this.requests.containsKey(key)) {
			long reqTime = requests.get(key);
			interval = timestamp - reqTime;
		} else {
		}

		// increase the action's counter, log the interval
		allIntervals.add(interval);

		if (connectionIntervals.containsKey(name)) {
			connectionIntervals.get(name).add(interval);
		} else {
			Vector<Long> v = new Vector<Long>();
			v.add(interval);
			connectionIntervals.put(name, v);
		}

		if (actionCount.containsKey(type)) {
			int count = actionCount.get(type);
			actionCount.put(type, count + 1);
		} else {
			actionCount.put(type, 1);
		}

		incrementSingleActionCounter(name, type);

		String result = "Action " + type + " for " + name + " after "
				+ interval + " ms";
		if (toFile) {
			logToFile(result);
		}
		if (toShell) {
			System.out.println(result);
		}

	}

	public void logSimStartPercept(String connName) {

		Integer k = this.simStartPercepts.get(connName);
		if (k == null) {
			this.simStartPercepts.put(connName, 1);
		} else {
			this.simStartPercepts.put(connName, k + 1);
		}
	}

	public void logSimEndPercept(String connName) {

		Integer k = this.simEndPercepts.get(connName);
		if (k == null) {
			this.simEndPercepts.put(connName, 1);
		} else {
			this.simEndPercepts.put(connName, k + 1);
		}
	}

	public void logRequestActionPercept(String connName) {

		Integer k = this.requestActionPercepts.get(connName);
		if (k == null) {
			this.requestActionPercepts.put(connName, 1);
		} else {
			this.requestActionPercepts.put(connName, k + 1);
		}
	}

	public void logByePercept(String connName) {

		Integer k = this.byePercepts.get(connName);
		if (k == null) {
			this.byePercepts.put(connName, 1);
		} else {
			this.byePercepts.put(connName, k + 1);
		}
	}

	/**
	 * @param name
	 *            the connection's name
	 * @param lastActionType
	 *            the type of the agent's last action
	 * @param lastActionResult
	 *            the result of the action
	 */
	public void submitActionResult(String name, String lastActionType,
			String lastActionResult) {

		if (lastActionType.equals("failed")
		// || lastActionType.equals("useless")
				|| lastActionType.equals("wrongParameter")) {

			HashMap<String, Integer> map = this.failedActions.get("name");
			if (map == null) {
				HashMap<String, Integer> newMap = new HashMap<String, Integer>();
				newMap.put(lastActionType, 1);
				this.failedActions.put(name, newMap);
			} else {
				Integer failCount = map.get(lastActionType);
				if (failCount == null) {
					map.put(lastActionType, 1);
				} else {
					map.put(lastActionType, failCount + 1);
				}
			}
		}

	}

	public void submitZoneScore(Integer score, String name) {

		Vector<Integer> zs = this.zoneScores.get(name);
		if (zs != null) {
			zs.add(score);
		} else {
			Vector<Integer> vec = new Vector<Integer>();
			vec.add(score);
			this.zoneScores.put(name, vec);
		}
	}

	public synchronized void submitZonesScore(Integer score, String name) {

		Vector<Integer> zs = this.zonesScores.get(name);
		if (zs != null) {
			zs.add(score);
		} else {
			Vector<Integer> vec = new Vector<Integer>();
			vec.add(score);
			this.zonesScores.put(name, vec);
		}
	}

	public synchronized void submitAchievement(String achievement, String name) {

		Vector<String> ach = this.achievements.get(name);
		if (ach != null) {
			ach.add(achievement);
		} else {
			Vector<String> vec = new Vector<String>();
			vec.add(achievement);
			this.achievements.put(name, vec);
		}
	}

	/**
	 * Increment the counter for the specified actionType of the specified
	 * connection.
	 * 
	 * @param name
	 *            the connection's name
	 * @param type
	 *            the action's type
	 */
	private void incrementSingleActionCounter(String name, String type) {

		HashMap<String, Integer> map = this.singleAgentActionCount.get(name);

		if (map != null) {

			Integer count = map.get(type);
			if (count == null) {

				map.put(type, 1);
			} else {

				map.put(type, count + 1);
			}
		} else {

			HashMap<String, Integer> innerMap = new HashMap<String, Integer>();
			innerMap.put(type, 1);
			this.singleAgentActionCount.put(name, innerMap);
		}
	}

	private FileWriter getFileWriter() throws IOException {

		if (this.writer == null) {
			this.writer = new FileWriter(this.file, true);
		}
		return this.writer;
	}

	/**
	 * 1: toFile 2: toShell 3: submit
	 * 
	 * @return array with 3 booleans
	 */
	public boolean[] getSettings() {
		boolean[] settings = new boolean[3];
		settings[0] = this.toFile;
		settings[1] = this.toShell;
		settings[2] = this.submit;
		return settings;
	}

	/**
	 * Apply toShell, toFile and submit-settings of given statistic to this
	 * statistic
	 * 
	 * @param statistic
	 */
	public void applySettingsOf(Statistic statistic) {
		boolean[] settings = statistic.getSettings();
		this.toFile = settings[0];
		this.toShell = settings[1];
		this.submit = settings[2];
	}

	class SubmitThread extends Thread {

		private Statistic stat;

		public SubmitThread(Statistic stat) {
			this.stat = stat;
		}

		@Override
		public void run() {

			try {
				System.out
						.println("Statistics: allowing other agent's to finish within 5 seconds...");
				Thread.sleep(5000);
				System.out.println("Statistics: generating final output...");
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}

			// prepare output message
			long sum = 0;
			for (long l : stat.allIntervals) {
				sum += l;
			}
			long average = 0;

			if (stat.allIntervals.size() != 0)
				average = sum / stat.allIntervals.size();

			String result = "Average time between request and response: "
					+ average + " ms" + ls;
			String singleAgentResults = "Detailed results for every agent:"
					+ ls;
			String teamResults = ls + "Team Results:" + ls;

			// Actions (overall)
			int allActionCount = 0;
			for (Integer k : actionCount.values()) {
				allActionCount += k;
			}
			for (String actionType : actionCount.keySet()) {
				int actions = actionCount.get(actionType);
				double percentage = .0;
				if (allActionCount != 0)
					percentage = ((double) actions) / (allActionCount / 100.0);

				result += "Action -" + actionType + "-: " + percentage + "% "
						+ ls;

			}

			// Percepts (overall)
			result += "Received Percepts:" + ls;
			int simStartPCount = 0, simEndPCount = 0, reqActionPCount = 0, byePCount = 0;
			for (Integer k : simStartPercepts.values()) {
				simStartPCount += k;
			}
			for (Integer k : simEndPercepts.values()) {
				simEndPCount += k;
			}
			for (Integer k : requestActionPercepts.values()) {
				reqActionPCount += k;
			}
			for (Integer k : byePercepts.values()) {
				byePCount += k;
			}
			result += simStartPCount + " SimStart-Percept(s)" + ls;
			result += simEndPCount + " SimEnd-Percept(s)" + ls;
			result += reqActionPCount + " Request Action-Percepts" + ls;
			result += byePCount + " Bye-Percept(s)" + ls;

			// single agent results
			for (String connection : stat.connectionIntervals.keySet()) {

				sum = 0;
				Vector<Long> vec = stat.connectionIntervals.get(connection);
				for (long l : vec) {
					sum += l;
				}

				average = 0;
				if (stat.connectionIntervals.get(connection).size() != 0)
					average = sum
							/ stat.connectionIntervals.get(connection).size();

				singleAgentResults += ls + "Statistics for " + connection + ":"
						+ ls + "Average response time: " + average + " ms" + ls;

				// append every action's count for this connection
				HashMap<String, Integer> actionMap = stat.singleAgentActionCount
						.get(connection);
				if (actionMap != null) {

					int singleAgentAllActionCount = 0;
					for (Integer k : actionMap.values()) {
						singleAgentAllActionCount += k;
					}

					for (String singleAction : actionMap.keySet()) {

						int failedCount = 0;
						HashMap<String, Integer> failedMap = stat.failedActions
								.get(connection);
						if (failedMap != null) {
							Integer i = failedMap.get(singleAction);
							if (i != null)
								failedCount = i;
						}

						int currentActionCount = actionMap.get(singleAction);

						double overallPercentage = .0d;
						if (allActionCount != 0)
							overallPercentage = ((double) currentActionCount)
									/ (allActionCount / 100.0);

						double singleAgentPercentage = .0d;
						if (singleAgentAllActionCount != 0)
							singleAgentPercentage = ((double) currentActionCount)
									/ (singleAgentAllActionCount / 100.0);

						singleAgentResults += "Action -" + singleAction + "-: "
								+ currentActionCount + " times total, "
								+ "failed " + failedCount + " time(s), "
								+ singleAgentPercentage
								+ "% of this agents' actions, "
								+ overallPercentage
								+ "% of all agents' actions" + ls;

					}
				}

				// append this agent's zoneScores
				Vector<Integer> zScores = stat.zoneScores.get(connection);
				if (zScores != null) {
					singleAgentResults += "ZoneScores: ";
					String str = "";
					for (Integer h : zScores) {
						str += h + ", ";
					}
					if (!str.equals("")) {
						str = str.substring(0, str.length() - 2);
					}
					singleAgentResults += str + ls;
				}

				// append number of percepts
				Integer k = stat.simStartPercepts.get(connection);
				if (k != null)
					singleAgentResults += k + " SimStart-Percept(s)" + ls;
				k = stat.simEndPercepts.get(connection);
				if (k != null)
					singleAgentResults += k + " SimEnd-Percept(s)" + ls;
				k = stat.requestActionPercepts.get(connection);
				if (k != null)
					singleAgentResults += k + " Request Action-Percepts" + ls;
				k = stat.byePercepts.get(connection);
				if (k != null)
					singleAgentResults += k + " Bye-Percept(s)" + ls;

			}

			// Filter team specific results (achievements and zonesScores)

			// create an array of the connections so that an unambiguous
			// iteration order is guaranteed
			String[] connections = stat.achievements.keySet().toArray(
					new String[achievements.keySet().size()]);

			Vector<String> achs = stat.achievements.get(connections[0]);
			Vector<Integer> zScores = stat.zonesScores.get(connections[0]);

			// append results for the first team
			teamResults += appendTeamResults(connections[0], achs, zScores);

			// try to find the results for the second team (i.e. a connection of
			// the other team)
			for (int i = 1; i < connections.length; i++) {

				String con = connections[i];

				if (stat.achievements.get(con).size() != achs.size()) { // found
																		// it
					teamResults += appendTeamResults(con,
							achievements.get(con), zonesScores.get(con));
					break;
				} else if (stat.zonesScores.get(con).size() != zScores.size()) {
					teamResults += appendTeamResults(con,
							achievements.get(con), zonesScores.get(con));
					break;
				} else { // check the sets' elements

					if (!(stat.achievements.get(con).containsAll(achs) && stat.zonesScores
							.get(con).containsAll(zScores))) {

						teamResults += appendTeamResults(con,
								achievements.get(con), zonesScores.get(con));
						break;
					}
				}
			}

			if (toFile) {
				logToFile("SIM-END:" + ls + result + singleAgentResults
						+ teamResults);

				// close writer
				try {
					getFileWriter().close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (toShell) {
				System.out.println("Sim-End statistic: " + result
						+ singleAgentResults + teamResults);
			}
			if (submit) {
				submitResults("Sim-End statistic: " + result
						+ singleAgentResults + teamResults);
			}
		}

	}

}
