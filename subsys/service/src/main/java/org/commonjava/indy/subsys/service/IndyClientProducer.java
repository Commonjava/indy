/**
 * Copyright (C) 2020 Red Hat, Inc.
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
import org.commonjava.indy.client.core.module.IndyStoreQueryClientModule;
import org.commonjava.indy.client.core.module.IndyStoresClientModule;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.subsys.service.config.RepositoryServiceConfig;
import org.commonjava.indy.subsys.service.inject.ServiceClient;
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
    private RepositoryServiceConfig serviceConfig;

    protected IndyClientProducer()
    {
    }

    protected IndyClientProducer( final RepositoryServiceConfig serviceConfig )
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
            client = new Indy( config, new MemoryPasswordManager(), new IndyObjectMapper( Collections.emptySet() ),
                               modules.toArray( new IndyClientModule[0] ) );
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
