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

import javax.inject.Inject;

import org.commonjava.web.config.ConfigurationException;

public abstract class AbstractAproxFeatureConfig<T, U extends T>
    implements AproxFeatureConfig
{

    private final Class<U> implCls;

    AbstractAproxFeatureConfig()
    {
        implCls = null;
    }

    public AbstractAproxFeatureConfig( final Class<U> implCls )
    {
        this.implCls = implCls;
    }

    @Inject
    private AproxConfigFactory factory;

    /* (non-Javadoc)
     * @see org.commonjava.aprox.conf.AproxFeatureConfig#getInfo()
     */
    @Override
    public abstract AproxConfigInfo getInfo();

    protected T getConfig()
        throws ConfigurationException
    {
        return factory.getConfiguration( implCls );
    }

}
