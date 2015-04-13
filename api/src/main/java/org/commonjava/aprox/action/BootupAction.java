/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.action;

/**
 * Performs some sort of service/subsystem bootup just before AProx is preparing to run.
 */
public interface BootupAction
    extends AproxLifecycleAction
{

    /**
     * Initialize some subsystem before migration actions start.
     */
    void init()
        throws AproxLifecycleException;

    /**
     * Used to sort the actions, with highest priority executing first.
     * Priority should generally be between 1-100.
     */
    int getBootPriority();

}
