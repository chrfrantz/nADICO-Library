package org.nzdis.nadico;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.Serializable;

import org.nzdis.nadico.components.Aim;
import org.nzdis.nadico.components.Attributes;
import org.nzdis.nadico.components.Conditions;
import org.nzdis.nadico.deonticRange.DeonticRange;
import org.sofosim.structures.Pair;
import org.sofosim.util.ObjectCloner;

public class NAdicoExpression<A extends Attributes, I extends Aim, C extends Conditions> implements Serializable {

	public static final String WILDCARD = "*";
	
	/**
	 * Expression nesting level.
	 * Do not manipulate manually but call {@link #setParent(NAdicoExpression)} instead.
	 */
	protected Integer level = 0;
	
	/**
	 * AND combinator
	 */
	public static final String AND = "AND";
	/**
	 * OR combinator (inclusive or)
	 */
	public static final String OR = "OR";
	/**
	 * XOR combinator (exclusive or)
	 */
	public static final String XOR = "XOR";
	/**
	 * Default combinator if non is specified
	 */
	public static String DEFAULT_COMBINATOR = AND;
	
	/**
	 * Reference to factory producing this statement.
	 * Don't access this directly - even from inside NAdicoExpression!
	 * Use {@link #getDeonticRange()} instead.
	 */
	private transient DeonticRange deonticRange = null;
	
	/**
	 * Reference to parent statement. Don't manipulate directly, 
	 * only via {@link #setParent(NAdicoExpression)}.
	 */
	private NAdicoExpression<A, I, C> parent = null;
	/**
	 * Attributes component
	 */
	public A attributes = null;
	/**
	 * Deontic component
	 */
	public Float deontic = null;
	/**
	 * Aim component
	 */
	public I aim = null;
	/**
	 * Conditions component
	 */
	public C conditions = null;
	/**
	 * Or else component
	 */
	protected NAdicoExpression<A, I, C> orElse = null;
	
	/**
	 * Horizontally nested expressions
	 */
	public LinkedHashSet<NAdicoExpression<A, I, C>> nestedExpressions = new LinkedHashSet<>();
	
	/**
	 * Combinator
	 */
	public String combinator = null;
	
	protected Integer count = null;
	protected Float probability = null;
	
	private final boolean debug = false;
	
	/**
	 * Indicates whether deontic value is inverted
	 */
	protected boolean deonticInverted = false;
	
	/**
	 * Type indicator for nAdicoAction.
	 */
	private static final String NADICO_ACTION = "ACTION";
	/**
	 * Type indicator for nAdicoStatement.
	 */
	private static final String NADICO_STATEMENT = "STATEMENT";
	/**
	 * Type indicator for nAdicoCombination.
	 */
	private static final String NADICO_COMBINATION = "COMBINATION";
	
	/**
	 * Type of expression
	 */
	private String type = null;
	
	/**
	 * Constructor used for factory
	 */
	protected NAdicoExpression() {
		
	}
	
	/**
	 * Constructor used to pass a DeonticRange reference along.
	 * @param deonticRange
	 */
	protected NAdicoExpression(final DeonticRange deonticRange) {
		this.deonticRange = deonticRange;
	}
	
	/**
	 * Copy constructor for NAdicoExpression
	 * @param expressionToBeCopied
	 */
	public NAdicoExpression(final NAdicoExpression<A, I, C> expressionToBeCopied) {
		this(expressionToBeCopied.deonticRange);
		
		if (debug) {
		    System.out.println("Expression to be copied (Type: " + expressionToBeCopied.type + "): " + expressionToBeCopied);
		}
		this.type = expressionToBeCopied.type;
		this.level = expressionToBeCopied.level;
		this.parent = expressionToBeCopied.parent;
		this.attributes = (A) new Attributes(expressionToBeCopied.attributes);
		this.deontic = expressionToBeCopied.deontic;
		this.aim = (I) new Aim(expressionToBeCopied.aim);
		this.conditions = (C) new Conditions(expressionToBeCopied.conditions);
		this.orElse = expressionToBeCopied.orElse;
		this.nestedExpressions = expressionToBeCopied.nestedExpressions;
		this.combinator = expressionToBeCopied.combinator;
		this.count = expressionToBeCopied.count;
		this.probability = expressionToBeCopied.probability;
		this.deonticInverted = expressionToBeCopied.deonticInverted;
	}
	
