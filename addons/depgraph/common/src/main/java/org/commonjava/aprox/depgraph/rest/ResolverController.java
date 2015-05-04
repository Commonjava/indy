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
package org.commonjava.aprox.depgraph.rest;

import static org.commonjava.aprox.model.util.HttpUtils.getBooleanParamWithDefault;
import static org.commonjava.aprox.model.util.HttpUtils.getLongParamWithDefault;

import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.depgraph.util.PresetParameterParser;
import org.commonjava.aprox.depgraph.util.RequestAdvisor;
import org.commonjava.aprox.util.ApplicationStatus;
import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.agg.AggregationOptions;
import org.commonjava.maven.cartographer.agg.DefaultAggregatorOptions;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.discover.DefaultDiscoveryConfig;
import org.commonjava.maven.cartographer.discover.DiscoverySourceManager;
import org.commonjava.maven.cartographer.ops.ResolveOps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class ResolverController
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ResolveOps ops;

    @Inject
    private ObjectMapper serializer;

    @Inject
    private DiscoverySourceManager sourceManager;

    @Inject
    private RequestAdvisor requestAdvisor;

    @Inject
    private PresetParameterParser presetParamParser;

    public String resolveGraph( final String from, final String groupId, final String artifactId, final String version,
                                final boolean recurse, final String workspaceId, final Map<String, String[]> params )
        throws AproxWorkflowException
    {
        URI source;
        try
        {
            source = sourceManager.createSourceURI( from );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Invalid source specification: {}. Reason: {}", e, from, e.getMessage() );
        }

        if ( source == null )
        {
            final String message =
                String.format( "Invalid source format: '%s'. Use the form: '%s' instead.", from,
                               sourceManager.getFormatHint() );
            logger.warn( message );
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(), message );
        }

        final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );
        final AggregationOptions options = createAggregationOptions( params, source );

        Set<ProjectVersionRef> resolved;
        ViewParams resolvedParams = null;
        try
        {
            resolvedParams = ops.resolve( workspaceId, options, ref );
            resolved = resolvedParams.getRoots();
            if ( resolved == null || resolved.isEmpty() )
            {
                resolved = Collections.singleton( ref );
            }

            return serializer.writeValueAsString( Collections.singletonMap( "resolvedTopLevelGAVs", resolved ) );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to resolve graph: {} from: {}. Reason: {}", e, ref, from,
                                              e.getMessage() );
        }
        catch ( final JsonProcessingException e )
        {
            throw new AproxWorkflowException( "Failed to serialize to JSON: %s", e, e.getMessage() );
        }
    }

    public void resolveIncomplete( final String from, final String groupId, final String artifactId,
                                   final String version, final boolean recurse, final String workspaceId,
                                   final Map<String, String[]> params )
        throws AproxWorkflowException
    {
        URI source;
        try
        {
            source = sourceManager.createSourceURI( from );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Invalid source specification: {}. Reason: {}", e, from, e.getMessage() );
        }

        if ( source == null )
        {
            final String message =
                String.format( "Invalid source format: '%s'. Use the form: '%s' instead.", from,
                               sourceManager.getFormatHint() );
            logger.warn( message );
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(), message );
        }

        final DefaultAggregatorOptions options = createAggregationOptions( params, source );
        options.setProcessIncompleteSubgraphs( true );

        final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );

        try
        {
            ops.resolve( workspaceId, options, ref );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to lookup incomplete subgraphs for: {} from: {}. Reason: {}", e,
                                              ref == null ? "all projects" : ref, from, e.getMessage() );
        }
    }

    private DefaultAggregatorOptions createAggregationOptions( final Map<String, String[]> params, final URI source )
    {
        final DefaultAggregatorOptions options = new DefaultAggregatorOptions();
        options.setFilter( requestAdvisor.createRelationshipFilter( params, presetParamParser.parse( params ) ) );

        final DefaultDiscoveryConfig dconf = new DefaultDiscoveryConfig( source );
        dconf.setEnabled( true );
        dconf.setTimeoutMillis( getLongParamWithDefault( params, "timeout", dconf.getTimeoutMillis() ) );

        options.setDiscoveryConfig( dconf );

        options.setProcessIncompleteSubgraphs( getBooleanParamWithDefault( params, "incomplete", true ) );
        options.setProcessVariableSubgraphs( getBooleanParamWithDefault( params, "variable", true ) );

        logger.debug( "AGGREGATOR OPTIONS:\n\n{}\n\n", options );

        return options;
    }

}
