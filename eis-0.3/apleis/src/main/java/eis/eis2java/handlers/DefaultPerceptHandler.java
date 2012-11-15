package eis.eis2java.handlers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import eis.eis2java.annotation.AsPercept;
import eis.eis2java.translation.Filter;
import eis.eis2java.util.EIS2JavaUtil;
import eis.exceptions.EntityException;
import eis.exceptions.PerceiveException;
import eis.iilang.Percept;

/**
 * Default {@link PerceptHandler} for EIS2Java. When called the
 * DefaultPercepthandler will call all percept methods on the agent and return
 * and translate their results.
 * 
 * @author mpkorstanje
 * 
 */
public final class DefaultPerceptHandler extends AbstractPerceptHandler {

	/** Collection of methods on the entity */
	protected final Collection<Method> perceptMethods;

	public DefaultPerceptHandler(Object entity) throws EntityException {
		super(entity);
		this.perceptMethods = EIS2JavaUtil.processPerceptAnnotations(entity
				.getClass());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * nl.tudelft.goal.EIS2Java.environment.PerceptHandler#getAllPercepts(java
	 * .lang.Object)
	 */
	@Override
	public final LinkedList<Percept> getAllPercepts() throws PerceiveException {

		LinkedList<Percept> percepts = new LinkedList<Percept>();

		for (Method method : perceptMethods) {
			percepts.addAll(getPercepts(method));
		}

		return percepts;
	}

	/**
	 * Creates new percepts by calling the given method on the entity.
	 * 
	 * @param entity
	 *            the entity to get the percept from.
	 * @param method
	 *            the method to invoke on the entity which must be annotated
	 *            with {@link AsPercept}.
	 * @return The percepts that were generated by invoking the method on the
	 *         entity.
	 * @throws PerceiveException
	 *             If the percepts couldn't be retrieved.
	 */
	private List<Percept> getPercepts(Method method) throws PerceiveException {

		// list of new objects for the percepts
		List<Object> perceptObjects = new ArrayList<Object>();

		// Optimization, don't call methods for once percepts if they have been
		// called before.
		AsPercept annotation = method.getAnnotation(AsPercept.class);
		Filter.Type filter = annotation.filter();
		if (filter != Filter.Type.ONCE || previousPercepts.get(method) == null) {
			perceptObjects = getPerceptObjects(method);
		}

		List<Percept> percepts = translatePercepts(method, perceptObjects);
		return percepts;
	}

	/**
	 * Get the percept objects for given percept name, using method.
	 * 
	 * @param method
	 * @param entity
	 * @param perceptName
	 * @return
	 * @throws PerceiveException
	 */
	private List<Object> getPerceptObjects(Method method)
			throws PerceiveException {

		AsPercept annotation = method.getAnnotation(AsPercept.class);
		String perceptName = annotation.name();

		Object returnValue;
		try {
			returnValue = method.invoke(entity);
		} catch (IllegalArgumentException e) {
			throw new PerceiveException("Unable to perceive " + perceptName, e);
		} catch (IllegalAccessException e) {
			throw new PerceiveException("Unable to perceive " + perceptName, e);
		} catch (InvocationTargetException e) {
			throw new PerceiveException("Unable to perceive " + perceptName, e);
		}

		return unpackPerceptObject(method, returnValue);
	}

}
