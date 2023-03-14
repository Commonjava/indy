/**
 * Copyright (C) 2011-2022 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.subsys.service;

import org.commonjava.indy.client.core.Indy;
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.client.core.auth.IndyClientAuthenticator;
import org.commonjava.indy.client.core.module.IndyStoreQueryClientModule;
import org.commonjava.indy.client.core.module.IndyStoresClientModule;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.subsys.trace.config.IndyTraceConfiguration;
import org.commonjava.indy.subsys.service.config.RepositoryServiceConfig;
import org.commonjava.indy.subsys.service.inject.ServiceClient;
import org.commonjava.indy.subsys.service.keycloak.KeycloakTokenAuthenticator;
import org.commonjava.util.jhttpc.auth.MemoryPasswordManager;
import org.commonjava.util.jhttpc.model.SiteConfig;
import org.commonjava.util.jhttpc.model.SiteConfigBuilder;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

@ApplicationScoped
public class IndyClientProducer
{
    private Indy client;

    @Inject
    RepositoryServiceConfig serviceConfig;

    @Inject
    IndyTraceConfiguration indyTraceConfig;

    @SuppressWarnings( "unused" )

    protected IndyClientProducer()
    {
    }

    public IndyClientProducer( final RepositoryServiceConfig serviceConfig )
    {
        // For unit testing
        this.serviceConfig = serviceConfig;
    }

    @PostConstruct
    public void init()
    {
        SiteConfig config = new SiteConfigBuilder( "indy", serviceConfig.getServiceUrl() ).withRequestTimeoutSeconds(
                serviceConfig.getRequestTimeout() ).build();
        Collection<IndyClientModule> modules =
                Arrays.asList( new IndyStoresClientModule(), new IndyStoreQueryClientModule() );

        try
        {
            final Indy.Builder builder = Indy.builder()
                                             .setLocation( config )
                                             .setObjectMapper( new IndyObjectMapper( Collections.emptySet() ) )
                                             .setExistedTraceConfig( indyTraceConfig )
                                             .setMdcCopyMappings( Collections.emptyMap() )
                                             .setModules( modules.toArray( new IndyClientModule[0] ) );
            if ( serviceConfig.isAuthEnabled() )
            {
                IndyClientAuthenticator authenticator =
                        new KeycloakTokenAuthenticator( serviceConfig.getKeycloakAuthUrl(),
                                                        serviceConfig.getKeycloakAuthRealm(),
                                                        serviceConfig.getKeycloakClientId(),
                                                        serviceConfig.getKeycloakClientSecret() );
                client = builder.setAuthenticator( authenticator ).build();
            }
            else
            {
                client = builder.setPasswordManager( new MemoryPasswordManager() ).build();
            }

        }
        catch ( IndyClientException e )
        {
            throw new RuntimeException( e );
        }
    }

    @Produces
    @ServiceClient
    public Indy getClient()
    {
        if ( client == null )
        {
            init();
        }
        return client;
    }
}
