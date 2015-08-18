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
package org.commonjava.aprox.depgraph.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.commonjava.aprox.client.core.Aprox;
import org.commonjava.aprox.client.core.AproxClientException;
import org.commonjava.aprox.client.core.AproxClientModule;
import org.commonjava.aprox.client.core.module.AproxRawObjectMapperModule;
import org.commonjava.aprox.depgraph.client.DepgraphAproxClientModule;
import org.commonjava.aprox.model.core.io.AproxObjectMapper;
import org.commonjava.cartographer.CartoDataException;
import org.commonjava.cartographer.Cartographer;
import org.commonjava.cartographer.ops.*;
import org.commonjava.cartographer.request.AbstractGraphRequest;
import org.commonjava.cartographer.request.GraphAnalysisRequest;
import org.commonjava.cartographer.request.MultiGraphRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by jdcasey on 8/17/15.
 */
public class ClientCartographer
        implements Cartographer
{

    private boolean internalClient;

    private Aprox client;

    private AproxObjectMapper objectMapper;

    private DepgraphAproxClientModule module;

    private Map<String, String> sourceAliases;

    public ClientCartographer( String baseUrl )
            throws AproxClientException
    {
        this.module = new DepgraphAproxClientModule();
        this.client = new Aprox( baseUrl, module, new AproxRawObjectMapperModule() ).connect();
        this.objectMapper = client.module( AproxRawObjectMapperModule.class ).getObjectMapper();
        this.internalClient = true;
    }

    public ClientCartographer( Aprox client )
    {
        this.client = client;
        this.internalClient = false;
        this.module = lookupOrUse( client, new DepgraphAproxClientModule() );
        this.objectMapper = lookupOrUse( client, new AproxRawObjectMapperModule() ).getObjectMapper();
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

    private <T extends AproxClientModule> T lookupOrUse( Aprox client, T external )
    {
        T result = external;
        Class<T> cls = (Class<T>) external.getClass();
        if ( client.hasModule( cls ) )
        {
            try
            {
                result = client.module( cls );
            }
            catch ( AproxClientException e )
            {
                // should not be possible.
                Logger logger = LoggerFactory.getLogger( getClass() );
                logger.error( "Failed to lookup " + external.getClass().getSimpleName(),
                              ", the AProx client says is registered!", e );
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
