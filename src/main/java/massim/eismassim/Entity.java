package massim.eismassim;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import eis.exceptions.ActException;
import eis.exceptions.PerceiveException;
import eis.iilang.Action;
import eis.iilang.Numeral;
import eis.iilang.Parameter;
import eis.iilang.Percept;

/**
 * This represents an abstract entity. Basically an entity is a structure that
 * represents a connection to the MASSim-Server. Thus is implements the official
 * MASSim client-server communication protocol. Additionally we expect from
 * subclasses of this class to implement methods that identify the type of the
 * entity, and methods that map IILang tokens to XML and vice versa.
 * 
 * @author tristanbehrens
 *
 */
public abstract class Entity implements Runnable {

	protected static EnvironmentInterface ei = null;

	/* For building and transforming XML-documents. */
	protected static DocumentBuilderFactory documentbuilderfactory = DocumentBuilderFactory
			.newInstance();
	protected static TransformerFactory transformerfactory = TransformerFactory
			.newInstance();

	/* For sending and receiving XML-documents. */
	private InputStream inputstream = null;
	private OutputStream outputstream = null;

	/* connection data */
	private Socket socket;
	private String name;
	private String host;
	private int port;
	private String username;
	private String password;

	boolean connected = false;

	/* if scheduling is true then only one action per action-id is sent */
	private static boolean scheduling = false;

	/* if true, percepts will have an additional parameter denoting the time */
	private static boolean timeAnnotations = false;

	/*
	 * defines after how many ms the attempt to perform an action or retrieve
	 * percepts is timed out
	 */
	private static int timeout = 5000;

	/* indicates whether percepts are sent as notifications */
	private static boolean notifications = false;

	private int currentActionId;
	private int lastUsedActionId;
	private int lastUsedActionIdPercept;
	// private long requestActionTimeStamp = 0;
	// private long simStartTimeStamp = 0;

	private Statistic stats;
	private int simCount = 0;
	private static boolean statsActivated = false;
	private static boolean queuedPercepts = false;

	// protected Collection<Percept> staticPercepts =
	// Collections.synchronizedSet(new HashSet<Percept>());
	// protected Collection<Percept> dynamicPercepts =
	// Collections.synchronizedSet(new HashSet<Percept>());
	protected Collection<Percept> simStartPercepts = Collections
			.synchronizedSet(new HashSet<Percept>());
	protected Collection<Percept> requestActionPercepts = Collections
			.synchronizedSet(new HashSet<Percept>());
	protected Collection<Percept> simEndPercepts = Collections
			.synchronizedSet(new HashSet<Percept>());
	protected Collection<Percept> byePercepts = Collections
			.synchronizedSet(new HashSet<Percept>());

	/*
	 * This queue is used to store the percepts in the order of arrival, if
	 * queuing is activated
	 */
	protected AbstractQueue<Collection<Percept>> perceptsQueue = new ConcurrentLinkedQueue<Collection<Percept>>();

	boolean gatherSpecimens = false;
	private static List<Percept> perceptSpecimens = Collections
			.synchronizedList(new LinkedList<Percept>());

	// verbose
	private boolean xml = false;
	private boolean iilang = false;

	/**
	 * Not supposed to be called by anyone else. Use factory method instead.
	 */
	protected Entity() {

	}

	public void enableXML() {
		xml = true;
	}

	public void enableIILang() {
		iilang = true;
	}

	/**
	 * Yields the name of the entity.
	 * 
	 * @return the name of the entity.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Establishes a connection to the MASSim-Server. Firstly a socket is
	 * created. Secondly an authenticication message is sent. Finally an reply
	 * from the server is expected.
	 */
	public void establishConnection() {

		// create a socket
		try {
			socket = new Socket(host, port);
			inputstream = socket.getInputStream();
			outputstream = socket.getOutputStream();
		} catch (UnknownHostException e) {
			println("unknown host " + e.getMessage());
			return;
		} catch (IOException e) {
			println(e.getMessage());
			return;
		}
		// println("host=" + host);
		// println("port=" + port);
		println("socket successfully created");

		// authenticate.. send XML message and wait for response
		println("sending authenticiation message");
		boolean result = authenticate(username, password);
		if (result == true) {
			println("authenticiation acknowledged");
		} else {
			println("authenticiation denied");
			return;
		}

		lastUsedActionId = -1;
		currentActionId = -1;
		lastUsedActionIdPercept = -1;
		connected = true;
		println("connection successfully authenticated");

		// start a listening thread
		new Thread(this).start();
		println("listening for incoming messages");

	}

