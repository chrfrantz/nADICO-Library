package org.nzdis.nadico;

import java.util.*;
import java.util.Map.Entry;

import org.nzdis.micro.inspector.annotations.Inspect;
import org.nzdis.micro.util.DataStructurePrettyPrinter;
import org.nzdis.nadico.components.Aim;
import org.nzdis.nadico.components.Attributes;
import org.nzdis.nadico.components.Conditions;
import org.nzdis.nadico.deonticRange.DeonticRange;
import org.nzdis.nadico.deonticRange.MemoryUpdateException;
import org.nzdis.nadico.listener.NAdicoGeneralizationProvider;
import org.nzdis.nadico.listener.NAdicoMemoryChangeListener;
import org.sofosim.structures.Pair;

public class NAdicoGeneralizer {

	/**
	 * Internal debug switch
	 */
	private final boolean debug = false;
	
	/**
	 * Owner agent
	 */
	private final String owner;

	/**
	 * Contextual information helpful during debugging (e.g., instance reference).
	 * Only printed for error output, not regular stdout.
	 */
	private final String context;
	
	/**
	 * NAdicoConfiguration (including deontic range specification).
	 */
	private final NAdicoConfiguration config;
	
	/**
	 * Deontic range reference for dynamic resolution of 
	 * deontic values used in NAdicoExpressions.
	 */
	@Inspect
	public final DeonticRange deonticRange;
	
	/**
	 * Aggregation strategy to derive valence of generalized expression 
	 * from individual actions. Either summing valences ({@link #AGGREGATION_STRATEGY_SUM}),
	 * average value ({@link #AGGREGATION_STRATEGY_MEAN}), or taking highest value pointing
	 * in direction of deontic bias ({@link #AGGREGATION_STRATEGY_OPPORTUNISTIC}).
	 */
	@Inspect
	private String aggregationStrategyGeneralization = AGGREGATION_STRATEGY_SUM;
	
	/**
	 * Summing Aggregation Strategy: Determines general statement's deontic based on sum of grouped instances.
	 */
	public static final String AGGREGATION_STRATEGY_SUM = "AGGREGATION_SUM";
	
	/**
	 * Rational Aggregation Strategy: Determines general statement's deontic as mean of grouped instances.
	 */
	public static final String AGGREGATION_STRATEGY_MEAN = "AGGREGATION_MEAN";
	
	/**
	 * Opportunistic Aggregation Strategy: Determines general statement's deontic based on max. deviation of valences from normative center of grouped instances.
	 */
	public static final String AGGREGATION_STRATEGY_OPPORTUNISTIC = "AGGREGATION_OPP";
	
	protected NAdicoFactory<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>> factory;
	
	/**
	 * Indicates whether non-Attribute aim properties are removed during generalization
	 */
	private boolean removeNonAttributeAimPropertiesDuringGeneralization = false;
	
	public NAdicoGeneralizer(final String owner, final String context, final NAdicoConfiguration config){
		this.owner = owner;
		this.context = context;
		this.config = config;
		// Initialize deontic range which automatically subscribes to NAdicoMemory (for registering changes)
		this.deonticRange = new DeonticRange(this, this.config.deonticRangeConfig);
		// Initialize factory and pass this deontic range instance
		factory = new NAdicoFactory<>(this.deonticRange);
	}

	/**
	 * Returns name of owning agent.
	 * @return
	 */
	public String getOwner() {
		return this.owner;
	}

	/**
	 * Returns context information.
	 * @return
	 */
	public String getContext() { return this.context; }

	/**
	 * Sets the aggregation strategy for valenced generalized statements 
	 * produced with {@link #generalizeValencedExpressions(Map)}.
	 * @param strategy Strategy as specified in AGGREGATION_STRATEGY_ constants.
	 */
	public void setAggregationStrategyForGeneralization(final String strategy){
		this.aggregationStrategyGeneralization = strategy;
	}
	
	/**
	 * Sets the aggregation strategy for ambiguous/conflicting nADICO 
	 * expressions produced with {@link #deriveADICStatements()}.
	 * @param strategy Strategy as specified in AGGREGATION_STRATEGY_ constants.
	 */
	/*public void setAggregationStrategyForNAdicoExpressions(final String strategy){
		this.aggregationStrategyNAdicoExpressions = strategy;
	}*/
	
	/**
	 * Indicates whether non-Attributes aim properties are removed during generalization.
	 * @param removeNonAttributeAimPropertiesDuringGeneralization Remove non-Attribute aim properties during generalization
	 */
	public void setGeneralizationOption(boolean removeNonAttributeAimPropertiesDuringGeneralization) {
		this.removeNonAttributeAimPropertiesDuringGeneralization = removeNonAttributeAimPropertiesDuringGeneralization;
	}
	
	/**
	 * Registered listeners notified upon change in generalized expressions.
	 */
	private LinkedHashSet<NAdicoMemoryChangeListener> listeners = new LinkedHashSet<>();
	
	/**
	 * Registers a listener that is called when memory entries for the NAdicoMemory change.
	 * @param listener
	 */
	public void registerMemoryChangeListener(NAdicoMemoryChangeListener listener){
		if(!this.listeners.contains(listener)){
			this.listeners.add(listener);
		}
	}
	
	/**
	 * Deregisters given NAdicoMemoryChangeListener.
	 * @param listener
	 */
	public void deregisterMemoryChangeListener(NAdicoMemoryChangeListener listener){
		this.listeners.remove(listener);
	}
	
	/**
	 * Notifies all currently registered listeners.
	 */
	protected void notifyMemoryChangeListeners() throws MemoryUpdateException {
		for(NAdicoMemoryChangeListener listener: this.listeners){
			listener.memoryChanged();
		}
	}
	
	/**
	 * Registered providers invoked for generalisation.
	 */
	private LinkedHashSet<NAdicoGeneralizationProvider> providers = new LinkedHashSet<>();
	
	/**
	 * Registers a NAdicoGeneralisationProvider (typically agent associated with generaliser).
	 * @param provider
	 */
	public void registerGeneralisationProvider(NAdicoGeneralizationProvider provider){
		if(!this.providers.contains(provider)){
			this.providers.add(provider);
		}
	}
	
	/**
	 * Deregisters given NAdicoGeneralisationProvider.
	 * @param provider
	 */
	public void deregisterGeneralisationProvider(NAdicoGeneralizationProvider provider){
		this.providers.remove(provider);
	}
	
