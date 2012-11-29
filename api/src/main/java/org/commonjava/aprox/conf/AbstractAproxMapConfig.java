package org.commonjava.aprox.conf;

import org.commonjava.web.config.section.MapSectionListener;

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

    public String getSectionName()
    {
        return sectionName;
    }

}
