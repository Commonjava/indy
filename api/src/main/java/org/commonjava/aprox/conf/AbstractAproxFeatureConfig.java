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

/**
 * Abstract implementation of {@link AproxFeatureConfig} that injects the {@link AproxConfigFactory} and implements a protected getConfig()
 * method. This method is meant to serve as a convenient way for implementors to retrieve (and re-cast) the configuration instance when exposing it
 * for CDI injection via an @Produces-style accessor method.
 * 
 * @author jdcasey
 *
 * @param <T> This is the interface class for the configuration managed by this feature config
 * @param <U> This is the implementation class for the configuration managed by this feature config
 */
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
