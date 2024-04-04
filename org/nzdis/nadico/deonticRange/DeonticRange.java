package org.nzdis.nadico.deonticRange;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.nzdis.nadico.listener.NAdicoMemoryChangeListener;
import org.nzdis.nadico.NAdicoExpression;
import org.nzdis.nadico.NAdicoGeneralizer;
import org.nzdis.micro.inspector.annotations.Inspect;
import org.sofosim.environment.memoryTypes.DiscreteNumericListMemory;
import org.sofosim.environment.memoryTypes.util.ScaleDifferenceCalculator;

public class DeonticRange implements NAdicoMemoryChangeListener{

	/**
	 * nADICO generalizer instance
	 */
	private NAdicoGeneralizer nadicoGeneralizer = null;
	/**
	 * Deontic range configuration.
	 */
	@Inspect
	private DeonticRangeConfiguration config = null;
	/**
	 * Current lower boundary of deontic range
	 */
	protected Float lowerBoundary = null;
	/**
	 * Current upper boundary of deontic range
	 */
	protected Float upperBoundary = null;
	/**
	 * Type of deontic range
	 */
	@Inspect
	private String chosenType = null;
	
	/**
	 * Deontic value mapper implementation to be used. Defaults to 
	 * {@link SymmetricDeonticValueMapper}.
	 */
	private Class<? extends DeonticValueMapper> mapperClass = SymmetricDeonticValueMapper.class;
	
	/**
	 * Mapper for values to deontics (String values)
	 */
	@Inspect
	private DeonticValueMapper valueMapper = null;
	
	
	/**
	 * Tolerance around extremes in percent of total deontic range
	 */
	protected Float toleranceAroundExtremeValuesInPercent;
	
	@Inspect
	private DiscreteNumericListMemory historyMemoryUpper = null;
	@Inspect
	private DiscreteNumericListMemory historyMemoryLower = null;
	
	private void initializeRemainingConfig(final DeonticRangeConfiguration config){
		this.config = config;
		if(this.chosenType == null){
			this.chosenType = this.config.deonticRangeType;
		}
		if(this.chosenType.equals(DeonticRangeConfiguration.DEONTIC_RANGE_TYPE_STATIC_MIN_MAX)){
			setupStaticRange(config.deonticRangeStaticLowerBoundary, config.deonticRangeStaticUpperBoundary);
		} else {
			setupByRangeType(this.chosenType);
		}
		toleranceAroundExtremeValuesInPercent = config.toleranceAroundExtremeValuesInPercent;
		if(this.config.mapper != null){
			this.mapperClass = this.config.mapper;
			System.out.println("Set up deontic value mapper implementation " + this.mapperClass.getSimpleName());
		}
		initializeValueMapper();
	}
	
	/**
	 * Sets up deontic range using nAdicoGeneralizer and configuration structure.
	 * @param generalizer
	 * @param config Configuration instance
	 */
	public DeonticRange(NAdicoGeneralizer generalizer, DeonticRangeConfiguration config){
		subscribeToMemoryListener(generalizer);
		initializeRemainingConfig(config);
	}
	
	/**
	 * Checks that the Deontic Range is subscribed to a NAdicoGeneralizer instance. 
	 * @param memory
	 */
	private void subscribeToMemoryListener(NAdicoGeneralizer memory){
		this.nadicoGeneralizer = memory;
		if(this.nadicoGeneralizer != null){
			this.nadicoGeneralizer.registerMemoryChangeListener(this);
		}
	}
	
	/**
	 * Does setup procedure for dynamic range types (non-static!).
	 * @param rangeType
	 */
	private void setupByRangeType(String rangeType){
		if(rangeType.equals(DeonticRangeConfiguration.DEONTIC_RANGE_TYPE_STATIC_MIN_MAX)){
			throw new RuntimeException("Should never be called for static min. and max. range.");
		}
		switch(rangeType){
			case DeonticRangeConfiguration.DEONTIC_RANGE_TYPE_EXPANDING_MIN_MAX:
				this.chosenType = DeonticRangeConfiguration.DEONTIC_RANGE_TYPE_EXPANDING_MIN_MAX;
				break;
			case DeonticRangeConfiguration.DEONTIC_RANGE_TYPE_HISTORY_MIN_MAX:
				this.chosenType = DeonticRangeConfiguration.DEONTIC_RANGE_TYPE_HISTORY_MIN_MAX;
				setupHistoryRange(config.deonticHistoryLength);
				break;
			case DeonticRangeConfiguration.DEONTIC_RANGE_TYPE_SITUATIONAL_MIN_MAX:
				this.chosenType = DeonticRangeConfiguration.DEONTIC_RANGE_TYPE_SITUATIONAL_MIN_MAX;
				break;
			case DeonticRangeConfiguration.DEONTIC_RANGE_TYPE_DISCRETE:
				this.chosenType = DeonticRangeConfiguration.DEONTIC_RANGE_TYPE_DISCRETE;
				break;
			default: 
				throw new RuntimeException("Deontic Range type unknown: " + rangeType);
		}
		System.out.println("Initialized Deontic Range: " + this.chosenType);
	}
	