	/**
	 * Aggregates nAdicoExpressions derived with {@link #deriveADICStatements()}. 
	 * Operates based on cached statements and prunes conditions and 
	 * consequential statements. NOTE: Only useful for deontic graph representation purposes;
	 * not textual representation.
	 * @return
	 */
	/*public Collection<NAdicoExpression<Attributes<String>, Aim<String>, Conditions<NAdicoExpression>>> getAggregatedNAdicoExpressions() {
		ArrayList<NAdicoExpression<Attributes<String>, Aim<String>, Conditions<NAdicoExpression>>> result = new ArrayList<>();
		for (int i = 0; i < cachedNAdicoStatements.size(); i++) {
			NAdicoExpression<Attributes<String>, Aim<String>, Conditions<NAdicoExpression>> stmt = cachedNAdicoStatements.get(i);
			NAdicoExpression<Attributes<String>, Aim<String>, Conditions<NAdicoExpression>> newStmt;
			if (aggregationStrategyNAdicoExpressions.equals(AGGREGATION_STRATEGY_SUM)) {
				if (stmt.isStatement()) {
					newStmt = factory.createNAdicoStatement(stmt.attributes, stmt.aim, null);
					if(result.contains(newStmt)){
						for(int j = 0; j < result.size(); j++) {
							if(result.get(j).equals(newStmt)){
								if(result.get(j).deontic == null){
									result.get(j).deontic = stmt.deontic;
								} else {
									result.get(j).deontic += stmt.deontic;
								}
							}
						}
					} else {
						newStmt.deontic = stmt.deontic;
						result.add(newStmt);
					}
				} else {
					throw new RuntimeException("Aggregation for type (probably combination) has not been implemented.");
				}
			} else if (aggregationStrategyNAdicoExpressions.equals(AGGREGATION_STRATEGY_MAX)) {
				throw new RuntimeException("nADICO expression integration based on MAX is not implemented yet.");
			}
		}
		return result;
	}*/
	
	/**
	 * Cached nADICO statements from Level 0 Generalisation
	 */
	@Inspect
	private LinkedHashSet<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>> cachedNAdicoStatements = new LinkedHashSet<>();
	
	/**
	 * Returns generated nADICO expressions (from Level 0 Generalisation).
	 * To retrieve action sequences prior to nADICO generation (i.e. no derived consequences), use {@link #getCachedGeneralizedValencedExpressions()}.
	 * @return
	 */
	public LinkedHashSet<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>> getNAdicoExpressions() {
		return cachedNAdicoStatements;
	}
	
	/**
	 * Cached map of nADICO statements from higher-level generalisations (organised by generalisation level).
	 */
	@Inspect
	private LinkedHashMap<Integer, LinkedHashSet<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>>> cachedNAdicoStatementsHigherLevel = new LinkedHashMap<>();
	
	/**
	 * Returns generated nADICO expressions on all possible levels of generalisation (organised by generalisation level, i.e. 0, 1, 2).
	 * Requires previous call to {@link #deriveADICStatements(int, Attributes)} or {@link #deriveADICStatements(LinkedHashMap, boolean, Attributes)}.
	 * @return
	 */
	public LinkedHashMap<Integer, LinkedHashSet<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>>> getMultiLevelNAdicoExpressions() {
		return cachedNAdicoStatementsHigherLevel;
	}
	
	/**
	 * This method uses cached generalized expressions (generated via {@link #generalizeValencedExpressions(Map)}) 
	 * to derive ADIC statements. Requires previous call to {@link #generalizeValencedExpressions(Map)} and operates 
	 * on its cached expressions to derive nADICO statements.
	 * The results are cached in {@link #cachedNAdicoStatements}.
	 * This method only work works on Level 0 generalisations.
	 * @param subjectiveAttributes Attribute set of subject from whose perspective generalisation and ADIC derivation is to be performed.
	 *                                If set to null, last attribute set of respective statement is used as perspective.
	 * @return
	 */
	@Inspect
	public LinkedHashSet<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>> deriveADICStatements(Attributes<LinkedHashSet<String>> subjectiveAttributes){
		// Assign for inspection
		cachedNAdicoStatements = new LinkedHashSet<>(deriveADICStatements(cachedGeneralizedExprs, false, subjectiveAttributes));
		return cachedNAdicoStatements;
	}
	
	/**
	 * Derives nADICO statements from higher-level generalisations using {@link #cachedGeneralizedExprsHigherLevel}, 
	 * generated via {@link #generalizeValencedExpressionsOnHigherLevel(Map, int)}.
	 * The results are cached in {@link #cachedNAdicoStatementsHigherLevel} with key being the level.
	 * @param level Generalisation level of expressions to be used for nADICO statements
	 * @param subjectAttributes Subject's attributes to reflect perspective from which the generalisation is performed.
	 *                             If set to null, last attribute set of respective statement is used as perspective.
	 * @return
	 */
	public LinkedHashSet<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>> deriveADICStatements(int level, Attributes<LinkedHashSet<String>> subjectAttributes){
		// Assign for inspection
		cachedNAdicoStatementsHigherLevel.put(level, deriveADICStatements(cachedGeneralizedExprsHigherLevel.get(level), false, subjectAttributes));
		return cachedNAdicoStatementsHigherLevel.get(level);
	}
		
