package org.nzdis.nadico.memory;

import java.util.*;
import java.util.Map.Entry;

import org.nzdis.nadico.NAdicoExpression;
import org.nzdis.nadico.NAdicoGeneralizer;
import org.nzdis.nadico.components.Aim;
import org.nzdis.nadico.components.Attributes;
import org.nzdis.nadico.components.Conditions;
import org.sofosim.environment.memoryTypes.DiscreteNonAggregatingMemory;

public class nAdicoActionMemory<A extends Attributes, I extends Aim, C extends Conditions> extends DiscreteNonAggregatingMemory<NAdicoExpression<A, I, C>, Float> {

	/**
	 * Debug switch
	 */
	public boolean debug = false;
	
	/**
	 * One-off debugging of query. Resets itself after query execution.
	 */
	public boolean oneOffDebug = false;
	
	/**
	 * Generaliser for NAdico statements
	 */
	private NAdicoGeneralizer generalizer = null;
	
	/**
	 * Instantiates action memory with given number of memory entries and owner reference.
	 * @param numberOfEntries
	 * @param owner
	 */
	public nAdicoActionMemory(Integer numberOfEntries, String owner) {
		super(numberOfEntries, owner);
	}
	
	/**
	 * Instantiates action memory with given number of entries, owner and generaliser instance.
	 * @param numberOfEntries
	 * @param owner
	 * @param generalizer Generaliser of owner, necessary for optional generalisations in 
	 *   {@link #getMaxNAdicoExpressionWithGivenExpressionAsPreviousExpression(NAdicoExpression, boolean, boolean, boolean, int)}.
	 */
	public nAdicoActionMemory(Integer numberOfEntries, String owner, NAdicoGeneralizer generalizer) {
		super(numberOfEntries, owner);
		this.generalizer = generalizer;
	}
	
	/**
	 * Returns the sum of values for memory entries that match the input action 
	 * statement. Requires an exact match of conditions (and does not generalise memory statements before comparison).
	 * @return Returns sum for matched input statement entry valences, or null if no statement match
	 */
	@Override
	public Float getValueForKey(NAdicoExpression<A, I, C> stmt) {
		return getValueForKey(stmt, AGGREGATION_SUM, false, true);
	}
	
	/**
	 * Returns the number of memory entries that match the input action statement. 
	 * Requires an exact match of conditions (and does not generalise memory statements before comparison).
	 * @param stmt action statement to match
	 * @return Returns count for matched input statement entries, or null if no statement match
	 */
	public Float getCountForKey(NAdicoExpression<A, I, C> stmt) {
		return getValueForKey(stmt, AGGREGATION_COUNT, false, true);
	}
	
	/**
	 * Returns all nADICO expressions contained in memory.
	 * @return
	 */
	public Collection<NAdicoExpression<A, I, C>> getAllKeys() {
		ArrayList<NAdicoExpression<A, I, C>> keys = new ArrayList<>();
		for (int i = 0; i < memoryArray.length; i++) {
			if (memoryArray[i] != null) {
				keys.add(memoryArray[i].key);
			}
		}
		return keys;
	}
	
	/**
	 * Returns the mean of matching memory entries. 
	 * Requires an exact match of conditions (and does not generalise memory statements before comparison).
	 * @param stmt
	 * @return Returns mean value for matched input statement entries, or null if no statement match
	 */
	public Float getMeanValueForKey(NAdicoExpression<A, I, C> stmt) {
		return getValueForKey(stmt, AGGREGATION_MEAN, false, true);
	}
	
	/**
	 * Returns *one* (the first) nADICO expression with highest value that has the given expression at the end of its action sequence (i.e., as last action).
	 * If the parameter operateOnGeneralisedStatements is set, input statements will be generalised before performing subsequence identification.
	 * @param stmt Subsequence to be identified in action sequence
	 * @param operateOnGeneralisedStatements Indicates whether the comparison should operate based on generalised statements
	 * @param strictMatchOnConditionsVsWildcardMatch Strict match on conditions (no wildcards) vs. match on wildcard conditions
	 * @return Returns null if no action found, else single expression with maximum reward
	 */
	public Entry<NAdicoExpression<A, I, C>, Float> getMaxNAdicoExpressionWithGivenExpressionAsLastExpression(NAdicoExpression<A, I, C> stmt, 
			final boolean operateOnGeneralisedStatements, final boolean strictMatchOnConditionsVsWildcardMatch, int valueAggregationStrategy) {
		Map<NAdicoExpression<A, I, C>, Float> map = getNAdicoExpressionsWithGivenExpression(stmt, false, true, 
				operateOnGeneralisedStatements, true, strictMatchOnConditionsVsWildcardMatch, valueAggregationStrategy);
		if (map == null || map.isEmpty()) {
			return null;
		}
		return map.entrySet().iterator().next();
	}
	
	/**
	 * Returns *all* nADICO expressions with highest value that have the given expression at the end of its action sequence (i.e., as last action).
	 * If the parameter operateOnGeneralisedStatements is set, input statements will be generalised before performing subsequence identification.
	 * @param stmt Subsequence to be identified in action sequence
	 * @param operateOnGeneralisedStatements Indicates whether the comparison should operate based on generalised statements
	 * @param strictMatchOnConditionsVsWildcardMatch Strict match on conditions (no wildcards) vs. match on wildcard conditions
	 * @param valueAggregationStrategy Aggregation strategy for returned values. Supports {@link #AGGREGATION_COUNT}, {@link #AGGREGATION_MEAN}, or {@link #AGGREGATION_SUM}.
	 * @return Returns null if no action found, else expression(s) with maximum reward (as per value aggregation strategy).
	 */
	public Map<NAdicoExpression<A, I, C>, Float> getMaxNAdicoExpressionsWithGivenExpressionAsLastExpression(NAdicoExpression<A, I, C> stmt, 
			final boolean operateOnGeneralisedStatements, final boolean strictMatchOnConditionsVsWildcardMatch, int valueAggregationStrategy) {
		Map<NAdicoExpression<A, I, C>, Float> map = getNAdicoExpressionsWithGivenExpression(stmt, false, true, 
				operateOnGeneralisedStatements, true, strictMatchOnConditionsVsWildcardMatch, valueAggregationStrategy);
		if (map == null || map.isEmpty()) {
			return null;
		}
		return map;
	}
	
