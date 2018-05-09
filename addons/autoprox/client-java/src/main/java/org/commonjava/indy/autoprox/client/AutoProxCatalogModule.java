/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.autoprox.client;

import org.apache.http.HttpStatus;
import org.commonjava.indy.autoprox.rest.dto.CatalogDTO;
import org.commonjava.indy.autoprox.rest.dto.RuleDTO;
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.client.core.util.UrlUtils;

public class AutoProxCatalogModule
    extends IndyClientModule
{

    public CatalogDTO getCatalog()
        throws IndyClientException
    {
        return http.get( "autoprox/catalog", CatalogDTO.class );
    }

    public void reparseCatalog()
        throws IndyClientException
    {
        http.delete( "autoprox/catalog", HttpStatus.SC_OK, HttpStatus.SC_NO_CONTENT );
    }

    public RuleDTO getRuleNamed( final String name )
        throws IndyClientException
    {
        return http.get( UrlUtils.buildUrl( "autoprox/catalog", name ), RuleDTO.class );
    }

    public RuleDTO storeRule( final RuleDTO dto )
        throws IndyClientException
    {
        final boolean success = http.put( UrlUtils.buildUrl( "autoprox/catalog", dto.getName() ), dto );
        if ( !success )
        {
            return null;
        }

        return getRuleNamed( dto.getName() );
    }

    public void deleteRuleNamed( final String name )
        throws IndyClientException
    {
        http.delete( UrlUtils.buildUrl( "autoprox/catalog", name ) );
    }

}
