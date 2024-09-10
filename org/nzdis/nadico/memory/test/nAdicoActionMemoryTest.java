package org.nzdis.nadico.memory.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nzdis.nadico.NAdicoConfiguration;
import org.nzdis.nadico.NAdicoExpression;
import org.nzdis.nadico.NAdicoFactory;
import org.nzdis.nadico.NAdicoGeneralizer;
import org.nzdis.nadico.components.Aim;
import org.nzdis.nadico.components.Attributes;
import org.nzdis.nadico.components.Conditions;
import org.nzdis.nadico.deonticRange.DeonticRangeConfiguration;
import org.nzdis.nadico.deonticRange.ZeroBasedEquiCompartmentDeonticValueMapper;
import org.nzdis.nadico.memory.nAdicoActionMemory;

public class nAdicoActionMemoryTest {

	nAdicoActionMemory<Attributes<Set<String>>, Aim<String>, Conditions<NAdicoExpression>> memory = null;
	
	NAdicoFactory<Attributes<Set<String>>, Aim<String>, Conditions<NAdicoExpression>> factory = new NAdicoFactory<>();
	
	NAdicoGeneralizer generaliser = null;
	
	DeonticRangeConfiguration deonticRangeConfiguration = new DeonticRangeConfiguration(
			DeonticRangeConfiguration.DEONTIC_RANGE_TYPE_HISTORY_MIN_MAX,
            100, 
            0.05f, 
            0.05f, 
            ZeroBasedEquiCompartmentDeonticValueMapper.class,
			true);
	
	int numberOfMemoryEntries = 50;
	
	String AGENT_NAME = "NAME";
	String ownerOne = "Agent01";
	String ownerTwo = "Agent02";
	
	String ROLE_NAME = "ROLE";
	String roleOne = "Role01";
	String roleTwo = "Role02";
	
	String actionOne = "Action01";
	String actionTwo = "Action02";
	String reactionOne = "Reaction01";
	String reactionTwo = "Reaction02";
	
	Float FEEDBACK_POSITIVE = 1.0f;
	Float FEEDBACK_POSITIVE_HIGH = 2.0f;
	Float FEEDBACK_NEGATIVE = -1.0f;
	Float FEEDBACK_NEGATIVE_HIGH = -2.0f;
	Float FEEDBACK_NEUTRAL = 0f;
	
	NAdicoExpression<Attributes<Set<String>>, Aim<String>, Conditions<NAdicoExpression>> expression0 = 
			factory.createNAdicoAction(
					new Attributes<>().addIndividualMarker(AGENT_NAME, ownerOne).addSocialMarker(ROLE_NAME, roleOne), 
					new Aim<String>(actionOne), new Conditions<NAdicoExpression>());
	
	NAdicoExpression<Attributes<Set<String>>, Aim<String>, Conditions<NAdicoExpression>> expression1 = 
			factory.createNAdicoAction(
					new Attributes<>().addIndividualMarker(AGENT_NAME, ownerTwo).addSocialMarker(ROLE_NAME, roleTwo), 
					new Aim<String>(actionOne), new Conditions<NAdicoExpression>());
	
	NAdicoExpression<Attributes<Set<String>>, Aim<String>, Conditions<NAdicoExpression>> expression2 =
			factory.createNAdicoAction(
					new Attributes<>().addIndividualMarker(AGENT_NAME, ownerOne).addSocialMarker(ROLE_NAME, roleOne), 
					new Aim<String>(actionTwo), new Conditions<NAdicoExpression>());
	
	NAdicoExpression<Attributes<Set<String>>, Aim<String>, Conditions<NAdicoExpression>> expression3 =
			factory.createNAdicoAction(
					new Attributes<>().addIndividualMarker(AGENT_NAME, ownerTwo).addSocialMarker(ROLE_NAME, roleTwo), 
					new Aim<String>(actionTwo), new Conditions<NAdicoExpression>());
	
	@Before
	public void setup() {
		generaliser = new NAdicoGeneralizer(ownerOne, "", new NAdicoConfiguration(deonticRangeConfiguration, true));
		memory = new nAdicoActionMemory<>(numberOfMemoryEntries, ownerOne, generaliser);
		
		System.out.println("Test setup done.");
	}
	