	/**
	 * Sets a specified Or Else. Note: Existing values will 
	 * be overwritten/deleted!
	 * Does not work on actions.
	 * @param expression
	 */
	public void setOrElse(final NAdicoExpression<A, I, C> expression){
		if(expression == null){
			return;
		}
		if (this.isAction()) {
			throw new RuntimeException("Cannot add 'or else' expression to action.");
		}
		if (expression.isAction()) {
			throw new RuntimeException("Cannot add non-statement/combination 'or else' to expression.");
		}
		
		//set this expression as parent
		expression.setParent(this);
		//assign expression to orElse
		this.orElse = expression;
	}
	
	/**
	 * Returns orElse statement/combination.
	 * @return
	 */
	public NAdicoExpression<A, I, C> orElse(){
		return this.orElse;
	}
	
	/**
	 * Returns the sum of consequential deontics.
	 * @return
	 */
	public Float getSumOfConsequentialDeontics(){
		float sum = 0f;
		NAdicoExpression<A, I, C> consequentialStatements = orElse();
		if(consequentialStatements != null){
			if(consequentialStatements.isCombination()){
				for(NAdicoExpression<A, I, C> expr: consequentialStatements.nestedExpressions){
					if(expr.isCombination()){
						//possibly multiple statements
						sum += expr.getSumOfConsequentialDeontics();
					} else{
						//possibly one statement
						sum += expr.deontic;
					}
				}
			}
		}
		return sum;
	}
	
	/**
	 * Sets given parent as a parent for this expression, 
	 * including adjustment of nesting level and deontic 
	 * range reference.
	 * @param parent Parent expression
	 */
	public void setParent(final NAdicoExpression<A,I,C> parent){
		if (this.parent != parent) {
			this.parent = parent;
			if(this.parent != null){
				this.level = parent.level + 1;
			} else if (this.parent == null){
				//if passed parent is null, reset level to top
				this.level = 0;
			}
			if (this.isCombination()) {
				//iterate through elements an update parent to my parent (not myself!)
				for (NAdicoExpression<A,I,C> expression: nestedExpressions) {
					expression.setParent(this.parent);
				}
			}
			//adjust orElse levels
			if (this.orElse != null) {
				this.orElse.setParent(this);
			}
		}
	}
	
	/**
	 * Retrieves an eventual parent of an expression. 
	 * @return
	 */
	public NAdicoExpression<A, I, C> parent(){
		return this.parent;
	}
	
	/**
	 * Deletes the reference to a parent statement/combination and 
	 * resets nesting level.
	 */
	public void deleteParent(){
		setParent(null);
	}
	
	/**
	 * Returns the deontic range used to contextualize this expression
	 * (e.g. transformation of deontic values to terms).
	 * @return
	 */
	protected DeonticRange getDeonticRange(){
		if(parent != null){
			//if this expression has a parent, this should know the deontic range
			return parent.getDeonticRange();
		}
		return deonticRange;
	}
	
	/**
	 * Converts this expression to action. 
	 * Note that this will not work for combinations 
	 * and statements.
	 * Note: Actions cannot contain orElse components, but only reflect individual actions.
	 * @return itself as action
	 */
	protected NAdicoExpression<A,I,C> makeAction() {
		if (this.type != null) {
			if (this.isCombination() || this.isStatement()) {
				throw new RuntimeException("Combinations and statements cannot be converted into actions.");
			}
		}
		this.type = NADICO_ACTION;
		return this;
	}
	
