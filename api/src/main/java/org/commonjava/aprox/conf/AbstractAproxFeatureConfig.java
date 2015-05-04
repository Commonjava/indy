/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
    public abstract AproxConfigClassInfo getInfo();

    protected T getConfig()
        throws ConfigurationException
    {
        return factory.getConfiguration( implCls );
    }

}
