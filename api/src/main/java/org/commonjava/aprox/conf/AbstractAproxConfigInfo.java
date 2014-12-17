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

import org.commonjava.web.config.ConfigUtils;

/**
 * Abstract implementation of {@link AproxConfigClassInfo} meant to make implementation as simple as providing a section name and configuration-info
 * registration class via the super() constructor.
 */
public abstract class AbstractAproxConfigInfo
    implements AproxConfigClassInfo
{

    private final Class<?> type;

    private final String sectionName;

    AbstractAproxConfigInfo()
    {
        type = Object.class;
        sectionName = null;
    }

    protected AbstractAproxConfigInfo( final Class<?> type )
    {
        this( type, null );
    }

    protected AbstractAproxConfigInfo( final Class<?> type, final String sectionName )
    {
        this.type = type;
        this.sectionName = sectionName;
    }

    /* (non-Javadoc)
     * @see org.commonjava.aprox.conf.AproxConfigInfo#getConfigurationClass()
     */
    @Override
    public Class<?> getConfigurationClass()
    {
        return type;
    }

    /* (non-Javadoc)
     * @see org.commonjava.aprox.conf.AproxConfigInfo#getSectionName()
     */
    @Override
    public String getSectionName()
    {
        return sectionName;
    }

    @Override
    public String toString()
    {
        final String key = sectionName == null ? ConfigUtils.getSectionName( type ) : sectionName;
        return key + " [" + type.getName() + "]";
    }

}
