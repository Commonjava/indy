package org.commonjava.aprox.autoprox.rest;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.audit.ChangeSummary;
import org.commonjava.aprox.autoprox.data.AutoProxCatalogManager;
import org.commonjava.aprox.autoprox.data.AutoProxRuleException;
import org.commonjava.aprox.autoprox.data.RuleMapping;
import org.commonjava.aprox.autoprox.rest.dto.CatalogDTO;
import org.commonjava.aprox.autoprox.rest.dto.RuleDTO;

@ApplicationScoped
public class AutoProxAdminController
{
    @Inject
    private AutoProxCatalogManager catalogManager;

    public CatalogDTO getCatalog()
    {
        return catalogManager.toDTO();
    }

    public RuleDTO storeRule( final RuleDTO dto, final String user )
        throws AutoProxRuleException
    {
        final RuleMapping mapping =
            catalogManager.storeRule( dto.getName(), dto.getSpec(), new ChangeSummary( user,
                                                                                       "Storing rule via REST api." ) );
        return mapping == null ? null : mapping.toDTO();
    }

    public RuleDTO getRule( final String name )
    {
        final RuleMapping mapping = catalogManager.getRuleNamed( name );
        return mapping == null ? null : mapping.toDTO();
    }

    public RuleDTO deleteRule( final String name, final String user )
        throws AutoProxRuleException
    {
        final RuleMapping mapping =
            catalogManager.removeRuleNamed( name, new ChangeSummary( user, "Deleting rule via REST api." ) );
        return mapping == null ? null : mapping.toDTO();
    }

    public void reparseCatalog()
        throws AutoProxRuleException
    {
        catalogManager.parseRules();
    }
}
