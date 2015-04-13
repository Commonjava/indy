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
 * Converts storage/data formats from older versions of AProx into up-to-date forms.
 * These are run right after the system boots, but before AProx enters its startup sequence (see {@link StartupAction}).
 */
public interface MigrationAction
    extends AproxLifecycleAction
{

    /**
     * Execute the migration, and return whether anything was changed as a result.
     */
    boolean migrate()
        throws AproxLifecycleException;

    /**
     * Used to sort the actions, with highest priority executing first.
     * Priority should generally be between 1-100.
     */
    int getMigrationPriority();

}
