package org.commonjava.aprox.autoprox.rest;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.autoprox.data.AutoProxCatalog;
import org.commonjava.aprox.autoprox.rest.dto.CatalogDTO;

@ApplicationScoped
public class AutoProxAdminController
{
    @Inject
    private AutoProxCatalog catalog;

    public CatalogDTO getCatalog()
    {
        return new CatalogDTO( catalog );
    }

    // TODO: The reset of CRUD (CUD)
}