	@After
	public void tearDown() {
		memory.setNumberOfMemoryEntries(0);
		memory = null;
		generaliser = null;
		
		System.out.println("Test tear down finished.");
	}
	
	/**
	 * Prefills memory.
	 */
	public void fillMemory() {
		memory.memorize(expression0.makeCopy(), FEEDBACK_POSITIVE);
		memory.memorize(expression1.makeCopy(), FEEDBACK_POSITIVE_HIGH);
		memory.memorize(expression2.makeCopy(), FEEDBACK_NEGATIVE);
		memory.memorize(expression3.makeCopy(), FEEDBACK_NEGATIVE_HIGH);
	}
	
	@Test
	public void findMaxAndMinMemoryEntry() {
		
		fillMemory();
		assertEquals("Highest-valued expression: " + expression1, expression1, memory.getKeyForHighestValue());
		
		assertEquals("Lowest-valued expression: " + expression3, expression3, memory.getKeyForLowestValue());
	}
	
	@Test
	public void searchStrictAndWildcardExpressionsOnAnyLevelAlongWithValueAggregationForDuplicateResults() {

		fillMemory();
		
		// Initial statement
		NAdicoExpression<Attributes<Set<String>>, Aim<String>, Conditions<NAdicoExpression>> expression = 
			factory.createNAdicoAction(
				new Attributes<>().addIndividualMarker(AGENT_NAME, ownerTwo).addSocialMarker(ROLE_NAME, roleOne), 
				new Aim<String>(actionTwo), new Conditions<NAdicoExpression>());
		
		// Create nested statement with expression on top level with one previous action
		NAdicoExpression<Attributes<Set<String>>, Aim<String>, Conditions<NAdicoExpression>> entryWithSearchExpressionInMiddle = expression1.makeCopy();
		NAdicoExpression<Attributes<Set<String>>, Aim<String>, Conditions<NAdicoExpression>> expressionBottom = expression2.makeCopy();
		NAdicoExpression<Attributes<Set<String>>, Aim<String>, Conditions<NAdicoExpression>> expressionMiddle = expression.makeCopy();
		
		expressionMiddle.conditions.setPreviousAction(expressionBottom);
		
		// Expression with search expression on middle level
		entryWithSearchExpressionInMiddle.conditions.setPreviousAction(expressionMiddle);
		
		memory.memorize(entryWithSearchExpressionInMiddle, FEEDBACK_POSITIVE_HIGH);
		
		// Expression on middle level
		
		// Strict condition
		Map<NAdicoExpression<Attributes<Set<String>>, Aim<String>, Conditions<NAdicoExpression>>, Float> resultsMiddleStrict = 
				memory.getMaxNAdicoExpressionsWithGivenExpressionOnAnyLevel(expression, false, true, true, nAdicoActionMemory.AGGREGATION_SUM);
		
		assertNull("Strict execution on any level (with search statement on middle level) should result in null (since statement does not have preceding action itself)", resultsMiddleStrict);
		
		// Wildcard condition
		Map<NAdicoExpression<Attributes<Set<String>>, Aim<String>, Conditions<NAdicoExpression>>, Float> resultsMiddleWildcard = 
				memory.getMaxNAdicoExpressionsWithGivenExpressionOnAnyLevel(expression, false, true, false, nAdicoActionMemory.AGGREGATION_SUM);
		
		assertTrue("Wildcard conditions execution on any level (with search statement on middle level) should produce one result", resultsMiddleWildcard.containsKey(entryWithSearchExpressionInMiddle));
		assertEquals("Wildcard conditions execution on any level (with search statement on middle level) should produce one result", 1, resultsMiddleWildcard.size());
		assertEquals("Wildcard result should contain highest feedback", (Float)2.0f, (Float)resultsMiddleWildcard.get(entryWithSearchExpressionInMiddle));
		
		// Search expression in middle (from previous test) and bottom level/previous action
		
		// Create expression with search expression at bottom
		NAdicoExpression<Attributes<Set<String>>, Aim<String>, Conditions<NAdicoExpression>> entryWithSearchExpressionOnBottom = expression1.makeCopy();
		entryWithSearchExpressionOnBottom.conditions.setPreviousAction(expression.makeCopy());
	
		memory.memorize(entryWithSearchExpressionOnBottom, FEEDBACK_POSITIVE_HIGH);
	
		// Strict condition
		Map<NAdicoExpression<Attributes<Set<String>>, Aim<String>, Conditions<NAdicoExpression>>, Float> resultsBottomStrict = 
				memory.getMaxNAdicoExpressionsWithGivenExpressionOnAnyLevel(expression, false, true, true, nAdicoActionMemory.AGGREGATION_SUM);
		
		assertEquals("Strict execution on any level (with search statement on bottom level) should result in 1 result", 1, resultsBottomStrict.size());
		assertEquals("Strict execution on any level (with search statement on bottom level) should result in expression", entryWithSearchExpressionOnBottom, resultsBottomStrict.keySet().iterator().next());
		
		// Wildcard condition
		Map<NAdicoExpression<Attributes<Set<String>>, Aim<String>, Conditions<NAdicoExpression>>, Float> resultsBottomWildcard = 
				memory.getMaxNAdicoExpressionsWithGivenExpressionOnAnyLevel(expression, false, true, false, nAdicoActionMemory.AGGREGATION_SUM);
		
		assertEquals("Wildcard execution on any level (with search statement on middle and bottom level) should result in 2 results", 2, resultsBottomWildcard.size());
		assertTrue("Wildcard execution on any level (with search statement on middle and bottom level) should result in expression", resultsBottomWildcard.containsKey(entryWithSearchExpressionInMiddle));
		assertTrue("Wildcard execution on any level (with search statement on middle and bottom level) should result in expression", resultsBottomWildcard.containsKey(entryWithSearchExpressionOnBottom));
		
		// Three valid expressions (single, middle level and bottom-level statement)
		
		memory.memorize(expression.makeCopy(), FEEDBACK_POSITIVE_HIGH);
		
		// Strict search (2 results)
		Map<NAdicoExpression<Attributes<Set<String>>, Aim<String>, Conditions<NAdicoExpression>>, Float> resultsSingleStrict = 
				memory.getMaxNAdicoExpressionsWithGivenExpressionOnAnyLevel(expression, false, true, true, nAdicoActionMemory.AGGREGATION_SUM);
		
		assertEquals("Strict execution on any level should result in two responses (single and bottom level)", 2, resultsSingleStrict.size());
		assertTrue("Strict execution on any level contains single expression", resultsSingleStrict.containsKey(expression));
		assertTrue("Strict execution on any level contains nested expression on previous level", resultsSingleStrict.containsKey(entryWithSearchExpressionOnBottom));
		
		// Wildcard search (3 results)
		Map<NAdicoExpression<Attributes<Set<String>>, Aim<String>, Conditions<NAdicoExpression>>, Float> resultsSingleWildcard = 
				memory.getMaxNAdicoExpressionsWithGivenExpressionOnAnyLevel(expression, false, true, false, nAdicoActionMemory.AGGREGATION_SUM);
		
		assertEquals("Wildcard execution on any level should result in two responses (single, middle and bottom level)", 3, resultsSingleWildcard.size());
		assertTrue("Wildcard execution on any level contains single expression", resultsSingleWildcard.containsKey(expression));
		assertTrue("Wildcard execution on any level contains nested expression on middle level", resultsSingleWildcard.containsKey(entryWithSearchExpressionInMiddle));
		assertTrue("Wildcard execution on any level contains nested expression on bottom level", resultsSingleWildcard.containsKey(entryWithSearchExpressionOnBottom));
		
		// Add second-last level in 4-level statement (requires two backtracking iterations)
		
		NAdicoExpression<Attributes<Set<String>>, Aim<String>, Conditions<NAdicoExpression>> tmpIntermediate = entryWithSearchExpressionOnBottom.makeCopy();
		// Append lower-level expression, so search expression moves one level up
		tmpIntermediate.conditions.getPreviousAction().conditions.setPreviousAction(expression3.makeCopy());
		NAdicoExpression<Attributes<Set<String>>, Aim<String>, Conditions<NAdicoExpression>> entryWithSearchExpressionOnSecondLastPosition = expression2.makeCopy();
		entryWithSearchExpressionOnSecondLastPosition.conditions.setPreviousAction(tmpIntermediate);
		
		memory.memorize(entryWithSearchExpressionOnSecondLastPosition, FEEDBACK_POSITIVE_HIGH);
		
		// Strict (2 results)
		Map<NAdicoExpression<Attributes<Set<String>>, Aim<String>, Conditions<NAdicoExpression>>, Float> resultsSecondLastStrict = 
				memory.getMaxNAdicoExpressionsWithGivenExpressionOnAnyLevel(expression, false, true, true, nAdicoActionMemory.AGGREGATION_SUM);
		
		assertEquals("Strict execution on any level should result in two responses (single and bottom level)", 2, resultsSecondLastStrict.size());
		assertTrue("Strict execution on any level contains single expression", resultsSecondLastStrict.containsKey(expression));
		assertTrue("Strict execution on any level contains nested expression on bottom level", resultsSecondLastStrict.containsKey(entryWithSearchExpressionOnBottom));
		
		// Wildcard (4 results)
		Map<NAdicoExpression<Attributes<Set<String>>, Aim<String>, Conditions<NAdicoExpression>>, Float> resultsSecondLastWildcard = 
				memory.getMaxNAdicoExpressionsWithGivenExpressionOnAnyLevel(expression, false, true, false, nAdicoActionMemory.AGGREGATION_SUM);
		
		assertEquals("Strict execution on any level should result in two responses (single and bottom level)", 4, resultsSecondLastWildcard.size());
		assertTrue("Wildcard execution on any level contains single expression", resultsSecondLastWildcard.containsKey(expression));
		assertEquals("Wildcard execution on any level contains single expression with value 2", (Float)2.0f, resultsSecondLastWildcard.get(expression));
		assertTrue("Wildcard execution on any level contains nested expression on middle level", resultsSecondLastWildcard.containsKey(entryWithSearchExpressionInMiddle));
		assertEquals("Wildcard execution on any level contains nested expression on middle level with value 2", (Float)2.0f, resultsSecondLastWildcard.get(entryWithSearchExpressionInMiddle));
		assertTrue("Wildcard execution on any level contains nested expression on second-last level", resultsSecondLastWildcard.containsKey(entryWithSearchExpressionOnSecondLastPosition));
		assertEquals("Wildcard execution on any level contains nested expression on second-last level with value 2", (Float)2.0f, resultsSecondLastWildcard.get(entryWithSearchExpressionOnSecondLastPosition));
		assertTrue("Wildcard execution on any level contains nested expression on bottom level", resultsSecondLastWildcard.containsKey(entryWithSearchExpressionOnBottom));
		assertEquals("Wildcard execution on any level contains nested expression on bottom level with value 2", (Float)2.0f, resultsSecondLastWildcard.get(entryWithSearchExpressionOnBottom));
		
		// Test value aggregation using duplicate single entry
		
		memory.memorize(expression.makeCopy(), FEEDBACK_POSITIVE_HIGH);
		
		// Strict (2 results, but different values)
		Map<NAdicoExpression<Attributes<Set<String>>, Aim<String>, Conditions<NAdicoExpression>>, Float> resultsDuplicateSingleStrict = 
			memory.getMaxNAdicoExpressionsWithGivenExpressionOnAnyLevel(expression, false, true, true, nAdicoActionMemory.AGGREGATION_SUM);
		
		assertEquals("Strict execution on any level should result in two responses (2 single and bottom level)", 2, resultsDuplicateSingleStrict.size());
		assertEquals("Duplicate entry should have twice the value (i.e., 4)", (Float)4.0f, resultsDuplicateSingleStrict.get(expression));
		
		// Wildcard (4 results, but different values)
		Map<NAdicoExpression<Attributes<Set<String>>, Aim<String>, Conditions<NAdicoExpression>>, Float> resultsDuplicateSingleWildcard = 
			memory.getMaxNAdicoExpressionsWithGivenExpressionOnAnyLevel(expression, false, true, false, nAdicoActionMemory.AGGREGATION_SUM);
		
		assertEquals("Strict execution on any level should result in two responses (single and bottom level)", 4, resultsDuplicateSingleWildcard.size());
		assertTrue("Wildcard execution on any level contains single expression", resultsDuplicateSingleWildcard.containsKey(expression));
		assertEquals("Wildcard execution on any level contains single expression with value 4", (Float)4.0f, resultsDuplicateSingleWildcard.get(expression));
		assertTrue("Wildcard execution on any level contains nested expression on middle level", resultsDuplicateSingleWildcard.containsKey(entryWithSearchExpressionInMiddle));
		assertEquals("Wildcard execution on any level contains nested expression on middle level with value 2", (Float)2.0f, resultsDuplicateSingleWildcard.get(entryWithSearchExpressionInMiddle));
		assertTrue("Wildcard execution on any level contains nested expression on second-last level", resultsDuplicateSingleWildcard.containsKey(entryWithSearchExpressionOnSecondLastPosition));
		assertEquals("Wildcard execution on any level contains nested expression on second-last level with value 2", (Float)2.0f, resultsDuplicateSingleWildcard.get(entryWithSearchExpressionOnSecondLastPosition));
		assertTrue("Wildcard execution on any level contains nested expression on bottom level", resultsDuplicateSingleWildcard.containsKey(entryWithSearchExpressionOnBottom));
		assertEquals("Wildcard execution on any level contains nested expression on bottom level with value 2", (Float)2.0f, resultsDuplicateSingleWildcard.get(entryWithSearchExpressionOnBottom));
		
		// TODO: Add statement with same expression on multiple nested levels
		
		//NAdicoExpression<Attributes<Set<String>>, Aim<String>, Conditions<NAdicoExpression>> expressionWithNestedRepeatedSearchExpression = expression1.makeCopy();
		//expressionWithNestedRepeatedSearchExpression.conditions.setPreviousAction(expression.makeCopy());
		//expressionWithNestedRepeatedSearchExpression.conditions.getPreviousAction().conditions.setPreviousAction(expression.makeCopy());
		//expressionWithNestedRepeatedSearchExpression.conditions.getPreviousAction().conditions.getPreviousAction().conditions.setPreviousAction(expression2);
	}
	
