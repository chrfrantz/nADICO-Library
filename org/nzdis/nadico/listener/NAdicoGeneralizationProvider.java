package org.nzdis.nadico.listener;

/**
 * Interface for generalisation provider, i.e. individual entity that generalises/transforms
 * individual markers, invoked by the generaliser.
 * 
 * @author Christopher Frantz
 *
 */
public interface NAdicoGeneralizationProvider<T> {

	/**
	 * Returns generalised markers. Is invoked by generaliser.
	 * @param markersToGeneralise Input markers
	 * @return generalised markers
	 */
	public T generalizeAttributes(T markersToGeneralise);
	
}
