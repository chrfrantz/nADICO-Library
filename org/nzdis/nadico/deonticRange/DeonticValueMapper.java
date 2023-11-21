package org.nzdis.nadico.deonticRange;

import java.util.LinkedHashMap;

import org.nzdis.nadico.NAdicoExpression;
import org.nzdis.micro.inspector.annotations.Inspect;

public abstract class DeonticValueMapper {

	protected DeonticRange range = null;
	
	public DeonticValueMapper(DeonticRange range){
		if(range == null){
			throw new RuntimeException("Cannot instantiate DeonticValues calculation without passed a valid deontic range.");
		}
		this.range = range;
	}
	
	/**
	 * Returns the deontic (compartment) the given value resolves to along the 
	 * situational deontic range.
	 * @param value Value to be resolved to deontic compartment
	 * @return
	 */
	public abstract String getDeonticForValue(Float value);
	
	/**
	 * Inverts the deontic value and returns its equivalent in the 
	 * opposite direction of the situational deontic range.
	 * @param value
	 * @return
	 */
	public abstract Float invertDeonticValue(Float value);
	
	/**
	 * Returns the situational normative center (indifference zone) 
	 * of the deontic range.
	 * @return
	 */
	public abstract Float getNormativeCenter();
	
	/**
	 * Returns situational normative center within the boundaries of change in 
	 * comparison to previous normative center.
	 * @param maxMovementInPercentOfPreviousDeonticRange Max. difference to previous center information.
	 * @return
	 */
	public abstract Float getNormativeCenter(Float maxMovementInPercentOfPreviousDeonticRange);
	
	/**
	 * Returns the normative valence for a given value in the situational deontic range, 
	 * i.e. valence indicates whether value is below, equal or greater than center.
	 * @param valence
	 * @return Integer representation of normative valence (see NormativeValence).
	 */
	public abstract Integer getNormativeValence(Float valence);
	
	/**
	 * Returns the normative valence for the deontic of a given nADICO expression.
	 * @param statement nADICO expression/statement
	 * @return Integer representation of normative valence (see NormativeValence).
	 */
	public abstract Integer getNormativeValence(NAdicoExpression statement);
	
	/**
	 * Returns the normative valence for a given deontic term.
	 * @param deonticTerm
	 * @return Integer representation of normative valence (see NormativeValence).
	 */
	public abstract Integer getNormativeValence(String deonticTerm);
	
	/**
	 * Returns a normalized deontic value for a given deontic value. 
	 * It returns values between 0 and 1 (e.g. to represent probabilities).
	 * @param valueForAction Non-normalized deontic value
	 * @return
	 */
	public Float getNormalizedValueForAction(Float valueForAction) {
		//ordered compartment integer (corrected to centre of compartment, i.e. -0.5) * (1/compartment slices)
		return (DeonticValues.deonticOrder.get(getDeonticForValue(valueForAction)) - 0.5f) * 
				(1 / (float)DeonticValues.signedDeonticOrder.size());
	}
	
	/**
	 * Returns a normalized deontic value for a given deontic term. 
	 * Values returned can range from 0 to 1 and describe the compartment centre
	 * for a given term. Alternatively, it can return the upper normalized boundary 
	 * of the compartment.
	 * @param deontic Deontic term to be scaled
	 * @param upperBoundary Indicates whether to return upper boundary instead of centre
	 * @return
	 */
	public Float getNormalizedValueForDeontic(String deontic, boolean upperBoundary) {
		//ordered compartment integer (corrected to boundary, or centre of compartment, i.e. -0.5) * (1/compartment slices)
		return (upperBoundary ? DeonticValues.deonticOrder.get(deontic) : 
					(DeonticValues.deonticOrder.get(deontic) - 0.5f)) * 
				(1 / (float)DeonticValues.signedDeonticOrder.size());
	}
	
	/**
	 * Returns a linked hashmap of all deontics ordered from lowest to highest deontic. 
	 * The respective values represent the upper boundary value for the respective deontic. 
	 * The lower boundary of the lowest deontic is thus omitted.
	 * @return
	 */
	public abstract LinkedHashMap<String, Float> getDeonticRangeInnerBoundaries();
	
	/**
	 * String representation of DeonticValueMapper instance.
	 */
	@Inspect
	public abstract String toString();
	
}