	@Test
	public void searchExpressionOnTopLevelForStrictAndWildcardConditionsComparison() {
		
		fillMemory();
		
		// Map of single expression
		assertEquals("Expression on same level should be found", expression1, memory.getMaxNAdicoExpressionsWithGivenExpressionAsLastExpression(expression1, false, true, nAdicoActionMemory.AGGREGATION_SUM).keySet().iterator().next());
		
		// Individual expression
		assertEquals("Expression on same level should be found", expression1, memory.getMaxNAdicoExpressionWithGivenExpressionAsLastExpression(expression1, false, true, nAdicoActionMemory.AGGREGATION_SUM).getKey());
		
		// Initial statement
		NAdicoExpression<Attributes<Set<String>>, Aim<String>, Conditions<NAdicoExpression>> expression = 
			factory.createNAdicoAction(
				new Attributes<>().addIndividualMarker(AGENT_NAME, ownerTwo).addSocialMarker(ROLE_NAME, roleOne), 
				new Aim<String>(actionTwo), new Conditions<NAdicoExpression>());
		
		// Introduce 2 preceding actions
		NAdicoExpression<Attributes<Set<String>>, Aim<String>, Conditions<NAdicoExpression>> advancedExpression = expression;
		NAdicoExpression<Attributes<Set<String>>, Aim<String>, Conditions<NAdicoExpression>> expr1Copy = expression1.makeCopy();
		expr1Copy.conditions.setPreviousAction(expression0);
		advancedExpression.conditions.setPreviousAction(expr1Copy);
		
		memory.memorize(advancedExpression, FEEDBACK_POSITIVE_HIGH);
		
		// Map with multiple entries, but only one that has required expression on given level
		assertEquals("Expression on same level should be found", expression1, memory.getMaxNAdicoExpressionsWithGivenExpressionAsLastExpression(expression1, false, true, nAdicoActionMemory.AGGREGATION_SUM).keySet().iterator().next());
		
		// Now add expression that has search expression on level 0 (current level)
		NAdicoExpression<Attributes<Set<String>>, Aim<String>, Conditions<NAdicoExpression>> expr1Copy2 = expression1.makeCopy();
		expr1Copy2.conditions.setPreviousAction(advancedExpression);
		
		memory.memorize(expr1Copy2, FEEDBACK_POSITIVE_HIGH);
		
		// Map with multiple entries, but only one that has required expression on given level
		Map<NAdicoExpression<Attributes<Set<String>>, Aim<String>, Conditions<NAdicoExpression>>, Float> result1 = memory.getMaxNAdicoExpressionsWithGivenExpressionAsLastExpression(expression1, false, true, nAdicoActionMemory.AGGREGATION_SUM);
		
		assertEquals("Expression on same level should be found once for strict query", 1, result1.keySet().size());
		assertEquals("Expression on same level should find exact expression for strict query", expression1, result1.keySet().iterator().next());
		assertEquals("Expression on same level should find exact value for strict query", (Float)2.0f, (Float)result1.get(expression1));
		
		Map<NAdicoExpression<Attributes<Set<String>>, Aim<String>, Conditions<NAdicoExpression>>, Float> result2 = memory.getMaxNAdicoExpressionsWithGivenExpressionAsLastExpression(expression1, false, false, nAdicoActionMemory.AGGREGATION_SUM);
		
		assertEquals("Expression on same level should be found once for weak conditions comparison", 1, result2.size());
		
		assertTrue("Expression on same level should find exact entry", result2.containsKey(expression1));
		assertEquals("Expression on same level should find exact value", (Float)4.0f, (Float)result2.get(expression1));
		
	}
	
