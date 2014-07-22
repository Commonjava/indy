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
package org.commonjava.aprox.conf;

import org.commonjava.web.config.section.MapSectionListener;

/**
 * Abstract class designed to make it simpler to create a subsystem or add-on configuration that has no specific set of configuration parameters. 
 * Instead, it knows how to accommodate a flexible mapping of key=value pairs into a usable configuration.
 */
public abstract class AbstractAproxMapConfig
    extends MapSectionListener
{

    private String sectionName;

    protected AbstractAproxMapConfig()
    {
    }

    protected AbstractAproxMapConfig( final String sectionName )
    {
        this.sectionName = sectionName;
    }

    /**
     * Return the name of the configuration subsection that pertains to this configuration class.
     */
    public String getSectionName()
    {
        return sectionName;
    }

}
