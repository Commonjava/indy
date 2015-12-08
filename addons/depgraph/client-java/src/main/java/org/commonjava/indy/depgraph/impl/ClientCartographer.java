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
package org.commonjava.indy.depgraph.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.commonjava.indy.client.core.Indy;
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.client.core.module.IndyRawObjectMapperModule;
import org.commonjava.indy.depgraph.client.DepgraphIndyClientModule;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.cartographer.Cartographer;
import org.commonjava.cartographer.ops.*;
import org.commonjava.cartographer.request.AbstractGraphRequest;
import org.commonjava.cartographer.request.GraphAnalysisRequest;
import org.commonjava.cartographer.request.MultiGraphRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jdcasey on 8/17/15.
 */
public class ClientCartographer
        implements Cartographer
{

    private boolean internalClient;

    private Indy client;

    private IndyObjectMapper objectMapper;

    private DepgraphIndyClientModule module;

    private Map<String, String> sourceAliases;

    public ClientCartographer( String baseUrl )
            throws IndyClientException
    {
        this.module = new DepgraphIndyClientModule();
        this.client = new Indy( baseUrl, module, new IndyRawObjectMapperModule() ).connect();
        this.objectMapper = client.module( IndyRawObjectMapperModule.class ).getObjectMapper();
        this.internalClient = true;
    }

    public ClientCartographer( Indy client )
    {
        this.client = client;
        this.internalClient = false;
        this.module = lookupOrUse( client, new DepgraphIndyClientModule() );
        this.objectMapper = lookupOrUse( client, new IndyRawObjectMapperModule() ).getObjectMapper();
    }

    public synchronized ClientCartographer setSourceAliases( Map<String, String> sourceAliases )
    {
        if ( sourceAliases != null )
        {
            this.sourceAliases = sourceAliases;
        }
        return this;
    }

    public synchronized ClientCartographer setSourceAlias( String alias, String source )
    {
        if ( sourceAliases == null )
        {
            sourceAliases = new HashMap<>();
        }

        sourceAliases.put(alias, source );
        return this;
    }

    private <T extends IndyClientModule> T lookupOrUse( Indy client, T external )
    {
        T result = external;
        Class<T> cls = (Class<T>) external.getClass();
        if ( client.hasModule( cls ) )
        {
            try
            {
                result = client.module( cls );
            }
            catch ( IndyClientException e )
            {
                // should not be possible.
                Logger logger = LoggerFactory.getLogger( getClass() );
                logger.error( "Failed to lookup " + external.getClass().getSimpleName(),
                              ", the Indy client says is registered!", e );
            }
        }
        else
        {
            client.setupExternal( external );
        }

        return result;
    }

    <T extends AbstractGraphRequest> T normalizeRequest( T request )
    {
        request.setSource( deAlias( request.getSource() ) );
        return request;
    }

    <T extends GraphAnalysisRequest> T normalizeRequests( T request )
    {
        for ( MultiGraphRequest req: request.getGraphRequests() )
        {
            req.setSource( deAlias( req.getSource() ) );
        }
        return request;
    }

    String deAlias( String source )
    {
        if ( sourceAliases != null )
        {
            String deref = sourceAliases.get( source );
            if ( deref != null )
            {
                return deref;
            }
        }

        return source;
    }

    @Override
    public ObjectMapper getObjectMapper()
    {
        return objectMapper;
    }

    @Override
    public CalculationOps getCalculator()
    {
        return new ClientCalculatorOps( this, module );
    }

    @Override
    public GraphOps getGrapher()
    {
        return new ClientGraphOps( this, module );
    }

    @Override
    public GraphRenderingOps getRenderer()
    {
        return new ClientGraphRenderingOps( this, module );
    }

    @Override
    public MetadataOps getMetadata()
    {
        return new ClientMetadataOps( this, module );
    }

    @Override
    public ResolveOps getResolver()
    {
        return new ClientResolverOps( this, module );
    }

    @Override
    public void close()
    {
        if ( internalClient )
        {
            client.close();
        }
    }
}
