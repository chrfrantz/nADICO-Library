package org.nzdis.nadico.deonticRange;

public abstract class NormativeValence {

	public static final Integer POSITIVE = 1; 
	public static final Integer NEUTRAL = 0;
	public static final Integer NEGATIVE = -1;
	
	/**
	 * Returns absolute valence for a given value.
	 * @param value Value to be mapped to valence
	 * @return Absolute valence (1 for positive, 0 for neutral, -1 for negative)
	 */
	public static Integer getAbsoluteValence(Float value){
		return (value > NEUTRAL ? POSITIVE : (value < NEUTRAL ? NEGATIVE : NEUTRAL));
	}

}