	/**
	 * This method uses cached generalized expressions (generated via {@link #generalizeValencedExpressions(Map)}) 
	 * to derive ADIC statements. Requires previous call to {@link #generalizeValencedExpressions(Map)} or {@link #generalizeValencedExpressionsOnHigherLevel(Map, int)} 
	 * and operates on its cached expressions passed as parameter to derive nADICO statements.
	 * @param generalisedExpressions Generalised expressions (e.g. from {@link #cachedGeneralizedExprs} or value of map {@link #cachedGeneralizedExprsHigherLevel})
	 * @param requireDifferingAttributesInPrecedingStatement Indicates whether a preceding expression needs to have a different attribute set to be considered monitored (Default: true for Level 0 generalisation, else false).
	 * @param attributesOfSubject Attributes elements of subject from whose perspective the statements are to be evaluated from. If set to null, the attributes of the respective last statement in a sequence is used.
	 * @return
	 */
	public LinkedHashSet<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>> deriveADICStatements(
			LinkedHashMap<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>, ArrayList<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>>> generalisedExpressions,
			boolean requireDifferingAttributesInPrecedingStatement, Attributes<LinkedHashSet<String>> attributesOfSubject){
		
		// Initialise result structure
		ArrayList<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>> adics = new ArrayList<>();
		
		if (generalisedExpressions == null || generalisedExpressions.isEmpty()) {
			System.err.println("Cannot derive ADIC statements yet. Not enough data.");
			return new LinkedHashSet<>(adics);
		}
		
		// Copy generalized expressions
		LinkedHashMap<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>,ArrayList<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>>> cacheCopy = 
				NAdicoGeneralizerHelper.makeCopyOfExpressionMap(generalisedExpressions);
		
		if (debug) {
			System.out.println("Cached statements prior to extraction of social markers: " + DataStructurePrettyPrinter.decomposeRecursively(cacheCopy, null));
		}
		
		if (debug) {
			System.out.println("---- all expressions to be converted: " + DataStructurePrettyPrinter.decomposeRecursively(cacheCopy.keySet(), null).toString());
			System.out.println(" - ONE BY ONE -");
		}
		
		for (Entry<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>, ArrayList<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>>> fullExpr : cacheCopy.entrySet()) {
			
			NAdicoExpression expr = fullExpr.getKey();
			
			if (debug) {
				System.out.println("==Expression to be converted: " + expr);
			}
			
			// Candidate for monitored statement
			NAdicoExpression candidateMonitored = null;
			
			if (requireDifferingAttributesInPrecedingStatement) {
				// Determine first nested expression with different attribute set - variably rely on provided subjective attribute set or assume last one in chain
				candidateMonitored = getPrecedingExpressionWithDifferentAttributes(attributesOfSubject != null ? attributesOfSubject : expr.attributes, expr);
			} else {
				// Simply take previous expression
				candidateMonitored = (NAdicoExpression) expr.conditions.getPreviousAction();
			}
			
			if (debug) {
				System.out.println("Candidate for monitored statement (different actor required? " + requireDifferingAttributesInPrecedingStatement + "): " + candidateMonitored);
			}
			
			if (candidateMonitored != null) {
				// Iterate through full statement and delete monitor candidate
				if (deleteMonitoredStatementFromStatementChain(expr, candidateMonitored)) {
					if (debug) System.out.println("Successfully deleted");
				}
				if (debug) {
					System.out.println("Consequential candidate after deletion: " + expr);
				}
			}
			
			// Initialize consequential statement with deontic range
			NAdicoExpression copyOfConsequential = expr.makeCopy(this.deonticRange);
			copyOfConsequential.makeStatement();
			
			// Take previous actions if there - if not, ignore
			if (candidateMonitored != null) {
				// Monitored statements are first conditions that have diverting attributes
				NAdicoExpression copyOfMonitored = candidateMonitored.makeCopy(this.deonticRange);
				
				// Convert monitored actions to statements - should work for combinations and statements
				copyOfMonitored.makeStatement();
				if (copyOfMonitored.conditions != null) {
					// Convert all monitored statement's conditions
					convertNAdicoActionsToStatements(copyOfMonitored.conditions.properties.values());
				}
				
				// Attach consequential statement to monitored
				copyOfMonitored.setOrElse(copyOfConsequential);
				
				// Copy deontic from consequential to leading monitored statement
				copyOfMonitored.deontic = copyOfConsequential.deontic;
				
				// Invert deontic of consequential statement
				copyOfConsequential.deontic = deonticRange.getDeonticValueMapper().invertDeonticValue(copyOfConsequential.deontic);
				copyOfConsequential.deonticInverted = true;
				
				// Add statistics of constituting statements
				copyOfMonitored.count = fullExpr.getValue().size();
				
				// Add to ADIC collection
				adics.add(copyOfMonitored);
				
				if (debug){
					System.out.println("==Expression after conversion: " + copyOfMonitored);
				}
			} else {
				// just a descriptive norm
				if (debug) {
					System.out.println("==Expression after ignoring conversion (mere descriptive): " + copyOfConsequential);
				}
				// Add statistics of constituting statements
				copyOfConsequential.count = fullExpr.getValue().size();
				
				// Add to collection
				adics.add(copyOfConsequential);
			}
		}

		// Sort in descending order
		adics.sort(new NAdicoExpressionCountComparator(false));
		
		//System.err.println("ADICs: " + DataStructurePrettyPrinter.decomposeRecursively(adics, null));
		return new LinkedHashSet<>(adics);
	}
	
	/**
	 * Deletes a given expression from the chain. Manipulates chain directly.
	 * @param chain Chain to delete from
	 * @param expressionToDelete Expression to delete
	 * @return Indicator whether deletion was successful
	 */
	private boolean deleteMonitoredStatementFromStatementChain(NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>> chain, NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>> expressionToDelete) {
		if (chain.equals(expressionToDelete)) {
			//if it is top-level statement, then is nothing left
			//System.out.println("To be deleted matches requested: Candidate: " + System.getProperty("line.separator") +
			// chain + System.getProperty("line.separator") + "Requested: " + System.getProperty("line.separator") + expressionToDelete);
			//TODO may still make problems....
			chain = null;
			return true;
		} else if(chain.isCombination()) {
			// Check all combination elements on first level
			for (NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>> element: chain.nestedExpressions) {
				// Depth first
				if (element.equals(expressionToDelete)) {
					// Check this expression itself
					chain.nestedExpressions.remove(element);
					return true;
				} else {
					// Dive into its conditions
					if (deleteMonitoredStatementFromStatementChain(element, expressionToDelete)) {
						return true;
					}
				}
			}
		} else {
			// Check conditions
			if (chain.conditions.getPreviousAction() != null) {
				if (chain.conditions.getPreviousAction().equals(expressionToDelete)) {
					// Do immediate check
					//System.out.println("Should have deleted property");
					chain.conditions.removePreviousAction();
					return true;
				} else {
					// Delegate to lower level
					return deleteMonitoredStatementFromStatementChain(chain.conditions.getPreviousAction(), expressionToDelete);
				}
			}
		}
		// else it could probably not be found
		return false;
	}
	
