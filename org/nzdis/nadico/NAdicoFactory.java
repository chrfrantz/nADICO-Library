package org.nzdis.nadico;

import org.nzdis.nadico.components.Aim;
import org.nzdis.nadico.components.Attributes;
import org.nzdis.nadico.components.Conditions;
import org.nzdis.nadico.deonticRange.DeonticRange;


public class NAdicoFactory<A extends Attributes, I extends Aim, C extends Conditions> {

	private final boolean performValidation;
	protected final DeonticRange deonticRange;
	
	public NAdicoFactory() {
		this(null);
	}
	
	protected NAdicoFactory(final boolean performValidation){
		this.deonticRange = null;
		this.performValidation = performValidation;
	}

	public NAdicoFactory(DeonticRange range) {
		this(range, true);
	}
	
	protected NAdicoFactory(final DeonticRange deonticRange, final boolean performValidation){
		this.deonticRange = deonticRange;
		this.performValidation = performValidation;
	}
	
	public NAdicoExpression<A,I,C> createNAdicoAction(final A attributes, final I aim, final C conditions){
		return createNAdicoAction(this.deonticRange, attributes, aim, conditions);
	}
	
	/**
	 * Creates a NAdicoAction instance with given attributes; all other components are initialised with
	 * new instances.
	 * @param attributes
	 * @return
	 */
	public NAdicoExpression<A,I,C> createNAdicoActionWithEmptyInstances(final A attributes){
		return createNAdicoAction(this.deonticRange, attributes, (I)new Aim(), (C)new Conditions());
	}
	
	/**
	 * Creates a NAdicoAction instance with given attributes and aim; all other components are initialised with
	 * new instances.
	 * @param attributes
	 * @param aim
	 * @return
	 */
	public NAdicoExpression<A,I,C> createNAdicoActionWithEmptyInstances(final A attributes, final I aim){
		return createNAdicoAction(this.deonticRange, attributes, aim, (C)new Conditions());
	}
	
	public NAdicoExpression<A,I,C> createNAdicoAction(final DeonticRange deonticRange, final A attributes, final I aim, final C conditions){
		final NAdicoExpression<A,I,C> instance = new NAdicoExpression<>(deonticRange != null ? deonticRange : this.deonticRange);
		instance.makeAction();
		instance.attributes = attributes;
		instance.aim = aim;
		instance.conditions = conditions;
		validate(instance);
		return instance;
	}
	
	public NAdicoExpression<A,I,C> createNAdicoStatement(final A attributes, final I aim, final C conditions){
		return createNAdicoStatement(attributes, null, aim, conditions, null);
	}
	
	public NAdicoExpression<A,I,C> createNAdicoStatement(final A attributes, final I aim){
		return createNAdicoStatement(attributes, null, aim, null, null);
	}
	
	public NAdicoExpression<A,I,C> createNAdicoStatement(final A attributes){
		return createNAdicoStatement(attributes, null, null, null, null);
	}
	
	public NAdicoExpression<A,I,C> createNAdicoStatement(final DeonticRange deonticRange, final A attributes, final I aim, final C conditions){
		return createNAdicoStatement(deonticRange, attributes, null, aim, conditions, null);
	}
	
	public NAdicoExpression<A,I,C> createNAdicoStatement(final A attributes, final Float deontics, final I aim, final C conditions){
		return createNAdicoStatement(attributes, deontics, aim, conditions, null);
	}
	
	public NAdicoExpression<A,I,C> createNAdicoStatement(final DeonticRange deonticRange, final A attributes, final Float deontics, final I aim, final C conditions){
		return createNAdicoStatement(deonticRange, attributes, deontics, aim, conditions, null);
	}
	
	public NAdicoExpression<A,I,C> createNAdicoStatement(final A attributes, final Float deontics, final I aim, final C conditions, final NAdicoExpression<A,I,C> orElse){
		return createNAdicoStatement(null, attributes, deontics, aim, conditions, orElse);
	}
	
	public NAdicoExpression<A,I,C> createNAdicoStatement(final DeonticRange deonticRange, final A attributes, final Float deontics, final I aim, final C conditions, final NAdicoExpression<A,I,C> orElse){
		final NAdicoExpression<A,I,C> instance = new NAdicoExpression<>(deonticRange != null ? deonticRange : this.deonticRange);
		instance.makeStatement();
		instance.attributes = attributes;
		instance.deontic = deontics;
		instance.aim = aim;
		instance.conditions = conditions;
		instance.setOrElse(orElse);
		validate(instance);
		return instance;
	}
	
	public NAdicoExpression<A,I,C> createNAdicoCombination(final String combinator, final NAdicoExpression<A,I,C>... expressions){
		return createNAdicoCombination(null, combinator, expressions);
	}
	
	public NAdicoExpression<A,I,C> createNAdicoCombination(final DeonticRange deonticRange, final String combinator, final NAdicoExpression<A,I,C>... expressions){
		final NAdicoExpression<A,I,C> instance = new NAdicoExpression<>(deonticRange != null ? deonticRange : this.deonticRange);
		instance.makeCombination(combinator);
		for(int i = 0; i < expressions.length; i++){
			instance.addExpression(expressions[i]);
		}
		validate(instance);
		return instance;
	}
	
	/**
	 * Performs basic field validation. 
	 * Accommodates incompleteness of combinations during incremental construction (e.g. yet missing nested statements).
	 * @param expr
	 */
	public void validate(final NAdicoExpression<A,I,C> expr) {
		if (performValidation) {
			if (!expr.isAction() && !expr.isCombination() && !expr.isStatement()) {
				throw new RuntimeException("Expression is neither Action, Statement, nor Combination: " + expr);
			}
			//basic fill check
			/*if ((expr.isAction() || expr.isStatement()) && (expr.attributes == null && expr.aim == null && expr.conditions == null)) {
				throw new RuntimeException((expr.isAction() ? "Action" : "Statement") + " should have at least one of the AIC elements filled.");
			}*/
			if (expr.isCombination()) {
				//Components
				if (expr.attributes != null) {
					throw new RuntimeException("Combinations must have empty Attributes: " + expr);
				}
				if (expr.aim != null) {
					throw new RuntimeException("Combinations must have empty Aim: " + expr);
				}
				if (expr.conditions != null) {
					throw new RuntimeException("Combinations must have empty Conditions: " + expr);
				}
				//Combinator
				if(expr.combinator != NAdicoExpression.AND
						&& expr.combinator != NAdicoExpression.OR
						&& expr.combinator != NAdicoExpression.XOR) {
					throw new RuntimeException("Combinations must have valid combinator: " + expr);
				}
				//Nested statements
				/*if(expr.nestedExpressions.size() < 2){
					throw new RuntimeException("Combinations must have at least two nested expressions: " + expr);
				}*/
			}
		}
	}

	@Override
	public String toString() {
		return "NAdicoFactory [performValidation=" + performValidation
				+ ", deonticRange=" + deonticRange + "]";
	}
	
}
