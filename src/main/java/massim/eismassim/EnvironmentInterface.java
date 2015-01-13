package massim.eismassim;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import eis.EIDefaultImpl;
import eis.exceptions.ActException;
import eis.exceptions.EntityException;
import eis.exceptions.EnvironmentInterfaceException;
import eis.exceptions.ManagementException;
import eis.exceptions.NoEnvironmentException;
import eis.exceptions.PerceiveException;
import eis.exceptions.RelationException;
import eis.iilang.Action;
import eis.iilang.EnvironmentState;
import eis.iilang.IILElement;
import eis.iilang.Percept;

/**
 * This is an environment interface to the MASSim-server. It is intended to
 * facilitate the comminication between agent-platforms that support the
 * EIS-0.5.0 standard on one side and the MASSim-server on the other side. </p>
 * The environment interface is configured via a configuration-file that is
 * parsed and evaluated when instantiating the class. This includes creating
 * entities, that is valid connections to the MASSim-server, using the
 * credentials that are provided in that very configuration-file.
 * 
 * @author tristanbehrens
 * @author W.Pasman modified jan2015 for EIS0.5.0 compatibility
 *
 */
// TODO use a logger to monitor events, i.e. connection lost etc. connection
// failed

@SuppressWarnings("serial")
public class EnvironmentInterface extends EIDefaultImpl implements Runnable {

	private Collection<Entity> entities;
	private String scenario;
	private Map<String, Entity> entityNamesToObjects;
	private Vector<Statistic> stats = new Vector<Statistic>();

	/**
	 * Instantiates the environment-interface. Firstly a configuration file is
	 * parsed and used to instantiate the entities. After that a first attempt
	 * is made to connect the entities to the MASSim-Server.
	 */
	public EnvironmentInterface() {

		Entity.setEnvironmentInterface(this);
		entities = new LinkedList<Entity>();
		entityNamesToObjects = new HashMap<String, Entity>();

		stats.add(new Statistic());

		// parse config-file
		try {
			parseConfig("eismassimconfig.xml");
		} catch (ParseException e) {
			// e.printStackTrace();
			System.out.println("Parse-exception: " + e.getMessage());
		}

		// add entities
		for (Entity e : entities) {
			try {
				addEntity(e.getName());
				assert e.getName().equals("dummyagent9") == false;
			} catch (EntityException e1) {
				e1.printStackTrace();
				assert false : "Handle!";
			}
			entityNamesToObjects.put(e.getName(), e);
		}

		// establish connections
		// for ( Entity e : entities ) {
		// e.establishConnection();
		// }

		// some structure for checking connections?
		// new Thread(this).start();

		// I only want to see Prolog stuff
		IILElement.toProlog = true;

		// set the state
		try {
			setState(EnvironmentState.PAUSED);
		} catch (ManagementException e1) {
			e1.printStackTrace();
		}

	}