	/**
	 * Iterates through the expression chain (starting with last, i.e., front action) and returns first expression with diverting attribute set. 
	 * Considers previous actions and nested expressions.
	 * @param leadingAttributes Attributes to be diverted from
	 * @param expression Expression to be iterated over
	 * @return expression with diverting attribute set, or null if non found (i.e. all the same attributes)
	 */
	private NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>> getPrecedingExpressionWithDifferentAttributes(final Attributes<LinkedHashSet<String>> leadingAttributes, final NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>> expression) {
		
		//move through previous actions until different attributes are found
		NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>> diffAttributesExpr = expression;
		
		//check for different attributes
		if ((diffAttributesExpr.attributes == null && !diffAttributesExpr.isCombination()) 
				|| (diffAttributesExpr.attributes != null && !diffAttributesExpr.attributes.equals(leadingAttributes))) {
			//either null (and not combination) or already differing
			return diffAttributesExpr;
		} else if (diffAttributesExpr.isCombination()) {
			//check nested expressions
			//check only first-order elements of combination - breadth first
			for (NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>> nestedExpression : diffAttributesExpr.nestedExpressions) {
				if(!nestedExpression.attributes.equals(leadingAttributes)) {
					//if different return entire combination (not just element)
					return diffAttributesExpr;
				}
			}
			//if nothing found on first level of combination, dig into nested expressions of individual elements - depth
			for (NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>> nestedExpression : diffAttributesExpr.nestedExpressions) {
				NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>> diverting 
					= getPrecedingExpressionWithDifferentAttributes(leadingAttributes, nestedExpression);
				if (diverting != null) {
					//if a nested expression has a diverting element, return this
					return diverting;
				}
			}
		} else {
			//attributes don't match --> dig deeper
			//check conditions
			if (diffAttributesExpr.conditions != null && diffAttributesExpr.conditions.getPreviousAction() != null) {
				return getPrecedingExpressionWithDifferentAttributes(leadingAttributes, diffAttributesExpr.conditions.getPreviousAction());
			}
			return null;
		}
		return null;
	}
	
	/**
	 * Recursively extracts social markers from collection of given statements.
	 * @param markers
	 * @param collection
	 * @return
	 */
	private LinkedHashMap<String, LinkedHashSet<String>> extractSocialMarkers(LinkedHashMap<String, LinkedHashSet<String>> markers, final Collection<NAdicoExpression> collection){
		if (markers == null) {
			markers = new LinkedHashMap<String, LinkedHashSet<String>>();
		}
		for (NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>> expr: collection) {
			if (expr != null) {
				if (expr.attributes != null) {
					// go through individual social marker entries
					for (Entry<String, LinkedHashSet<String>> entry: expr.attributes.socialMarkers.entrySet()) {
						// if markers with matching keys had been extracted before, add newly found values
						if (markers.containsKey(entry.getKey())) {
							LinkedHashSet<String> contained = markers.get(entry.getKey());
							contained.addAll(entry.getValue());
							markers.put(entry.getKey(), contained);
						} else {
							// else blindly add found markers
							markers.put(entry.getKey(), entry.getValue());
						}
						
					}
				}
				if (expr.conditions != null) {
					extractSocialMarkers(markers, expr.conditions.properties.values());
				}
			}
		}
		return markers;
	}
	
	/**
	 * Recursively converts all top-level and nested instances of NAdicoAction to NAdicoStatement. 
	 * Particularly useful to pass conditions for conversion.
	 * @param collection Collection of NAdicoExpressions (preferably actions)
	 * @return
	 */
	private void convertNAdicoActionsToStatements(Collection<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>> collection){
		if (collection == null) {
			return;
		}
		for (NAdicoExpression expr: collection) {
			if (expr != null) {
				//convert statement itself
				expr.makeStatement();
				
				//test for conditions
				if (expr.conditions != null && expr.conditions.properties != null && !expr.conditions.properties.isEmpty()) {
					//System.out.println("Going one level down.");
					convertNAdicoActionsToStatements(expr.conditions.properties.values());
				}
				if (expr.isCombination()) {
					//System.out.println("Converting nested statements.");
					convertNAdicoActionsToStatements(expr.nestedExpressions);
				}
				
				//System.out.println("Now statement is: " + expr);
			}
		}
	}
	
	/**
	 * Cached generalized statements.
	 */
	@Inspect
	private LinkedHashMap<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>, ArrayList<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>>> cachedGeneralizedExprs = new LinkedHashMap<>();
	
	/**
	 * Cached higher level generalised statements. Map key is level, Map value is a map containing generalized statements as key and associated action instances constituting the corresponding generalized statement as values.
	 */
	@Inspect
	private LinkedHashMap<Integer,LinkedHashMap<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>, ArrayList<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>>>> cachedGeneralizedExprsHigherLevel = new LinkedHashMap<>();
	
	/**
	 * Returns generalized expressions. 
	 * @return
	 */
	@Inspect
	private Set<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>> getGeneralizedExpressions(){
		return cachedGeneralizedExprs.keySet();
	}
	
	/**
	 * Returns cached generalized expressions including associated action instance statements used to derive 
	 * the former (without invoking generalization process via {@link #generalizeValencedExpressions(Map)}).
	 * The generalized expression comprises the entire action sequence prior to conversion to nADICO statement. 
	 * To retrieve nADICO statements (i.e. action sequences including derived sanctions), use {@link #getNAdicoExpressions()}.
	 * @return
	 */
	public LinkedHashMap<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>, ArrayList<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>>> getCachedGeneralizedValencedExpressions(){
		return cachedGeneralizedExprs;
	}
	
	/**
	 * Returns minimal or maximal statement of cached generalized expressions.
	 * @param minVsMax true indicating minimal value, false indicating maximal value
	 * @return expression that matched min. or max. value
	 */
	private NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>> getStatementWithExtremeValence(final boolean minVsMax){
		Float extreme;
		if (minVsMax) {
			extreme = Float.MAX_VALUE;
		} else {
			extreme = -Float.MAX_VALUE;
		}
		NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>> extremeStmt = null;
		for (NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>> expr: cachedGeneralizedExprs.keySet()) {
			if (minVsMax) {
				//minimal
				if (expr.deontic < extreme) {
					extreme = expr.deontic;
					extremeStmt = expr;
				}
			} else {
				//maximal
				if (expr.deontic > extreme) {
					extreme = expr.deontic;
					extremeStmt = expr;
				}
			}//ignore equal case - no change
		}
		return extremeStmt;
	}
	
	/**
	 * Returns statement with minimum valence.
	 * @return
	 */
	public NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>> getStatementWithMinValence(){
		return getStatementWithExtremeValence(true);
	}
	
	/**
	 * Returns statement with maximum valence.
	 * @return
	 */
	public NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>> getStatementWithMaxValence(){
		return getStatementWithExtremeValence(false);
	}
	
