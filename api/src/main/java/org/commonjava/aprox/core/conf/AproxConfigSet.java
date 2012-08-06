package org.commonjava.aprox.core.conf;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.commonjava.web.config.ConfigurationException;

public class AproxConfigSet<T, U extends T>
{

    private final Class<U> implCls;

    private final String sectionName;

    public AproxConfigSet( final Class<U> implCls )
    {
        this( implCls, null );
    }

    public AproxConfigSet( final Class<U> implCls, final String sectionName )
    {
        this.implCls = implCls;
        this.sectionName = sectionName;
    }

    @Inject
    private AproxConfigFactory factory;

    @Produces
    @Default
    public T getConfig()
        throws ConfigurationException
    {
        return factory.getConfiguration( implCls );
    }

    @Singleton
    public final class ConfigSection
        implements AproxConfigSection<U>
    {

        @Override
        public Class<U> getConfigurationClass()
        {
            return implCls;
        }

        @Override
        public String getSectionName()
        {
            return sectionName;
        }
    }

}
