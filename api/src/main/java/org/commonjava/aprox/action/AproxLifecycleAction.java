package org.commonjava.aprox.action;

/**
 * Some sort of action in the lifecycle of AProx (migration, startup, shutdown).
 */
public interface AproxLifecycleAction
{
    /** Used mainly for reporting, this is a unique identifier for this action. */
    String getId();

    /**
     * Used to sort the actions, with highest priority executing first.
     * Priority should generally be between 1-100.
     */
    int getPriority();

}
