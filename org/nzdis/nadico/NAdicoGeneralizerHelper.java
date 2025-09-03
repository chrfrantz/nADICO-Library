package org.nzdis.nadico;

import java.util.*;
import java.util.Map.Entry;

import org.nzdis.nadico.components.Aim;
import org.nzdis.nadico.components.Attributes;
import org.nzdis.nadico.components.Conditions;

import org.sofosim.environment.memoryTypes.util.PairValueComparison;

public class NAdicoGeneralizerHelper {

	/**
	 * NAdico Helper-specific debug switch
	 */
	public static boolean debug = false;

	/**
	 * Prefix for debug output
	 */
	private static final String PREFIX = "NAdicoGeneralizerHelper: ";

	/**
	 * Returns a map containing social markers, an aim's activity (without other properties) as key, and the deontic value associated with it as value.
	 * Input for the method can be produced using the nAdicoGeneralizer (e.g. getNAdicoExpressions()).
	 * Output is useful for RadarChart filling or time series. Does not print full attribute key-value pairs; only values.
	 * @param cachedGeneralExpressions Set of general expressions
	 * @param includeDeonticAsPartOfKey Indicates whether deontics (e.g. SHOULD) are included as part of the generated keys (else simple AIC statements)
	 * @param useNormativeValenceInsteadOfValue Indicates whether normative valence (e.g. -1, 0, 1) is used instead of deontic value
	 * @return see above
	 */
	public static LinkedHashMap<String, Float> getAimAndDeonticValue(Collection<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<Float>, Conditions<NAdicoExpression>>> cachedGeneralExpressions, boolean includeDeonticAsPartOfKey, HashSet<String> ignoredDeontics, boolean useNormativeValenceInsteadOfValue){
		return generateNAdicoStmtAbbrAndDeontic(cachedGeneralExpressions, false, true, includeDeonticAsPartOfKey, null, ignoredDeontics, useNormativeValenceInsteadOfValue);
	}
	
	/**
	 * Returns a map containing all (generalised) markers, aim (including other properties), conditions,
	 *  and the deontic value associated with it.
	 * Input for the method can be produced using the nAdicoGeneralizer (e.g. getNAdicoExpressions()).
	 * Output is useful for RadarChart filling or time series.
	 * @param cachedGeneralExpressions Set of general expressions
	 * @param includeDeonticAsPartOfKey Indicates whether deontics (e.g. SHOULD) are included as part of the generated keys (else simple AIC statements)
	 * @param useNormativeValenceInsteadOfValue Indicates whether normative valence is used instead of deontic value
	 * @return see above
	 */
	public static LinkedHashMap<String, Float> getStringifiedNAdicoStmtAndDeonticValue(Collection<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<Float>, Conditions<NAdicoExpression>>> cachedGeneralExpressions, boolean includeDeonticAsPartOfKey, boolean useNormativeValenceInsteadOfValue){
		return generateNAdicoStmtAbbrAndDeontic(cachedGeneralExpressions, true, false, includeDeonticAsPartOfKey, null, null, useNormativeValenceInsteadOfValue);
	}
	
	/**
	 * Returns a map containing all (generalised) markers, aim (including other properties), conditions,
	 *  and the deontic value associated with it.
	 * Input for the method can be produced using the nAdicoGeneralizer (e.g. getNAdicoExpressions()).
	 * Output is useful for RadarChart filling or time series.
	 * @param cachedGeneralExpressions Set of general expressions
	 * @param printAttributeValuesOnly Indicates whether only attributes component values (not keys) should be included in output string
	 * @param includeDeonticAsPartOfKey Indicates whether deontics (e.g. SHOULD) are included as part of the generated keys (else simple AIC statements)
	 * @param ignoredKeys Aim properties to be ignored when building complex nAdicoStmt abbreviations (can be null)
	 * @param useNormativeValenceInsteadOfValue Indicates whether normative valence is used instead of deontic value
	 * @return see above
	 */
	public static LinkedHashMap<String, Float> getStringifiedNAdicoStmtAndDeonticValue(Collection<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<Float>, Conditions<NAdicoExpression>>> cachedGeneralExpressions, boolean printAttributeValuesOnly, boolean includeDeonticAsPartOfKey, HashSet<String> ignoredKeys, boolean useNormativeValenceInsteadOfValue){
		return generateNAdicoStmtAbbrAndDeontic(cachedGeneralExpressions, true, printAttributeValuesOnly, includeDeonticAsPartOfKey, ignoredKeys, null, useNormativeValenceInsteadOfValue);
	}
	