	@Test
	public void searchSubExpressionIn2Levels() {
		
		fillMemory();
		
		// Initial statement
		NAdicoExpression<Attributes<Set<String>>, Aim<String>, Conditions<NAdicoExpression>> expression = 
			factory.createNAdicoAction(
				new Attributes<>().addIndividualMarker(AGENT_NAME, ownerTwo).addSocialMarker(ROLE_NAME, roleOne), 
				new Aim<String>(actionTwo), new Conditions<NAdicoExpression>());
		
		// Introduce reaction
		NAdicoExpression<Attributes<Set<String>>, Aim<String>, Conditions<NAdicoExpression>> advancedExpression = expression;
		advancedExpression.conditions.setPreviousAction(expression0);
		
		// Memorize 2-layered statement
		memory.memorize(advancedExpression, FEEDBACK_POSITIVE_HIGH);
		
		// Both tests return identical statements (full and partial output is identical in 2-level case)
		assertEquals("Search first statement in multi-level statement " + advancedExpression + "and return partial statement (search statement + one successor)", 
				advancedExpression, 
				memory.getMaxNAdicoExpressionWithGivenExpressionAsPreviousExpression(expression0, false, false, true, nAdicoActionMemory.AGGREGATION_SUM).getKey());
		
		assertEquals("Search first statement in multi-level statement " + advancedExpression + "and return full statement", 
				advancedExpression, 
				memory.getMaxNAdicoExpressionWithGivenExpressionAsPreviousExpression(expression0, false, true, true, nAdicoActionMemory.AGGREGATION_SUM).getKey());
		
	}
	