	/**
	 * Calculates deontic value of generalized statement based on greatest deviation of instance statement from normative center (as opposed to sum of instance deontics).
	 * @param generalizedExpressions
	 * @return Generalized expressions with greatest deviating deontic filled in leading statement's deontic field.
	 */
	private LinkedHashMap<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>, ArrayList<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>>> assignMinMaxValuesBasedOnDeonticRange(final LinkedHashMap<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>, ArrayList<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>>> generalizedExpressions) {
		Float normativeCenter = deonticRange.getNormativeCenter();
		if (normativeCenter == null || Float.isNaN(normativeCenter) || Float.POSITIVE_INFINITY == normativeCenter || Float.NEGATIVE_INFINITY == normativeCenter) {
			normativeCenter = 0f;
		}
		// Iterate over grouped structure
		for (Entry<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>,ArrayList<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>>> generalizedEntry: generalizedExpressions.entrySet()){
			Float min = Float.MAX_VALUE;
			Float max = -Float.MAX_VALUE;
			for (int i = 0; i < generalizedEntry.getValue().size(); i++) {
				NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>> instanceAction = generalizedEntry.getValue().get(i);
				if (instanceAction.deontic != null) {
					if (instanceAction.deontic < min) {
						min = instanceAction.deontic;
					} else if (instanceAction.deontic > max) {
						max = instanceAction.deontic;
					} // ignore equals case
				} else {
					System.out.println("Aggregation based on deontic: Instance action is null: " + instanceAction);
				}
			}
			if (min == Float.MAX_VALUE) {
				min = normativeCenter;
			}
			if (max == -Float.MAX_VALUE) {
				max = normativeCenter;
			}

			// Check for deviation from normative center
			Float maxDiff = max - normativeCenter;
			Float minDiff = normativeCenter - min;
			// and assign deontic with greatest deviation to leading/general statement's deontic
			generalizedEntry.getKey().deontic = (Math.abs(maxDiff) > Math.abs(minDiff) ? max : min);
		}
		return generalizedExpressions;
	}
	
	/**
	 * Calculates deontic value of generalized statement based on mean deontic value of instance statements (using summed values as input).
	 * @param generalizedExpressions generalized expressions holding sum of all instance deontics in deontic field.
	 * @return Generalized expressions with mean deontic values, calculated by dividing sum of deontic values by number of instance statements.
	 */
	private LinkedHashMap<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>, ArrayList<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>>> assignMeanValuesBasedOnDeonticRange(final LinkedHashMap<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>,ArrayList<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>>> generalizedExpressions) {
		for (Entry<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>, ArrayList<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>>> entry: generalizedExpressions.entrySet()) {
			// Calculates mean value based on number of instance values for aggregated deontic value
			entry.getKey().deontic = entry.getKey().deontic / (float)entry.getValue().size();
		}
		return generalizedExpressions;
	}
	
	/**
	 * Generalises an individual nADICO expression.
	 * @param expression
	 * @return
	 */
	public NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>> generalizeExpression(final NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>> expression) {
		return generalizeExpression(expression, true);
	}
	
	/**
	 * Generalises a map of expressions and used the values to determine aggregate values stored in the deontic field of the generalised statement. Only generalizes based on AIC!
	 * This represents Level 0 Generalisation (i.e. operating on all attributes, not subset of those).
	 * @param valencednAdicoExpressions Individual valenced observations
	 * @return LinkedHashMap with generalised expressions in key (including aggregated input values as deontic) and collection of instances those have been derived from as value. 
	 * Instances have the valences as their deontic values.
	 */
	public LinkedHashMap<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>, ArrayList<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>>> generalizeValencedExpressions(final Map<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>, Float> valencednAdicoExpressions) throws MemoryUpdateException {
		
		LinkedHashMap<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>, Float> exprs = 
				NAdicoGeneralizerHelper.makeCopyOfValencedExpressions(valencednAdicoExpressions);
		
		// Initialize generalized expressions structure
		LinkedHashMap<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>, ArrayList<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>>> generalizedExprs = new LinkedHashMap<>();
		
		// Now check individual expressions against existing generalised ones
		for (Entry<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>, Float> entry: exprs.entrySet()){
			//new expression
			final NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>> generalNewInputExpr = generalizeExpression(entry.getKey(), true);
			if (entry.getKey().attributes.individualMarkers.isEmpty()) {
				throw new RuntimeException("Individual attributes are empty in action observation: " + entry.getKey());
			}
			
			//Aggregate instance expressions and maintain reference to instance expressions
			generalizedExprs = aggregateExpressionInstances(generalizedExprs, generalNewInputExpr, entry.getKey(), entry.getValue());
		}
		
		// Statements' deontic values are summed by default (AGGREGATION_STRATEGY_SUM), but may be adjusted depending on activated strategy
		generalizedExprs = assignStrategySpecificDeonticToGeneralizedExpressions(generalizedExprs);
		
		// Cache generalised expressions
		this.cachedGeneralizedExprs = generalizedExprs;
		// Call deontic range
		notifyMemoryChangeListeners();
		return generalizedExprs;
	}
	
	/**
	 * Assigns deontic values to generalized expressions dependent on activated strategy ({@link #AGGREGATION_STRATEGY_OPPORTUNISTIC}, {@link #AGGREGATION_STRATEGY_MEAN}).
	 * @param generalizedExprs
	 * @return
	 */
	private LinkedHashMap<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>, ArrayList<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>>> assignStrategySpecificDeonticToGeneralizedExpressions(
			LinkedHashMap<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>, ArrayList<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>>> generalizedExprs){
		if (aggregationStrategyGeneralization.equals(AGGREGATION_STRATEGY_OPPORTUNISTIC)) {
			// Reiterate and assign max deviating value for generalized deontic
			generalizedExprs = assignMinMaxValuesBasedOnDeonticRange(generalizedExprs);
		} else if (aggregationStrategyGeneralization.equals(AGGREGATION_STRATEGY_MEAN)) {
			generalizedExprs = assignMeanValuesBasedOnDeonticRange(generalizedExprs);
		}
		if (debug) {
			DataStructurePrettyPrinter.setMapDecompositionThreshold(1);
			System.out.println(owner + ": " + generalizedExprs.size() + " grouped expressions: " + System.getProperty("line.separator") + 
					DataStructurePrettyPrinter.decomposeRecursively(generalizedExprs, new StringBuffer()).toString()); 
		}
		return generalizedExprs;
	}
	
