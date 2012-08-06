package org.commonjava.aprox.core.conf;

import org.commonjava.web.config.ConfigUtils;

public abstract class AproxConfigInfo
{

    private final Class<?> type;

    private final String sectionName;

    protected AproxConfigInfo( final Class<?> type )
    {
        this( type, null );
    }

    protected AproxConfigInfo( final Class<?> type, final String sectionName )
    {
        this.type = type;
        this.sectionName = sectionName;
    }

    public final Class<?> getConfigurationClass()
    {
        return type;
    }

    public final String getSectionName()
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
