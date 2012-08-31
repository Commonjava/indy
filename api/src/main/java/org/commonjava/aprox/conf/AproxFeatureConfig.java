package org.commonjava.aprox.conf;

import javax.inject.Inject;

import org.commonjava.web.config.ConfigurationException;

public abstract class AproxFeatureConfig<T, U extends T>
{

    private final Class<U> implCls;

    public AproxFeatureConfig( final Class<U> implCls )
    {
        this.implCls = implCls;
    }

    @Inject
    private AproxConfigFactory factory;

    public abstract AproxConfigInfo getInfo();

    protected T getConfig()
        throws ConfigurationException
    {
        return factory.getConfiguration( implCls );
    }

}