	/**
     * Returns a map containing all (generalised) markers, aim (including other properties), conditions,
     *  and the deontic value associated with it.
     * Input for the method can be produced using the nAdicoGeneralizer (e.g. getNAdicoExpressions()).
     * Output is useful for RadarChart filling or time series.
     * @param cachedGeneralExpressions Set of general expressions
     * @param printAttributeValuesOnly Indicates whether only attributes component values (not keys) should be included in output string
     * @param includeDeonticAsPartOfKey Indicates whether deontics (e.g. SHOULD) are included as part of the generated keys (else simple AIC statements)
     * @param ignoredKeys Aim properties to be ignored when building complex nAdicoStmt abbreviations (can be null)
     * @param ignoredDeontics Deontics for which nADICO statements are not generated (i.e. if deontic applies to statement, the entire statement is not included in output); can be null
     * @param useNormativeValenceInsteadOfValue Indicates whether normative valence (e.g. -1, 0, 1) is used instead of deontic (e.g. SHOULD)
     * @return see above
     */
    public static LinkedHashMap<String, Float> getStringifiedNAdicoStmtAndDeonticValue(Collection<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<Float>, Conditions<NAdicoExpression>>> cachedGeneralExpressions, boolean printAttributeValuesOnly, boolean includeDeonticAsPartOfKey, HashSet<String> ignoredKeys, HashSet<String> ignoredDeontics, boolean useNormativeValenceInsteadOfValue){
        return generateNAdicoStmtAbbrAndDeontic(cachedGeneralExpressions, true, printAttributeValuesOnly, includeDeonticAsPartOfKey, ignoredKeys, ignoredDeontics, useNormativeValenceInsteadOfValue);
    }
	
	/**
	 * Generates Map of abbreviated nADICO expression (i.e. including nested structure) and corresponding deontic according to desired complexity.
	 * Not complex: social markers, aim's activity and previous actions
	 * Complex: individual & social markers, full aim's properties, full condition properties
	 * @param cachedGeneralExpressions
	 * @param complex Indicates whether produces output should be complex (including individual markers, aim and conditions properties)
	 * @param printAttributeValuesOnly Indicates whether only attributes component values (not keys) should be included in output string
	 * @param includeDeonticAsPartOfKey Indicates whether deontics (e.g. SHOULD) are included as part of the generated keys (else simple AIC statements)
	 * @param ignoredKeys Aim properties to be ignored when building activity string (can be null)
	 * @param ignoredDeontics Deontics for which nADICO statements are not generated (i.e. if deontic applies to statement, the entire statement is not included in output); can be null
	 * @param useNormativeValenceInsteadOfDeonticString Indicates whether normative valence (e.g. -1, 0, 1) is used instead of deontic string based on deontic compartment (e.g. SHOULD)
	 * @return
	 */
	private static LinkedHashMap<String, Float> generateNAdicoStmtAbbrAndDeontic(Collection<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<Float>,
			Conditions<NAdicoExpression>>> cachedGeneralExpressions, boolean complex, boolean printAttributeValuesOnly, boolean includeDeonticAsPartOfKey, 
			HashSet<String> ignoredKeys, HashSet<String> ignoredDeontics, boolean useNormativeValenceInsteadOfDeonticString) {
		
		LinkedHashMap<String, Float> valencedActivities = new LinkedHashMap<>();
		
		for (NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<Float>, Conditions<NAdicoExpression>> entry: cachedGeneralExpressions) {
			
		    boolean includeStmtInOutput = true;
		    // Check whether statement should be ignored in output based on deontic
		    if (ignoredDeontics != null && !ignoredDeontics.isEmpty()) {
		        if (ignoredDeontics.contains(entry.getDeonticRange().getDeonticValueMapper().getDeonticForValue(entry.deontic))) {
		            includeStmtInOutput = false;
		        }
		    }
		    if (includeStmtInOutput) {
    			NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<Float>, Conditions<NAdicoExpression>> conds = entry;
    			StringBuilder builder = new StringBuilder();
    			
    			// Develop string with activities
    			builder = buildActivityString(entry, builder, complex, ignoredKeys, printAttributeValuesOnly, true, includeDeonticAsPartOfKey, useNormativeValenceInsteadOfDeonticString);
    			
    			final String key = builder.toString();
    			
    			if (valencedActivities.containsKey(key) ){
    				valencedActivities.put(key, valencedActivities.get(key) + entry.deontic);
    			} else {
    				valencedActivities.put(key, entry.deontic);
    			}
		    }
		}
		return valencedActivities;
	}
	
