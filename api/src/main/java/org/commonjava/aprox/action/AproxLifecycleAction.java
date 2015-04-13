package org.commonjava.aprox.action;

/**
 * Some sort of action in the lifecycle of AProx (migration, startup, shutdown).
 */
public interface AproxLifecycleAction
{
    /** Used mainly for reporting, this is a unique identifier for this action. */
    String getId();

}
