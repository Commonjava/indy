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
package org.commonjava.indy.client.core;

import java.io.Closeable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.Module;
import org.commonjava.indy.client.core.auth.IndyClientAuthenticator;
import org.commonjava.indy.client.core.module.IndyContentClientModule;
import org.commonjava.indy.client.core.module.IndySchedulerClientModule;
import org.commonjava.indy.client.core.module.IndyStatsClientModule;
import org.commonjava.indy.client.core.module.IndyStoresClientModule;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.stats.IndyVersioning;

public class Indy
    implements Closeable
{

    private final IndyClientHttp http;

    private final Set<IndyClientModule> moduleRegistry;

    public Indy( final String baseUrl, final IndyClientModule... modules )
        throws IndyClientException
    {
        this( baseUrl, null, null, modules );
    }

    public Indy( final String baseUrl, final IndyClientAuthenticator authenticator,
                  final IndyClientModule... modules )
        throws IndyClientException
    {
        this( baseUrl, authenticator, null, modules );
    }

    public Indy( final String baseUrl, final IndyObjectMapper mapper, final IndyClientModule... modules )
        throws IndyClientException
    {
        this( baseUrl, null, mapper, modules );
    }

    public Indy( final String baseUrl, final IndyClientAuthenticator authenticator, final IndyObjectMapper mapper,
                  final IndyClientModule... modules )
        throws IndyClientException
    {
        this.http =
            new IndyClientHttp( baseUrl, authenticator, mapper == null ? new IndyObjectMapper( true ) : mapper );
        this.moduleRegistry = new HashSet<>();

        setupStandardModules();
        for ( final IndyClientModule module : modules )
        {
            setup( module );
        }
    }

    public Indy( final String baseUrl, final Collection<IndyClientModule> modules )
        throws IndyClientException
    {
        this( baseUrl, null, null, modules );
    }

    public Indy( final String baseUrl, final IndyClientAuthenticator authenticator,
                  final Collection<IndyClientModule> modules )
        throws IndyClientException
    {
        this( baseUrl, authenticator, null, modules );
    }

    public Indy( final String baseUrl, final IndyObjectMapper mapper, final Collection<IndyClientModule> modules )
        throws IndyClientException
    {
        this( baseUrl, null, mapper, modules );
    }

    public Indy( final String baseUrl, final IndyClientAuthenticator authenticator, final IndyObjectMapper mapper,
                  final Collection<IndyClientModule> modules )
        throws IndyClientException
    {
        this.http =
            new IndyClientHttp( baseUrl, authenticator, mapper == null ? new IndyObjectMapper( true ) : mapper );
        this.moduleRegistry = new HashSet<>();

        setupStandardModules();
        for ( final IndyClientModule module : modules )
        {
            module.setup( this, http );
            moduleRegistry.add( module );
        }
    }

    public void setupExternal( final IndyClientModule module )
    {
        setup( module );
    }

    public Indy connect()
    {
        http.connect();
        return this;
    }

    @Override
    public void close()
    {
        http.close();
    }

    public IndyVersioning getVersionInfo()
        throws IndyClientException
    {
        return http.get( "/stats/version-info", IndyVersioning.class );
    }

    public IndyStoresClientModule stores()
        throws IndyClientException
    {
        return module( IndyStoresClientModule.class );
    }

    public IndySchedulerClientModule schedules()
            throws IndyClientException
    {
        return module( IndySchedulerClientModule.class );
    }

    public IndyContentClientModule content()
        throws IndyClientException
    {
        return module( IndyContentClientModule.class );
    }

    public IndyStatsClientModule stats()
        throws IndyClientException
    {
        return module( IndyStatsClientModule.class );
    }

    public <T extends IndyClientModule> T module( final Class<T> type )
        throws IndyClientException
    {
        for ( final IndyClientModule module : moduleRegistry )
        {
            if ( type.isInstance( module ) )
            {
                return type.cast( module );
            }
        }

        throw new IndyClientException( "Module not found: %s.", type.getName() );
    }

    public boolean hasModule( Class<?> type )
    {
        for ( final IndyClientModule module : moduleRegistry )
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
        final Set<IndyClientModule> standardModules = new HashSet<>();
        standardModules.add( new IndyStoresClientModule() );
        standardModules.add( new IndyContentClientModule() );
        standardModules.add( new IndySchedulerClientModule() );
        standardModules.add( new IndyStatsClientModule() );

        for ( final IndyClientModule module : standardModules )
        {
            setup( module );
        }
    }

    private void setup( IndyClientModule module )
    {
        module.setup( this, http );
        moduleRegistry.add( module );

        Iterable<Module> serMods = module.getSerializerModules();
        if ( serMods != null )
        {
            http.getObjectMapper().registerModules( serMods );
        }
    }

}
