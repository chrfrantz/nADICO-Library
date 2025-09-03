package org.nzdis.nadico.deonticRange;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.nzdis.nadico.NAdicoExpression;
import org.sofosim.environment.memoryTypes.util.ScaleDifferenceCalculator;

/**
 * This DeonticValueMapper implementation generally assumes that the deontic 
 * range is zero-based, i.e. has 0 at its center. Deontic compartments are 
 * thus not generally symmetric (as with the SymmetricDeonticValueMapper), 
 * but only symmetric for the respective side of the deontic range (above 
 * center and below center). If upper and lower boundary are both below or 
 * above zero (i.e. do not cross zero with one being below, the other above), 
 * the range boundary closest to zero is set as center, leaving all its deontic 
 * compartments compressed to one value. 
 * 
 * @author Christopher Frantz
 *
 */
public class ZeroBasedEquiCompartmentDeonticValueMapper extends DeonticValueMapper{

	//show debug output
	private final boolean debug = false;
	
	public ZeroBasedEquiCompartmentDeonticValueMapper(DeonticRange range) {
		super(range);
	}

	/**
	 * Maps value to deontic term. Will break if deontic range is fully negative or positive!
	 */
	@Override
	public String getDeonticForValue(Float value) {
		Float center = center();
		if(value == null){
			throw new RuntimeException("Passed null value when retrieving deontic.");
		}
		//if not range yet developed, return INDIFFERENT
		if(value.equals(center) || (range.upperBoundary.equals(range.lowerBoundary))){
			return DeonticValues.INDIFFERENT;
		}
		//calculate tolerance for either positive or negative extreme and center
		Float tolerance = range.toleranceAroundExtremeValuesInPercent * (value > center ? (range.upperBoundary - center) : (center - range.lowerBoundary));
		//Float tolerance = range.toleranceAroundExtremeValuesInPercent * range.upperBoundary - range.lowerBoundary;
		if(value < center + tolerance && value > center - tolerance){
			return DeonticValues.INDIFFERENT;
		}
		if(value > center && value >= range.upperBoundary - tolerance){
			return DeonticValues.MUST;
		}
		if(value < center && value <= range.lowerBoundary + tolerance){
			return DeonticValues.MUST_NOT;
		}
		if(debug){
			System.out.println("Checking for value within lower and upper: " + range.lowerBoundary + ", " + range.upperBoundary);
		}
		//else it must be non-extreme deontics
		//Is value in upper or lower half of zero-based deontic range
		float diffBoundaryCenter = (value > center ? range.upperBoundary - center : center - range.lowerBoundary);
		//separate negative from positive deontics
		int halfOfDeontics = DeonticValues.rangeDeontics.size() / 2;
		//calculate compartment size for this side
		float compartmentSize = diffBoundaryCenter / (float)halfOfDeontics;
		if(debug){
			System.out.println("Range width in upper/lower half: " + diffBoundaryCenter);
			System.out.println("Compartment size: " + compartmentSize);
			System.out.println("Input Value: " + value);
			System.out.println("Center: " + center);
			//System.out.println("Half of deontics: " + halfOfDeontics);
		}
		//check how many compartments can be captured by value (counted from bottom) + check whether correction for positive values (upper half of array)
		if(value > center){
			//check from center upwards
			if(debug){
				System.out.println("Upper half");
			}
			//calculate difference between value and current range center
			float diffValueCenter = ScaleDifferenceCalculator.calculateDifferenceOnScale(value, center);
			
			Integer deonticCompartmentFromCenter = new Float(diffValueCenter / compartmentSize).intValue();
			if(debug){
				String resolvedDeontic = DeonticValues.rangeDeontics.get(halfOfDeontics + deonticCompartmentFromCenter);
				System.out.println("Deontic: " + resolvedDeontic);
				return resolvedDeontic;
			} else {
				return DeonticValues.rangeDeontics.get(halfOfDeontics + deonticCompartmentFromCenter);
			}
		} else {
			if(debug){
				System.out.println("Lower half");
			}
			//calculate difference between value and current range center
			float diffValueCenter = ScaleDifferenceCalculator.calculateDifferenceOnScale(value, center);
			
			Integer deonticCompartmentFromCenter = new Float(diffValueCenter / compartmentSize).intValue();
			//reducing by one because of inverted perspective (halfDeontics index = center)
			String resolvedDeontic = DeonticValues.rangeDeontics.get(halfOfDeontics - 1 - deonticCompartmentFromCenter);
			if(debug){
				System.out.println("Deontic: " + resolvedDeontic);
			}
			return resolvedDeontic;
			/*
			if(range.upperBoundary < 0f && range.lowerBoundary < 0f){
				if(value < 0f){
					//value is below center and entire range is below zero
					Integer deonticCompartmentFromCenter = new Float(diffValueCenter / compartmentSize).intValue();
					String resolvedDeontic = rangeDeontics.get(halfOfDeontics - deonticCompartmentFromCenter);
					System.out.println("Deontic: " + resolvedDeontic);
					return resolvedDeontic;
				} else {
					throw new RuntimeException("Value greater than zero should not be occurring here: " + value);
				}
				
			} else if(range.upperBoundary > 0f && range.lowerBoundary > 0f){
				if(value > 0f){
					//value is above zero and range is above zero
					Integer deonticCompartmentFromCenter = new Float(diffValueCenter / compartmentSize).intValue();
					String resolvedDeontic = rangeDeontics.get(halfOfDeontics - deonticCompartmentFromCenter);
					System.out.println("Deontic: " + resolvedDeontic);
					return resolvedDeontic;
				} else {
					throw new RuntimeException("Value below zero should not be occurring here: " + value);
				}
			}
			//otherwise scale crosses zero
			//check from bottom
			System.out.println("Lower half: " + (new Float(range.lowerBoundary + (diff - Math.abs(value)) / compartmentSize)));
			return rangeDeontics.get(new Float(range.lowerBoundary + (diff - Math.abs(value)) / compartmentSize).intValue());
			*/
		}
	}
	