	/**
	 * Sends an authentication-message to the server and waits for the reply.
	 * 
	 * @param username
	 *            the username of the cowboy.
	 * @param password
	 *            the password of the cowboy.
	 * @return true on success.
	 */
	public boolean authenticate(String username, String password) {

		// 1. Send message

		// the document to be sent
		Document doc = null;

		// construct the auth-request-message
		try {

			doc = documentbuilderfactory.newDocumentBuilder().newDocument();
			Element root = doc.createElement("message");
			root.setAttribute("type", "auth-request");
			doc.appendChild(root);

			Element auth = doc.createElement("authentication");
			auth.setAttribute("username", username);
			auth.setAttribute("password", password);
			root.appendChild(auth);

		} catch (ParserConfigurationException e) {

			System.err
					.println("unable to create new document for authentication.");

			// could but should not happen
			return false;

		}

		// sending the document
		try {
			sendDocument(doc);
		} catch (IOException e1) {
			println(e1.getMessage());
			println("Sending document failed");
			return false;
		}

		// 2. receive reply
		Document reply;
		try {
			reply = receiveDocument();
		} catch (IOException e) {

			e.printStackTrace();
			return false;

		}

		// check for success
		Element root = reply.getDocumentElement();
		if (root == null)
			return false;
		if (!root.getAttribute("type").equalsIgnoreCase("auth-response"))
			return false;
		NodeList nl = root.getChildNodes();
		Element authresult = null;
		for (int i = 0; i < nl.getLength(); i++) {
			Node n = nl.item(i);
			if (n.getNodeType() == Element.ELEMENT_NODE
					&& n.getNodeName().equalsIgnoreCase("authentication")) {
				authresult = (Element) n;
				break;
			}
		}
		if (!authresult.getAttribute("result").equalsIgnoreCase("ok"))
			return false;

		// success
		this.username = username;
		this.password = password;

		return true;

	}

	/**
	 * Receives a document
	 * 
	 * @return the received document.
	 * @throws IOException
	 *             is thrown if reception failed.
	 */
	public Document receiveDocument() throws IOException {

		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		int read = inputstream.read();
		while (read != 0) {
			if (read == -1) {
				throw new IOException();
			}
			buffer.write(read);
			try {
				read = inputstream.read();
			} catch (IOException e) {
				throw new IOException("Reading from input-stream failed.");
			}
		}

		byte[] raw = buffer.toByteArray();

		Document doc;
		try {
			doc = documentbuilderfactory.newDocumentBuilder().parse(
					new ByteArrayInputStream(raw));
		} catch (SAXException e) {
			throw new IOException("Error parsing");
		} catch (IOException e) {
			throw new IOException("Error parsing");
		} catch (ParserConfigurationException e) {
			throw new IOException("Error parsing");
		}

		if (xml)
			println(XMLToString(doc) + " received");
		return doc;

	}

	/**
	 * Sends a document.
	 * 
	 * @param doc
	 *            is the document to be sent.
	 * @throws IOException
	 *             is thrown if the document could not be sent.s
	 */
	private void sendDocument(Document doc) throws IOException {

		try {
			transformerfactory.newTransformer().transform(new DOMSource(doc),
					new StreamResult(outputstream));
			ByteArrayOutputStream temp = new ByteArrayOutputStream();
			transformerfactory.newTransformer().transform(new DOMSource(doc),
					new StreamResult(temp));
			outputstream.write(0);
			outputstream.flush();

		} catch (TransformerConfigurationException e) {
			throw new IOException("transformer config error");
		} catch (TransformerException e) {
			System.out.println(XMLToString(doc));
			System.out.println(e.getMessage());
			throw new IOException("transformer error");
		} catch (IOException e) {
			throw new IOException();
		}

		if (xml)
			println(XMLToString(doc) + " sent");

	}

	protected void println(Object obj) {
		// assert obj != null;
		System.out.println("Entity " + name + ": " + obj);
	}

	public static Entity createEntity(String name, String scenario,
			String host, int port, String username, String password) {

		// create an entity specialized to the scenario
		Entity ret = null;
		if (scenario.equals("mars2011")) {
			ret = new Mars2011Entity();
		} else if (scenario.equals("mars2012")) {
			ret = new Mars2012Entity();
		} else if (scenario.equals("mars2013")) {
			ret = new Mars2013Entity();
		} else if (scenario.equals("cowboys2010")) {
			ret = new CowboysEntity();
		} else if (scenario.equals("cowboys2009")) {
			ret = new CowboysEntity();
		} else if (scenario.equals("cowboys2008")) {
			ret = new CowboysEntity();
		} else if (scenario.equals("goldminers2007")) {
			ret = new GoldminersEntity();
		} else if (scenario.equals("goldminers2006")) {
			ret = new GoldminersEntity();
		} else {
			assert false : "handle scenario " + scenario;
		}

		// set common fields
		ret.name = name;
		// TODO take type into account ... newXXXEntity...
		ret.host = host;
		ret.port = port;
		ret.username = username;
		ret.password = password;

		return ret;

	}

