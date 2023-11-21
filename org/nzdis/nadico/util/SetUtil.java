package org.nzdis.nadico.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class SetUtil<K> {
	
	/**
	 * Calculate a normalised ordered intersection metric (between 0 and 1) of two sets based on 
	 * a) matching order of entries, and
	 * b) the intersection of the sets irrespective of order.
	 * Both components are equally weighted.
	 * 
	 * @param set1 Input set 1
	 * @param set2 Input set 2
	 * @return Returns -1 is any input set is is null; else a value of 0 to 1 is returned
	 */
	public float calculateIntersectionMetric(Set<K> set1, Set<K> set2) {
		
		// Check for null
		if (set1 == null || set2 == null) {
			return -1;
		}
		// Empty set implies no match
		if (set1.isEmpty()) {
			return 0;
		}
		float match;
		Iterator<K> it1 = set1.iterator();
		Iterator<K> it2 = set2.iterator();
		int matchCt = 0;
		// Check iteratively whether entries match
		while (it1.hasNext() && it2.hasNext()) {
			if (it1.next().equals(it2.next())) {
				matchCt++;
			}
		}
		// Complete match does not require further checks
		if (matchCt == set1.size()) {
			return 1;
		}
		
		// Partial match
		match = matchCt / (float)set1.size();
		
		// Check for ordering done - now check whether items intersect at all
		
		// Create copy
		HashSet<K> internalSet = new HashSet<>(set1);
		
		// Calculate the intersection
		boolean changed = internalSet.retainAll(set2);
		
		if (!changed) {
			// No items change, i.e., full intersection; weigh input from order check accordingly
			return 0.5f + 0.5f * match;
		} else {
			// Calculate fraction of changed items and add to match on order check
			return 0.5f * (internalSet.size()/(float)set1.size()) + 0.5f * match;
		}
	}

}