	/**
	 * Converts this expression to statement. 
	 * Note if this expression is a combination with multiple 
	 * nested expressions, the expressions are converted to statements 
	 * but this instance remains a combination.
	 * If the combination contains one or none nested statement it is 
	 * converted to a statement.
	 * @return itself as statement
	 */
	protected NAdicoExpression<A,I,C> makeStatement() {
		if(type != null){
			if (this.isCombination() && this.nestedExpressions.size() > 1) {
				//convert individual entries to statements but leave in combination
				for (NAdicoExpression expr : this.nestedExpressions) {
					expr.makeStatement();
				}
				//no further processing - return a combination with now nested statements
				return this;
				//throw new RuntimeException("Combination with multiple entries cannot be converted into statement.");
			} else if (this.isCombination() && this.nestedExpressions.size() == 1) {
				//can convert from one nested statement
				NAdicoExpression<A,I,C> src = this.nestedExpressions.iterator().next();
				this.attributes = src.attributes;
				this.deontic = src.deontic;
				this.aim = src.aim;
				this.conditions = src.conditions;
				this.orElse = src.orElse;
				//clear nested expressions
				this.nestedExpressions.clear();
			} else if (this.isCombination() && this.nestedExpressions.isEmpty()) {
				//should also be alright...
			}
		}
		this.type = NADICO_STATEMENT;
		//check for consistency if orElse exists
		if (this.orElse != null) {
			this.orElse.setParent(this);
		}
		return this;
	}
	
	/**
	 * Converts this expression to combination.
	 * Throws an exception if it is already a combination.
	 * @return itself as combination
	 */
	protected NAdicoExpression<A,I,C> makeCombination(final String combinator){
		if (this.type != null && this.isCombination()) {
			throw new RuntimeException("Combination cannot be made into combination.");
		}
		this.type = NADICO_COMBINATION;
		this.combinator = combinator == null ? DEFAULT_COMBINATOR : combinator;
		
		if (attributes != null || aim != null || conditions != null) {
			//generate new statement
			NAdicoExpression<A,I,C> newStatement = new NAdicoFactory<A,I,C>(false).createNAdicoStatement(attributes, deontic, aim, conditions, orElse);
			//move to nested expressions
			newStatement.setParent(parent);
			nestedExpressions.add(newStatement);
		}
		//clear up behind
		this.attributes = null;
		this.deontic = null;
		this.aim = null;
		this.conditions = null;
		this.orElse = null;
		return this;
	}
	
	/**
	 * Indicates if this instance is a NAdicoAction.
	 * @return
	 */
	public boolean isAction(){
		return this.type.equals(NADICO_ACTION);
	}
	
	/**
	 * Indicates if this instance is a nAdicoStatement.
	 * @return
	 */
	public boolean isStatement(){
		return this.type.equals(NADICO_STATEMENT);
	}
	
	/**
	 * Indicates if this instance is a NAdicoCombination.
	 * @return
	 */
	public boolean isCombination(){
		return this.type.equals(NADICO_COMBINATION);
	}
	
	/**
	 * Adds an expression on the same level and uses the existing combinator value.
	 * @param expression Expression to be added.
	 * @return
	 */
	public NAdicoExpression<A,I,C> addExpression(final NAdicoExpression<A,I,C> expression){
		return addExpression(expression, null);
	}
	
