package org.commonjava.aprox.autoprox.client;

import org.apache.http.HttpStatus;
import org.commonjava.aprox.autoprox.rest.dto.CatalogDTO;
import org.commonjava.aprox.autoprox.rest.dto.RuleDTO;
import org.commonjava.aprox.client.core.AproxClientException;
import org.commonjava.aprox.client.core.AproxClientModule;
import org.commonjava.aprox.client.core.util.UrlUtils;

public class AutoProxCatalogModule
    extends AproxClientModule
{

    public CatalogDTO getCatalog()
        throws AproxClientException
    {
        return http.get( "autoprox/catalog", CatalogDTO.class );
    }

    public void reparseCatalog()
        throws AproxClientException
    {
        http.delete( "autoprox/catalog", HttpStatus.SC_OK, HttpStatus.SC_NO_CONTENT );
    }

    public RuleDTO getRuleNamed( final String name )
        throws AproxClientException
    {
        return http.get( UrlUtils.buildUrl( "autoprox/catalog", name ), RuleDTO.class );
    }

    public RuleDTO storeRule( final RuleDTO dto )
        throws AproxClientException
    {
        final boolean success = http.put( UrlUtils.buildUrl( "autoprox/catalog", dto.getName() ), dto );
        if ( !success )
        {
            return null;
        }

        return getRuleNamed( dto.getName() );
    }

    public void deleteRuleNamed( final String name )
        throws AproxClientException
    {
        http.delete( UrlUtils.buildUrl( "autoprox/catalog", name ) );
    }

}
