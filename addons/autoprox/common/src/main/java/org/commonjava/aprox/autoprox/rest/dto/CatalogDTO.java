package org.commonjava.aprox.autoprox.rest.dto;

import java.util.ArrayList;
import java.util.List;

import org.commonjava.aprox.autoprox.data.AutoProxCatalog;
import org.commonjava.aprox.autoprox.data.RuleMapping;

public class CatalogDTO
{

    private boolean enabled;

    private List<RuleDTO> rules;

    public CatalogDTO()
    {
    }

    public CatalogDTO( final AutoProxCatalog catalog )
    {
        enabled = catalog.isEnabled();
        rules = new ArrayList<RuleDTO>();

        final List<RuleMapping> mappings = catalog.getRuleMappings();
        for ( final RuleMapping mapping : mappings )
        {
            rules.add( new RuleDTO( mapping ) );
        }
    }

    public List<RuleDTO> getRules()
    {
        return rules;
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

}
