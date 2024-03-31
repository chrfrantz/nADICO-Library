package org.nzdis.nadico.deonticRange;

/**
 * Exception thrown when an error during memory update occurs (e.g., overrunning value ranges).
 */
public class MemoryUpdateException extends Exception {

    public MemoryUpdateException(String message) {
        super(message);
    }

}