	/**
	 * Returns *all* nADICO expressions that have the given expression at the end of its action sequence (i.e., as last action).
	 * If the parameter operateOnGeneralisedStatements is set, input statements will be generalised before performing subsequence identification.
	 * @param stmt Subsequence to be identified in action sequence
	 * @param operateOnGeneralisedStatements Indicates whether the comparison should operate based on generalised statements
	 * @param strictMatchOnConditionsVsWildcardMatch Strict match on conditions (no wildcards) vs. match on wildcard conditions
	 * @param valueAggregationStrategy Aggregation strategy for returned values. Supports {@link #AGGREGATION_COUNT}, {@link #AGGREGATION_MEAN}, or {@link #AGGREGATION_SUM}.
	 * @return Returns null if no action found, else expression(s) with maximum reward
	 */
	public Map<NAdicoExpression<A, I, C>, Float> getNAdicoExpressionsWithGivenExpressionAsLastExpression(NAdicoExpression<A, I, C> stmt, 
			final boolean operateOnGeneralisedStatements, final boolean strictMatchOnConditionsVsWildcardMatch, int valueAggregationStrategy) {
		Map<NAdicoExpression<A, I, C>, Float> map = getNAdicoExpressionsWithGivenExpression(stmt, false, false, 
				operateOnGeneralisedStatements, true, strictMatchOnConditionsVsWildcardMatch, valueAggregationStrategy);
		if (map == null || map.isEmpty()) {
			return null;
		}
		return map;
	}
	
	/**
	 * Returns *one* (the first) nADICO expression with highest value that has the given expression as a subsequence of their previous action sequence.
	 * If the parameter operateOnGeneralisedStatements is set, input statements will be generalised before performing subsequence identification.
	 * @param stmt Subsequence to be identified in action sequence
	 * @param operateOnGeneralisedStatements Indicates whether the comparison should operate based on generalised statements
	 * @param strictMatchOnConditionsVsWildcardMatch Strict match on conditions (no wildcards) vs. match on wildcard conditions
	 * @param valueAggregationStrategy Aggregation strategy for returned values. Supports {@link #AGGREGATION_COUNT}, {@link #AGGREGATION_MEAN}, or {@link #AGGREGATION_SUM}.
	 * @return Returns null if no action found, else single expression with maximum reward
	 */
	public Entry<NAdicoExpression<A, I, C>, Float> getMaxNAdicoExpressionWithGivenExpressionAsPreviousExpression(NAdicoExpression<A, I, C> stmt, 
			final boolean operateOnGeneralisedStatements, final boolean returnCompleteExpressionVsOnlySearchStmtAndNextExpressionInSequence, 
			final boolean strictMatchOnConditionsVsWildcardMatch, int valueAggregationStrategy) {
		Map<NAdicoExpression<A, I, C>, Float> map = getNAdicoExpressionsWithGivenExpression(stmt, true, true, operateOnGeneralisedStatements, 
				returnCompleteExpressionVsOnlySearchStmtAndNextExpressionInSequence, strictMatchOnConditionsVsWildcardMatch, valueAggregationStrategy);
		if (map == null || map.isEmpty()) {
			return null;
		}
		return map.entrySet().iterator().next();
	}
	
	/**
	 * Returns *all* nADICO expressions with highest value that have the given expression as a subsequence of their previous action sequence.
	 * If the parameter operateOnGeneralisedStatements is set, input statements will be generalised before performing subsequence identification.
	 * @param stmt Subsequence to be identified in action sequence
	 * @param operateOnGeneralisedStatements Indicates whether the comparison should operate based on generalised statements
	 * @param returnCompleteExpressionVsOnlySearchStmtAndNextExpressionInSequence Indicates whether to return full expression sequence (e.g., if looking for B, return C + B + A) vs. input sequence with next expression element only (e.g., if looking for B, return only C + B).
	 * @param strictMatchOnConditionsVsWildcardMatch Strict match on conditions (no wildcards) vs. match on wildcard conditions
	 * @param valueAggregationStrategy Aggregation strategy for returned values. Supports {@link #AGGREGATION_COUNT}, {@link #AGGREGATION_MEAN}, or {@link #AGGREGATION_SUM}.
	 * @return Returns null if no action found, else expression(s) with maximum reward
	 */
	public Map<NAdicoExpression<A, I, C>, Float> getMaxNAdicoExpressionsWithGivenExpressionAsPreviousExpression(NAdicoExpression<A, I, C> stmt, 
			boolean operateOnGeneralisedStatements, boolean returnCompleteExpressionVsOnlySearchStmtAndNextExpressionInSequence, 
			final boolean strictMatchOnConditionsVsWildcardMatch, int valueAggregationStrategy) {
		return getNAdicoExpressionsWithGivenExpression(stmt, true, true, 
				operateOnGeneralisedStatements, returnCompleteExpressionVsOnlySearchStmtAndNextExpressionInSequence, 
				strictMatchOnConditionsVsWildcardMatch, valueAggregationStrategy);
	}
	
	/**
	 * Returns *all* nADICO expressions that have the given expression as a subsequence of their previous action sequence.
	 * If the parameter operateOnGeneralisedStatements is set, input statements will be generalised before performing subsequence identification.
	 * @param stmt Subsequence to be identified in action sequence
	 * @param operateOnGeneralisedStatements Indicates whether the comparison should operate based on generalised statements
	 * @param returnCompleteExpressionVsOnlySearchStmtAndNextExpressionInSequence Indicates whether to return full expression sequence (e.g., if looking for B, return C + B + A) vs. input sequence with next expression element only (e.g., if looking for B, return only C + B).
	 * @param strictMatchOnConditionsVsWildcardMatch Strict match on conditions (no wildcards) vs. match on wildcard conditions
	 * @param valueAggregationStrategy Aggregation strategy for returned values. Supports {@link #AGGREGATION_COUNT}, {@link #AGGREGATION_MEAN}, or {@link #AGGREGATION_SUM}.
	 * @return Returns null if no action found, else expression(s) with maximum reward
	 */
	public Map<NAdicoExpression<A, I, C>, Float> getNAdicoExpressionsWithGivenExpressionAsPreviousExpression(NAdicoExpression<A, I, C> stmt, 
			boolean operateOnGeneralisedStatements, boolean returnCompleteExpressionVsOnlySearchStmtAndNextExpressionInSequence, 
			final boolean strictMatchOnConditionsVsWildcardMatch, int valueAggregationStrategy) {
		return getNAdicoExpressionsWithGivenExpression(stmt, true, false, 
				operateOnGeneralisedStatements, returnCompleteExpressionVsOnlySearchStmtAndNextExpressionInSequence, 
				strictMatchOnConditionsVsWildcardMatch, valueAggregationStrategy);
	}
	
