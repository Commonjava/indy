/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.client.core.module;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.client.core.helper.HttpResources;
import org.commonjava.indy.model.core.dto.EndpointViewListing;
import org.commonjava.indy.model.spi.AddOnListing;
import org.commonjava.indy.stats.IndyVersioning;

public class IndyStatsClientModule
    extends IndyClientModule
{
    
    public IndyVersioning getVersionInfo()
        throws IndyClientException
    {
        return getHttp().get( "/stats/version-info", IndyVersioning.class );
    }
    
    public EndpointViewListing getAllEndpoints()
        throws IndyClientException
    {
        return getHttp().get( "/stats/all-endpoints", EndpointViewListing.class );
    }
    
    public AddOnListing getActiveAddons()
        throws IndyClientException
    {
        return getHttp().get( "/stats/addons/active", AddOnListing.class );
    }

    public InputStream getActiveAddonsJS()
        throws IndyClientException
    {
        final HttpResources resources = getHttp().getRaw( "/stats/addons/active.js" );

        if ( resources.getStatusCode() != 200 )
        {
            IOUtils.closeQuietly( resources );
            throw new IndyClientException( resources.getStatusCode(), "Response returned status: %s.",
                                            resources.getStatusLine() );
        }

        try
        {
            return resources.getResponseStream();
        }
        catch ( final IOException e )
        {
            throw new IndyClientException( "Failed to open response content stream: %s", e, e.getMessage() );
        }
    }

}