	/**
	 * Returns a LinkedHashMap of different nADICO statement's leading activities along with their aggregated deontic values.
	 * @param cachedGeneralExpressions Cached expressions generated using the generalizer.
	 * @return
	 */
	public static LinkedHashMap<String, Float> getLeadingActivityWithAggregatedDeonticValue(Collection<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>> cachedGeneralExpressions){
		LinkedHashMap<String, Float> valencedActivities = new LinkedHashMap<>();
		
		for (NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>> entry: cachedGeneralExpressions) {
			
			StringBuilder builder = new StringBuilder();
			builder.append(entry.aim.activity);
			
			final String key = builder.toString();
			
			if (valencedActivities.containsKey(key) ){
				valencedActivities.put(key, valencedActivities.get(key) + entry.deontic);
			} else {
				valencedActivities.put(key, entry.deontic);
			}
		}
		return valencedActivities;
	}
	
	/**
	 * Returns the aggregated deontic value for a given leading activity for a collection of nADICO expressions.
	 * @param activity
	 * @param source
	 * @return
	 */
	public static Float getDeonticValueForActivity(String activity, Collection<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>> source) {
		LinkedHashMap<String, Float> valencedActivities = getLeadingActivityWithAggregatedDeonticValue(source);
		return valencedActivities.get(activity);
	}
	
	/**
	 * Tests whether any NAdicoExpression in a given collection contains a given activity (on any nesting level).
	 * @param containedActivity
	 * @param expressions to test for existence of activity
	 * @return
	 */
	public static boolean containsActivity(String containedActivity, Collection<NAdicoExpression> expressions){
		if (containedActivity == null) {
			throw new RuntimeException(PREFIX + "Cannot test for null action.");
		}
		
		for (NAdicoExpression entry: expressions) {
			if (entry.isCombination()) {
				if (containsActivity(containedActivity, entry.nestedExpressions)) {
					return true;
				}
			} else if (entry.isStatement()) {
				if (entry.aim.activity.equals(containedActivity)) {
					return true;
				} else {
					if (entry.conditions.getPreviousAction() != null) {
						return containsActivity(containedActivity, (NAdicoExpression) entry.conditions.getPreviousAction());
					}
				}
			}
		}
		return false;
	}
	
	/**
	 * Tests whether an individual NAdicoExpression or its nested elements contains a given activity.
	 * @param containedActivity
	 * @param expression
	 * @return
	 */
	public static boolean containsActivity(String containedActivity, NAdicoExpression expression){
		ArrayList<NAdicoExpression> list = new ArrayList<NAdicoExpression>();
		list.add(expression);
		return containsActivity(containedActivity, list);
	}
	
	/**
	 * Returns aggregated deontic value for statements containing a given activity.
	 * @param cachedGeneralExpressions Cached expressions generated using the generalizer.
	 * @return
	 */
	public static Float getDeonticValueForStatementContainingActivity(String containedActivity, Collection<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>> cachedGeneralExpressions){
		if (containedActivity == null) {
			throw new RuntimeException(PREFIX + "Cannot determine deontic value for null action.");
		}
		
		Float deonticValue = 0f;
		
		for (NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>> entry: cachedGeneralExpressions) {
			
			if (entry.aim.activity.equals(containedActivity)) {
				deonticValue += entry.deontic;
			} else {
				// Check conditions in nested levels
				Conditions<NAdicoExpression> conditions = entry.conditions;
				if (conditions.getPreviousAction() != null) {
					
					if (conditions.getPreviousAction().isCombination()) {
						if (containsActivity(containedActivity, conditions.getPreviousAction().nestedExpressions)) {
							deonticValue += entry.deontic;
						}
					} else if (conditions.getPreviousAction().isStatement()) {
						if(containsActivity(containedActivity, conditions.getPreviousAction())) {
							deonticValue += entry.deontic;
						}
					}
				}
			}
		}
		return (deonticValue == null ? 0f : deonticValue);
	}
	
	/**
	 * Returns a stringified AIC statement including social attribute values, aim and the previous action (from conditions), but no further properties of each.
	 * @param expression
	 * @return
	 */
	public static String getStringifiedAICStatement(NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<Float>, Conditions<NAdicoExpression>> expression) {
		return buildActivityString(expression, null, false, null, true, true, false, false).toString();
	}

