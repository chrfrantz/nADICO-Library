package org.nzdis.nadico;

import org.nzdis.nadico.components.Aim;
import org.nzdis.nadico.components.Attributes;
import org.nzdis.nadico.components.Conditions;

import java.util.Comparator;
import java.util.LinkedHashSet;

public class NAdicoExpressionCountComparator implements Comparator<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>>  {


	private boolean ascendingOrder = true;

	/**
	 * Instantiates comparator indicating sorting order. Variably sorts in ascending (true) and descending order (false).
	 * @param ascendingOrder
	 */
	public NAdicoExpressionCountComparator(boolean ascendingOrder) {
		this.ascendingOrder = ascendingOrder;
	}


	@Override
	public int compare(NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>> o1, NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>> o2) {
		// Check for equality first
		if (o1.count.equals(o2.count)) {
			return 0;
		}
		// Only reordering if more constituting statements (hence more generalization weight) - sorts in descending order
		if (this.ascendingOrder) {
			return o1.count > o2.count ? 1: -1;
		}
		// descending order
		return o1.count > o2.count ? -1: 1;
	}
}