	/**
	 * Generalize and group a collection of expressions to AIC statements. Only generalizes based on AIC!
	 * @param nAdicoExpressions
	 * @return LinkedHashMap with generalized expressions in key and collection of instances those have been derived from as value
	 */
//	public LinkedHashMap<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>, ArrayList<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>>> generalizeExpressions(final Collection<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>> nAdicoExpressions){
//		ArrayList<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>> exprs = null;
//		try {
//			exprs = new ArrayList<>((Collection<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>>)ObjectCloner.deepCopy(nAdicoExpressions));
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		LinkedHashMap<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>,ArrayList<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>>> generalizedExprs = new LinkedHashMap<>();
//		for(int i = 0; i < exprs.size(); i++){
//			//new expression
//			NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>> generalNewInputExpr = generalizeExpression(exprs.get(i), true);
//			if(!generalizedExprs.isEmpty()){
//				boolean added = false;
//				//check for existing general expressions and eventually add matching instance statement
//				for(Entry<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>,ArrayList<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>>> generalEntry: generalizedExprs.entrySet()){
//					if(generalEntry.getKey().equalsOnAIC(generalNewInputExpr)){
//						generalEntry.getValue().add(exprs.get(i));
//						added = true;
//						break;
//					}
//				}
//				if(!added){
//					//add new general expression with instance statement
//					ArrayList<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>> newList = new ArrayList<>();
//					newList.add(exprs.get(i));
//					generalizedExprs.put(generalNewInputExpr, newList);
//				}
//			} else {
//				//add first element to general expressions
//				ArrayList<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>> newList = new ArrayList<>();
//				newList.add(exprs.get(i));
//				generalizedExprs.put(generalNewInputExpr, newList);
//			}
//		}
//		if(debug){
//			DataStructurePrettyPrinter.mapDecompositionThreshold = 1;
//			System.out.println(owner + ": " + generalizedExprs.size() + " grouped expressions: \n" + DataStructurePrettyPrinter.decomposeRecursively(generalizedExprs, new StringBuffer()).toString()); 
//		}
//		//probably do check if they really changed.
//		this.cachedGeneralizedExprs = generalizedExprs;
//		return generalizedExprs;
//	}
	
	/**
	 * Generalize individual expression. 
	 * @param expression Expression to generalize
	 * @param generalizeCopy If set to true, a copy of the expression is generalized. Otherwise, the generalization is directly performed on the input.
	 * @return
	 */
	private NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>> generalizeExpression(final NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>> expression, final boolean generalizeCopy){
		
		if (expression.isCombination()) {
			NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>> copiedCombination = expression;
			if (generalizeCopy) {
				copiedCombination = expression.makeCopy();	
			}
			for(NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>> expr : copiedCombination.nestedExpressions){
				// The value for generalizeCopy should always be true to ensure in-situ generalisation
				generalizeExpression(expr, generalizeCopy);
			}
			return copiedCombination;
		} else if (expression.isAction()) {
			// Assume generalisation of individual action (not full nADICO statement)
			NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>> generalizedAction = expression;
			if (generalizeCopy) {
				generalizedAction = expression.makeCopy(this.deonticRange);
			}
			generalizedAction.attributes = generalizeAttributes(expression.attributes);
			generalizedAction.aim = generalizeAim(expression.aim, removeNonAttributeAimPropertiesDuringGeneralization);
			generalizedAction.conditions = generalizeConditions(expression.conditions);
			return generalizedAction;
		} else if (expression.isStatement()) {
			// De facto termination condition for generalisation of statement combinations (consist only of statements, not action statements)
			if (debug) {
				System.out.println("No generalisation needed, since it is statement. Statement: " + expression);
			}
			return expression;
		} else {
			throw new RuntimeException("Unknown expression type for generalization. Type: " + expression);
		}
	}
	
	/**
	 * Generalizes attributes based on individual and social markers (simply retained). 
	 * For individual markers it invokes registered NAdicoGeneralizationProvider.
	 * If none are registered, individual markers are simply deleted.
	 * Social markers are simply retained (as deemed generalised by design).
	 * @param attributes Attributes to be generalised
	 * @return
	 */
	private Attributes<LinkedHashSet<String>> generalizeAttributes(final Attributes<LinkedHashSet<String>> attributes){
		Attributes<LinkedHashSet<String>> attr = new Attributes<LinkedHashSet<String>>().copyFrom(attributes);
		
		if (!providers.isEmpty()) {
			if (providers.size() > 1) {
				throw new RuntimeException("Individual attribute generalisation for multiple providers is not yet handled.");
			}
			for (NAdicoGeneralizationProvider provider: providers) {
				try {
				    attr.replaceIndividualMarkers((HashMap<String, LinkedHashSet<String>>) provider.generalizeAttributes(attr.individualMarkers));
				} catch (NullPointerException e) {
				    throw new RuntimeException("GeneralisationProvider returns null values. Check for proper implementation of generalisation in agent " + provider);
				}
			}
		} else {
			if (debug) {
				System.err.println("No individual generaliser registered. Individual markers are simply removed for generalisation.");
			}
			// Remove all individual markers
			attr.individualMarkers.clear();
		}
		return attr;
	}
	
	/**
	 * Generalize aim based on activity.
	 * @param aim Aim to be generalized
	 * @param removeNonAttributeAimPropertiesDuringGeneralization Indicates whether non-Attributes in aim properties values are removed during generalization
	 * @return
	 */
	private Aim<String> generalizeAim(final Aim<String> aim, boolean removeNonAttributeAimPropertiesDuringGeneralization){
		Aim<String> am = new Aim<String>().copyFrom(aim);
		// Check for attributes in property values and generalise those
		for (Entry entry : am.properties.entrySet()) {
			if (entry.getValue().getClass().equals(Attributes.class)) {
				Attributes generalisedAttr = generalizeAttributes((Attributes<LinkedHashSet<String>>) entry.getValue());
				entry.setValue(generalisedAttr);
			} else if (removeNonAttributeAimPropertiesDuringGeneralization) {
				// Reset values for non-Attribute properties
				entry.setValue("");
			}
		}
		return am;
	}
	
