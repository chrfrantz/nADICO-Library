package org.nzdis.nadico;

import org.nzdis.nadico.components.Aim;
import org.nzdis.nadico.components.IDAttributeResolver;
import org.nzdis.nadico.deonticRange.DeonticRangeConfiguration;
import org.nzdis.nadico.deonticRange.SymmetricDeonticValueMapper;
import org.nzdis.nadico.deonticRange.ZeroBasedEquiCompartmentDeonticValueMapper;

public class NAdicoConfiguration {

	/**
	 * Aim implementation for eventual instantiation.
	 */
	Class<? extends Aim> aimClass;
	/**
	 * Resolver from IDs to Attributes. Simulation-specific
	 */
	IDAttributeResolver attributesResolver = null;
	/**
	 * Rounds of values reaching into extreme of the deontic range 
	 * before becoming pre- or proscriptive norm.
	 */
	int roundsOfStabilityBeforeNormEstablishment = 100;
	/**
	 * Rounds of deviation before a pre- or proscriptive norms becomes 
	 * permissive. 
	 */
	int roundsOfDeviationBeforeSwitchingToPermissiveDeontic = 200;
	/**
	 * Deontic range configuration
	 */
	DeonticRangeConfiguration deonticRangeConfig = new DeonticRangeConfiguration(DeonticRangeConfiguration.DEONTIC_RANGE_TYPE_HISTORY_MIN_MAX, 100, 0f, 0f, 0.05f, 1f, ZeroBasedEquiCompartmentDeonticValueMapper.class);
	
	/**
	 * Default constructor
	 */
	public NAdicoConfiguration() {
	}
	
	/**
	 * Instantiates a NAdicoConfiguration instance with a given 
	 * DeonticRangeConfiguration.
	 * @param deonticRangeConfig Deontic range configuration to be used
	 */
	public NAdicoConfiguration(DeonticRangeConfiguration deonticRangeConfig) {
		this.deonticRangeConfig = deonticRangeConfig;
	}
}
