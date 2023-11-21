package org.nzdis.nadico.components;

public abstract class IDAttributeResolver<T extends Attributes> {

	public abstract T getAttributes(String id);
	
}
