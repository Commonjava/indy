package org.commonjava.aprox.conf;

import javax.inject.Inject;

import org.commonjava.web.config.ConfigurationException;

public abstract class AbstractAproxFeatureConfig<T, U extends T>
    implements AproxFeatureConfig<T, U>
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
