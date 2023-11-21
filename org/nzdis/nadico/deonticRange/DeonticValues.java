package org.nzdis.nadico.deonticRange;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

public abstract class DeonticValues {

	//discrete deontic at lowest end
	public static final String MUST = "MUST";
	
	public static final String SHOULD = "SHOULD";
	public static final String MAY = "MAY";
	
	//discrete deontic at center
	public static final String INDIFFERENT = "INDIFFERENT";
	
	public static final String MAY_NOT = "MAY NOT";
	public static final String SHOULD_NOT = "SHOULD NOT";
	
	//discrete deontic at highest end
	public static final String MUST_NOT = "MUST NOT";

	/**
	 * Data structure holding deontic inversion mapping. 
	 */
	private static final HashMap<String, String> deonticInversion = new HashMap<String, String>();
	
	/**
	 * Data structure holding all non-discrete deontics ordered from negative to positive
	 */
	public static final ArrayList<String> rangeDeontics = new ArrayList<>();
	
	/**
	 * Data structure holding order of all deontics with map values indicating their position. 
	 * Optimized for fast lookup.
	 */
	public static final LinkedHashMap<String, Integer> deonticOrder = new LinkedHashMap<>();
	
	/**
	 * Data structure holding all deontics with symmetric mapping to signed integers centred on 0 for 
	 * {@link #INDIFFERENT}.
	 */
	public static final LinkedHashMap<String, Integer> signedDeonticOrder = new LinkedHashMap<>();
	
	/**
	 * Ordered zero-centred range deontics.
	 */
	public static final LinkedHashMap<String, Integer> rangeDeonticsSignedOrder = new LinkedHashMap<>();
	
	/**
	 * Data structure holding discrete deontics (MUST NOT, MAY, MUST), with MUST as infinite +, MAY as 0, MUST NOT as infinite -.
	 */
	public static final LinkedHashMap<String, Float> discreteDeontics = new LinkedHashMap<>();
	
	static {
		//add non-discrete deontics progressing from most proscriptive (prohibition) to most prescriptive (obligation) one
		rangeDeontics.add(DeonticValues.SHOULD_NOT);
		rangeDeontics.add(DeonticValues.MAY_NOT);
		rangeDeontics.add(DeonticValues.MAY);
		rangeDeontics.add(DeonticValues.SHOULD);
		
		//continuous deontics with signed order
		rangeDeonticsSignedOrder.put(DeonticValues.SHOULD_NOT, -2);
		rangeDeonticsSignedOrder.put(DeonticValues.MAY_NOT, -1);
		rangeDeonticsSignedOrder.put(DeonticValues.MAY, 1);
		rangeDeonticsSignedOrder.put(DeonticValues.SHOULD, 2);
		
		//fill inversion map
		deonticInversion.put(INDIFFERENT, INDIFFERENT);
		deonticInversion.put(MUST, MUST_NOT);
		deonticInversion.put(MAY, MAY_NOT);
		deonticInversion.put(SHOULD, SHOULD_NOT);
		deonticInversion.put(MUST_NOT, MUST);
		deonticInversion.put(MAY_NOT, MAY);
		deonticInversion.put(SHOULD_NOT, SHOULD);
		
		//linear increasing ordering based on map values
		deonticOrder.put(DeonticValues.MUST_NOT, 1);
		deonticOrder.put(DeonticValues.SHOULD_NOT, 2);
		deonticOrder.put(DeonticValues.MAY_NOT, 3);
		deonticOrder.put(DeonticValues.INDIFFERENT, 4);
		deonticOrder.put(DeonticValues.MAY, 5);
		deonticOrder.put(DeonticValues.SHOULD, 6);
		deonticOrder.put(DeonticValues.MUST, 7);
		
		//zero-centred signed ordering with relative valence
		signedDeonticOrder.put(DeonticValues.MUST_NOT, -3);
		signedDeonticOrder.put(DeonticValues.SHOULD_NOT, -2);
		signedDeonticOrder.put(DeonticValues.MAY_NOT, -1);
		signedDeonticOrder.put(DeonticValues.INDIFFERENT, 0);
		signedDeonticOrder.put(DeonticValues.MAY, 1);
		signedDeonticOrder.put(DeonticValues.SHOULD, 2);
		signedDeonticOrder.put(DeonticValues.MUST, 3);
		
		//discrete deontic representations
		discreteDeontics.put(DeonticValues.MUST, Float.MAX_VALUE);
		discreteDeontics.put(DeonticValues.MAY, new Float(0));
		discreteDeontics.put(DeonticValues.MUST_NOT, Float.MIN_VALUE);
	}
	
	/**
	 * Inverts the value for a given deontic into the opposite.
	 * @param deontic Deontic value
	 * @return Inverted deontic
	 */
	public static String invert(String deontic){
		return deonticInversion.get(deontic);
	}
}
