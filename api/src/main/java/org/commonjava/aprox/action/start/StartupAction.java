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
package org.commonjava.aprox.action.start;

/**
 * Converts storage/data formats from older versions of AProx into up-to-date forms.
 * These are run right after the system boots, but before it listens for requests.
 */
public interface StartupAction
{

    /** Used mainly for reporting, this is a unique identifier for this migration action. */
    String getId();

    /**
     * Execute the migration, and return whether anything was changed as a result.
     */
    void start()
        throws AproxInitException;

}