	/**
	 * Returns *all* nADICO expressions that *either have the given expression as a subsequence of their previous action sequence 
	 * or on the same level*.
	 * If the parameter operateOnGeneralisedStatements is set, input statements will be generalised before performing subsequence identification.
	 * @param stmt Subsequence to be identified in action sequence
	 * @param maximumStatementsOnly Indicates whether only statements with maximum aggregated feedback should be returned
	 * @param operateOnGeneralisedStatements Indicates whether the comparison should operate based on generalised statements
	 * @param returnCompleteExpressionVsOnlySearchStmtAndNextExpressionInSequence Indicates whether to return full expression sequence (e.g., if looking for B, return C + B + A) vs. input sequence with next expression element only (e.g., if looking for B, return only C + B).
	 * @param strictMatchOnConditionsVsWildcardMatch Strict match on conditions (no wildcards) vs. match on wildcard conditions
	 * @param valueAggregationStrategy Aggregation strategy for returned values. Supports {@link #AGGREGATION_COUNT}, {@link #AGGREGATION_MEAN}, or {@link #AGGREGATION_SUM}.
	 * @return Returns null if no action found, else expression(s) with maximum reward
	 */
	private Map<NAdicoExpression<A, I, C>, Float> getNAdicoExpressionsWithGivenExpressionOnAnyLevel(NAdicoExpression<A, I, C> stmt, boolean maximumStatementsOnly, 
			boolean operateOnGeneralisedStatements, boolean returnCompleteExpressionVsOnlySearchStmtAndNextExpressionInSequence, 
			final boolean strictMatchOnConditionsVsWildcardMatch, int valueAggregationStrategy) {
		if (debug) {
			System.out.println("= Starting search for " + (maximumStatementsOnly ? "max" : "") + " " + stmt + " on any action level");
			System.out.println(" -> Starting search for " + (maximumStatementsOnly ? "max" : "") + " " + stmt + " on previous action level");
		}
		// Previous expression
		Map<NAdicoExpression<A, I, C>, Float> map = getNAdicoExpressionsWithGivenExpression(stmt, true, maximumStatementsOnly, 
				operateOnGeneralisedStatements, returnCompleteExpressionVsOnlySearchStmtAndNextExpressionInSequence, 
				strictMatchOnConditionsVsWildcardMatch, valueAggregationStrategy);
		if (debug) {
			System.out.println(" <- Finished search for " + (maximumStatementsOnly ? "max" : "") + " " + stmt + " on previous action level (" + 
					(map == null ? "0" : map.size()) + " entries)");
			System.out.println(" -> Starting search for " + (maximumStatementsOnly ? "max" : "") + " " + stmt + " on same action level");
		}
		
		// Expression on same level
		Map<NAdicoExpression<A, I, C>, Float> mapSameLevel = getNAdicoExpressionsWithGivenExpression(stmt, false, maximumStatementsOnly, 
				operateOnGeneralisedStatements, returnCompleteExpressionVsOnlySearchStmtAndNextExpressionInSequence, 
				strictMatchOnConditionsVsWildcardMatch, valueAggregationStrategy);
		
		if (mapSameLevel != null) {
			if (map == null) {
				map = new HashMap<>();
			}
			//TODO: Consider recurrent expression matching on multiple levels (mind performance)
			map.putAll(mapSameLevel);
		}
		
		if (debug) {
			System.out.println(" <- Finished search for " + (maximumStatementsOnly ? "max" : "") + " " + stmt + " on same action level (" + 
					(mapSameLevel == null ? 0 : mapSameLevel.size()) + " entries)");
			System.out.println("= Finished search for " + (maximumStatementsOnly ? "max" : "") + " " + stmt + " across all action levels (" + 
					(map == null ? "0" : map.size()) + " entries): " + 
					(map == null ? "" : map) + ")");
		}
		if (map == null || map.isEmpty()) {
			return null;
		}
		return map;
	}
	
	/**
	 * Returns *all* nADICO expressions with highest value that *either have the given expression as a subsequence of their previous action sequence 
	 * or on the same level*.
	 * If the parameter operateOnGeneralisedStatements is set, input statements will be generalised before performing subsequence identification.
	 * @param stmt Subsequence to be identified in action sequence
	 * @param operateOnGeneralisedStatements Indicates whether the comparison should operate based on generalised statements
	 * @param returnCompleteExpressionVsOnlySearchStmtAndNextExpressionInSequence Indicates whether to return full expression sequence (e.g., if looking for B, return C + B + A) vs. input sequence with next expression element only (e.g., if looking for B, return only C + B).
	 * @param strictMatchOnConditionsVsWildcardMatch Strict match on conditions (no wildcards) vs. match on wildcard conditions
	 * @param valueAggregationStrategy Aggregation strategy for returned values. Supports {@link #AGGREGATION_COUNT}, {@link #AGGREGATION_MEAN}, or {@link #AGGREGATION_SUM}.
	 * @return Returns null if no action found, else expression(s) with maximum reward
	 */
	public Map<NAdicoExpression<A, I, C>, Float> getMaxNAdicoExpressionsWithGivenExpressionOnAnyLevel(NAdicoExpression<A, I, C> stmt, 
			boolean operateOnGeneralisedStatements, boolean returnCompleteExpressionVsOnlySearchStmtAndNextExpressionInSequence, 
			final boolean strictMatchOnConditionsVsWildcardMatch, int valueAggregationStrategy) {
		return getNAdicoExpressionsWithGivenExpressionOnAnyLevel(stmt, true, operateOnGeneralisedStatements, returnCompleteExpressionVsOnlySearchStmtAndNextExpressionInSequence, strictMatchOnConditionsVsWildcardMatch, valueAggregationStrategy);
	}
	