	/**
	 * Parses a configuration-file.
	 * 
	 * @param filename
	 * @throws ParseException
	 */
	private void parseConfig(String filename) throws ParseException {

		File file = new File(filename);

		// parse the XML document
		Document doc = null;
		try {
			DocumentBuilderFactory documentbuilderfactory = DocumentBuilderFactory
					.newInstance();
			doc = documentbuilderfactory.newDocumentBuilder().parse(file);
		} catch (SAXException e) {
			throw new ParseException(file.getPath(), "error parsing "
					+ e.getMessage());

		} catch (IOException e) {
			throw new ParseException(file.getPath(), "error parsing "
					+ e.getMessage());

		} catch (ParserConfigurationException e) {
			throw new ParseException(file.getPath(), "error parsing "
					+ e.getMessage());
		}

		// get the root
		Element root = doc.getDocumentElement();
		if (root.getNodeName().toLowerCase().equals("interfaceconfig") == false)
			throw new ParseException(file.getPath(),
					"root-element must be interfaceconfig");

		// attributes
		String host = root.getAttribute("host");
		String scenario = root.getAttribute("scenario");
		String portStr = root.getAttribute("port");

		if (host == null || host.equals(""))
			throw new ParseException(
					"missing host-attribute of <interfaceconfig>");
		if (scenario == null || scenario.equals(""))
			throw new ParseException(
					"missing scenario-attribute of <interfaceconfig>");
		if (portStr == null || portStr.equals(""))
			throw new ParseException(
					"missing port-attribute of <interfaceconfig>");
		this.scenario = scenario;
		int port = new Integer(portStr).intValue();

		// annotate percepts with time
		String str = root.getAttribute("times");
		if (str != null && str.equals("") == false) {
			if (str.equals("yes") || str.equals("true")) {
				Entity.enableTimeAnnotations();
			} else if (str.equals("no") || str.equals("false")) {
			} else {
				throw new ParseException("invalid value \"" + str
						+ "\" for times-attribute of <interfaceconfig>");
			}
		}

		// enable scheduling
		str = root.getAttribute("scheduling");
		if (str != null && str.equals("") == false) {
			if (str.equals("yes") || str.equals("true")) {
				Entity.enableScheduling();
			} else if (str.equals("no") || str.equals("false")) {
			} else {
				throw new ParseException("invalid value \"" + str
						+ "\" for scheduling-attribute of <interfaceconfig>");
			}
		}

		// enable notifications
		str = root.getAttribute("notifications");
		if (str != null && str.equals("") == false) {
			if (str.equals("yes") || str.equals("true")) {
				Entity.enableNotifications();
			} else if (str.equals("no") || str.equals("false")) {
			} else {
				throw new ParseException("invalid value \"" + str
						+ "\" for notifications-attribute of <interfaceconfig>");
			}
		}

		// timeout
		String timeout = root.getAttribute("timeout");
		if (timeout != null && timeout.equals("") == false) {
			Entity.setTimeout(new Integer(timeout).intValue());
		}

		boolean statistics = false;
		// statistic to file
		str = root.getAttribute("statisticsFile");
		if (str != null && (str.equals("yes") || str.equals("true"))) {
			this.stats.get(0).setLogToFile();
			statistics = true;
		}

		// statistic to shell
		str = root.getAttribute("statisticsShell");
		if (str != null && (str.equals("yes") || str.equals("true"))) {
			this.stats.get(0).setLogToShell();
			statistics = true;
		}

		// submit statistics
		str = root.getAttribute("submitStatistic");
		if (str != null
				&& (str.equalsIgnoreCase("no") || str.equalsIgnoreCase("false"))) {
			this.stats.get(0).disableSend();
		} else {
			statistics = true;
		}

		if (statistics) {
			Entity.activateStatistics();
		}

		// queue
		str = root.getAttribute("queued");
		if (str != null && str.equals("") == false) {
			if (str.equals("yes") || str.equals("true")) {
				Entity.activatePerceptQueue();
			} else if (str.equals("no") || str.equals("false")) {
			} else {
				throw new ParseException("invalid value \"" + str
						+ "\" for queued-attribute of <interfaceconfig>");
			}
		}

		// process root's children
		NodeList rootChildren = root.getChildNodes();
		for (int a = 0; a < rootChildren.getLength(); a++) {

			Node rootChild = rootChildren.item(a);

			// ignore text and comment
			if (rootChild.getNodeName().equals("#text")
					|| rootChild.getNodeName().equals("#comment"))
				continue;

			// parse the entites list
			if (rootChild.getNodeName().equals("entities")) {
				parseEntities(rootChild, host, port, scenario);
			} else {
				System.out.println("unrecognized xml-tag "
						+ rootChild.getNodeName());
			}

		}

	}

	/**
	 * Extracts entity-specifications from an XML-node and creates the
	 * respective entities. Used during configuration-parsing.
	 * 
	 * @param node
	 *            is the entities-tag
	 * @param host
	 *            is the hostname of the server
	 * @param port
	 *            is the port of the server
	 * @param scenario
	 *            is the scenario
	 * @param times
	 *            denotes whether percepts should be annotated with the current
	 *            step
	 * @throws ParseException
	 */
	private void parseEntities(Node node, String host, int port, String scenario)
			throws ParseException {

		for (int a = 0; a < node.getChildNodes().getLength(); a++) {

			Node child = node.getChildNodes().item(a);

			// ignore text and comment
			if (child.getNodeName().equals("#text")
					|| child.getNodeName().equals("#comment"))
				continue;

			if (child.getNodeName().endsWith("entity")) {

				String name = ((Element) child).getAttribute("name");
				String username = ((Element) child).getAttribute("username");
				String password = ((Element) child).getAttribute("password");

				if (name == null || name.equals(""))
					throw new ParseException(
							"missing name-attribute of <entity>");
				if (username == null || username.equals(""))
					throw new ParseException(
							"missing username-attribute of <entity>");
				if (password == null || password.equals(""))
					throw new ParseException(
							"missing password-attribute of <entity>");

				// instantiate entity
				Entity entity = Entity.createEntity(name, scenario, host, port,
						username, password);
				entity.setStatistic(stats.get(0));

				// verbose
				String xml = ((Element) child).getAttribute("xml");
				if (xml != null && xml.equals("") == false) {
					if (xml.equals("yes") || xml.equals("true")) {
						entity.enableXML();
					} else if (xml.equals("no") || xml.equals("false")) {
					} else {
						throw new ParseException("invalid value \"" + xml
								+ "\" for xml-attribute of <entity>");
					}
				}
				String iilang = ((Element) child).getAttribute("iilang");
				if (iilang != null && iilang.equals("") == false) {
					if (iilang.equals("yes") || iilang.equals("true")) {
						entity.enableIILang();
					} else if (iilang.equals("no") || iilang.equals("false")) {
					} else {
						throw new ParseException("invalid value \"" + iilang
								+ "\" for iilang-attribute of <entity>");
					}
				}

				entities.add(entity);

			} else {
				System.out.println("unrecognized xml-tag "
						+ child.getNodeName());
			}

		}

	}