	@Override
	public Float invertDeonticValue(final Float value) {
		if (value == null) {
			throw new RuntimeException("Deontic value for inversion is null. Deontic Range: " + range);
		}
		if (value > center()) {
			//System.out.println("Value " + value + " is greater than center " + center());
			//calculate difference between upper boundary and center and determine fraction of value within that
			Float fraction = (Math.abs(value) / ScaleDifferenceCalculator.calculateDifferenceOnScale(center(), range.upperBoundary));
			//System.out.println("Value " + value + " is fraction " + fraction + " of upper range");
			//apply fraction to opposite direction
			Float valueInOppositeDirection = fraction * ScaleDifferenceCalculator.calculateDifferenceOnScale(center(), range.lowerBoundary);
			//return absolute value
			return center() - valueInOppositeDirection;
		} else if (value < center()) {
			//calculate difference between center and lower boundary and determine fraction of value within that
			Float fraction = (Math.abs(value) / ScaleDifferenceCalculator.calculateDifferenceOnScale(center(), range.lowerBoundary));
			/*if(fraction > 1){
				System.err.println("Fraction " + fraction + " for value " + value + " is greater than 1.");
			}*/
			//System.out.println("Value " + value + " is fraction " + fraction + " of lower range");
			//apply fraction to opposite direction
			Float valueInOppositeDirection = fraction * ScaleDifferenceCalculator.calculateDifferenceOnScale(center(), range.upperBoundary);
			//return absolute value
			return center() + valueInOppositeDirection;
		} else if (value.floatValue() == center().floatValue()) {
			// explicitly uses equality comparator to capture equality of -0.0 and 0.0
			return value;
		}
		throw new RuntimeException("Inversion of value should always be possible. Check deontic range. Value: " + value + ", Deontic Range: " + range);
	}

	@Override
	public Float getNormativeCenter() {
		return center();
	}

	@Override
	public Integer getNormativeValence(Float valence) {
		Float center = center();
		return (valence > center ? NormativeValence.POSITIVE : (valence < center ? NormativeValence.NEGATIVE : NormativeValence.NEUTRAL));
	}

