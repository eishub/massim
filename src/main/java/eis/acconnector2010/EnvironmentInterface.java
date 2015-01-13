package eis.acconnector2010;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;

import eis.EIDefaultImpl;
import eis.exceptions.ActException;
import eis.exceptions.EntityException;
import eis.exceptions.EnvironmentInterfaceException;
import eis.exceptions.NoEnvironmentException;
import eis.iilang.Action;
import eis.iilang.Identifier;
import eis.iilang.Numeral;
import eis.iilang.Parameter;
import eis.iilang.Percept;

/**
 * Establishes connections to the MASSim-Server.
 * 
 * Entities are single connection to the server.
 * 
 * @author tristanbehrens
 * @author W.Pasman #3396 modified for EIS0.5
 * 
 *
 */
@SuppressWarnings("serial")
public class EnvironmentInterface extends EIDefaultImpl implements

ConnectionListener, Runnable {
	/*
	 * This EnvironmentInterface does not fit with {@link AbstractEnvironment}
	 * because we are going to receive a complete List<Percept> from the server.
	 * Splitting that into separate Percepts to meet the requirements of
	 * AbstractEnv does not make sense.
	 */

	/**
	 * Used to facilitate the EIS-to-environment connection. Each entity is a
	 * connection.
	 */
	private HashMap<String, Connection> entitiesToConnections = new HashMap<String, Connection>();

	/** Stores the percept-queue. getAllPercepts returns the first element. */
	private HashMap<String, LinkedList<LinkedList<Percept>>> entitiesToPercepts = new HashMap<String, LinkedList<LinkedList<Percept>>>();

	/** For checking connections. */
	private boolean running = true;

	/**
	 * Contructs an environment.
	 */
	public EnvironmentInterface() {

		// add all entities
		// we have 20 cowboys in 2010
		try {

			addEntity("connector1");
			addEntity("connector2");
			addEntity("connector3");
			addEntity("connector4");
			addEntity("connector5");
			addEntity("connector6");
			addEntity("connector7");
			addEntity("connector8");
			addEntity("connector9");
			addEntity("connector10");
			addEntity("connector11");
			addEntity("connector12");
			addEntity("connector13");
			addEntity("connector14");
			addEntity("connector15");
			addEntity("connector16");
			addEntity("connector17");
			addEntity("connector18");
			addEntity("connector19");
			addEntity("connector20");

		} catch (EntityException e) {
			e.printStackTrace();
		}

		// start connections checker
		new Thread(this).start();

	}

	/**
	 * Returns the percepts of an entity. That is first percept in the queue,
	 * that contains the percepts sent by the server.
	 */
	@Override
	public LinkedList<Percept> getAllPerceptsFromEntity(String entity) {

		LinkedList<LinkedList<Percept>> queue = entitiesToPercepts.get(entity);

		if (queue == null)
			return new LinkedList<Percept>();

		if (queue.isEmpty())
			return new LinkedList<Percept>();

		LinkedList<Percept> ret = queue.removeFirst();

		return ret;

	}

	@Override
	protected Percept performEntityAction(String entity, Action action)
			throws ActException {
		try {
			return performEntityActionByIntrospection(entity, action);
		} catch (EntityException | NoSuchMethodException | SecurityException
				| IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw new ActException("execute action " + action + " failed", e);
		}

	}

	private Percept performEntityActionByIntrospection(String entity,
			Action action) throws ActException, EntityException,
			NoSuchMethodException, SecurityException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {
		if (getAssociatedAgents(entity).isEmpty()) {
			throw new ActException(ActException.NOTREGISTERED);
		}

		// get the parameters
		LinkedList<Parameter> params = action.getParameters();
		// targetEntities contains all entities that should perform the action
		// params contains all parameters
		// determine class parameters for finding the method
		// and store the parameters as objects
		Class<?>[] classParams = new Class[params.size() + 1];
		classParams[0] = String.class; // entity name
		for (int a = 0; a < params.size(); a++)
			classParams[a + 1] = params.get(a).getClass();

		// lookup the method
		String methodName = "action" + action.getName();
		// System.out.println(methodName);
		Method m = this.getClass().getMethod(methodName, classParams);
		m.setAccessible(true);

		// invoke
		Object[] objParams = new Object[params.size() + 1];
		objParams[0] = entity; // entity name
		for (int a = 0; a < params.size(); a++)
			objParams[a + 1] = params.get(a);
		Percept ret = (Percept) m.invoke(this, objParams);

		return ret;
	}

	@Override
	public void kill() {

		// stop execution
		running = false;

		// take down all connections

		for (Connection c : this.entitiesToConnections.values())
			try {
				c.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	/**
	 * Connects to a MASSim-server.
	 * 
	 * @param entity
	 *            the entity that is going to be associated to the connection.
	 * @param server
	 *            the server.
	 * @param port
	 *            the port
	 * @param user
	 *            the username
	 * @param password
	 *            the password
	 * @return the result of the action, either connected of failed.
	 * @throws ActException
	 */
	public Percept actionconnect(String entity, Identifier server,
			Numeral port, Identifier user, Identifier password)
			throws ActException {

		Connection c = this.entitiesToConnections.get(entity);

		// there is a connection; close ot
		if (c != null) {

			try {
				c.close();
			} catch (IOException e) {
				throw new ActException(ActException.FAILURE,
						"Existing connection could not be closed", e);
			}

			c = null;

		}

		assert c == null;

		// open socket
		try {

			c = new Connection(this, server.getValue(),
					(port.getValue()).intValue());

		} catch (UnknownHostException e) {

			throw new ActException(ActException.FAILURE, "Unknown host "
					+ server.getValue());

		} catch (IOException e) {

			e.printStackTrace();
			throw new ActException(ActException.FAILURE, "IO Error");

		}

		assert c != null;

		// authent
		boolean result = c.authenticate(user.getValue(), password.getValue());

		if (result == false) {

			LinkedList<Percept> percepts = new LinkedList<Percept>();
			Percept percept = new Percept("failed");
			percepts.add(percept);
			enqueuePercepts(entity, percepts);

			return percept;

		}

		// store
		entitiesToConnections.put(entity, c);

		// start thread
		new Thread(c).start();

		// return success
		LinkedList<Percept> percepts = new LinkedList<Percept>();
		Percept percept = new Percept("connected");
		percepts.add(percept);
		enqueuePercepts(entity, percepts);

		return percept;

	}

	/**
	 * Puts a list of percepts into the percept-queue.
	 * 
	 * @param entity
	 * @param percepts
	 */
	private void enqueuePercepts(String entity, LinkedList<Percept> percepts) {

		LinkedList<LinkedList<Percept>> queue = entitiesToPercepts.get(entity);

		if (queue == null) {

			queue = new LinkedList<LinkedList<Percept>>();

		}

		queue.add(percepts);

		entitiesToPercepts.put(entity, queue);

	}

	/**
	 * Movement action.
	 * 
	 * @param entity
	 * @param direction
	 * @return the result of the action
	 * @throws ActException
	 * @throws NoEnvironmentException
	 */
	public Percept actionmove(String entity, Identifier direction)
			throws ActException, NoEnvironmentException {

		Connection c = this.entitiesToConnections.get(entity);

		if (c == null)
			throw new NoEnvironmentException("Not connected");

		c.act(direction.getValue());

		return new Percept("moved");

	}

	/**
	 * Skip action.
	 * 
	 * @param entity
	 * @return the result of the action
	 * @throws ActException
	 * @throws NoEnvironmentException
	 */
	public Percept actionskip(String entity) throws ActException,
			NoEnvironmentException {

		Connection c = this.entitiesToConnections.get(entity);

		if (c == null)
			throw new NoEnvironmentException("Not connected");

		c.act("skip");

		return new Percept("skipped");

	}

	/**
	 * Handles an incoming message.
	 */
	public void handleMessage(Connection connection,
			LinkedList<Percept> percepts) {

		for (Entry<String, Connection> e : this.entitiesToConnections
				.entrySet()) {

			// look up entity
			if (connection.equals(e.getValue())) {

				String entity = e.getKey();

				// store last percept
				enqueuePercepts(entity, percepts);

				// notify agents
				for (Percept p : percepts)
					try {
						this.notifyAgentsViaEntity(p, entity);
					} catch (EnvironmentInterfaceException e1) {
						e1.printStackTrace();
					}

			}

		}

		// System.out.println(container);

	}

	/**
	 * Prefixes a list of percepts. Returns a new list that contains the same
	 * percepts with a prefix added.
	 * 
	 * @param percepts
	 *            the percepts to be prefixed
	 * @param prefix
	 *            the prefix itself
	 * @return a new list with prefixed percepts
	 */
	private LinkedList<Percept> prefixPercepts(LinkedList<Percept> percepts,
			String prefix) {

		LinkedList<Percept> ret = new LinkedList<Percept>();

		for (Percept p : percepts) {

			Percept prefixed = (Percept) p.clone();
			prefixed.setName(prefix + p.getName());
			ret.add(prefixed);

		}

		return ret;

	}

	/**
	 * Check all connections every couple of seconds.
	 */
	public void run() {

		while (running) {

			// check the connections
			for (Entry<String, Connection> entry : this.entitiesToConnections
					.entrySet()) {

				String e = entry.getKey();
				Connection c = entry.getValue();

				if (c.isBound() == false || c.isClosed() == true
						|| c.isConnected() == false || c.isExecuting() == false) {
					entitiesToConnections.remove(e);
				}

			}

			try {
				Thread.sleep(60000);
			} catch (InterruptedException e) {
			}

		}

	}

	@Override
	protected boolean isSupportedByEntity(Action arg0, String arg1) {
		return true;
	}

	@Override
	protected boolean isSupportedByEnvironment(Action arg0) {
		return true;
	}

	@Override
	protected boolean isSupportedByType(Action arg0, String arg1) {
		return true;
	}

}
