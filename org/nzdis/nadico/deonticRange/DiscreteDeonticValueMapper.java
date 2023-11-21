package org.nzdis.nadico.deonticRange;

import java.util.LinkedHashMap;
import org.nzdis.nadico.NAdicoExpression;

/**
 * Value Mapper for discrete deontics conception (conventional differentiation between MUST NOT, MAY, and MUST 
 * without continuity conception).
 * @author Christopher Frantz
 *
 */
public class DiscreteDeonticValueMapper extends DeonticValueMapper{

	public DiscreteDeonticValueMapper(DeonticRange range) {
		super(range);
	}

	@Override
	public String getDeonticForValue(Float value) {
		return value > DiscreteDeonticValues.MAY ? DeonticValues.MUST : value < DiscreteDeonticValues.MAY ? DeonticValues.MUST_NOT : DeonticValues.MAY;
	}

	@Override
	public Float invertDeonticValue(Float value) {
		return value * -1;
	}

	@Override
	public Float getNormativeCenter() {
		return DiscreteDeonticValues.MAY;
	}

	@Override
	public Float getNormativeCenter(
			Float maxMovementInPercentOfPreviousDeonticRange) {
		return DiscreteDeonticValues.MAY;
	}

	@Override
	public Integer getNormativeValence(Float valence) {
		return valence > DiscreteDeonticValues.MAY ? NormativeValence.POSITIVE : valence < DiscreteDeonticValues.MAY ? NormativeValence.NEGATIVE : NormativeValence.NEUTRAL;
	}

	@Override
	public Integer getNormativeValence(NAdicoExpression statement) {
		return statement.deontic > DiscreteDeonticValues.MAY ? NormativeValence.POSITIVE : statement.deontic < DiscreteDeonticValues.MAY ? NormativeValence.NEGATIVE : NormativeValence.NEUTRAL;
	}

	@Override
	public Integer getNormativeValence(String deonticTerm) {
		switch(deonticTerm){
			case DeonticValues.MUST:
				return NormativeValence.POSITIVE;
			/*case DeonticValues.SHOULD:
				return NormativeValence.POSITIVE;*/
			case DeonticValues.MAY:
				return NormativeValence.NEUTRAL;
			/*case DeonticValues.INDIFFERENT:
				return NormativeValence.NEUTRAL;
			case DeonticValues.MAY_NOT:
				return NormativeValence.NEGATIVE;
			case DeonticValues.SHOULD_NOT:
				return NormativeValence.NEGATIVE;*/
			case DeonticValues.MUST_NOT:
				return NormativeValence.NEGATIVE;
			default: throw new RuntimeException("Requested normative valence for unknown deontic " + deonticTerm); 
		}
	}

	@Override
	public LinkedHashMap<String, Float> getDeonticRangeInnerBoundaries() {
		throw new RuntimeException("No inner range boundaries in discrete deontics range configurations (only discrete MUST, MAY, and MUST NOT).");
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(System.getProperty("line.separator"));
		builder.append(DeonticValues.MUST_NOT).append(": ").append(DiscreteDeonticValues.MUST_NOT).append(System.getProperty("line.separator"));
		builder.append(DeonticValues.INDIFFERENT).append(": ").append(DiscreteDeonticValues.MAY).append(System.getProperty("line.separator"));
		builder.append(DeonticValues.MUST).append(": ").append(DiscreteDeonticValues.MUST);
		return builder.toString();
	}

}