	/**
	 * Add expressions (statements or combinations) to this expression on the same nesting level. 
	 * @param expression Expression to be added.
	 * @param combinator Combinator expressing logical relationship to other expressions
	 * @return
	 */
	public NAdicoExpression<A,I,C> addExpression(final NAdicoExpression<A,I,C> expression, final String combinator){
		if (debug) {
			System.out.println("Called addExpression() with expression: " + expression + " and combinator " + combinator);
		}
		if (combinator != null) {
			if (!validateNAdicoCombination(combinator)) {
				throw new RuntimeException("Invalid combinator value provided: " + combinator);
			}
		}
		if (expression == null) {
			throw new RuntimeException("Passed null expression to expression " + this);
		}
		if (this.combinator == null && combinator == null) {
			throw new RuntimeException("Cannot pass null combinator for first element in NAdicoCombination.");
		}
		
		//check for existence of or else first
		if (this.orElse != null || expression.orElse != null) {
			//if either of us has an or else, it must be separate entities
			
			if (this.isStatement() || this.isAction()) {
				//make myself combination with other's combinator
				this.makeCombination(combinator);
			}
			//share the parent
			expression.setParent(parent);
			//and add it to me as separate combination
			nestedExpressions.add(expression);
			//return myself
			return this;
		}
		
		//check whether this is combination 
		if (this.isCombination()) {
			if (expression.isCombination()) {
				if (this.combinator.equals(expression.combinator)) {
					//if the combinator of both is the same, just add all nested expressions to same level
					
					//set my parent as parent as on same level
					expression.setParent(parent);
					//add all nested expressions
					nestedExpressions.addAll(expression.nestedExpressions);
					//return myself
					return this;
				} else if (!this.combinator.equals(expression.combinator)) {
					//else add to lower level (and leave combination intact)
					
					//set my parent as parent
					expression.setParent(parent);
					//add combination
					nestedExpressions.add(expression);
					//return myself
					return this;
				}
			} else if (expression.isStatement() || expression.isAction()) {
				if (combinator != null && !this.combinator.equals(combinator)) {
					//if combinator differs, make up new combination and add this is a child
					
					//make incoming expression combination
					expression.makeCombination(combinator);
					//make my parent its parent
					expression.setParent(parent);
					//and add myself
					return expression.addExpression(this);
				} else if (combinator == null || this.combinator.equals(combinator)) {
					//make my parent its parent
					expression.setParent(parent);
					//add to this combination
					nestedExpressions.add(expression);
					//return myself
					return this;
				} else {
					throw new RuntimeException("Unknown combinator situation. Unmanaged case in addExpression(): \nOriginal: " + this + "\nAdded: " + expression);
				}
			} else {
				throw new RuntimeException("Type of incoming expression is not handled for addExpression(): \nOriginal: " + this + "\nAdded: " + expression);
			}
		} else if (this.isStatement() || this.isAction()) {
			//if this is a statement
			
			if (expression.isCombination()) {
				//check if incoming expression is combination
	
				if (expression.combinator.equals(combinator)) {
					//if the combinator of the combinator is in line with requested combinator, just add combinator elements
					
					this.makeCombination(combinator);
					//make my parent its parent
					expression.setParent(parent);
					//add individual expressions
					nestedExpressions.addAll(expression.nestedExpressions);
					//return myself
					return this;
				} else if (!expression.combinator.equals(combinator)){
					//if the requested combinator is different, make incoming expression a child of me (once converted to combination)
					
					this.makeCombination(combinator);
					//make my parent its parent (same level)
					expression.setParent(parent);
					//add the combination in its entirety
					nestedExpressions.add(expression);
					//return myself
					return this;
				} else {
					throw new RuntimeException("Unknown case for adding combination to statement: \nOriginal: " + this + "\nAdded: " + expression);
				}
			} else if (expression.isStatement() || expression.isAction()) {
				//check if the incoming expression is statement
				
				//make myself a combination and add the statement
				this.makeCombination(combinator);
				//share my parent with incoming expression
				expression.setParent(parent);
				//add it to myself
				nestedExpressions.add(expression);
				//return myself
				return this;
			} else {
				throw new RuntimeException("Incoming expression is neither statement nor combination. Addition of expression is not supported. \nOriginal: " + this + "\nAdded: " + expression);
			}
		} else {
			throw new RuntimeException("This expression is neither statement nor combination. Addition of expression is not accepted. This expression: " + this);
		}
		return null;
	}
	
	/**
	 * Adds multiple expressions to statement.
	 * @param expressions
	 * @param combinator
	 */
	public void addExpressions(final LinkedHashSet<NAdicoExpression<A, I, C>> expressions, final String combinator){
		for(NAdicoExpression<A, I, C> expression: expressions){
			addExpression(expression, combinator);
		}
	}
	
