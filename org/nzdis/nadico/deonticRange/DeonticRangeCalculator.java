package org.nzdis.nadico.deonticRange;

import java.util.LinkedHashMap;
import org.sofosim.environment.memoryTypes.util.ScaleDifferenceCalculator;

public class DeonticRangeCalculator {

	/**
	 * Returns map with calculated deontic boundaries (from negative to positive) 
	 * for given deontic range boundaries. Applies symmetric boundaries.
	 * Doublechecks if a symmetric deontic value mapper is used. If not, the output 
	 * of this method will not match the boundary values agents operate on.
	 * @param lowerBoundary
	 * @param upperBoundary
	 * @param mapper
	 * @return
	 */
	public static LinkedHashMap<String, Float> calculateSymmetricDeonticBoundaries(Float lowerBoundary, Float upperBoundary, DeonticValueMapper mapper){
		if(mapper == null){
			System.err.println("Warning: DeonticValueMapper is null. Will calculate symmetric deontic ranges for given lower and upper boundary values.");
		} else {
			if(!mapper.getClass().equals(SymmetricDeonticValueMapper.class)){
				System.err.println("Warning: Requested deontic range mapping for non-symmetric ValueMapper instance. This will produce invalid range calculations!");
			}
		}
		try{
			Float fullRange = ScaleDifferenceCalculator.calculateDifferenceOnScale(lowerBoundary, upperBoundary);
			Float deonticWidth = fullRange / (float)DeonticValues.rangeDeontics.size();
			Float center = upperBoundary - (fullRange/(float)2);
			
			LinkedHashMap<String, Float> mappedDeontics = new LinkedHashMap<>();
			mappedDeontics.put(DeonticValues.MUST_NOT, lowerBoundary);
			boolean centerPrinted = false;
			for(int i = 0; i < DeonticValues.rangeDeontics.size(); i++){
				if(!centerPrinted && i >= (DeonticValues.rangeDeontics.size() / 2) && center != null){
					mappedDeontics.put(DeonticValues.INDIFFERENT, center);
					centerPrinted = true;
				}
				mappedDeontics.put(DeonticValues.rangeDeontics.get(i), lowerBoundary + (deonticWidth * (i + 1)));
			}
			mappedDeontics.put(DeonticValues.MUST, upperBoundary);
			return mappedDeontics;
		} catch (NullPointerException e){
			System.err.println("DeonticRangeCalculator: Not sufficient information for calculating range.");
		}
		return null;
	}

}