	/**
	 * Sets up static deontic range between specified values.
	 * @param lowerBoundary
	 * @param upperBoundary
	 */
	private void setupStaticRange(Float lowerBoundary, Float upperBoundary){
		if(lowerBoundary == null){
			throw new RuntimeException("Initialized static Deontic Range without specifying lower boundary.");
		}
		if(upperBoundary == null){
			throw new RuntimeException("Initialized static Deontic Range without specifying lower boundary.");
		}
		this.lowerBoundary = lowerBoundary;
		this.upperBoundary = upperBoundary;
		this.chosenType = DeonticRangeConfiguration.DEONTIC_RANGE_TYPE_STATIC_MIN_MAX;
		System.out.println("Initialized Deontic Range: " + this.chosenType + ", lower boundary: " + this.lowerBoundary + ", upper boundary: " + this.upperBoundary);
	}
	
	/**
	 * Initializes a history memory instance and initializes it with a 
	 * given historyLength (number of entries).
	 * @param historyLength Length of history
	 */
	private void setupHistoryRange(int historyLength){
		this.historyMemoryLower = new DiscreteNumericListMemory(historyLength);
		this.historyMemoryUpper = new DiscreteNumericListMemory(historyLength);
		this.lowerBoundary = 0f;
		this.upperBoundary = 0f;
	}
	
	/**
	 * Returns type of deontic range (see DeonticRange constants for potential responses).
	 */
	public String getDeonticRangeType(){
		return this.chosenType;
	}
	
	/**
	 * Should be called upon memory change to update boundaries. 
	 * Works for all deontic range types.
	 */
	private void updateDeonticRange() throws MemoryUpdateException {
		if(nadicoGeneralizer != null){
			// Check for valid values first
			NAdicoExpression minExpr = nadicoGeneralizer.getStatementWithMinValence();
			Float minValence = null;
			if (minExpr != null) {
				minValence = nadicoGeneralizer.getStatementWithMinValence().deontic;
			}
			NAdicoExpression maxExpr = nadicoGeneralizer.getStatementWithMaxValence();
			Float maxValence = null;
			if (maxExpr != null) {
				maxValence = nadicoGeneralizer.getStatementWithMaxValence().deontic;
			}
			if (minValence == null || maxValence == null) {
				System.out.println("Ignored deontic range update since memory is empty.");
				return;
			}
			if (minValence <= -Float.MAX_VALUE || minValence.isNaN() || minValence.isInfinite()) {
				throw new MemoryUpdateException(nadicoGeneralizer.getOwner() + ": Deontic Range - Update failed, since input value is outside lower boundary of number range. Value: " + minValence + "; Context: " + nadicoGeneralizer.getContext());
			}
			if (maxValence >= Float.MAX_VALUE || maxValence.isNaN() || maxValence.isInfinite()) {
				throw new MemoryUpdateException(nadicoGeneralizer.getOwner() + ": Deontic Range - Update failed, since input value is outside upper boundary of number range. Value: " + maxValence + "; Context: " + nadicoGeneralizer.getContext());
			}

			switch(this.chosenType){
				case DeonticRangeConfiguration.DEONTIC_RANGE_TYPE_DISCRETE:
					//do nothing
					break;
				case DeonticRangeConfiguration.DEONTIC_RANGE_TYPE_STATIC_MIN_MAX:
					//do nothing
					break;
				case DeonticRangeConfiguration.DEONTIC_RANGE_TYPE_EXPANDING_MIN_MAX:
					if(this.lowerBoundary == null){
						this.lowerBoundary = minValence;
					}
					if(this.upperBoundary == null){
						this.upperBoundary = maxValence;
					}
					this.lowerBoundary = Math.min(this.lowerBoundary, minValence);
					this.upperBoundary = Math.max(this.upperBoundary, maxValence);
					break;
				case DeonticRangeConfiguration.DEONTIC_RANGE_TYPE_SITUATIONAL_MIN_MAX:
					this.lowerBoundary = minValence;
					this.upperBoundary = maxValence;
					break;
				case DeonticRangeConfiguration.DEONTIC_RANGE_TYPE_HISTORY_MIN_MAX:
					try{
						historyMemoryLower.memorize(minValence);
					} catch(NullPointerException e){
						historyMemoryLower.memorize(0f);
					}
					try{
						historyMemoryUpper.memorize(maxValence);
					} catch(NullPointerException e){
						historyMemoryUpper.memorize(0f);
					}
					this.lowerBoundary = historyMemoryLower.getMeanOfAllEntries();
					this.upperBoundary = historyMemoryUpper.getMeanOfAllEntries();
					break;
				default: throw new MemoryUpdateException("Deontic Range Update: Unknown deontic range type " + this.chosenType + "; Context: " + nadicoGeneralizer.getContext());
			}
		} else {
			throw new MemoryUpdateException("No nAdicoGeneralizer appears to be registered for the calculation of the deontic range." + " Context: " + nadicoGeneralizer.getContext());
		}
	}

