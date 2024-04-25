package org.nzdis.nadico.deonticRange;

public class DeonticRangeConfiguration {

	/**
	 * The deontic range dynamically expands (i.e. widens) if higher or lower boundary values are detected.
	 * The range never reduces in this mode.
	 */
	public static final String DEONTIC_RANGE_TYPE_EXPANDING_MIN_MAX = "Expanding Min./Max. values (no reduction)";
	/**
	 * The deontic range dynamically adapts over time by calculating the mean over historical boundary values.
	 * This mode is the most flexible one with regards to the variation over time and allows the representation of 
	 * memory/experience. The history window needs to be specified for this range type upon initialization.
	 */
	public static final String DEONTIC_RANGE_TYPE_HISTORY_MIN_MAX = "History-based Min./Max. Values";
	/**
	 * The deontic range reflects only the experience of the past round and is thus rapidly adjusted in each round 
	 * (both expanding and retracting).
	 * Basically the same as {@link #DEONTIC_RANGE_TYPE_HISTORY_MIN_MAX} with a history length of one round.
	 */
	public static final String DEONTIC_RANGE_TYPE_SITUATIONAL_MIN_MAX = "Situational Min./Max. Values";
	/**
	 * The deontic range is static once initialized and never changes. All intermediate deontics (SHOULD, SHOULD NOT, etc.) are mapped.
	 * This is in contrast to {@link #DEONTIC_RANGE_TYPE_DISCRETE}, which only allows for the traditional deontics.
	 */
	public static final String DEONTIC_RANGE_TYPE_STATIC_MIN_MAX = "Static Min./Max. Values";
	/**
	 * The deontic range is fixed (similar to {@link #DEONTIC_RANGE_TYPE_STATIC_MIN_MAX}), but only offers/maps
	 * discrete MUST, MAY, and MUST NOT (no intermediate deontic values). This is compatible with the conventional deontics concept.
	 */
	public static final String DEONTIC_RANGE_TYPE_DISCRETE = "Discrete Deontic Values";
	
	/**
	 * Constructor primarily designed to instantiate discrete deontic range ({@link #DEONTIC_RANGE_TYPE_DISCRETE}). 
	 * All other types will be initialised without additional configuration.
	 * @param deonticRangeType Deontic range type
	 * @param mapper Deontic value mapper
	 */
	public DeonticRangeConfiguration(String deonticRangeType, Class<? extends DeonticValueMapper> mapper) {
		super();
		this.deonticRangeType = deonticRangeType;
		this.mapper = mapper;
		if(!this.deonticRangeType.equals(DEONTIC_RANGE_TYPE_DISCRETE)) {
			throw new IllegalArgumentException("DeonticRangeConfiguration for non-discrete ranges requires additional configuration.");
		}
	}
	
	/**
	 * Instantiates all non-static types of deontic ranges along with respective configuration parameters.
	 * {@link #DEONTIC_RANGE_TYPE_EXPANDING_MIN_MAX}, {@link #DEONTIC_RANGE_TYPE_HISTORY_MIN_MAX}, 
	 * {@link #DEONTIC_RANGE_TYPE_SITUATIONAL_MIN_MAX}
	 * For {@link #DEONTIC_RANGE_TYPE_DISCRETE} use constructor {@link #DeonticRangeConfiguration(String, Class)}. 
	 * @param deonticRangeType Range type
	 * @param deonticHistoryLength Length of history (for types that require history)
	 * @param toleranceAroundExtremeValuesInPercent Tolerance zone (in percent of total range) around extremes.
	 * @param maximumMovementInDeonticCenter Tolerance around deontic centre
	 * @param mapper Deontic Value Mapper to determine association of values with deontic compartments (DiscreteDeonticValueMapper, SymmetricDeonticValueMapper, ZeroBasedDeonticValueMapper).
	 */
	public DeonticRangeConfiguration(String deonticRangeType,
			int deonticHistoryLength, 
			float toleranceAroundExtremeValuesInPercent,
			float maximumMovementInDeonticCenter,
			Class<? extends DeonticValueMapper> mapper) {
		super();
		this.deonticRangeType = deonticRangeType;
		this.deonticHistoryLength = deonticHistoryLength;
		this.toleranceAroundExtremeValuesInPercent = toleranceAroundExtremeValuesInPercent;
		this.maximumMovementInDeonticCenter = maximumMovementInDeonticCenter;
		this.mapper = mapper;
	}
	
