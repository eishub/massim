package eis.eis2java.handlers;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import eis.eis2java.annotation.AsPercept;
import eis.eis2java.exception.TranslationException;
import eis.eis2java.translation.Filter;
import eis.eis2java.translation.Translator;
import eis.exceptions.PerceiveException;
import eis.iilang.Function;
import eis.iilang.Parameter;
import eis.iilang.Percept;

/**
 * Abstract handler for percepts. Provides access to the entity, the translation
 * and unpacking phases of EIS2java.
 * 
 * @author mpkorstanje
 * 
 */
public abstract class AbstractPerceptHandler extends PerceptHandler {

	/**
	 * The entity associated with this handler.
	 */
	protected final Object entity;

	public AbstractPerceptHandler(Object entity) {
		assert entity != null;
		this.entity = entity;
	}

	/**
	 * map of previous percepts that we got for each method.
	 */
	protected final Map<Method, List<Object>> previousPercepts = new HashMap<Method, List<Object>>();

	/**
	 * Translates the percept objects and applies filtering as described by
	 * {@link AsPercept#filter()}.
	 * 
	 * @param entity
	 *            the entity which produced the percepts
	 * @param method
	 *            the method which produced the percepts
	 * @param perceptObjects
	 *            the generated percept objects.
	 * @return
	 * @throws PerceiveException
	 */
	protected final List<Percept> translatePercepts(Method method,
			List<Object> perceptObjects) throws PerceiveException {
		// the add and delete list based on the perceived objects
		// list of object that we had last round but not at this moment.
		List<Object> addList = new ArrayList<Object>();
		List<Object> delList = new ArrayList<Object>();

		AsPercept annotation = method.getAnnotation(AsPercept.class);
		Filter.Type filter = annotation.filter();
		String perceptName = annotation.name();

		List<Object> previous = previousPercepts.get(method);

		// Avoid translating objects that don't need to be translated.
		if (filter == Filter.Type.ONCE && previous != null) {
			return new ArrayList<Percept>();
		}

		if (previous == null) {
			previous = new LinkedList<Object>();
			previousPercepts.put(method, previous);
		}

		// do the proper filtering.
		switch (filter) {
		case ALWAYS:
		case ONCE:
			addList = perceptObjects;
			break;
		case ON_CHANGE:
			if (!perceptObjects.equals(previous)) {
				addList = perceptObjects;
			}
			break;
		case ON_CHANGE_NEG:
			addList.addAll(perceptObjects);
			if (previous != null) {
				addList.removeAll(previous);
				delList.addAll(previous);
				delList.removeAll(perceptObjects);
			}
			break;
		}

		// Translate addList.
		List<Percept> percepts = new ArrayList<Percept>();
		for (Object javaObject : addList) {
			Parameter[] parameters;
			try {
				parameters = Translator.getInstance().translate2Parameter(
						javaObject);
			} catch (TranslationException e) {
				throw new PerceiveException("Unable to translate percept "
						+ perceptName, e);
			}
			percepts.add(new Percept(perceptName, parameters));
		}

		// Translate delList.
		for (Object javaObject : delList) {
			Parameter[] parameters;
			try {
				parameters = Translator.getInstance().translate2Parameter(
						javaObject);
			} catch (TranslationException e) {
				throw new PerceiveException("Unable to translate percept "
						+ perceptName, e);
			}
			percepts.add(new Percept("not", new Function(perceptName,
					parameters)));
		}

		previousPercepts.put(method, perceptObjects);
		return percepts;
	}

	/**
	 * Depending on {@link AsPercept#multiplePercepts()} a perceptObject is
	 * either a collection that contains multiple percept objects or a single
	 * percept. This method unpacks either case into a list of percept objects.
	 * 
	 * @param method
	 *            that generated the percept object
	 * @param perceptObject
	 *            the perceptObject generated by the method.
	 * @return a unpacked version of the percept object
	 * @throws PerceiveException
	 */
	protected final List<Object> unpackPerceptObject(Method method,
			Object perceptObject) throws PerceiveException {
		AsPercept annotation = method.getAnnotation(AsPercept.class);
		String perceptName = annotation.name();

		// This is percept does not provide multiples.
		if (!annotation.multiplePercepts()) {
			List<Object> unpacked = new ArrayList<Object>(1);
			if (perceptObject != null) {
				unpacked.add(perceptObject);
			}

			return unpacked;
		}

		// The method claims to provide multiple but doesn't provide a
		// collection.
		if (!(perceptObject instanceof Collection<?>)) {
			throw new PerceiveException("Unable to perceive " + perceptName
					+ " because a collection was expected but a "
					+ perceptObject.getClass() + " was returned instead");
		}

		// The multiple percepts are a collection, put them in a list.
		Collection<?> javaCollection = (Collection<?>) perceptObject;
		ArrayList<Object> unpacked = new ArrayList<Object>(javaCollection);

		return unpacked;
	}

}