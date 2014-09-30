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
 * Performs some sort of service/subsystem startup as AProx is preparing to run.
 */
public interface StartupAction
    extends AproxLifecycleAction
{

    /**
     * Start some service after migration actions are completed, just before AProx is ready for use.
     */
    void start()
        throws AproxLifecycleException;

}