	/**
	 * Yields the type of the entity.
	 * 
	 * @return type
	 */
	abstract public String getType();

	/**
	 * Yields all percepts or the first one in the queue, if queuing is
	 * activated
	 * 
	 * @return a linked list containing all percepts or the first one in the
	 *         queue, if queuing is activated
	 * @throws ActException
	 */
	public LinkedList<Percept> getAllPercepts() throws PerceiveException {

		if (scheduling == true && !queuedPercepts) {
			long startTime = System.currentTimeMillis();
			while (currentActionId <= lastUsedActionIdPercept
					|| currentActionId == -1) {
				// System.out.println("waiting for valid id. last=" +
				// lastUsedActionIdPercept + " current=" + currentActionId);
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
				}
				long diff = System.currentTimeMillis() - startTime;
				if (diff >= timeout) {
					// releaseConnection();
					throw new PerceiveException(
							"timeout. no valid action-id available in time");
				}
			}
			lastUsedActionIdPercept = currentActionId;
		}

		if (!queuedPercepts) {
			// return all percepts
			LinkedList<Percept> ret = new LinkedList<Percept>();
			ret.addAll(simStartPercepts);
			ret.addAll(requestActionPercepts);
			ret.addAll(simEndPercepts);
			ret.addAll(byePercepts);
			if (iilang)
				println(ret);
			return ret;
		}

