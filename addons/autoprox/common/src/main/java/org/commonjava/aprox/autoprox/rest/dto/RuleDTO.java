package org.commonjava.aprox.autoprox.rest.dto;

import org.commonjava.aprox.autoprox.data.RuleMapping;

public class RuleDTO
{

    private String name;

    private String spec;

    public RuleDTO()
    {
    }

    public RuleDTO( final RuleMapping mapping )
    {
        name = mapping.getScriptName();
        spec = mapping.getSpecification();
    }

    public String getName()
    {
        return name;
    }

    public void setName( final String name )
    {
        this.name = name;
    }

    public String getSpec()
    {
        return spec;
    }

    public void setSpec( final String spec )
    {
        this.spec = spec;
    }

}
