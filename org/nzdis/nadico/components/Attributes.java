package org.nzdis.nadico.components;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Attributes consists of individual and social markers. Both marker types are 
 * stored as HashMaps, with key being String and value being of type T (T extends Set<String>).
 * @author Christopher Frantz
 *
 * @param <T>
 */
public final class Attributes<T extends Set<String>> implements Serializable {

	/**
	 * List of individual markers.
	 */
	public final HashMap<String,T> individualMarkers = new HashMap<>();
	
	/**
	 * List of social markers.
	 */
	public final HashMap<String,T> socialMarkers = new HashMap<>();
	
	/**
	 * Wildcard for attributes.
	 */
	public static final Attributes WILDCARD_CONDITION = new Attributes();
	
	/**
	 * Instantiates empty Attributes.
	 */
	public Attributes(){
		
	}
	
	/**
	 * Copy constructor for deep copy
	 * @param attributes
	 */
	public Attributes(Attributes<T> attributes) {
	    if (attributes == null) {
	        // copies statement with empty attributes
            //System.out.println("nADICO: Deep copying statement with empty attributes");
	        return;
	    }
		HashMap<String,T> map = attributes.individualMarkers;
		for (Entry<String, T> entry: map.entrySet()) {
			Set valueSet = new LinkedHashSet<>();
			for (String element: entry.getValue()) {
				valueSet.add(element);
			}
			this.individualMarkers.put(entry.getKey(), (T) valueSet);
		}
		map = attributes.socialMarkers;
		for (Entry<String, T> entry: map.entrySet()) {
			Set valueSet = new LinkedHashSet<>();
			for (String element: entry.getValue()) {
				valueSet.add(element);
			}
			this.socialMarkers.put(entry.getKey(), (T) valueSet);
		}
	}
	
	/**
	 * Instantiates Attributes with an initial individual marker.
	 * @param firstIndividualMarkerCategory Marker category
	 * @param firstIndividualMarker Marker value
	 */
	public Attributes(final String firstIndividualMarkerCategory, final String firstIndividualMarker) {
		addIndividualMarker(firstIndividualMarkerCategory, firstIndividualMarker);
	}
	
	/**
	 * Adds individual marker and returns attributes
	 * including the added marker.
	 * @param markerCategory Marker category (i.e., key of the category, e.g., "NAME")
	 * @param marker Marker value (i.e., corresponding value for category, e.g., "Agent 1")
	 * @return Attributes component for easy inline use
	 */
	public Attributes<T> addIndividualMarker(final String markerCategory, final String marker) {
		if (!individualMarkers.containsKey(markerCategory)) {
			// Not beautiful, but works for now
			T markers = (T) new LinkedHashSet<String>();
			markers.add(marker);
			individualMarkers.put(markerCategory, markers);
		} else {
			//contains other markers already
			T existingMarkers = individualMarkers.get(markerCategory);
			//add marker
			existingMarkers.add(marker);
			individualMarkers.put(markerCategory, existingMarkers);
		}
		return this;
	}
	
	/**
	 * Adds one or more social markers specified as key-value pairs.
	 * @param individualMarkers Social markers to be added
	 * @return Attributes component for easy inline use
	 */
	public Attributes<T> addIndividualMarkers(final Object... individualMarkers) {
		// Length must be even
		if (individualMarkers.length % 2 != 0) {
			throw new IllegalArgumentException("Individual markers must be specified in key-value pairs.");
		}
		for (int i = 0; i < individualMarkers.length; i++) {
			addIndividualMarker(individualMarkers[i].toString(), individualMarkers[i+1].toString());
			// additional increment
			i++;
		}
		return this;
	}

	/**
	 * Replaces specific current individual marker(s) of a given category with new one. Leaves all other marker categories unchanged.
	 * @param markerCategory Category of marker to be replaced.
	 * @param marker Marker value to be set associated with #markerCategory.
	 * @return
	 */
	public Attributes<T> replaceIndividualMarker(final String markerCategory, final String marker) {
		// Not optimal (since it enforces string), but sufficient at this stage.
		T markers = (T) new LinkedHashSet<String>();
		markers.add(marker);
		individualMarkers.put(markerCategory, markers);
		return this;
	}
	
	/**
	 * Replaces current individual markers with new ones. All old ones are removed, before new ones are added.
	 * @param newIndividualMarkers Markers that replace old ones.
	 * @return
	 */
	public Attributes<T> replaceIndividualMarkers(final Map<String, T> newIndividualMarkers) {
		individualMarkers.clear();
		individualMarkers.putAll(newIndividualMarkers);
		return this;
	}
	