	@Test
	public void searchFirstSubExpressionIn3Levels() {
		
		fillMemory();
		
		// Initial statement
		NAdicoExpression<Attributes<Set<String>>, Aim<String>, Conditions<NAdicoExpression>> expression = 
			factory.createNAdicoAction(
				new Attributes<>().addIndividualMarker(AGENT_NAME, ownerTwo).addSocialMarker(ROLE_NAME, roleOne), 
				new Aim<String>(actionTwo), new Conditions<NAdicoExpression>());
		
		// Introduce 2 preceding actions
		NAdicoExpression<Attributes<Set<String>>, Aim<String>, Conditions<NAdicoExpression>> advancedExpression = expression;
		expression1.conditions.setPreviousAction(expression0);
		advancedExpression.conditions.setPreviousAction(expression1);
		
		// Memorize 3-layered statement
		memory.memorize(advancedExpression, FEEDBACK_POSITIVE_HIGH);
		
		assertEquals("Search in multi-level statement " + advancedExpression + " for " + expression0 + "and return partial statement (search statement + one successor)", 
				expression1, 
				memory.getMaxNAdicoExpressionWithGivenExpressionAsPreviousExpression(expression0, false, false, true, nAdicoActionMemory.AGGREGATION_SUM).getKey());
		
		assertTrue("Search in multi-level statement " + advancedExpression + " for " + expression0 + " and return full statement",  
				memory.getMaxNAdicoExpressionsWithGivenExpressionAsPreviousExpression(expression0, false, true, true, nAdicoActionMemory.AGGREGATION_SUM).containsKey(advancedExpression));
		
	}
	