	/**
	 * Returns highest-ranking statement, with value being aggregated across all statements that contain this activity as initial expression.
	 * @param rankedExpressions Ranked expressions to be filtered and for which values are partially aggregated
	 * @param permissibleActions Permissible actions that are permissible
	 * @return
	 */
	public static PairValueComparison<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<Float>, Conditions<NAdicoExpression>>, Float>
		aggregateValueForMaxActivity(List<PairValueComparison<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<Float>, Conditions<NAdicoExpression>>, Float>> rankedExpressions, HashSet<String> permissibleActions) {

		// Check whether expressions are empty
		if (rankedExpressions.isEmpty()) {
			return null;
		}

		int rankedExpressionsIdx = 0;
		// Identify only statements that contain permissible actions
		while (!rankedExpressions.get(rankedExpressionsIdx).getKey().containsAnyActivityRecursively(permissibleActions)) {
			if (debug) {
				System.out.println(PREFIX + "Ranked expression '" + rankedExpressions.get(rankedExpressionsIdx) + "' does not contain any of the actions '" + permissibleActions + "'.");
			}
			rankedExpressionsIdx++;
		}
		// Return early if no statement contains it
		if (rankedExpressions.size() < rankedExpressionsIdx + 1) {
			if (debug) {
				System.out.println(PREFIX + "Falling back to exploration due to lack of memory entries.");
			}
			return null;
		}

		// Extract maximum-ranked initial action
		NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<Float>, Conditions<NAdicoExpression>> maxExpr = rankedExpressions.get(rankedExpressionsIdx).getKey().getInitialExpressions(1);

		float aggregateValue = 0f;
		// Go through the remaining ones and aggregate value
		for (int i = rankedExpressionsIdx; i < rankedExpressions.size(); i++) {
			if (rankedExpressions.get(i).getKey().containsActivityRecursively(maxExpr.aim.activity)) {
				if (debug) {
					System.out.println(PREFIX + "Adding up values for activity '" + maxExpr.aim.activity + "' from expression " + rankedExpressions.get(i));
				}
				// Add up
				aggregateValue += rankedExpressions.get(i).value;
			}
		}

		if (debug) {
			System.out.println(PREFIX + "Total value for action '" + maxExpr.aim.activity + "': " + aggregateValue);
		}

		PairValueComparison<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<Float>, Conditions<NAdicoExpression>>, Float> outputPair = new PairValueComparison<>(maxExpr, aggregateValue);

		return outputPair;
	}
	
	/**
	 * Builds String of activities of a given NAdicoExpression. Recursively iterates through nested statements.
	 * @param expression Expression to explore
	 * @param builder StringBuilder to append to (can be null)
	 * @param extended Indicates whether to construct a simple string
	 * only consisting of social markers, aim and conditions (Value: false), 
	 * or a complex one that considers individual markers, aim and conditions component properties.
	 * @param ignoredKeys Aim properties to be ignored when building activity string
	 * @param onlyPrintAttributeValues Indicates if only attributes values should be included in the string (i.e. not the complete key-value pair)
	 * @param includeConditionsAttributes Include attributes for previous actions in conditions
	 * @param includeDeonticInOutput Include deontic in the generated activity string
	 * @param useNormativeValenceInsteadOfValue Indicates if the deontic valence should be shown instead of the value
	 * @return StringBuilder instance including explored expression
	 */
	private static StringBuilder buildActivityString(NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<Float>, Conditions<NAdicoExpression>> expression, StringBuilder builder,
	        boolean extended, HashSet<String> ignoredKeys, boolean onlyPrintAttributeValues, boolean includeConditionsAttributes, boolean includeDeonticInOutput, boolean useNormativeValenceInsteadOfValue) {
		if (builder == null) {
			builder = new StringBuilder();
		}
		if (expression != null) {
			if (!expression.isCombination()) {
				if (expression.attributes != null) {
					// Append attributes
					if (extended) {
						// Advanced representations include individual markers
						builder.append(onlyPrintAttributeValues ? expression.attributes.individualMarkers.values() : expression.attributes.individualMarkers);
					}
					builder.append(onlyPrintAttributeValues ? expression.attributes.socialMarkers.values() : expression.attributes.socialMarkers).append(": ");
				}
				if (expression.aim != null) {
					// Append deontic
					if (includeDeonticInOutput && expression.deontic != null) {
						if (useNormativeValenceInsteadOfValue) {
							// deontic valence
							builder.append(expression.getDeonticRange().getDeonticValueMapper().getNormativeValence(expression));
							builder.append(" ");
						} else {
							// full mapped deontic
							builder.append(expression.getDeonticRange().getDeonticValueMapper().getDeonticForValue(expression.deontic));
							builder.append(" ");
						}
					}
					// Append activity
					builder.append(expression.aim.activity);
					// Append activity properties
					if (extended && expression.aim.properties != null && !expression.aim.properties.isEmpty()) {
						builder.append(" (");
						int max = expression.aim.properties.entrySet().size();
						int ct = 0;
						for (Entry props: expression.aim.properties.entrySet()) {
							if (ignoredKeys == null || !ignoredKeys.contains(props.getKey())) {
								builder.append(props.getKey()).append("-").append(props.getValue());
							}
							// Increase counter independent of actual printing
							ct++;
							if (ct < max) {
								builder.append(", ");
							}
						}
						builder.append(")");
					}
				}
				if (expression.conditions != null && !expression.conditions.properties.isEmpty()) {
					// Check for previous actions in conditions
					if (!extended && expression.conditions.getPreviousAction() != null) {
						builder.append("-");
						builder = buildActivityString(expression.conditions.getPreviousAction(), builder, extended, ignoredKeys, onlyPrintAttributeValues, includeConditionsAttributes, includeDeonticInOutput, useNormativeValenceInsteadOfValue);
					}
					// Include conditions properties
					if (extended) {
						builder.append(" (");
						int max = expression.conditions.properties.entrySet().size();
						int ct = 0;
						for (Entry props: expression.conditions.properties.entrySet()) {
							if (ignoredKeys == null || !ignoredKeys.contains(props.getKey())) {
								builder.append(props.getKey()).append("-");
								builder = buildActivityString((NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<Float>, Conditions<NAdicoExpression>>) props.getValue(), builder,
								        extended, ignoredKeys, onlyPrintAttributeValues, includeConditionsAttributes, includeDeonticInOutput, useNormativeValenceInsteadOfValue);
							}
							// Increase counter independent of actual printing
							ct++;
							if (ct < max) {
								builder.append(", ");
							}
						}
						builder.append(")");
					}
				}
			} else {
				// Check for nested expressions
				Iterator<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<Float>, Conditions<NAdicoExpression>>> it = expression.nestedExpressions.iterator();
				if (it.hasNext()) {
					builder.append("(");
				}
				while (it.hasNext()) {
					NAdicoExpression next = it.next();
					builder = buildActivityString(next, builder, extended, ignoredKeys, onlyPrintAttributeValues, includeConditionsAttributes, includeDeonticInOutput, useNormativeValenceInsteadOfValue);
					if (it.hasNext()) {
						builder.append(" ").append(expression.combinator).append(" ");
					} else {
						builder.append(")");
					}
				}
			}
		}
		return builder;
	}
	