	/**
	 * Recursively checks whether this expression's attributes contains given social markers.
	 * Only returns true if all nested statements contain the given social marker.
	 * @param marker Marker to look for
	 * @return true if marker is present, false if not
	 */
	public boolean containsSocialMarkerRecursively(Pair<String,String> marker) {
		if (isAction() || isStatement()) {
			// Check own markers
			if(!attributes.socialMarkers.containsKey(marker.left) || 
					!((Set<String>)attributes.socialMarkers.get(marker.left)).contains(marker.right)) {
				return false;
			}
			// Check preceding action's markers
			if(conditions != null && conditions.getPreviousAction() != null) {
				return ((NAdicoExpression<A, I, C>)conditions.getPreviousAction()).containsSocialMarkerRecursively(marker);
			}
		} else if (isCombination()) {
			// Check combination's nested statements
			for (NAdicoExpression<A, I, C> nestedExpression: nestedExpressions) {
				if(!nestedExpression.containsSocialMarkerRecursively(marker)) {
					return false;
				}
			}
		} else {
			throw new RuntimeException("Cannot check attributes of unknown nAdicoExpression (neither statement, action, nor combination)");
		}
		return true;
	}
	
	/**
	 * Recursively checks whether a given activity is contained in this or 
	 * previous action expressions, or any nested statement.
	 * Note: It returns true if it is contained in ANY statement.
	 * @param activity
	 * @return
	 */
	public boolean containsActivityRecursively(String activity) {
	    if (isAction() || isStatement()) {
	        // Check this instance's aim
	        if (aim != null && aim.activity.equals(activity)) {
	            return true;
	        }
	        if (conditions != null && conditions.getPreviousAction() != null) {
	            return ((NAdicoExpression<A, I, C>)conditions.getPreviousAction()).containsActivityRecursively(activity);
	        }
	    } else if (isCombination()) {
            // Check combination's nested statements
            for (NAdicoExpression<A, I, C> nestedExpression: nestedExpressions) {
                if(nestedExpression.containsActivityRecursively(activity)) {
                    return true;
                }
            }
        } else {
            throw new RuntimeException("Cannot check aim of unknown nAdicoExpression (neither statement, action, nor combination)");
        }
        return false;
	}

