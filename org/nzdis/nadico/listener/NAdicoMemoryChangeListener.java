package org.nzdis.nadico.listener;

import org.nzdis.nadico.deonticRange.MemoryUpdateException;

public interface NAdicoMemoryChangeListener {

	public void memoryChanged() throws MemoryUpdateException;
	
}