	/**
	 * Returns *all* nADICO expressions that *either have the given expression as a subsequence of their previous action sequence 
	 * or on the same level*.
	 * If the parameter operateOnGeneralisedStatements is set, input statements will be generalised before performing subsequence identification.
	 * @param stmt Subsequence to be identified in action sequence
	 * @param operateOnGeneralisedStatements Indicates whether the comparison should operate based on generalised statements
	 * @param returnCompleteExpressionVsOnlySearchStmtAndNextExpressionInSequence Indicates whether to return full expression sequence (e.g., if looking for B, return C + B + A) vs. input sequence with next expression element only (e.g., if looking for B, return only C + B).
	 * @param strictMatchOnConditionsVsWildcardMatch Strict match on conditions (no wildcards) vs. match on wildcard conditions
	 * @param valueAggregationStrategy Aggregation strategy for returned values. Supports {@link #AGGREGATION_COUNT}, {@link #AGGREGATION_MEAN}, or {@link #AGGREGATION_SUM}.
	 * @return Returns null if no action found, else expression(s) with maximum reward
	 */
	public Map<NAdicoExpression<A, I, C>, Float> getNAdicoExpressionsWithGivenExpressionOnAnyLevel(NAdicoExpression<A, I, C> stmt, 
			boolean operateOnGeneralisedStatements, boolean returnCompleteExpressionVsOnlySearchStmtAndNextExpressionInSequence, 
			final boolean strictMatchOnConditionsVsWildcardMatch, int valueAggregationStrategy) {
		return getNAdicoExpressionsWithGivenExpressionOnAnyLevel(stmt, false, operateOnGeneralisedStatements, returnCompleteExpressionVsOnlySearchStmtAndNextExpressionInSequence, strictMatchOnConditionsVsWildcardMatch, valueAggregationStrategy);
	}
	
	/**
	 * Returns the keys of a map for a given value.
	 * Inspiration: http://thispointer.com/java-how-to-get-keys-by-a-value-in-hashmap-search-by-value-in-map/
	 * @param map Input map with all keys and values
	 * @param value Value for which map entries are sought
	 * @return
	 */
	static <K, V> Map<K,V> getAllMapEntriesForValue(Map<K, V> map, V value) 
	{
		Map<K, V> listOfMaps = null;
		 
		//Check if Map contains the given value
		if(map.containsValue(value))
		{
			// Create an Empty List
			listOfMaps = new LinkedHashMap<>();
					
			// Iterate over each entry of map using entrySet
			for (Map.Entry<K, V> entry : map.entrySet()) 
			{
				// Check if value matches with given value
				if (entry.getValue().equals(value))
				{
					// Store the key from entry to the list
					listOfMaps.put(entry.getKey(), value);
				}
			}
		}
		// Return the map entries for given value.
		return listOfMaps;	
	}
	
	/**
	 * Returns nADICO expressions along with values that have the given statement's action/expression either as any previous action/expression (backtracking through the statements), 
	 * or as expression on same level (i.e., as last action).
	 * If the parameter compareGeneralisedStatements is set, input statements will be generalised before performing subsequence identification.
	 * @param stmt Statement for which matching statements with subsequent actions should be found for
	 * @param givenExpressionAsPreviousVsExpressionOnSameLevel Indicates whether the given expression should be treated as previous expression only (not on same level)
	 * @param maxOnly Return only expressions with the maximum value (as opposed to all that match)
	 * @param compareGeneralisedStatements Indicates whether statements are only compared in the generalised form (i.e. both input statements will be generalised).
	 * @param returnCompleteExpressionVsOnlyNextExpressionInSequence Indicates whether to return full expression sequence vs. input sequence with next expression element only.
	 * @param strictMatchOnConditionsVsWildcardMatch Strict match on conditions (no wildcards) vs. match on wildcard conditions.
	 * @param valueAggregationStrategy Aggregation strategy for returned values. Supports {@link #AGGREGATION_COUNT}, {@link #AGGREGATION_MEAN}, or {@link #AGGREGATION_SUM}.
	 * @return Map including matching statements along with values. Returns null if no matching statements.
	 */
	private Map<NAdicoExpression<A, I, C>, Float> getNAdicoExpressionsWithGivenExpression(NAdicoExpression<A, I, C> stmt, 
			boolean givenExpressionAsPreviousVsExpressionOnSameLevel, boolean maxOnly, 
			boolean compareGeneralisedStatements, boolean returnCompleteExpressionVsOnlyNextExpressionInSequence, 
			boolean strictMatchOnConditionsVsWildcardMatch, int valueAggregationStrategy) {

		if (valueAggregationStrategy != 1 && valueAggregationStrategy != 2 && valueAggregationStrategy != 3) {
			throw new RuntimeException("Invalid value aggregation strategy. Provided value: " + valueAggregationStrategy);
		}

		// Create new statement, with old statement as previous action
		NAdicoExpression<A, I, C> newStmt = stmt.makeCopy();
		
		// Retrieve all statements for iteration
		Collection<NAdicoExpression<A, I, C>> keys = getAllKeys();
		
		// Map containing all statements that match input sequence
		Map<NAdicoExpression<A, I, C>, Float> matchingStatements = new HashMap<>();
		
		if (compareGeneralisedStatements) {
			if (generalizer == null) {
				throw new RuntimeException("NAdicoGeneralizer has not been specified during instantiation of NAdicoMemory.");
			}
			newStmt = (NAdicoExpression<A, I, C>) generalizer.generalizeExpression((NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>) newStmt);
		}

		for (NAdicoExpression<A, I, C> item : keys) {

			// Generalise input statements if required
			if (compareGeneralisedStatements) {
				item = (NAdicoExpression<A, I, C>) generalizer.generalizeExpression((NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>) item);
			}
			
			// Only add new items if they do not already exist (with generalisation). Values should have already been correctly aggregated.
			if (!matchingStatements.containsKey(item)) {
				// Perform actual matching (with matching on preceding statements or on same level)
				if (match(newStmt, item, givenExpressionAsPreviousVsExpressionOnSameLevel, strictMatchOnConditionsVsWildcardMatch)) {
					if (returnCompleteExpressionVsOnlyNextExpressionInSequence) {
						// COMPLETE STATEMENT: add complete original statement; override strict memory aggregation in getValueForKey() by bypassing it
						matchingStatements.put(item, getValueForKey(item, valueAggregationStrategy, compareGeneralisedStatements, strictMatchOnConditionsVsWildcardMatch));
					} else {
						// SHORT STATEMENT: add statement sequence including original input statements and one additional subsequent one (i.e. all further expressions are removed); override strict memory aggregation in getValueForKey() by bypassing it
						matchingStatements.put(item.getInitialExpressions(stmt.getTotalExpressionSequenceLength() + 1), 
								getValueForKey(item, valueAggregationStrategy, compareGeneralisedStatements, strictMatchOnConditionsVsWildcardMatch));
					}
				}
			}
		}
		
		// Test if any matching statement has been found - else return null
		if (matchingStatements.isEmpty()) {
			return null;
		}
		
		if (maxOnly && matchingStatements.size() > 1) {
			// Reduce to only max entry
			if (debug) {
				System.out.println("Matching statements before filtering by max value (Options: generalised statements: " +
						compareGeneralisedStatements + ", strict conditions comparison: " + strictMatchOnConditionsVsWildcardMatch +
						", return complete statements: " + returnCompleteExpressionVsOnlyNextExpressionInSequence + "): (" + 
						matchingStatements.size() + "):" + matchingStatements);
			}
			Entry<NAdicoExpression<A, I, C>, Float> maxEntry = Collections.max((Collection)matchingStatements.entrySet(), Map.Entry.comparingByValue());
			matchingStatements = getAllMapEntriesForValue(matchingStatements, maxEntry.getValue());
			if (debug) {
				System.out.println("Matching statements after filtering by max value: (" + matchingStatements.size() + "):" + matchingStatements);
			}
		}
		return matchingStatements;
	}