	@Test
	public void searchMiddleSubExpressionIn3Levels() {
		
		fillMemory();
		
		// Initial statement
		NAdicoExpression<Attributes<Set<String>>, Aim<String>, Conditions<NAdicoExpression>> expression = 
			factory.createNAdicoAction(
				new Attributes<>().addIndividualMarker(AGENT_NAME, ownerTwo).addSocialMarker(ROLE_NAME, roleOne), 
				new Aim<String>(actionTwo), new Conditions<NAdicoExpression>());
		
		// Cache search expression
		NAdicoExpression<Attributes<Set<String>>, Aim<String>, Conditions<NAdicoExpression>> searchExpression = expression1.makeCopy();
		NAdicoExpression<Attributes<Set<String>>, Aim<String>, Conditions<NAdicoExpression>> advancedResultExpression = expression;
		advancedResultExpression.conditions.setPreviousAction(expression1);
		
		// Introduce 2 preceding actions
		NAdicoExpression<Attributes<Set<String>>, Aim<String>, Conditions<NAdicoExpression>> advancedExpression = expression;
		expression1.conditions.setPreviousAction(expression0);
		advancedExpression.conditions.setPreviousAction(expression1);
		
		// Memorize 3-layered statement
		memory.memorize(advancedExpression, FEEDBACK_POSITIVE_HIGH);
		
		// Middle expressions only should not be returning any statement, since the action sequence is incomplete
		assertNull("Search in multi-level statement " + advancedExpression + " for " + searchExpression + " should return null", 
				memory.getMaxNAdicoExpressionWithGivenExpressionAsPreviousExpression(searchExpression, false, false, true, nAdicoActionMemory.AGGREGATION_SUM));
		
		// Should return the same as previous one, since succeeding statement has same length
		assertNull("Search in multi-level statement " + advancedExpression + " for " + searchExpression +  " should return null", 
				memory.getMaxNAdicoExpressionWithGivenExpressionAsPreviousExpression(searchExpression, false, true, true, nAdicoActionMemory.AGGREGATION_SUM));
		
	}
	