		else {
			// return only the first queued elements
			LinkedList<Percept> ret = new LinkedList<Percept>();
			if (perceptsQueue.peek() != null) {
				ret.addAll(perceptsQueue.poll());
			}
			return ret;
		}
	}

	/**
	 * Maps an IILang-action to XML.
	 * 
	 * @param action
	 * @return XML {@link Document}
	 */
	abstract protected Document actionToXML(Action action);

	/**
	 * Maps the sim-start-message (XML) to IILang.
	 * 
	 * @param document
	 * @return {@link Percept}s
	 */
	abstract protected Collection<Percept> simStartToIIL(Document document);

	/**
	 * Maps the request-action-message (XML) to IILang.
	 * 
	 * @param document
	 * @return {@link Percept}s
	 */
	abstract protected Collection<Percept> requestActionToIIL(Document document);

	/**
	 * Maps the sim-end-message (XML) to IILang.
	 * 
	 * @param document
	 * @return {@link Percept}s
	 */
	abstract protected Collection<Percept> simEndToIIL(Document document);

	/**
	 * Maps the bye-message (XML) to IILang.
	 * 
	 * @param document
	 * @return {@link Percept}s
	 */
	abstract protected Collection<Percept> byeToIIL(Document document);

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 * 
	 * Listens for incoming messages. If a message is received it will be
	 * transformed to an IILang-Action and handled.
	 */
	public void run() {

		while (true) {

			if (connected == false) {
				establishConnection();
				continue;
			}

			Document doc = null;
			try {
				doc = receiveDocument();
				// println(XMLToString(doc));
			} catch (IOException e) {
				// println(e.getMessage());
				releaseConnection();
				continue;
			}

			if (doc != null) {
				// println("received document ");
			} else {
				// println("could not receive document");
				continue;
			}

			// getting the type of the message
			String type = null;
			Element root = doc.getDocumentElement();
			type = root.getAttribute("type");
			// println("message type is " + type);

			if (type.equalsIgnoreCase("sim-start")) {
				if (statsActivated)
					this.stats.logSimStartPercept(getName());
				// simEndPercepts.clear();
				simStartPercepts.clear();
				simStartPercepts.add(new Percept("simStart"));
				simStartPercepts.addAll(simStartToIIL(doc));
				if (timeAnnotations) {
					String str = root.getAttribute("timestamp");
					long timeStamp = new Long(str).longValue();
					annotatePercepts(simStartPercepts, new Numeral(timeStamp));
				}
				if (notifications)
					ei.sendNotifications(this.getName(), simStartPercepts);
				if (gatherSpecimens)
					processSpecimens(simStartPercepts);

				if (queuedPercepts) {
					Collection<Percept> perceptCollection = Collections
							.synchronizedSet(new HashSet<Percept>());
					perceptCollection.addAll(simStartPercepts);
					perceptsQueue.add(perceptCollection);
				}

				// add role to entitytype
				if (this instanceof Mars2012Entity) {
					NodeList tags = root.getElementsByTagName("simulation");
					Element tag = (Element) tags.item(0);
					String str = tag.getAttribute("role");
					((Mars2012Entity) this).setRole(str);
				}
			}

			else if (type.equalsIgnoreCase("request-action")) {
				if (statsActivated)
					this.stats.logRequestActionPercept(getName());
				String str = null;
				NodeList tags = root.getElementsByTagName("perception");
				assert tags.getLength() == 1;
				Element tag = (Element) tags.item(0);
				str = tag.getAttribute("id");
				assert str != null && str.equals("") == false;
				int id = new Integer(str).intValue();

				// grab some info from the percept and post it to statistic
				if (statsActivated) {
					// <self>
					tags = root.getElementsByTagName("self");
					tag = (Element) tags.item(0);
					str = tag.getAttribute("lastActionResult");
					this.stats.submitActionResult(getName(),
							tag.getAttribute("lastAction"), str);

					str = tag.getAttribute("zoneScore");
					// </self>
					// <team>
					tags = root.getElementsByTagName("team");
					tag = (Element) tags.item(0);
					str = tag.getAttribute("zonesScore");
					if (!str.equals(""))
						this.stats.submitZonesScore(Integer.decode(str),
								getName());
					// <achievements>
					tags = tag.getElementsByTagName("achievements");
					tag = (Element) tags.item(0);
					if (tag != null) {
						tags = tag.getElementsByTagName("achievement");
						for (int i = 0; i < tags.getLength(); i++) {
							tag = (Element) tags.item(0);
							this.stats.submitAchievement(
									tag.getAttribute("name"), getName());
						}
					}
					// </achievements>
					// </team>
				}

				// println("current action id " + currentActionId);
				requestActionPercepts.clear();
				requestActionPercepts.add(new Percept("requestAction"));
				requestActionPercepts.addAll(requestActionToIIL(doc));
				if (timeAnnotations) {
					str = root.getAttribute("timestamp");
					assert str != null && str.equals("") == false;
					long timeStamp = new Long(str).longValue();
					annotatePercepts(requestActionPercepts, new Numeral(
							timeStamp));
				}
				if (notifications)
					ei.sendNotifications(this.getName(), requestActionPercepts);
				if (gatherSpecimens)
					processSpecimens(requestActionPercepts);
				currentActionId = id;
				if (statsActivated)
					stats.submitRequest(name, id);

				if (queuedPercepts) {
					Collection<Percept> perceptCollection = Collections
							.synchronizedSet(new HashSet<Percept>());
					perceptCollection.addAll(requestActionPercepts);
					perceptsQueue.add(perceptCollection);
				}
			} else if (type.equalsIgnoreCase("sim-end")) {
				if (statsActivated) {
					this.stats.logSimEndPercept(getName());
					this.stats.onSimulationEnd();
					this.simCount++;
					this.stats = Entity.ei.requestNewStatistics(simCount);
				}
				simStartPercepts.clear();
				requestActionPercepts.clear();
				simEndPercepts.clear();
				simEndPercepts.add(new Percept("simEnd"));
				simEndPercepts.addAll(simEndToIIL(doc));
				if (timeAnnotations) {
					String str = root.getAttribute("timestamp");
					assert str != null && str.equals("") == false;
					long timeStamp = new Long(str).longValue();
					annotatePercepts(simEndPercepts, new Numeral(timeStamp));
				}
				if (notifications)
					ei.sendNotifications(this.getName(), simEndPercepts);
				if (gatherSpecimens)
					processSpecimens(simEndPercepts);

				if (queuedPercepts) {
					Collection<Percept> perceptCollection = Collections
							.synchronizedSet(new HashSet<Percept>());
					perceptCollection.addAll(simEndPercepts);
					perceptsQueue.add(perceptCollection);
				}

			} else if (type.equalsIgnoreCase("bye")) {
				if (statsActivated)
					this.stats.logByePercept(getName());
				simStartPercepts.clear();
				requestActionPercepts.clear();
				// simEndPercepts.clear();
				byePercepts.clear();
				byePercepts.add(new Percept("bye"));
				// byePercepts.addAll(byeToIIL(doc));
				if (timeAnnotations) {
					String str = root.getAttribute("timestamp");
					assert str != null && str.equals("") == false;
					long timeStamp = new Long(str).longValue();
					annotatePercepts(byePercepts, new Numeral(timeStamp));
				}
				if (notifications)
					ei.sendNotifications(this.getName(), byePercepts);
				if (gatherSpecimens) {
					processSpecimens(byePercepts);
				}

				if (queuedPercepts) {
					Collection<Percept> perceptCollection = Collections
							.synchronizedSet(new HashSet<Percept>());
					perceptCollection.addAll(byePercepts);
					perceptsQueue.add(perceptCollection);
				}

			} else {
				println("unexpected type " + type);
			}

			if (gatherSpecimens) {
				printSpecimens();
			}
		}

	}

	private void annotatePercepts(Collection<Percept> percepts, Parameter param) {
		for (Percept p : percepts)
			p.addParameter(param);
	}

	/**
	 * Performs an action. The action is fistly transformed to an XML-message.
	 * Secondly it is sent off.
	 * 
	 * @param action
	 * @throws ActException
	 */
	public void performAction(Action action) throws ActException {

		if (connected == false) {
			establishConnection();
			if (connected == false) {
				releaseConnection();
				throw new ActException(ActException.FAILURE,
						"no valid connection");
			}
		}

		// waiting for a valid action id
		long startTime = System.currentTimeMillis();
		if (scheduling == true) {
			while (currentActionId <= lastUsedActionId || currentActionId == -1) {
				// System.out.println("waiting for valid id. last=" +
				// lastUsedActionId + " current=" + currentActionId);
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
				}
				long diff = System.currentTimeMillis() - startTime;
				if (diff >= timeout) {
					// releaseConnection();
					throw new ActException(ActException.FAILURE,
							"timeout. no valid action-id available in time");
				}
			}
		}

		//
		Document doc = null;
		doc = actionToXML(action);

		// send
		try {
			assert currentActionId != lastUsedActionId;
			// println("using action id " + currentActionId);
			sendDocument(doc);
			lastUsedActionId = currentActionId;
		} catch (IOException e) {
			// e.printStackTrace();
			// TODO some text
			// System.out.println("connection lost");
			// connected = false;
			releaseConnection();
			throw new ActException(ActException.FAILURE,
					"sending failed action", e);
		}

		// println("sent");
		if (statsActivated)
			stats.submitAction(name, currentActionId, action.getName(),
					startTime);
	}

	/**
	 * Converts an XML-node to a simple string.
	 * 
	 * @param node
	 * @return string representation of xml node
	 */
	public static String XMLToString(Node node) {
		try {
			Source source = new DOMSource(node);
			StringWriter stringWriter = new StringWriter();
			Result result = new StreamResult(stringWriter);
			TransformerFactory factory = TransformerFactory.newInstance();
			Transformer transformer = factory.newTransformer();
			transformer.transform(source, result);
			return stringWriter.getBuffer().toString();
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Yields the current action-id.
	 * 
	 * @return action number
	 */
	protected int getCurrentActionId() {
		return currentActionId;
	}

	private void releaseConnection() {

		if (socket != null) {

			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		println("connection released");

	}

	/**
	 * Returns true if the entity is connected to the MASSim-server.
	 * 
	 * @return true iff connected
	 */
	public boolean isConnected() {
		return connected;
	}

	public static void enableScheduling() {
		scheduling = true;
		System.out.println("scheduling enabled");
	}

	public static void enableTimeAnnotations() {
		timeAnnotations = true;
		System.out.println("time-annotations enabled");
	}

	public static void enableNotifications() {
		notifications = true;
		System.out.println("notifications enabled");
	}

	public static void setEnvironmentInterface(EnvironmentInterface ei) {
		Entity.ei = ei;
	}

	public static void setTimeout(int t) {
		timeout = t;
		System.out.println("timeout set to " + t + "ms");
	}

	private void processSpecimens(Collection<Percept> percepts) {

		for (Percept p : percepts) {

			// check if we already have a specimen
			boolean contained = false;
			for (Percept p2 : perceptSpecimens) {
				if (p.getName().equals(p2.getName())) {
					contained = true;
					break;
				}
			}
			if (contained)
				continue;

			// add sorted
			boolean added = false;
			for (Percept p2 : perceptSpecimens) {

				if (p.getName().compareTo(p2.getName()) > 0)
					continue;
				perceptSpecimens.add(perceptSpecimens.indexOf(p2), p);
				added = true;
				break;
			}
			if (added == false)
				perceptSpecimens.add(p);
		}

	}

	private void printSpecimens() {

		for (Percept p : perceptSpecimens) {
			System.out.println("specimen: " + p.toProlog());
		}

	}

	public void setStatistic(Statistic stats) {
		this.stats = stats;
	}

	public static void activateStatistics() {
		statsActivated = true;
	}

	public static void activatePerceptQueue() {
		queuedPercepts = true;
	}
}