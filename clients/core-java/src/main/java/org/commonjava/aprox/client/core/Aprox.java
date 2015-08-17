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
package org.commonjava.aprox.client.core;

import java.io.Closeable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.commonjava.aprox.client.core.auth.AproxClientAuthenticator;
import org.commonjava.aprox.client.core.module.AproxContentClientModule;
import org.commonjava.aprox.client.core.module.AproxStatsClientModule;
import org.commonjava.aprox.client.core.module.AproxStoresClientModule;
import org.commonjava.aprox.model.core.io.AproxObjectMapper;
import org.commonjava.aprox.stats.AProxVersioning;

public class Aprox
    implements Closeable
{

    private final AproxClientHttp http;

    private final Set<AproxClientModule> moduleRegistry;

    public Aprox( final String baseUrl, final AproxClientModule... modules )
        throws AproxClientException
    {
        this( baseUrl, null, null, modules );
    }

    public Aprox( final String baseUrl, final AproxClientAuthenticator authenticator,
                  final AproxClientModule... modules )
        throws AproxClientException
    {
        this( baseUrl, authenticator, null, modules );
    }

    public Aprox( final String baseUrl, final AproxObjectMapper mapper, final AproxClientModule... modules )
        throws AproxClientException
    {
        this( baseUrl, null, mapper, modules );
    }

    public Aprox( final String baseUrl, final AproxClientAuthenticator authenticator, final AproxObjectMapper mapper,
                  final AproxClientModule... modules )
        throws AproxClientException
    {
        this.http =
            new AproxClientHttp( baseUrl, authenticator, mapper == null ? new AproxObjectMapper( true ) : mapper );
        this.moduleRegistry = new HashSet<>();

        setupStandardModules();
        for ( final AproxClientModule module : modules )
        {
            module.setup( this, http );
            moduleRegistry.add( module );
        }
    }

    public Aprox( final String baseUrl, final Collection<AproxClientModule> modules )
        throws AproxClientException
    {
        this( baseUrl, null, null, modules );
    }

    public Aprox( final String baseUrl, final AproxClientAuthenticator authenticator,
                  final Collection<AproxClientModule> modules )
        throws AproxClientException
    {
        this( baseUrl, authenticator, null, modules );
    }

    public Aprox( final String baseUrl, final AproxObjectMapper mapper, final Collection<AproxClientModule> modules )
        throws AproxClientException
    {
        this( baseUrl, null, mapper, modules );
    }

    public Aprox( final String baseUrl, final AproxClientAuthenticator authenticator, final AproxObjectMapper mapper,
                  final Collection<AproxClientModule> modules )
        throws AproxClientException
    {
        this.http =
            new AproxClientHttp( baseUrl, authenticator, mapper == null ? new AproxObjectMapper( true ) : mapper );
        this.moduleRegistry = new HashSet<>();

        setupStandardModules();
        for ( final AproxClientModule module : modules )
        {
            module.setup( this, http );
            moduleRegistry.add( module );
        }
    }

    public void setupExternal( final AproxClientModule module )
    {
        module.setup( this, http );
    }

    public Aprox connect()
    {
        http.connect();
        return this;
    }

    @Override
    public void close()
    {
        http.close();
    }

    public AProxVersioning getVersionInfo()
        throws AproxClientException
    {
        return http.get( "/stats/version-info", AProxVersioning.class );
    }

    public AproxStoresClientModule stores()
        throws AproxClientException
    {
        return module( AproxStoresClientModule.class );
    }

    public AproxContentClientModule content()
        throws AproxClientException
    {
        return module( AproxContentClientModule.class );
    }

    public AproxStatsClientModule stats()
        throws AproxClientException
    {
        return module( AproxStatsClientModule.class );
    }

    public <T extends AproxClientModule> T module( final Class<T> type )
        throws AproxClientException
    {
        for ( final AproxClientModule module : moduleRegistry )
        {
            if ( type.isInstance( module ) )
            {
                return type.cast( module );
            }
        }

        throw new AproxClientException( "Module not found: %s.", type.getName() );
    }

    public boolean hasModule( Class<?> type )
    {
        for ( final AproxClientModule module : moduleRegistry )
        {
            if ( type.isInstance( module ) )
            {
                return true;
            }
        }

        return false;
    }

    public String getBaseUrl()
    {
        return http.getBaseUrl();
    }

    private void setupStandardModules()
    {
        final Set<AproxClientModule> standardModules = new HashSet<>();
        standardModules.add( new AproxStoresClientModule() );
        standardModules.add( new AproxContentClientModule() );
        standardModules.add( new AproxStatsClientModule() );

        for ( final AproxClientModule module : standardModules )
        {
            module.setup( this, http );
            moduleRegistry.add( module );
        }
    }

}
