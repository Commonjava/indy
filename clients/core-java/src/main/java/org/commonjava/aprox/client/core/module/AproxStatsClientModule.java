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
package org.commonjava.aprox.client.core.module;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.client.core.AproxClientException;
import org.commonjava.aprox.client.core.AproxClientModule;
import org.commonjava.aprox.client.core.helper.HttpResources;
import org.commonjava.aprox.model.core.dto.EndpointViewListing;
import org.commonjava.aprox.model.spi.AddOnListing;
import org.commonjava.aprox.stats.AProxVersioning;

public class AproxStatsClientModule
    extends AproxClientModule
{
    
    public AProxVersioning getVersionInfo()
        throws AproxClientException
    {
        return getHttp().get( "/stats/version-info", AProxVersioning.class );
    }
    
    public EndpointViewListing getAllEndpoints()
        throws AproxClientException
    {
        return getHttp().get( "/stats/all-endpoints", EndpointViewListing.class );
    }
    
    public AddOnListing getActiveAddons()
        throws AproxClientException
    {
        return getHttp().get( "/stats/addons/active", AddOnListing.class );
    }

    public InputStream getActiveAddonsJS()
        throws AproxClientException
    {
        final HttpResources resources = getHttp().getRaw( "/stats/addons/active.js" );

        if ( resources.getStatusCode() != 200 )
        {
            IOUtils.closeQuietly( resources );
            throw new AproxClientException( resources.getStatusCode(), "Response returned status: %s.",
                                            resources.getStatusLine() );
        }

        try
        {
            return resources.getResponseStream();
        }
        catch ( final IOException e )
        {
            throw new AproxClientException( "Failed to open response content stream: %s", e, e.getMessage() );
        }
    }

}