	/**
	 * Creates deep copy of map containing valenced expressions.
	 * @param valencedExpressions
	 * @return
	 */
	public static LinkedHashMap<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<Float>, Conditions<NAdicoExpression>>, Float>
		makeCopyOfValencedExpressions(Map<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<Float>, Conditions<NAdicoExpression>>, Float> valencedExpressions) {
		
		LinkedHashMap<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<Float>, Conditions<NAdicoExpression>>, Float> copy = new LinkedHashMap<>();
		
		// Omitted null check for performance reasons
		for (Entry<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<Float>, Conditions<NAdicoExpression>>, Float> entry: valencedExpressions.entrySet()) {
			copy.put(new NAdicoExpression<>(entry.getKey()), entry.getValue().floatValue());
		}
		
		return copy;
	}
	
	/**
	 * Creates deep copy of map containing generalised NAdicoExpressions with corresponding underlying instances. 
	 * @param mappedGeneralisationAndInstances
	 * @return
	 */
	public static LinkedHashMap<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<Float>, Conditions<NAdicoExpression>>,ArrayList<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<Float>, Conditions<NAdicoExpression>>>>
		makeCopyOfExpressionMap(Map<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<Float>, Conditions<NAdicoExpression>>,ArrayList<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<Float>, Conditions<NAdicoExpression>>>> mappedGeneralisationAndInstances) {
			
			LinkedHashMap<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<Float>, Conditions<NAdicoExpression>>,
				ArrayList<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<Float>, Conditions<NAdicoExpression>>>> copy = new LinkedHashMap<>();
			
			for (Entry<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<Float>, Conditions<NAdicoExpression>>,
					ArrayList<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<Float>, Conditions<NAdicoExpression>>>> entry: mappedGeneralisationAndInstances.entrySet()) {
				
				ArrayList<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<Float>, Conditions<NAdicoExpression>>> copiedValues = new ArrayList<>();
				for (NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<Float>, Conditions<NAdicoExpression>> expr: entry.getValue()) {
					copiedValues.add(new NAdicoExpression<>(expr));
				}
				
				copy.put(new NAdicoExpression<>(entry.getKey()), copiedValues);
			}
			
			return copy;
	}

}
