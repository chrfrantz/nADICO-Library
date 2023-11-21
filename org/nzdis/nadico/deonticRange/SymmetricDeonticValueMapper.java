package org.nzdis.nadico.deonticRange;

import java.util.LinkedHashMap;

import org.nzdis.nadico.NAdicoExpression;
import org.sofosim.environment.memoryTypes.util.ScaleDifferenceCalculator;


/**
 * Non-zero-based deontic value range with equally sized deontic compartments.
 * 
 * @author cfrantz
 *
 */
public class SymmetricDeonticValueMapper extends DeonticValueMapper{


	public SymmetricDeonticValueMapper(DeonticRange range){
		super(range);
	}
	
	//Full deontic range
	private Float fullRange = 0f;
	//Width for individual deontic on scale
	private Float deonticWidth = null;
	//Center on deontics scale
	private Float center = 0f;
	//Tolerance for deontic extremes and center
	private Float tolerance = 0f;
	
	//show debug output
	private final boolean debug = false;
	
	//cache for old lower and upper boundary values (to check whether recalculation of center is required)
	private Float oldLower = 0f;
	private Float oldUpper = 0f;
	private Float oldCenter = 0f;
	private Float oldRange = 0f;
	
	/**
	 * Returns a deontic value for a given value 
	 * based on its allocation within the deontic
	 * range. 
	 * @param value Valence value to be mapped to deontic value
	 * @return Deontic associated with it based on deontic range
	 */
	@Override
	public String getDeonticForValue(Float value){
		calculateCenterAndDeonticWidth();
		//check for extreme values
		if(value >= (range.upperBoundary - tolerance)){
			return DeonticValues.MUST;
		} else if(value <= (range.lowerBoundary + tolerance)){
			return DeonticValues.MUST_NOT;
		} else //if(value == 0f){
				if(value >= (center - tolerance)
				&& value <= (center + tolerance)){
			//if around center value (with tolerance)
			return DeonticValues.INDIFFERENT;
		}
		//check how often width fits into value difference from lowest end
		return DeonticValues.rangeDeontics.get(new Float((value - range.lowerBoundary) / deonticWidth).intValue());
	}
	
	@Override
	public Float invertDeonticValue(final Float value) {
		if (value > center) {
			//calculate difference between upper boundary and center and determine fraction of value within that
			Float fraction = (Math.abs(value) / ScaleDifferenceCalculator.calculateDifferenceOnScale(center, range.upperBoundary));
			//apply fraction to opposite direction
			Float valueInOppositeDirection = fraction * ScaleDifferenceCalculator.calculateDifferenceOnScale(center, range.lowerBoundary);
			//return absolute value
			return center - valueInOppositeDirection;
		} else if (value < center) {
			//calculate difference between center and lower boundary and determine fraction of value within that
			Float fraction = (Math.abs(value) / ScaleDifferenceCalculator.calculateDifferenceOnScale(center, range.lowerBoundary));
			//apply fraction to opposite direction
			Float valueInOppositeDirection = fraction * ScaleDifferenceCalculator.calculateDifferenceOnScale(center, range.upperBoundary);
			//return absolute value
			return center + valueInOppositeDirection;
		} else if (value.equals(center)) {
			return value;
		}
		throw new RuntimeException("Inversion of value should always be possible. Check deontic range. Value: " + value + ", Deontic Range: " + range);
	}
	
	/**
	 * Returns the normative valence for a statement mapped against 
	 * current deontic range.
	 * @param statement NAdicoExpression whose deontic value is to be extracted
	 * @return
	 */
	@Override
	public Integer getNormativeValence(NAdicoExpression statement){
		calculateCenterAndDeonticWidth();
		return getNormativeValence(statement.deontic);
		//throw new RuntimeException("Statement should never have an undefined valence! - Statement: " + statement);
	}
	
	/**
	 * Returns the normative valence for a given value when mapped 
	 * against the current deontic range.
	 * @param value Value to be mapped
	 * @return
	 */
	@Override
	public Integer getNormativeValence(Float value){
		calculateCenterAndDeonticWidth();
		if(value >= (center - tolerance)
				&& value <= (center + tolerance)){
			//if around center value (with tolerance)
			return NormativeValence.NEUTRAL;
		} else if(value > (center + tolerance)) {
			return NormativeValence.POSITIVE;
		} else if(value < (center - tolerance)){
			return NormativeValence.NEGATIVE;
		}
		throw new RuntimeException("Value " + value + " does match deontic range category.");
	}
	
