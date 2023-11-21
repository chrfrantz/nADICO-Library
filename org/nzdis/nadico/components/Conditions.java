package org.nzdis.nadico.components;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.nzdis.nadico.NAdicoExpression;

public final class Conditions<T> implements Serializable {

	/**
	 * Conditions
	 */
	public final LinkedHashMap<String, T> properties = new LinkedHashMap<>();
	
	/**
	 * Wildcard condition
	 */
	public static final Conditions WILDCARD_CONDITION = new Conditions();
	
	/**
	 * Key for previous action
	 */
	public static final String PREVIOUS_ACTION = "PREVIOUS_ACTION";
	
	/**
	 * Instantiates empty conditions.
	 */
	public Conditions() {
		
	}
	
	/**
	 * Copy constructor for deep copy
	 * @param conditions
	 */
	public Conditions(Conditions<T> conditions) {
	    if (conditions == null) {
	        // copies statement with empty conditions
            //System.out.println("nADICO: Deep copying statement with empty conditions");
            return;
        }
		LinkedHashMap<String,T> map = conditions.properties;
		for (Entry<String, T> entry: map.entrySet()) {
			if (entry.getValue().getClass().equals(NAdicoExpression.class)) {
				T expr = (T) new NAdicoExpression((NAdicoExpression)entry.getValue());
				this.properties.put(entry.getKey(), expr);
				continue;
			}
			System.err.println("NAdicoExpression Conditions contain unknown object type (Key: " + 
					entry.getKey() + ", Value: " + entry.getValue() + ", Value Type: " +
					entry.getValue().getClass() + 
					"). Performing simply copying. Verify proper deep copy functionality in Conditions.java.");
			this.properties.put(entry.getKey(), entry.getValue());
		}
	}
	
	/**
	 * Instantiates Conditions with previous action as parameter
	 * @param previousAction Value for previous action
	 */
	public Conditions(final T previousAction) {
		setPreviousAction(previousAction);
	}
	
	/**
	 * Instantiates Conditions with given condition properties.
	 * @param conditionsKey Key associated with condition
	 * @param conditionsValue Value associated with condition
	 */
	public Conditions(final String conditionsKey, final T conditionsValue) {
		properties.put(conditionsKey, conditionsValue);
	}
	
	/**
	 * Instantiates Conditions with given properties. Properties must be specified 
	 * as key-value pairs.
	 * @param properties Conditions as key-value pairs
	 */
	public Conditions(Object... properties) {
		addProperties(properties);
	}
	
	/**
	 * Adds individual properties (in pairs of key and value) to the conditions instance.
	 * @param properties Property pairs to be added
	 * @return the conditions instance itself for easy inline use
	 */
	public Conditions<T> addProperties(Object... properties) {
		// Length must be even
		if (properties.length % 2 != 0) {
			throw new IllegalArgumentException("Conditions property arguments must be specified in key-value pairs.");
		}
		for (int i = 0; i < properties.length; i++) {
			this.properties.put(properties[i].toString(), (T)properties[i+1]);
			// additional increment
			i++;
		}
		return this;
	}
	
	/**
	 * Sets the previous action.
	 * @param previousAction
	 * @return this Conditions instance including the added/updated previous action
	 */
	public Conditions<T> setPreviousAction(final T previousAction) {
		properties.put(PREVIOUS_ACTION, previousAction);
		return this;
	}
	
	/**
	 * Returns the previous action stored in the properties.
	 * @return
	 */
	public T getPreviousAction() {
		return properties.get(PREVIOUS_ACTION);
	}
	
	/**
	 * Removes previous action from properties.
	 * @return Removed action or null of none found
	 */
	public T removePreviousAction() {
		return properties.remove(PREVIOUS_ACTION);
	}
	
	/**
	 * Clears conditions and returns reference to itself.
	 * @return empty conditions instance
	 */
	public Conditions<T> clear() {
		properties.clear();
		return this;
	}
	
	/**
	 * Copies all property values from input instance and returns itself.
	 * If the input is null, the returned instance is empty.
	 * @param instanceWhoseValuesToCopy
	 * @return Conditions instances with copied values
	 */
	public Conditions<T> copyFrom(final Conditions<T> instanceWhoseValuesToCopy) {
		if (instanceWhoseValuesToCopy != null) {
			this.properties.putAll(instanceWhoseValuesToCopy.properties);
		}
		return this;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((properties == null) ? 0 : properties.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Conditions other = (Conditions) obj;
		if (properties == null) {
			if (other.properties != null)
				return false;
		} else if (!properties.equals(other.properties))
			return false;
		//System.out.println("Conditions match!");
		return true;
	}

	@Override
	public String toString() {
		return "C(" + ((properties == null || properties.isEmpty()) ? "*" : properties) + ")";
	}

}