	/**
	 * Returns the memory entry (consisting of nADICO expression and associated value) with the highest value,
	 * irrespective of content.
	 * @return
	 */
	public PairValueComparison<NAdicoExpression<A, I, C>, Number> getMaxNAdicoExpression() {

		// Effectively act as helper function for API integrity
		PairValueComparison<NAdicoExpression<A, I, C>, Number> pair = getKeyValuePairForHighestValue();

		return pair;
	}

	/**
	 * Generalizes memorized statements and aggregates those based on given aggregation strategy #aggregationMode.
	 * @param aggregationMode Aggregation mode
	 * @return
	 */
	private HashMap<NAdicoExpression<A, I, C>, Float> generalizeAndAggregateGroupedNAdicoExpressions(int aggregationMode) {

		// Map holding final entries
		HashMap<NAdicoExpression<A, I, C>, DiscreteNonAggregatingMemory<NAdicoExpression<A, I, C>, Float>.CountSumEntry> intermediateMap = new HashMap<>();

		// Memory entries to be processed
		HashMap<NAdicoExpression<A, I, C>, DiscreteNonAggregatingMemory<NAdicoExpression<A, I, C>, Float>.CountSumEntry> entries = getCompleteEntries();

		// Iterate through memory entry
		for (Entry<NAdicoExpression<A, I, C>, CountSumEntry> entry : entries.entrySet()) {

			// Generalize each memory entry before aggregation
			NAdicoExpression<A, I, C> generalizedExpr = (NAdicoExpression<A, I, C>) generalizer.generalizeExpression((NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>) entry.getKey());

			// Manage generalized expressions
			if (!intermediateMap.containsKey(generalizedExpr)) {
				intermediateMap.put(generalizedExpr, entry.getValue());
			} else {
				DiscreteNonAggregatingMemory<NAdicoExpression<A, I, C>, Float>.CountSumEntry tempEntry = entry.getValue();
				intermediateMap.get(generalizedExpr).count += tempEntry.count;
				intermediateMap.get(generalizedExpr).sum += tempEntry.sum;
			}

		}

		// Perform final calculations

		// Map holding final entries
		HashMap<NAdicoExpression<A, I, C>, Float> outputMap = new HashMap<>();

		System.out.println("Intermediate map " + owner + ": " + intermediateMap);

		// Perform aggregation based on specified aggregation mode
		for (NAdicoExpression<A, I, C> key: intermediateMap.keySet()) {

			// Perform intended aggregation
			switch (aggregationMode) {
				case AGGREGATION_COUNT:
					// Count of statements for entry
					outputMap.put(key, (float)intermediateMap.get(key).count);
					break;
				case AGGREGATION_MEAN:
					System.out.println("Applied mean");
					// Mean value aggregation
					outputMap.put(key, intermediateMap.get(key).sum/(float)intermediateMap.get(key).count);
					break;
				case AGGREGATION_SUM:
					// Sum value
					outputMap.put(key, intermediateMap.get(key).sum);
					break;
				default:
					throw new RuntimeException("Unknown aggregation mode when returning entries: " + aggregationMode);
			}
		}
		return outputMap;
	}

	/**
	 * Returns list of memory entries ranked by values (highest to lowest) based on a given aggregation strategy.
	 * @return
	 */
	public List<PairValueComparison<NAdicoExpression<A, I, C>, Float>> getRankedNAdicoExpressions(int aggregationMode) {

		// Generalize and aggregate memory statements based on given strategy, before converting the structure
		HashMap<NAdicoExpression<A, I, C>, Float> map = generalizeAndAggregateGroupedNAdicoExpressions(aggregationMode);

		// List to be populated
		List<PairValueComparison<NAdicoExpression<A, I, C>, Float>> list = new ArrayList<>();

		// Iterate through generalized values
		for (Entry<NAdicoExpression<A, I, C>, Float> entry: map.entrySet()) {
			list.add(new PairValueComparison<NAdicoExpression<A, I, C>, Float>(entry.getKey(), entry.getValue()));
		}

		// Sort entries
		list.sort(Comparator.comparing(PairValueComparison::getValue));
		// Reverse order
		Collections.sort(list, Collections.reverseOrder());
		return list;
	}