	/**
	 * Generalize conditions by recursively generalizing elements of previous actions.
	 * @param conditions
	 * @return
	 */
	private Conditions<NAdicoExpression> generalizeConditions(final Conditions<NAdicoExpression> conditions){
		Conditions<NAdicoExpression> con = null;
		try {
			con = new Conditions(conditions);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>> action: con.properties.values()) {
			if (action.isCombination()) {
				for (NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>> entry: action.nestedExpressions) {
					entry = generalizeExpression(entry, false);
				}
			} else {
				if (debug) {
					System.out.println("Non-combination to be generalised: " + action);
				}
				if (action.attributes != null) {
					action.attributes = generalizeAttributes(action.attributes);
				}
				if (action.aim != null) {
					action.aim = generalizeAim(action.aim, this.removeNonAttributeAimPropertiesDuringGeneralization);
				}
				if (action.conditions != null) {
					action.conditions = generalizeConditions(action.conditions);
				}
			}
		}
		return con;
	}
	
	/**
	 * Identifies the maximum number of social markers in attributes of given expressions.
	 * @param expressions
	 * @return
	 */
	private int determineMaxNumberOfSocialMarkerAttributes(Map<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>, Float> expressions) {
		int maxSocialAttributes = 0;
		for (NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>> expr: expressions.keySet()) {
			int numberOfSocialAttributes = expr.attributes.socialMarkers.size();
			if (numberOfSocialAttributes > maxSocialAttributes) {
				maxSocialAttributes = numberOfSocialAttributes;
			}
			//nice but inefficient
			//maxSocialAttributes = expr.attributes.socialMarkers.size() > maxSocialAttributes ? expr.attributes.socialMarkers.size() : maxSocialAttributes;
		}
		return maxSocialAttributes;
	}
	
	/**
	 * Generalises valenced expressions on higher levels ({@link #generalizeValencedExpressions(Map)} only works on Level 0 (i.e. all attributes)).
	 * It takes observed nADICO expressions as input and attempts to generalise them on <generalisationLevel> attributes (e.g. 2 instead of all), 
	 * in order to facilitate more abstract generalisations.
	 * The results are returned and cached to {@link #cachedGeneralizedExprsHigherLevel}.
	 * @param valencednAdicoExpressions
	 * @param generalisationLevel
	 * @return
	 */
	public LinkedHashMap<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>, ArrayList<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>>> generalizeValencedExpressionsOnHigherLevel(
			final Map<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>, Float> valencednAdicoExpressions, 
			final int generalisationLevel) {
		
		// Initialize empty expressions
		LinkedHashMap<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>, Float> exprs = null;
		
		// Determine the maximum number of social markers
		int maxSocialMarkers = determineMaxNumberOfSocialMarkerAttributes(valencednAdicoExpressions);
		
		// Determine size of social marker set sizes to perform generalisation over
		int setSize = maxSocialMarkers - generalisationLevel;
		
		if (setSize < 1) {
			System.err.println("Cannot generalize on level " + generalisationLevel + ". Max. generalisation level: " + (maxSocialMarkers - 1));
			return null;
		}
		
		// Deep copy the input first
		exprs = NAdicoGeneralizerHelper.makeCopyOfValencedExpressions(valencednAdicoExpressions);
		
		// Initialize empty generalised expressions
		LinkedHashMap<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>, ArrayList<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>>> generalizedExprs = new LinkedHashMap<>();
		
		if (exprs == null) {
			System.err.println("Input expressions are null. No generalisation possible.");
			return generalizedExprs;
		}
		
		// Retrieve social markers
		LinkedHashMap<String,LinkedHashSet<String>> socialMarkers = extractSocialMarkers(null, (Collection)exprs.keySet());
		
		//System.out.println("Social markers: " + socialMarkers);
		
		// Stringify and combine key and values; normalises multiple values into individual key-value statements
		LinkedHashSet<Pair<String,String>> stringifiedMarkers = new LinkedHashSet<>();
		for (Entry<String,LinkedHashSet<String>> val: socialMarkers.entrySet()) {
			if (val.getValue().size() == 1) {
				stringifiedMarkers.add(new Pair(val.getKey(), val.getValue().iterator().next()));
			} else {
				for (String nested: val.getValue()) {
					stringifiedMarkers.add(new Pair(val.getKey(), nested));
				}
			}
		}
		if (debug) {
			System.out.println("HIGHER-ORDER GENERALISATION: Stringified markers: " + stringifiedMarkers);
		}
		
		// Calculate powerset
		Set<Set<Pair<String,String>>> powerset = getPowerset(stringifiedMarkers);
		
		// Filter powerset by desired set sizes
		Set<Set<Pair<String,String>>> filteredSets = filterBySetSize(powerset, setSize);
		
		if (debug) {
			System.out.println("HIGHER-ORDER GENERALISATION: Filtered sets: " + filteredSets);
		}
		
		// Now check individual expressions against existing generalised ones
		for (Entry<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>, Float> entry: exprs.entrySet()) {
			// Generalise individual expression for comparison
			final NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>> generalNewInputExpr = generalizeExpression(entry.getKey(), true);
			
			if (debug) {
				System.out.println("Social markers of generalised instance expression: " + generalNewInputExpr.attributes.socialMarkers);
			}
			
			// Iterate through higher-order sets (generalisation) and check whether they are contained in expression instance
			for (Set<Pair<String,String>> filteredEntry: filteredSets) {
				boolean contains = true;
				for (Pair<String,String> element: filteredEntry) { 
					
					if (!generalNewInputExpr.containsSocialMarkerRecursively(element)) {
						contains = false;
						//System.err.println("Does not contain " + element + ": " + generalNewInputExpr);
					}
					
				}
				//if all elements are contained, ...
				if (contains) {
					//System.err.println("Contains " + filteredEntry + ": " + generalNewInputExpr);
					
					// Store as abstract statement (deep copy) and associate instance with it
					NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>> instanceCopy = 
							new NAdicoExpression<>(generalNewInputExpr);
					
					// Determine markers to replace existing ones
					LinkedHashMap<String, LinkedHashSet<String>> updatedSocialMarkers = convertPairsToMarkerStructure(filteredEntry);
					
					//System.err.println("Updated markers: " + updatedSocialMarkers);
					
					if (instanceCopy == null) {
						throw new RuntimeException("Instance copy in nADICO generaliser (higher level generalisation) is null.");
					}
					
					// Recursively replace social markers
					instanceCopy.replaceSocialMarkersRecursively(new HashMap<String, Set<String>>(updatedSocialMarkers));
					
					//System.out.println("After marker replacement: " + instanceCopy);
															
					// Aggregate instance expressions and maintain reference to instance expressions
					generalizedExprs = aggregateExpressionInstances(generalizedExprs, instanceCopy, entry.getKey(), entry.getValue());
				}	
			}	
		} // End of iteration over individual expressions
			
		// statements' deontic values are summed by default (AGGREGATION_STRATEGY_SUM), but may be adjusted depending on activated strategy
		generalizedExprs = assignStrategySpecificDeonticToGeneralizedExpressions(generalizedExprs);
				
		// Cache them
		this.cachedGeneralizedExprsHigherLevel.put(generalisationLevel, generalizedExprs);
		// Call deontic range
        try {
            notifyMemoryChangeListeners();
        } catch (MemoryUpdateException e) {
            throw new RuntimeException(e);
        }
        return generalizedExprs;
	}
	