	@Override
	public Integer getNormativeValence(NAdicoExpression statement) {
		return getNormativeValence(statement.deontic);
	}

	@Override
	public Integer getNormativeValence(String deontic) {
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
		}
		throw new RuntimeException("Requested normative valence for unknown deontic " + deontic);
	}
	
	public LinkedHashMap<String, Float> getDeonticRangeInnerBoundaries(){
		LinkedHashMap<String, Float> deonticBoundaries = new LinkedHashMap<>();
		Float center = center();
		//running value for compartment borders
		float currentValue = range.lowerBoundary;
		int halfOfDeontics = DeonticValues.rangeDeontics.size() / 2;
		//calculate tolerance around center
		Float tolerance = range.toleranceAroundExtremeValuesInPercent * (center - range.lowerBoundary);
		//calculate compartment size for negative side
		float compartmentSize = (center - range.lowerBoundary) / (float)halfOfDeontics;
		boolean aboveCenter = false;
		for(int i = 0; i < DeonticValues.rangeDeontics.size(); i++){
			if(!aboveCenter && i >= halfOfDeontics && center != null){
				//revise last entry by reducing tolerance
				//System.out.println("Moved lower tolerance to " + (center - tolerance));
				deonticBoundaries.put(DeonticValues.rangeDeontics.get(i-1), center - tolerance);
				//calculate upper tolerance
				tolerance = range.toleranceAroundExtremeValuesInPercent * (range.upperBoundary - center);
				//if halfway through range deontic list, print center values
				deonticBoundaries.put(DeonticValues.INDIFFERENT, center + tolerance);
				aboveCenter = true;
				//recalculate compartment size for positive side
				compartmentSize = (range.upperBoundary - center) / (float)halfOfDeontics;
			}
			currentValue += compartmentSize;
			deonticBoundaries.put(DeonticValues.rangeDeontics.get(i), currentValue);
		}
		return deonticBoundaries;
	}
	
	@Override
	public String toString(){
		StringBuilder builder = new StringBuilder();
		//Float center = center();
		builder.append(System.getProperty("line.separator"));
		builder.append(DeonticValues.MUST_NOT).append(": ").append(range.lowerBoundary).append(System.getProperty("line.separator"));
		/*boolean centerPrinted = false;
		//split positive from negative deontics
		int halfOfDeontics = DeonticValues.rangeDeontics.size() / 2;
		//calculate compartment size for negative side
		float compartmentSize = (center - range.lowerBoundary) / (float)halfOfDeontics;
		//running value for compartment borders
		float currentValue = range.lowerBoundary;
		for(int i = 0; i < DeonticValues.rangeDeontics.size(); i++){
			if(!centerPrinted && i >= halfOfDeontics && center != null){
				//if halfway through range deontic list, print center values
				builder.append(DeonticValues.INDIFFERENT).append(": ").append(center).append(System.getProperty("line.separator"));
				centerPrinted = true;
				//recalculate compartment size for positive side
				compartmentSize = (range.upperBoundary - center) / (float)halfOfDeontics;
			}
			currentValue += compartmentSize;
			builder.append(DeonticValues.rangeDeontics.get(i)).append(": to ").append(currentValue).append(System.getProperty("line.separator"));
		}*/
		LinkedHashMap<String, Float> maps = getDeonticRangeInnerBoundaries();
		for(Entry<String, Float> entry: maps.entrySet()){
			builder.append(entry.getKey()).append(": to ").append(entry.getValue()).append(System.getProperty("line.separator"));
		}
		builder.append(DeonticValues.MUST).append(": ").append(range.upperBoundary);
		return builder.toString();
	}
	
	private Float center(){
		/*if(range.lowerBoundary > 0f && range.upperBoundary > 0f){
			return range.lowerBoundary;
		} else if (range.lowerBoundary < 0f && range.upperBoundary < 0f){
			return range.upperBoundary;
		}*/
		//should always be centred at zero as objective ground truth
		return 0f;
	}

	@Override
	public Float getNormativeCenter(
			Float maxMovementInPercentOfPreviousDeonticRange) {
		return center();
	}


}