	/**
	 * Adds social marker and returns attributes 
	 * including the added marker.
	 * @param markerCategory Marker category (i.e., key of the category, e.g., "ROLE")
	 * @param marker Marker value (i.e., corresponding value for category, e.g., "Trader")
	 * @return Attributes component for easy inline use
	 */
	public Attributes<T> addSocialMarker(final String markerCategory, final String marker) {
		if (!socialMarkers.containsKey(markerCategory)) {
			// Not beautiful, but works for now
			T markers = (T) new LinkedHashSet<String>();
			markers.add(marker);
			socialMarkers.put(markerCategory, markers);
		} else {
			//contains other markers already
			T existingMarkers = socialMarkers.get(markerCategory);
			//add marker
			existingMarkers.add(marker);
			socialMarkers.put(markerCategory, existingMarkers);
		}
		return this;
	}
	
	/**
	 * Adds one or more social markers specified as key-value pairs.
	 * @param socialMarkers Social markers to be added
	 * @return Attributes component for easy inline use
	 */
	public Attributes<T> addSocialMarkers(final Object... socialMarkers) {
		// Length must be even
		if (socialMarkers.length % 2 != 0) {
			throw new IllegalArgumentException("Social markers must be specified in key-value pairs.");
		}
		for (int i = 0; i < socialMarkers.length; i++) {
			addSocialMarker(socialMarkers[i].toString(), socialMarkers[i+1].toString());
			// additional increment
			i++;
		}
		return this;
	}

	/**
	 * Replaces specific current social marker(s) of a given category with new one. Leaves all other marker categories unchanged.
	 * @param markerCategory Category of marker to be replaced.
	 * @param marker Marker value to be set associated with #markerCategory.
	 * @return
	 */
	public Attributes<T> replaceSocialMarker(final String markerCategory, final String marker) {
		// Not optimal (since it enforces string), but sufficient at this stage.
		T markers = (T) new LinkedHashSet<String>();
		markers.add(marker);
		socialMarkers.put(markerCategory, markers);
		return this;
	}
	
	/**
	 * Replaces current social markers with new ones. All old ones are removed, before new ones are added.
	 * @param newSocialMarkers Markers that replace old ones.
	 * @return
	 */
	public Attributes<T> replaceSocialMarkers(final Map<String, T> newSocialMarkers) {
		socialMarkers.clear();
		socialMarkers.putAll(newSocialMarkers);
		return this;
	}
	
	/**
	 * Clears attributes and returns reference to itself.
	 * @return empty attributes
	 */
	public Attributes<T> clear() {
		individualMarkers.clear();
		socialMarkers.clear();
		return this;
	}
	
	/**
	 * Copies all individual and social marker references from the input instance 
	 * and returns itself. Performs shallow copy.
	 * If the input is null, the returned instance is empty.
	 * @param instanceWhoseValuesToCopy
	 * @return Attributes instance with copied markers
	 */
	public Attributes<T> copyFrom(final Attributes<T> instanceWhoseValuesToCopy){
		if (instanceWhoseValuesToCopy != null) {
		    this.individualMarkers.putAll(instanceWhoseValuesToCopy.individualMarkers);
		    this.socialMarkers.putAll(instanceWhoseValuesToCopy.socialMarkers);
		}
		return this;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((individualMarkers == null) ? 0 : individualMarkers
						.hashCode());
		result = prime * result
				+ ((socialMarkers == null) ? 0 : socialMarkers.hashCode());
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
		Attributes other = (Attributes) obj;
		if (individualMarkers == null) {
			if (other.individualMarkers != null)
				return false;
		} else if (!individualMarkers.equals(other.individualMarkers))
			return false;
		if (socialMarkers == null) {
			if (other.socialMarkers != null)
				return false;
		} else if (!socialMarkers.equals(other.socialMarkers))
			return false;
		//System.out.println("Attributes match.");
		return true;
	}

	@Override
	public String toString() {
		if(individualMarkers.isEmpty() && socialMarkers.isEmpty()){
			return "A(*)";
		}
		return "A(" + (individualMarkers.isEmpty() ? "*" : individualMarkers)
				+ ", " + (socialMarkers.isEmpty() ? "*" : socialMarkers) + ")";
	}
	
}
