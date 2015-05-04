/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