	@Override
	public void memoryChanged() throws MemoryUpdateException {
		updateDeonticRange();
	}
	
	/**
	 * Initializes a Deontic Value Mapper for this range 
	 * (i.e. the mapping of deontic value to term).
	 */
	private void initializeValueMapper(){
		if(this.valueMapper == null){
			Constructor ctor;
			try {
				ctor = mapperClass.getDeclaredConstructor(DeonticRange.class);
				ctor.setAccessible(true);
			    this.valueMapper = (DeonticValueMapper)ctor.newInstance(this);
			} catch (NoSuchMethodException | SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//this.valueMapper = /*new ZeroBasedEquiCompartmentDeonticValueMapper(this);*/ new SymmetricDeonticValueMapper(this);
		}
	}
	
	/**
	 * Applies a given NAdicoStatement or NAdicoOpinion and returns the deontic associated with it.
	 * @param statementOrOpinion NAdicoStatement/Opinion to be mapped into deontic
	 */
	public String getDeonticTermForExpression(NAdicoExpression statementOrOpinion){
		initializeValueMapper();
		if(statementOrOpinion == null || statementOrOpinion.isCombination()){
			throw new RuntimeException("Passed invalid parameter (either null or NAdicoCombination) for norm key generation: " + statementOrOpinion);
		}
		return this.valueMapper.getDeonticForValue(statementOrOpinion.deontic);
	}
	
	/**
	 * Returns the current lower boundary of the deontic range.
	 * @return
	 */
	public Float getLowerBoundary() {
		initializeValueMapper();
		return lowerBoundary;
	}

	/**
	 * Returns the current upper boundary of the deontic range.
	 * @return
	 */
	public Float getUpperBoundary() {
		initializeValueMapper();
		return upperBoundary;
	}
	
	/**
	 * Returns the situational normative center of the deontic range.
	 * @return
	 */
	//@Inspect
	public Float getNormativeCenter(){
		initializeValueMapper();
		return valueMapper.getNormativeCenter(config.maximumMovementInDeonticCenter);
	}

	/**
	 * Returns the valence for a given value, i.e. 
	 * indicates if it is positive (1), neutral (0) 
	 * or negative (-1).
	 * @param valence
	 * @return
	 */
	public Integer getNormativeValence(Float valence){
		initializeValueMapper();
		return valueMapper.getNormativeValence(valence);
	}
	
	/**
	 * Returns the normative valence for the deontic of 
	 * a given NAdicoStatement, indicating positive (1), 
	 * neutral (0) or negative (-1) valence.
	 * @param statement
	 * @return
	 */
	public Integer getNormativeValence(NAdicoExpression statement){
		initializeValueMapper();
		return valueMapper.getNormativeValence(statement);
	}
	
	/**
	 * Returns normative valence for a deontic String representation
	 * @param deontic String representation of deontic
	 * @return Normative valence ranging from -1 (negative) to 1 (positive)
	 */
	public Integer getNormativeValence(String deontic){
		initializeValueMapper();
		return valueMapper.getNormativeValence(deontic);
	}
	
	/**
	 * Returns deontic value mapper instance.
	 * @return
	 */
	public DeonticValueMapper getDeonticValueMapper(){
		return this.valueMapper;
	}
	
	/**
	 * Indicates if a given value is within the tolerance around 
	 * the upper deontic range boundary.
	 * @param value Value to be tested
	 * @return True indicates that the value lies within tolerance zone
	 */
	public boolean valueWithinUpperBoundaryTolerance(Float value){
		return ((this.upperBoundary - (ScaleDifferenceCalculator.calculateDifferenceOnScale(lowerBoundary, upperBoundary) * toleranceAroundExtremeValuesInPercent)) <= value); 
	}
	
	/**
	 * Indicates if a given value is within the tolerance around 
	 * the lower deontic range boundary.
	 * @param value Value to be tested
	 * @return True indicates that the value lies within tolerance zone
	 */
	public boolean valueWithinLowerBoundaryTolerance(Float value){
		return ((this.lowerBoundary + (ScaleDifferenceCalculator.calculateDifferenceOnScale(lowerBoundary, upperBoundary) * toleranceAroundExtremeValuesInPercent)) >= value);
	}
	
	@Override
	public String toString(){
		if(valueMapper != null){
			return valueMapper.toString();
		} else {
			return "DeonticRange still empty!";
		}
	}

}
