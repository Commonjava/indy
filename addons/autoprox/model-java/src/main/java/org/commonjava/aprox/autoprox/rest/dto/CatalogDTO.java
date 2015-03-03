package org.commonjava.aprox.autoprox.rest.dto;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;

public class CatalogDTO
{

    private boolean enabled;

    private List<RuleDTO> rules;

    public CatalogDTO()
    {
    }

    public CatalogDTO( final boolean enabled, final List<RuleDTO> rules )
    {
        this.enabled = enabled;
        this.rules = rules;
    }

    public List<RuleDTO> getRules()
    {
        return rules == null ? Collections.<RuleDTO> emptyList() : rules;
    }

    public void setRules( final List<RuleDTO> rules )
    {
        this.rules = rules;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled( final boolean enabled )
    {
        this.enabled = enabled;
    }

    @Override
    public String toString()
    {
        return String.format( "CatalogDTO [enabled=%s]:\n  ", enabled, StringUtils.join( rules, "\n  " ) );
    }

}