	/**
	 * Returns the aggregated value for given nADICO action statements using a given aggregation method ({@link #AGGREGATION_COUNT}, {@link #AGGREGATION_MEAN}, {@link #AGGREGATION_SUM}).
	 * strictMatchOnConditionsVsWildcardMatch indicates whether conditions should be strictly matched
	 * @param actionStatement Action statement to be looked up
	 * @param aggregationMethod Aggregation method
	 * @param generaliseMemoryEntriesBeforeComparison Indicates whether memory entries are to be generalised before comparison (does *not* consider/generalise actionStatement)
	 * @param strictMatchOnConditionsVsWildcardMatch Indicates whether conditions have to be strictly matched (no wildcards).
	 * @return Returns null if no match for input statement
	 */
	private Float getValueForKey(final NAdicoExpression<A, I, C> actionStatement, final int aggregationMethod, 
			final boolean generaliseMemoryEntriesBeforeComparison, final boolean strictMatchOnConditionsVsWildcardMatch) {
		
		int numberOfPassedChecks = 0;
		// collected results
		Float result = null;
		
		if (debug || oneOffDebug) {
			System.out.println("==== Started value aggregation for particular query: " + actionStatement);
		}
		
		NAdicoExpression<A, I, C> memoryEntry;
		
		// do matching
		for (int i = 0; i < memoryArray.length; i++) {
			if (memoryArray[i] != null && memoryArray[i].key != null && memoryArray[i].value != null) { 
				
				// Temporarily assignment for matching (and potential generalisation)
				memoryEntry = memoryArray[i].key;
				
				// Generalise if specified
				if (generaliseMemoryEntriesBeforeComparison) {
					if (generalizer == null) {
						throw new RuntimeException("NAdicoGeneralizer has not been specified during instantiation of NAdicoMemory.");
					}
					memoryEntry = (NAdicoExpression<A, I, C>) 
							generalizer.generalizeExpression((NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>) memoryEntry);
				}
				// Perform match on instances (match is exact, so all input needs to be generalised if operating on generalised expressions)
				if (match(actionStatement, memoryEntry, false, strictMatchOnConditionsVsWildcardMatch)) {
					if (result == null) {
						result = 0f;
					}
					switch (aggregationMethod) {
						case AGGREGATION_MEAN:
							result += memoryArray[i].value;
							// Count iterations
							numberOfPassedChecks++;
							break;
						case AGGREGATION_SUM:
							result += memoryArray[i].value;
							break;
						case AGGREGATION_COUNT:
							// Count matches
							result++;
							break;
						default: throw new RuntimeException("Invalid aggregation method " + aggregationMethod + " in nAdicoActionMemory.");
					}
					if (debug || oneOffDebug) {
						System.out.println("Statement\n " + memoryEntry + " (generalised: " + generaliseMemoryEntriesBeforeComparison + 
								")\n  matches\n  Query " + actionStatement + "\nAdded value: " + memoryArray[i].value);
					}
				} else {
					if (debug || oneOffDebug) {
						System.out.println("Statement\n " + memoryEntry + " (generalised: " + generaliseMemoryEntriesBeforeComparison + 
								")\n  does NOT match\n  Query " + actionStatement);
					}
				}
			}
		}
		if (oneOffDebug) {
			System.out.println("Aggregation method: " + aggregationMethod);
			System.out.println("Aggregated value: " + result);
			oneOffDebug = false;
		}
		if (debug || oneOffDebug) {
			System.out.println("==== Finished value aggregation for particular query: " + actionStatement + ", Value: " + result);
		}
		if (result == null) {
			// can't do further operation on null
			return result;
		}
		if (aggregationMethod == AGGREGATION_MEAN) {
			return result/(float)numberOfPassedChecks;
		}
		return result;
	}
	
	/**
	 * Generic entry point for matching NAdicoExpressions.
	 * @param actionStatement New statement
	 * @param matchCandidate Existing statement to be tested against new statement
	 * @param matchPrecedingSubsequence Matches on preceding subsequence only (i.e. not on expressions of same length)
	 * @param strictMatchOnConditionsVsWildcardMatch Strict match on conditions (no wildcards) vs. match on wildcard conditions
	 * @return
	 */
	private boolean match(final NAdicoExpression<A, I, C> actionStatement, final NAdicoExpression<A, I, C> matchCandidate, 
			final boolean matchPrecedingSubsequence, final boolean strictMatchOnConditionsVsWildcardMatch) {
		if (debug) {
			System.out.println("Starting match of new statement");
			System.out.println("Candidate: " + actionStatement);
			System.out.println("Existing statement: " + matchCandidate);
		}
		if (actionStatement.isAction() && matchCandidate.isAction()) {
			return matchActions(actionStatement, matchCandidate, matchPrecedingSubsequence, strictMatchOnConditionsVsWildcardMatch);
		} else if (actionStatement.isCombination() && matchCandidate.isCombination()) {
			return matchCombinations(actionStatement, matchCandidate, matchPrecedingSubsequence, strictMatchOnConditionsVsWildcardMatch);
		} else if ((actionStatement.isAction() && !matchCandidate.isAction()) ||
				(!actionStatement.isAction() && matchCandidate.isAction())) {
			//Types don't match --> no match
			if(debug){
				System.out.println("FAIL: Existing statement is of different type than candidate statement.");
			}
			return false;
		} else {
			throw new RuntimeException("Unsupported nADICO type comparison: New statement: " + 
					actionStatement + ", Match candidate: " + matchCandidate);
		}
	}
	