	/**
	 * Checks whether any of the activities in the passed set is contained within the invoked
	 * NestedExpression (on any level). Returns true if at least one of the given
	 * activities is found at some nesting level. Otherwise, it returns false.
	 * @param activities
	 * @return
	 */
	public boolean containsAnyActivityRecursively(Set<String> activities) {
		for (String act : activities) {
			if (containsActivityRecursively(act)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Recursively counts whether a given activity is contained in this or 
	 * previous action expressions, or any nested statement.
	 * @param activity
	 * @return number of occurrences
	 */
	public int countActivityOccurrenceRecursively(String activity) {
		return countActivityOccurrenceRecursively(activity, 0);
	}
	
	/**
	 * Recursively counts whether a given activity is contained in this or 
	 * previous action expressions, or any nested statement.
	 * @param activity
	 * @param ct 
	 * @return number of occurrences
	 */
	private int countActivityOccurrenceRecursively(String activity, int ct) {
	    if (isAction() || isStatement()) {
	        // Check this instance's aim
	        if (aim != null && aim.activity.equals(activity)) {
	            ct++;
	        }
	        if (conditions != null && conditions.getPreviousAction() != null) {
	            return ((NAdicoExpression<A, I, C>)conditions.getPreviousAction()).countActivityOccurrenceRecursively(activity, ct);
	        }
	    } else if (isCombination()) {
            // Check combination's nested statements
            for (NAdicoExpression<A, I, C> nestedExpression: nestedExpressions) {
                return nestedExpression.countActivityOccurrenceRecursively(activity, ct);
            }
        } else {
            throw new RuntimeException("Cannot check aim of unknown nAdicoExpression (neither statement, action, nor combination)");
        }
        return ct;
	}
	
	/**
	 * Recursively replaces social markers in attributes component.
	 * @param socialMarkers New social markers to replace existing ones.
	 */
	public void replaceSocialMarkersRecursively(final Map<String, Set<String>> socialMarkers) {
		if (isAction() || isStatement()) {
			// Replace own markers
			attributes.replaceSocialMarkers(socialMarkers);
			// Replace preceding action's markers
			if(conditions != null && conditions.getPreviousAction() != null) {
				((NAdicoExpression<A, I, C>)conditions.getPreviousAction()).replaceSocialMarkersRecursively(socialMarkers);
			}
		} else if (isCombination()) {
			// Replace nested statements' markers
			for (NAdicoExpression<A, I, C> nestedExpression: nestedExpressions) {
				nestedExpression.replaceSocialMarkersRecursively(socialMarkers);
			}
		} else {
			throw new RuntimeException("Cannot modify attributes of unknown nAdicoExpression (neither statement, action, nor combination)");
		}
	}
	
	/**
	 * Identifies the number of PREVIOUS_ACTION (see Conditions.java for constant) sequences (i.e. the number
	 * of preceding action sequences before this one).
	 * 0 means that no previous action statements (i.e. before the current) exist; 
	 * higher numbers indicate the number of preceding statements
	 * @return
	 */
	public int getNumberOfPrecedingExpressions() {
		int ct = 0;
		NAdicoExpression<A, I, C> previous = ((NAdicoExpression<A, I, C>)conditions.getPreviousAction());
		while (previous != null) {
			previous = (NAdicoExpression<A, I, C>) previous.conditions.getPreviousAction();
			ct++;
		}
		return ct;
	}
	
	/**
	 * Returns the total length of the expression sequence (i.e. current + preceding expressions).
	 * @return
	 */
	public int getTotalExpressionSequenceLength() {
		return getNumberOfPrecedingExpressions() + 1;
	}
	
	/**
	 * Walks backwards along the expression sequence for a given number of levels (levels).
	 * 0 would imply current expression (with all previous ones); 1 would return one previous expression (and all preceding ones), etc.
	 * @param levels
	 * @return
	 */
	public NAdicoExpression<A,I,C> backtrackThroughPrecedingExpressionsForGivenLevels(int levels) {
		if (levels < 0) {
			throw new RuntimeException("NAdicoGeneralizer: Illegal backtracking specification: " + levels);
		}
		NAdicoExpression<A,I,C> desired = this;
		for (int i = 1; i <= levels; i++) {
			desired = (NAdicoExpression<A, I, C>) desired.conditions.getPreviousAction();
		}
		return desired;
	}
	
	/**
	 * Returns a given number of initial expressions (i.e. the sequence beginning).
	 * For example, 1 would return only the first expression, 2 the first two, etc.
	 * @param number
	 * @return
	 */
	public NAdicoExpression<A,I,C> getInitialExpressions(int number) {
		if (number < 1) {
			throw new RuntimeException("NAdicoGeneralizer: Cannot return less than 1 initial expression.");
		}
		// Identifies number of expressions that need to be ignored
		int excluded = getTotalExpressionSequenceLength() - number;
		if (excluded < 0) {
			throw new RuntimeException("NAdicoGeneralizer: Length of expression sequence is too small (" + 
					getTotalExpressionSequenceLength() + ") to extract " + number + " entries.");
		}
		return backtrackThroughPrecedingExpressionsForGivenLevels(excluded);
	}
	
	/**
	 * Validates a given combinator and throws exception if invalid.
	 * @param combinator Combinator
	 * @param exceptionMessage Message shown in exception
	 * @return boolean indicating successful validation
	 */
	protected static boolean validateNAdicoCombinationWithException(final String combinator, final String exceptionMessage){
		if (!validateNAdicoCombination(combinator)) {
			throw new RuntimeException(exceptionMessage);
		}
		return true;
	}

	/**
	 * Validates a given combinator.
	 * @param combinator Combinator
	 * @return true if combinator is valid, false if invalid
	 */
	protected static boolean validateNAdicoCombination(final String combinator){
		if (!combinator.equals(AND)
				&& !combinator.equals(OR)
				&& !combinator.equals(XOR)) {
			return false;
		}
		return true;
	}
	
	/**
	 * Makes a deep copy of this expression but
	 * maintains eventual reference to deontic range instance.
	 * @return deep copy of this expression
	 */
	public NAdicoExpression<A,I,C> makeCopy() {
		//return makeCopy(null);
		return new NAdicoExpression<A,I,C>(this);
	}
	
	/**
	 * Makes a deep copy of this expression but
	 * allows passing a custom deontic range to be assigned
	 * to the newly created expression.
	 * @param deonticRange
	 * @return deep copy of this expression with passed deontic range assigned
	 */
	public NAdicoExpression<A,I,C> makeCopy(final DeonticRange deonticRange) {
		NAdicoExpression<A,I,C> newExpression = new NAdicoExpression<A,I,C>(this);
		newExpression.deonticRange = deonticRange;
		return newExpression;
	}
	
	/**
	 * Makes a deep copy of this expression but
	 * allows passing a custom deontic range to be assigned
	 * to the newly created expression.
	 * Note: Very comprehensive, but very slow!
	 * @param deonticRange
	 * @return deep copy of this expression with passed deontic range assigned
	 */
	public NAdicoExpression<A,I,C> makeClone(final DeonticRange deonticRange) {
		try {
			NAdicoExpression<A,I,C> copiedExpression = 
					(NAdicoExpression<A,I,C>)ObjectCloner.deepCopy(this);
			if(deonticRange != null){
				//if deontic range has been passed as parameter, assign that
				copiedExpression.deonticRange = deonticRange;
			} else {
				//else take over the one from source expression
				copiedExpression.deonticRange = this.deonticRange;
			}
			return copiedExpression;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public static final String EQUALS_BASED_ON_nADICO = "nADICO";
	public static final String EQUALS_BASED_ON_ADIC = "ADIC";
	public static final String EQUALS_BASED_ON_AIC = "AIC";
	
	/**
	 * Indicates which nADICO components are considered for equals() check.
	 */
	private String equalsBasedOn = EQUALS_BASED_ON_AIC;
	
	/**
	 * Configures the equals method for the use of selected fields 
	 * based on chosen granularity as specified in the EQUALS_ constants.
	 * @param equalsConstant Granularity level of comparison
	 */
	public void compareBasedOn(final String equalsConstant){
		equalsBasedOn = equalsConstant;
		System.out.println("NAdicoExpression: Activated comparison based on level " + equalsBasedOn);
	}

	@Override
	public int hashCode() {
		//System.out.println("Entered NAdicoExpression hashcode.");
		final int prime = 31;
		int result = 1;
		result = prime * result + ((aim == null) ? 0 : aim.hashCode());
		result = prime * result
				+ ((attributes == null) ? 0 : attributes.hashCode());
		result = prime * result
				+ ((combinator == null) ? 0 : combinator.hashCode());
		result = prime * result
				+ ((conditions == null) ? 0 : conditions.hashCode());
		result = prime * result + ((count == null) ? 0 : count.hashCode());
		if(equalsBasedOn.equals(EQUALS_BASED_ON_ADIC) || equalsBasedOn.equals(EQUALS_BASED_ON_nADICO)){
			result = prime * result + ((deontic == null) ? 0 : deontic.hashCode());
		}
		if(equalsBasedOn.equals(EQUALS_BASED_ON_nADICO)){
			result = prime * result + ((level == null) ? 0 : level.hashCode());
			result = prime
					* result
					+ ((nestedExpressions == null) ? 0 : nestedExpressions.hashCode());
			result = prime * result + ((orElse == null) ? 0 : orElse.hashCode());
			result = prime * result + ((parent == null) ? 0 : parent.hashCode());
		}
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		//System.out.println("Entered NAdicoExpression equals.");
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NAdicoExpression other = (NAdicoExpression) obj;
		if (aim == null) {
			if (other.aim != null)
				return false;
		} else if (!aim.equals(other.aim))
			return false;
		if (attributes == null) {
			if (other.attributes != null)
				return false;
		} else if (!attributes.equals(other.attributes))
			return false;
		if (combinator == null) {
			if (other.combinator != null)
				return false;
		} else if (!combinator.equals(other.combinator))
			return false;
		if (conditions == null) {
			if (other.conditions != null)
				return false;
		} else if (!conditions.equals(other.conditions))
			return false;
		if (count == null) {
			if (other.count != null)
				return false;
		} else if (!count.equals(other.count))
			return false;
		if(equalsBasedOn.equals(EQUALS_BASED_ON_ADIC) || equalsBasedOn.equals(EQUALS_BASED_ON_nADICO)){
			if (deontic == null) {
				if (other.deontic != null)
					return false;
			} else if (!deontic.equals(other.deontic))
				return false;
		}
		if(equalsBasedOn.equals(EQUALS_BASED_ON_nADICO)){
			if (level == null) {
				if (other.level != null)
					return false;
			} else if (!level.equals(other.level))
				return false;
			if (nestedExpressions == null) {
				if (other.nestedExpressions != null)
					return false;
			} else if (!nestedExpressions.equals(other.nestedExpressions))
				return false;
			if (orElse == null) {
				if (other.orElse != null)
					return false;
			} else if (!orElse.equals(other.orElse))
				return false;
			if (parent == null) {
				if (other.parent != null)
					return false;
			} else if (!parent.equals(other.parent))
				return false;
		}
		//System.out.println("Passed NAdicoAction equals.");
		return true;
	}
	
	/**
	 * Performs equals functionality based on AIC components and ignores the rest.
	 * @param obj
	 * @return
	 */
	public boolean equalsOnAIC(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NAdicoExpression other = (NAdicoExpression) obj;
		if (aim == null) {
			if (other.aim != null)
				return false;
		} else if (!aim.equals(other.aim))
			return false;
		if (attributes == null) {
			if (other.attributes != null)
				return false;
		} else if (!attributes.equals(other.attributes))
			return false;
		if (combinator == null) {
			if (other.combinator != null)
				return false;
		} else if (!combinator.equals(other.combinator))
			return false;
		if (conditions == null) {
			if (other.conditions != null)
				return false;
		} else if (!conditions.equals(other.conditions))
			return false;
		return true;
	}
	
	public String toString(){
		if (this.isStatement()) {
			return "L" + level 
					+ (probability == null ? "" : " (p: " + probability + ")")
					+ (count == null ? "" : " (Count: " + count + ")")
					+ ": A=" + attributes 
					+ (deontic != null ? ", D=" + deontic : "") + (deonticInverted ? " (inv)" : "")
					+ ((deontic != null && getDeonticRange() != null) 
							? (" (" + getDeonticRange().getDeonticValueMapper().getDeonticForValue(deontic) + ")") 
							: "")
					+ ", I=" + aim
					+ ", C=" + conditions + ", " 
					+ (orElse() == null ? "" : System.getProperty("line.separator") + "   ")
					+ "O=(" + orElse() + ")";
		} else if (this.isCombination()) {
			StringBuilder out = new StringBuilder();
			out.append(System.getProperty("line.separator"));
			if(nestedExpressions != null && !nestedExpressions.isEmpty()){
				int ct = 0;
				out.append("(");//.append(System.getProperty("line.separator"));
				for(NAdicoExpression<A, I, C> expression : nestedExpressions){
					// indent based on nesting level, but only from second expression onwards
					if(ct > 0) {
						for(int i = 0; i < expression.level; i++){
							out.append("  ");
						}
						out.append(" ");
					}
					// print actual expression
					out.append("(").append(expression).append(")");
					// print combinators
					if(ct < nestedExpressions.size() - 1){
						out.append(" ").append(this.combinator)
							.append(System.getProperty("line.separator"));
					}
					ct++;
				}
				out.append(")").append(System.getProperty("line.separator"));
				// handle potential orElse
				if(orElse() != null) {
					for(int i = 0; i < orElse().level; i++){
						out.append("  ");
					}
					out.append("O=(" + orElse() + ")");
				}
			} else {
				out.append("Empty NAdicoCombinator with logical combinator ").append(combinator);
			}
			return out.toString();
		} else if (this.isAction()) {
			return "NAdicoAction [A=" + attributes 
					+ (deontic != null ? ", D=" + deontic : "")  + (deonticInverted ? " (inv)" : "")
					+ ", I=" + aim
					+ ", C=" + conditions + "]";
		}
		return "Invalid Expression type " + this.type;
	}

}