	@Override
	protected LinkedList<Percept> getAllPerceptsFromEntity(String entity)
			throws PerceiveException, NoEnvironmentException {

		Entity e = entityNamesToObjects.get(entity);
		if (e.isConnected() == false) {
			throw new PerceiveException("no valid connection");
		}

		LinkedList<Percept> percepts = e.getAllPercepts();

		// assert false:percepts;

		return percepts;

	}

	@Override
	protected boolean isSupportedByEntity(Action action, String entity) {

		Vector<String> actions = new Vector<String>();
		actions.add("attack");
		actions.add("buy");
		actions.add("goto");
		actions.add("inspect");
		actions.add("parry");
		actions.add("probe");
		actions.add("recharge");
		actions.add("repair");
		actions.add("skip");
		actions.add("survey");

		if (actions.contains(action.getName())) {
			return true;
		}

		return false;
	}

	@Override
	protected boolean isSupportedByEnvironment(Action action) {
		return true;
	}

	@Override
	protected boolean isSupportedByType(Action action, String type) {
		return true;
	}

	@Override
	protected Percept performEntityAction(String entity, Action action)
			throws ActException {
		Entity e = entityNamesToObjects.get(entity);
		if (e.isConnected() == false) {
			// throw new
			// ActException(ActException.FAILURE,"no valid connection");
		}
		try {
			e.performAction(action);
		} catch (ActException ee) {
			throw ee;
		}
		return new Percept("done");
	}

	@Override
	public String requiredVersion() {
		return "0.5";
	}

	@Override
	public String getType(String entity) {
		return entityNamesToObjects.get(entity).getType();
	}

	@Override
	public boolean isInitSupported() {
		return false;
	}

	/*
	 * @Override public void init(Map<String, Parameter> parameters) throws
	 * ManagementException {
	 * 
	 * 
	 * for ( Entry<String,Parameter> e : parameters.entrySet()) {
	 * 
	 * 
	 * if ( e.getKey().equals("scheduling") ) { boolean b =
	 * ((TruthValue)e.getValue()).getBooleanValue(); if ( b ) {
	 * Entity.enableScheduling(); } } else { assert false :
	 * "unrecognized init-parameter \"" + e.getKey() + "\""; }
	 * 
	 * }
	 * 
	 * super.init(parameters);
	 * 
	 * // set the state try { setState(EnvironmentState.PAUSED); } catch
	 * (ManagementException e1) { e1.printStackTrace(); }
	 * 
	 * }
	 */

	@Override
	public void run() {

		while (this.getState() != EnvironmentState.KILLED) {

			// sleep for a couple of seconds
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
			}

			// check connections and attempt to reconnect if necessary
			for (Entity e : entities) {
				System.out.println("entity \"" + e.getName()
						+ "\" is not connected. trying to connect.");
				if (e.isConnected() == false) {
					e.establishConnection();
				}
			}

		}
	}

	public void sendNotifications(String name, Collection<Percept> percepts) {

		if (this.getState() != EnvironmentState.RUNNING)
			return;

		for (Percept p : percepts) {
			try {
				this.notifyAgentsViaEntity(p, name);
			} catch (EnvironmentInterfaceException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	@Override
	public void associateEntity(String agent, String entity)
			throws RelationException {
		super.associateEntity(agent, entity);

		Entity e = this.entityNamesToObjects.get(entity);
		if (e.isConnected() == false)
			e.establishConnection();

	}

	public synchronized Statistic requestNewStatistics(int simCount) {
		if (this.stats.size() >= simCount + 1) {
			return this.stats.get(simCount);
		} else {
			Statistic nstat = new Statistic();
			stats.add(nstat);
			nstat.applySettingsOf(stats.get(stats.size() - 2));
			return nstat;
		}
	}

}