	/**
	 * Aggregates generalised instance expressions (generalisation occurs outside of method) and aggregates deontic values (by addition), while
	 * maintaining reference to the original (non-generalised) instance values.
	 * @param generalizedExpressions Structure holding aggregated generalised expressions (<general,ArrayList<Instance>>)
	 * @param generalizedInstanceExpression Generalised instance expression - generalisation of instanceExpression performed before calling this method
	 * @param instanceExpression Actual instance expression to be linked to generalised expression
	 * @param value deontic value of instance expression (feedback)
	 * @return
	 */
	private LinkedHashMap<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>, ArrayList<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>>> aggregateExpressionInstances(
			LinkedHashMap<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>, ArrayList<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>>> generalizedExpressions, 
			NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>> generalizedInstanceExpression, 
			NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>> instanceExpression, 
			Float value) {
		if (instanceExpression.attributes.individualMarkers.isEmpty()) {
			throw new RuntimeException("Individual markers of attributes of action observation are empty: " + instanceExpression);
		}
		//System.out.println("Original expression: " + instanceExpression);
		//System.out.println("Generalized expression: " + generalizedInstanceExpression);
		if (!generalizedExpressions.isEmpty()) {
			boolean added = false;
			//check for existing general expressions and eventually add matching instance statement
			for (Entry<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>,
					ArrayList<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>>> generalEntry: generalizedExpressions.entrySet()) {
				if (debug) {
					System.out.println("Checking existing" + System.getProperty("line.separator") + 
							generalEntry.getKey() + System.getProperty("line.separator") + "against new" +
							System.getProperty("line.separator") + generalizedInstanceExpression);
				}
				if (generalEntry.getKey().equals(generalizedInstanceExpression)) {
					//save action value in action instance deontic
					instanceExpression.deontic = value;
					//add instance statement to derived general statement
					generalEntry.getValue().add(instanceExpression);
					//add instance value to generalized value
					if (generalEntry.getKey().deontic == null) {
						generalEntry.getKey().deontic = value;
					} else {
						generalEntry.getKey().deontic += value;
					}
					added = true;
					if (debug) {
						System.out.println("Statements match.");
					}
					break;
				} else {
					if (debug) {
						System.out.println("Statements do not equal on AIC components: Existing:" + System.getProperty("line.separator") +
								generalEntry.getKey() + ", new:" + System.getProperty("line.separator") + generalizedInstanceExpression);
					}
				}
			}
			if (!added) {
				//add new general expression with instance statement
				ArrayList<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>> newList = new ArrayList<>();
				//save action value in action instance deontic
				instanceExpression.deontic = value;
				newList.add(instanceExpression);
				//add instance value to generalized value
				if (generalizedInstanceExpression.deontic == null) {
					generalizedInstanceExpression.deontic = value;
				} else {
					generalizedInstanceExpression.deontic += value;
				}
				generalizedExpressions.put(generalizedInstanceExpression, newList);
			}
		} else {
			//add first element to general expressions
			ArrayList<NAdicoExpression<Attributes<LinkedHashSet<String>>, Aim<String>, Conditions<NAdicoExpression>>> newList = new ArrayList<>();
			//save action value in action instance deontic
			instanceExpression.deontic = value;
			newList.add(instanceExpression);
			//add instance value to generalized value
			if (generalizedInstanceExpression.deontic == null) {
				generalizedInstanceExpression.deontic = value;
			} else {
				generalizedInstanceExpression.deontic += value;
			}
			generalizedExpressions.put(generalizedInstanceExpression, newList);
		}
		return generalizedExpressions;
	}
	
	
	/**
	 * Returns powerset of given list of elements
	 * @param set
	 * @return
	 * Source: http://rosettacode.org/wiki/Power_Set#Iterative
	 */
	public static <T> Set<Set<T>> getPowerset(Collection<T> set) {
		Set<Set<T>> ps = new LinkedHashSet<Set<T>>();
		ps.add(new LinkedHashSet<T>());   // add the empty set
		
		// for every item in the original list
		for (T item : set) {
			Set<Set<T>> newPs = new LinkedHashSet<Set<T>>();
		
			for (Set<T> subset : ps) {
				// copy all of the current powerset's subsets
				newPs.add(subset);
		
				// plus the subsets appended with the current item
				Set<T> newSubset = new LinkedHashSet<T>(subset);
				newSubset.add(item);
				newPs.add(newSubset);
			}
			ps = newPs;
		}
		return ps;
	}
	
	/**
	 * Filters a set of sets by number of elements of contained sets.
	 * @param set Input set of sets
	 * @param size Number of elements in subsets
	 * @return
	 */
	public static <T> Set<Set<T>> filterBySetSize(Set<Set<T>> set, int size) {
		Set<Set<T>> filteredSet = new LinkedHashSet<Set<T>>();
		for (Set<T> subset: set) {
			if (subset.size() == size) {
				filteredSet.add(subset);
			}
		}
		return filteredSet;
	}
	
	/**
	 * Converts the normalised Pair<K,V> structure to the attributes structure used by nADICO.
	 * @param pairs
	 * @return
	 */
	private LinkedHashMap<String,LinkedHashSet<String>> convertPairsToMarkerStructure(final Set<Pair<String,String>> pairs) {
		LinkedHashMap<String,LinkedHashSet<String>> markerStructure = new LinkedHashMap<String, LinkedHashSet<String>>();
		for (Pair<String,String> pair: pairs) {
			if (markerStructure.containsKey(pair.left)) {
				markerStructure.get(pair.left).add(pair.right);
			} else {
				LinkedHashSet<String> values = new LinkedHashSet<String>();
				values.add(pair.right);
				markerStructure.put(pair.left, values);
			}
		}
		return markerStructure;
	}
	
}