	/**
	 * Compares two NAdicoCombinations. Checks the matching of their elements in orderly fashion.
	 * @param actionStatement
	 * @param matchCandidate
	 * @param matchPrecedingSubsequence
	 * @param strictMatchOnConditionsVsWildcardMatch
	 * @return
	 */
	private boolean matchCombinations(final NAdicoExpression<A, I, C> actionStatement, 
			final NAdicoExpression<A, I, C> matchCandidate, 
			final boolean matchPrecedingSubsequence, 
			final boolean strictMatchOnConditionsVsWildcardMatch) {
		if (!actionStatement.combinator.equals(matchCandidate.combinator)) {
			if(debug){
				System.out.println("FAIL: Original statement has different combinator than candidate statement.");
			}
			// combinator differs
			return false;
		}
		if (actionStatement.nestedExpressions.size() > matchCandidate.nestedExpressions.size()) {
			if(debug){
				System.out.println("FAIL: Original statement has more nested elements than candidate statement.");
			}
			// statement to match against cannot have more nested statements and still be match
			return false;
		}
		ArrayList<NAdicoExpression<A, I, C>> origNestedElements = new ArrayList<>(actionStatement.nestedExpressions);
		ArrayList<NAdicoExpression<A, I, C>> candidateNestedElements = new ArrayList<>(matchCandidate.nestedExpressions);
		// take according element from match candidate set
		for (int i = 0; i < origNestedElements.size(); i++) {
			NAdicoExpression<A, I, C> origElement = origNestedElements.get(i);
			NAdicoExpression<A, I, C> candElement = candidateNestedElements.get(i);
			if (!match(origElement, candElement, matchPrecedingSubsequence, strictMatchOnConditionsVsWildcardMatch)) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Compares a candidate nAdicoAction's AIC components against a reference action statement's AIC component. 
	 * Returns true if candidate statement contains all fields and properties specified in actionStatement.
	 * Caution: Only call this method from matchActions(), since it does not do null checks on passed statements (done before).
	 * @param newStatement New statement
	 * @param existingStatement Existing statement to be matched
	 * @param strictMatchOnConditionsVsWildcardMatch Strict match on conditions (no wildcards) vs. match on wildcard conditions
	 * @return true if match successful
	 */
	private boolean matchAIC(final NAdicoExpression<A, I, C> newStatement, final NAdicoExpression<A, I, C> existingStatement, final boolean strictMatchOnConditionsVsWildcardMatch) {

		//empty action statement is wildcard, defaults to true
		boolean pass = true;
		
		// Attributes
		pass = matchAttributes(newStatement.attributes, existingStatement.attributes);
		if (!pass) {
			if (debug) {
				System.out.println("Comparison of Attributes failed. (New statement: " + newStatement.attributes + 
						"; existing statement: " + existingStatement.attributes + ")");
			}
			return pass;
		}
		
		// Aim
		pass = matchAim(newStatement.aim, existingStatement.aim);
		if (!pass) {
			if (debug) {
				System.out.println("Comparison of Aim failed. (New statement: " + newStatement.aim + 
						"; existing statement: " + existingStatement.aim + ")");
			}
			return pass;
		}
		
		// Conditions
		pass = matchConditions(newStatement.conditions, existingStatement.conditions, strictMatchOnConditionsVsWildcardMatch);
		if (!pass) {
			if (debug) {
				System.out.println("Comparison of Conditions failed (strict vs. wildcards: " + strictMatchOnConditionsVsWildcardMatch + 
						"). (New statement: " + newStatement.conditions + 
						"; existing statement: " + existingStatement.conditions + ")");
			}
			return pass;
		}
		
		return pass;
	}
	
	/**
	 * Compares a candidate nAdicoAction against a reference action statement. 
	 * Returns true if candidate statement contains all fields and properties 
	 * specified in actionStatement.
	 * @param newStatement New statement
	 * @param existingStatement Existing statement to be matched
	 * @param matchPrecedingSubsequence Matches on preceding subsequence
	 * @param strictMatchOnConditionsVsWildcardMatch Strict match on conditions (no wildcards) vs. match on wildcard conditions
	 * @return true if match successful
	 */
	private boolean matchActions(final NAdicoExpression<A, I, C> newStatement, final NAdicoExpression<A, I, C> existingStatement, final boolean matchPrecedingSubsequence, final boolean strictMatchOnConditionsVsWildcardMatch) {
		
		NAdicoExpression<A, I, C> oldStmt = existingStatement;
		
		//empty action statement is wildcard, i.e. defaults to true
		boolean pass = true;
		
		if (newStatement == null && oldStmt == null) {
			if (debug) {
				System.out.println("Both statements to be compared were null - thus the same.");
			}
			return pass;
		}
		if (oldStmt == null) {
			throw new RuntimeException("Existing nAdicoAction statement (for comparison with new one) should not be null.");
		}
		
		if (newStatement != null) {
			
			if (matchPrecedingSubsequence) {
				int newStmtLength = newStatement.getTotalExpressionSequenceLength();
				//System.out.println("New stmt: " + newStatement);
				//System.out.println("New stmt length: " + newStmtLength);
				//System.out.println("Old stmt length: " + oldStmt.getTotalExpressionSequenceLength());
				if (oldStmt.getTotalExpressionSequenceLength() <= newStmtLength) {
					// If length of existing statement is smaller than new one, it can't hold a subsequence, so we can abort comparison here
					if (debug) {
						System.out.println("Matching preceding subsequence: Comparison of Statement failed - old statement too short (matching on subsequence: " + 
								matchPrecedingSubsequence +"). (New statement: " + newStatement.conditions + "; existing statement: " + 
								oldStmt.conditions + ")");
					}
					return false;
				}
				//System.out.println("Old before: " + oldStmt);
				if (strictMatchOnConditionsVsWildcardMatch) {
					oldStmt = oldStmt.getInitialExpressions(newStmtLength);
					if (debug) {
						System.out.println("Strict conditions comparison: Switched to base statement length for comparison. Refined: " + oldStmt);
					}
				} else {
					// wildcard-based conditions matching
					// In the wildcard case, backtrack through individual levels and attempt matching until levels are exhausted
					do {
						if (oldStmt.getTotalExpressionSequenceLength() > 1) {
							oldStmt = oldStmt.backtrackThroughPrecedingExpressionsForGivenLevels(1);
							if (debug) {
								System.out.println("Wildcard conditions comparison: Iterated one level down for statement comparison: " + oldStmt);
							}
						} else {
							if (debug) {
								System.out.println("Wildcard conditions comparison: Cannot traverse through previous action, since only one statement level.");
							}
							return false;
						}
					} while (!matchAIC(newStatement, oldStmt, strictMatchOnConditionsVsWildcardMatch));
					// if you end up here, it has passed
					if (debug) {
						System.out.println("Wildcard conditions comparison: Comparison with previous actions successful. (New statement: " + newStatement +
								"; existing statement: " + oldStmt + "; original existing statement: " + existingStatement + ")");
					}
					return true;
				}
				//System.out.println("Old after: " + oldStmt);
			}
			
			// check only on given level
			pass = matchAIC(newStatement, oldStmt, strictMatchOnConditionsVsWildcardMatch);
		}
		if (debug) {
			System.out.println("Comparison on given level successful. (New statement: " + newStatement +
					"; existing statement: " + oldStmt + ")");
		}
		return pass;
	}
	
	/**
	 * Matches new statement attributes (newAttributes) against existing attributes to be matched (attributesToBeCompared).
	 * @param newAttributes New attributes
	 * @param attributesToBeCompared Existing attributes
	 * @return
	 */
	public static boolean matchAttributes(final Attributes<LinkedHashSet<String>> newAttributes, final Attributes<LinkedHashSet<String>> attributesToBeCompared) {
		boolean pass = false;
		if (newAttributes == null || (newAttributes.individualMarkers.isEmpty() && newAttributes.socialMarkers.isEmpty())) {
			//anything matches against empty attributes
			return true;
		} else {
			// individual markers
			for (Entry<String, LinkedHashSet<String>> entry: newAttributes.individualMarkers.entrySet()) {
				if (attributesToBeCompared.individualMarkers.containsKey(entry.getKey()) &&
						attributesToBeCompared.individualMarkers.get(entry.getKey()).equals(entry.getValue())) {
					//element is contained
					pass = true;
				} else {
					return false;
				}
			}
			
			// social markers
			for (Entry<String, LinkedHashSet<String>> entry: newAttributes.socialMarkers.entrySet()) {
				if (attributesToBeCompared.socialMarkers.containsKey(entry.getKey()) &&
						attributesToBeCompared.socialMarkers.get(entry.getKey()).equals(entry.getValue())) {
					//element is contained
					pass = true;
				} else {
					return false;
				}
			}
		}
		return pass;
	}
	
	/**
	 * Matches new aim (newAim) against new aim to be matched (aimToBeCompared). 
	 * Returns true if new aim is null, or if its activity is null and properties empty.
	 * @param newAim
	 * @param aimToBeCompared
	 * @return
	 */
	public static boolean matchAim(final Aim<String> newAim, final Aim<String> aimToBeCompared) {
		boolean pass = false;
		if (newAim == null | (newAim.activity == null && newAim.properties.isEmpty())) {
			// pass if aim or activity is null and if properties are empty
			return true;
		} else {
			if (newAim.activity != null && !newAim.activity.isEmpty()) {
				if (aimToBeCompared.activity != null && !aimToBeCompared.activity.isEmpty()) {
					if (aimToBeCompared.activity.equals(newAim.activity)) {
						//same
						/*if(debug){
							System.out.println(aimToBeCompared.activity + " is the same as " + referenceAim.activity);
						}*/
						pass = true;
					} else {
						//different
						/*if(debug){
							System.out.println(aimToBeCompared.activity + " is different from " + referenceAim.activity);
						}*/
						return false;
					}
				} else {
					//no pass as no activity (despite query statement having one)
					return false;
				}
			}
			//check properties (independent from activity, e.g. if two activities share property)
			if (newAim.properties != null) {
				for (Entry<String, String> property: newAim.properties.entrySet()) {
					if (aimToBeCompared.properties.containsKey(property.getKey())) {
						if (aimToBeCompared.properties.get(property.getKey()).equals(property.getValue())) {
							pass = true;
						} else {
							//value is different
							return false;
						}
					} else {
						//key is not contained
						return false;
					}
				}
			}
		}
		return pass;
	}
	
	/**
	 * Matches existing conditions (referenceConditions) against new aim to be matched (conditionsToBeCompared).
	 * Returns true if strictComparisonVsWildcardComparison is false and referenceConditions are null or empty.
	 * If strictComparisonVsWildcardComparison is true it returns true if all condition properties are found 
	 * in referenceConditions.
	 * @param referenceConditions Existing conditions
	 * @param conditionsToBeCompared New conditions
	 * @param strictMatchOnConditionsVsWildcardMatch Strict comparison ensures complete match on conditions, else
	 * wildcard conditions on referenceConditions match everything
	 * @return
	 */
	public static boolean matchConditions(final Conditions<NAdicoExpression> referenceConditions, final Conditions<NAdicoExpression> conditionsToBeCompared, boolean strictMatchOnConditionsVsWildcardMatch) {
		boolean pass = false;
		if (!strictMatchOnConditionsVsWildcardMatch && 
				(referenceConditions == null || referenceConditions.properties == null || referenceConditions.properties.isEmpty())) {
			// Passes any comparison
			return true;
		}
		if (strictMatchOnConditionsVsWildcardMatch && 
				(referenceConditions == null || referenceConditions.properties == null || referenceConditions.properties.isEmpty())) {
				if (conditionsToBeCompared == null || conditionsToBeCompared.properties == null || conditionsToBeCompared.properties.isEmpty()) {
					// Passes if everything is null
					return true;
				}
				// Strict comparison fails if conditions to be compared are not null or empty
				return false;
		}
		// Else do complete check
		if (conditionsToBeCompared != null && conditionsToBeCompared.properties != null) {
			// match entire sequence
			for (Entry<String, NAdicoExpression> property: referenceConditions.properties.entrySet()) {
				if (conditionsToBeCompared.properties.containsKey(property.getKey())) {
					if (conditionsToBeCompared.properties.get(property.getKey()).equals(property.getValue())) {
						pass = true;
					} else {
						//value is different
						return false;
					}
				} else {
					//key is not contained
					return false;
				}
			}
		} else {
			//should have conditions but hasn't
			return false;
		}
		return pass;
	}
} 
