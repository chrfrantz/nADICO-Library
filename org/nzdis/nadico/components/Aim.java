package org.nzdis.nadico.components;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

public final class Aim<T> implements Serializable {

	/**
	 * Activity this aim represents
	 */
	public String activity;
	
	/**
	 * Properties of the aim.
	 */
	public final LinkedHashMap<String, T> properties = new LinkedHashMap<>();
	
	/**
	 * Instantiates an empty aim.
	 */
	public Aim() {
		
	}
	
	/**
	 * Instantiates an aim with a given activity.
	 * @param activity Aim identifier
	 */
	public Aim(String activity) {
		this.activity = activity;
	}
	
	/**
	 * Instantiates an aim with a given activity and one property.
	 * @param activity Aim identifier
	 * @param propertyKey Key of additional property
	 * @param propertyValue Value of property
	 */
	public Aim(String activity, String propertyKey, T propertyValue) {
		this.activity = activity;
		this.properties.put(propertyKey, propertyValue);
	}
	
	/**
	 * Instantiates an aim with a give activity and arbitrary number of further properties.
	 * @param activity Aim identifier
	 * @param properties Pairs of properties
	 */
	public Aim(String activity, Object... properties) {
		this.activity = activity;
		addProperties(properties);
	}
	
	/**
	 * Copy constructor for deep copy
	 * @param aim
	 */
	public Aim(Aim<T> aim) {
	    if (aim == null) {
	        // copies statement with empty aim
	        //System.out.println("nADICO: Deep copying statement with empty aim");
            return;
        }
		this.activity = aim.activity;
		LinkedHashMap<String,T> map = aim.properties;
		for (Entry<String, T> entry: map.entrySet()) {
			this.properties.put(entry.getKey(), entry.getValue());
		}
	}
	
	/**
	 * Sets the activity for a given aim.
	 * @param activity
	 * @return the aim itself for easy inline use
	 */
	public Aim<T> setActivity(String activity) {
		this.activity = activity;
		return this;
	}
	
	/**
	 * Adds individual properties (in pairs of key and value) to the aim.
	 * @param properties Property pairs to be added
	 * @return the aim itself for easy inline use
	 */
	public Aim<T> addProperties(Object... properties) {
		// Length must be even
		if (properties.length % 2 != 0) {
			throw new IllegalArgumentException("Aim property arguments must be specified in key-value pairs.");
		}
		for (int i = 0; i < properties.length; i++) {
			this.properties.put(properties[i].toString(), (T)properties[i+1]);
			// additional increment
			i++;
		}
		return this;
	}
	
	/**
	 * Clears aim and returns reference to itself.
	 * @return empty aim instance
	 */
	public Aim<T> clear() {
		this.activity = null;
		this.properties.clear();
		return this;
	}
	
	/**
	 * Copies all values from the input instance and returns itself.
	 * If the input is null, the returned instance is empty.
	 * @param instanceWhoseValuesToCopy
	 * @return Aim instance with copied values
	 */
	public Aim<T> copyFrom(Aim<T> instanceWhoseValuesToCopy) {
	    if (instanceWhoseValuesToCopy != null) {
	        this.activity = instanceWhoseValuesToCopy.activity;
	        this.properties.putAll(instanceWhoseValuesToCopy.properties);
	    }
		return this;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((activity == null) ? 0 : activity.hashCode());
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
		Aim other = (Aim) obj;
		if (activity == null) {
			if (other.activity != null)
				return false;
		} else if (!activity.equals(other.activity))
			return false;
		if (properties == null) {
			if (other.properties != null)
				return false;
		} else if (!properties.equals(other.properties))
			return false;
		//System.out.println("Aim matches.");
		return true;
	}

	@Override
	public String toString() {
		return "I(" + activity + ", " + (properties.isEmpty() ? "*" : properties) + ")";
	}
	
}