	/**
	 * Instantiates arbitrary types of deontic ranges along with respective configuration parameters.
	 * {@link #DEONTIC_RANGE_TYPE_DISCRETE}, {@link #DEONTIC_RANGE_TYPE_EXPANDING_MIN_MAX}, 
	 * {@link #DEONTIC_RANGE_TYPE_HISTORY_MIN_MAX}, {@link #DEONTIC_RANGE_TYPE_SITUATIONAL_MIN_MAX}, 
	 * {@link #DEONTIC_RANGE_TYPE_STATIC_MIN_MAX}
	 * For {@link #DEONTIC_RANGE_TYPE_DISCRETE} use constructor {@link #DeonticRangeConfiguration(String, Class)}.
	 * @param deonticRangeType Range type
	 * @param deonticHistoryLength Length of history (for types that require history)
	 * @param deonticRangeStaticUpperBoundary Upper boundary for statically initialised deontic range {@link #DEONTIC_RANGE_TYPE_STATIC_MIN_MAX}
	 * @param deonticRangeStaticLowerBoundary Lower boundary for statically initialised deontic range {@link #DEONTIC_RANGE_TYPE_STATIC_MIN_MAX}
	 * @param toleranceAroundExtremeValuesInPercent Tolerance zone (in percent of total range) around extremes.
	 * @param maximumMovementInDeonticCenter Tolerance around deontic centre
	 * @param mapper Deontic Value Mapper to determine association of values with deontic compartments (DiscreteDeonticValueMapper, SymmetricDeonticValueMapper, ZeroBasedDeonticValueMapper).
	 */
	public DeonticRangeConfiguration(String deonticRangeType,
			int deonticHistoryLength, 
			float deonticRangeStaticUpperBoundary,
			float deonticRangeStaticLowerBoundary,
			float toleranceAroundExtremeValuesInPercent,
			float maximumMovementInDeonticCenter,
			Class<? extends DeonticValueMapper> mapper) {
		super();
		this.deonticRangeType = deonticRangeType;
		this.deonticHistoryLength = deonticHistoryLength;
		this.deonticRangeStaticUpperBoundary = deonticRangeStaticUpperBoundary;
		this.deonticRangeStaticLowerBoundary = deonticRangeStaticLowerBoundary;
		this.toleranceAroundExtremeValuesInPercent = toleranceAroundExtremeValuesInPercent;
		this.maximumMovementInDeonticCenter = maximumMovementInDeonticCenter;
		this.mapper = mapper;
	}
	
	/**
	 * Type of deontic range. 
	 */
	protected String deonticRangeType;
	/**
	 * Length of deontic history.
	 */
	protected int deonticHistoryLength;
	/**
	 * Upper boundary of deontic range if deontic 
	 * range is static.
	 */
	protected float deonticRangeStaticUpperBoundary;
	/**
	 * Lower boundary of deontic range if deontic 
	 * range is static.
	 */
	protected float deonticRangeStaticLowerBoundary;
	/**
	 * Tolerance around extreme values in percent.
	 */
	protected float toleranceAroundExtremeValuesInPercent;
	/**
	 * Indicates the maximal amount of movement of the normative center.
	 */
	protected float maximumMovementInDeonticCenter;
	/**
	 * Deontic mapper class to be instantiated for mapping of deontic values 
	 * to terms. 
	 */
	protected Class<? extends DeonticValueMapper> mapper = null;
	
}