	/**
	 * Returns the normative valence for a given deontic String representation.
	 * @param deontic String representation of a deontic
	 * @return Normative valence ranging between -1 (negative) and 1 (positive)
	 */
	@Override
	public Integer getNormativeValence(String deontic){
		switch(deontic){
			case DeonticValues.MUST:
				return NormativeValence.POSITIVE;
			case DeonticValues.SHOULD:
				return NormativeValence.POSITIVE;
			case DeonticValues.MAY:
				return NormativeValence.POSITIVE;
			case DeonticValues.INDIFFERENT:
				return NormativeValence.NEUTRAL;
			case DeonticValues.MAY_NOT:
				return NormativeValence.NEGATIVE;
			case DeonticValues.SHOULD_NOT:
				return NormativeValence.NEGATIVE;
			case DeonticValues.MUST_NOT:
				return NormativeValence.NEGATIVE;
			default: throw new RuntimeException("Requested normative valence for unknown deontic " + deontic); 
		}
	}
	
	/**
	 * Returns the situational normative center of the deontic range.
	 * @return
	 */
	@Override
	public Float getNormativeCenter(){
		calculateCenterAndDeonticWidth();
		if(debug){
			System.out.println("Upper boundary: " + range.upperBoundary + ", Lower boundary: " + range.lowerBoundary + ", center: " + this.center);
		}
		return this.center;
	}
	
	@Override
	public Float getNormativeCenter(
			Float maxMovementInPercentOfPreviousDeonticRange) {
		calculateCenterAndDeonticWidth();
		//calculate max. move
		Float maxMove = maxMovementInPercentOfPreviousDeonticRange * oldRange;
		//move center by max. value
		return this.oldCenter.equals(this.center) ? this.oldCenter > this.center ? this.oldCenter - maxMove : this.oldCenter + maxMove : this.center;   
	}
	
	@Override
	public String toString(){
		calculateCenterAndDeonticWidth();
		StringBuilder builder = new StringBuilder();
		builder.append(System.getProperty("line.separator"));
		builder.append(DeonticValues.MUST_NOT).append(": ").append(range.lowerBoundary).append(System.getProperty("line.separator"));
		boolean centerPrinted = false;
		for(int i = 0; i < DeonticValues.rangeDeontics.size(); i++){
			if(!centerPrinted && i >= (DeonticValues.rangeDeontics.size() / 2) && this.center != null){
				//if halfway through range deontic list, print center values
				builder.append(DeonticValues.INDIFFERENT).append(": ").append(this.center).append(System.getProperty("line.separator"));
				centerPrinted = true;
			}
			if(deonticWidth != null){
				builder.append(DeonticValues.rangeDeontics.get(i)).append(": to ").append(range.lowerBoundary + deonticWidth + deonticWidth * i).append(System.getProperty("line.separator"));
			}
		}
		builder.append(DeonticValues.MUST).append(": ").append(range.upperBoundary);
		return builder.toString();
	}
	
	/**
	 * Calculates center, width of each deontic compartment, full deontic range and tolerance 
	 * based on parameterized percentage.
	 */
	private void calculateCenterAndDeonticWidth(){
		if(!oldLower.equals(range.lowerBoundary) || !oldUpper.equals(range.upperBoundary)){
			this.oldRange = this.fullRange;
			this.fullRange = ScaleDifferenceCalculator.calculateDifferenceOnScale(range.lowerBoundary, range.upperBoundary);
			this.oldLower = range.lowerBoundary;
			this.oldUpper = range.upperBoundary;
			if(debug){
				System.out.println("Changing full range from " + oldRange + " to " + this.fullRange);
			}
			this.deonticWidth = fullRange / (float)DeonticValues.rangeDeontics.size();
			this.oldCenter = this.center;
			this.center = range.upperBoundary - (fullRange/(float)2);
			if(debug){
				System.out.println("Deontic range center: Changed from " + oldCenter + " to " + this.center);
			}
			this.tolerance = range.toleranceAroundExtremeValuesInPercent * fullRange;
		}
	}

	@Override
	public LinkedHashMap<String, Float> getDeonticRangeInnerBoundaries() {
		return DeonticRangeCalculator.calculateSymmetricDeonticBoundaries(range.lowerBoundary, range.upperBoundary, this);
	}
	
}