	@Test
	public void searchFullOrPartialSubExpressionIn3Levels() {
		
		fillMemory();
		
		// Initial statement
		NAdicoExpression<Attributes<Set<String>>, Aim<String>, Conditions<NAdicoExpression>> expression = 
			factory.createNAdicoAction(
				new Attributes<>().addIndividualMarker(AGENT_NAME, ownerTwo).addSocialMarker(ROLE_NAME, roleOne), 
				new Aim<String>(actionTwo), new Conditions<NAdicoExpression>());
		
		// Cache search expression
		NAdicoExpression<Attributes<Set<String>>, Aim<String>, Conditions<NAdicoExpression>> searchExpression = expression1;
		NAdicoExpression<Attributes<Set<String>>, Aim<String>, Conditions<NAdicoExpression>> advancedResultExpression = expression;
		advancedResultExpression.conditions.setPreviousAction(expression1);
		
		// Introduce 2 preceding actions
		NAdicoExpression<Attributes<Set<String>>, Aim<String>, Conditions<NAdicoExpression>> advancedExpression = expression;
		expression1.conditions.setPreviousAction(expression0);
		expression2.conditions.setPreviousAction(expression1);
		advancedExpression.conditions.setPreviousAction(expression2);
		
		// Memorize 3-layered statement
		memory.memorize(advancedExpression, FEEDBACK_POSITIVE_HIGH);
		
		assertEquals("Search in multi-level statement " + advancedExpression + " for " + searchExpression + " and return partial statement (search statement + one successor)", 
				expression2, 
				memory.getMaxNAdicoExpressionWithGivenExpressionAsPreviousExpression(expression1, false, false, true, nAdicoActionMemory.AGGREGATION_SUM).getKey());
		
		assertEquals("Search in multi-level statement " + advancedExpression + " for " + searchExpression + " and return full statement", 
				advancedResultExpression, 
				memory.getMaxNAdicoExpressionWithGivenExpressionAsPreviousExpression(expression1, false, true, true, nAdicoActionMemory.AGGREGATION_SUM).getKey());
		
	}
	
	//TODO: test for value aggregation for generalised statements
	//TODO: test for getNAdicoExpressionsOnAnyLevel (i.e., non-max variant of method)

}
