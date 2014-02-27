/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.depgraph.rest;

import static org.commonjava.aprox.rest.util.RequestUtils.getBooleanParamWithDefault;
import static org.commonjava.aprox.rest.util.RequestUtils.getLongParamWithDefault;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.depgraph.inject.DepgraphSpecific;
import org.commonjava.aprox.depgraph.util.PresetParameterParser;
import org.commonjava.aprox.depgraph.util.RequestAdvisor;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.aprox.rest.util.ApplicationStatus;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.agg.AggregationOptions;
import org.commonjava.maven.cartographer.agg.DefaultAggregatorOptions;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.discover.DefaultDiscoveryConfig;
import org.commonjava.maven.cartographer.discover.DiscoverySourceManager;
import org.commonjava.maven.cartographer.ops.ResolveOps;
import org.commonjava.web.json.ser.JsonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ResolverController
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ResolveOps ops;

    @Inject
    @DepgraphSpecific
    private JsonSerializer serializer;

    @Inject
    private DiscoverySourceManager sourceManager;

    @Inject
    private RequestAdvisor requestAdvisor;

    @Inject
    private PresetParameterParser presetParamParser;

    public String resolveGraph( final String from, final String groupId, final String artifactId, final String version, final boolean recurse,
                                final Map<String, String[]> params )
        throws AproxWorkflowException
    {
        final URI source = sourceManager.createSourceURI( from );
        if ( source == null )
        {
            final String message = String.format( "Invalid source format: '%s'. Use the form: '%s' instead.", from, sourceManager.getFormatHint() );
            logger.warn( message );
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST, message );
        }

        final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );
        final AggregationOptions options = createAggregationOptions( params, source );

        List<ProjectVersionRef> resolved;
        try
        {
            resolved = ops.resolve( from, options, ref );
            if ( resolved == null || resolved.isEmpty() )
            {
                resolved = Collections.singletonList( ref );
            }

            return serializer.toString( Collections.singletonMap( "resolvedTopLevelGAVs", resolved ) );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to resolve graph: {} from: {}. Reason: {}", e, ref, from, e.getMessage() );
        }
    }

    public String resolveIncomplete( final String from, final String groupId, final String artifactId, final String version, final boolean recurse,
                                     final Map<String, String[]> params )
        throws AproxWorkflowException
    {
        final URI source = sourceManager.createSourceURI( from );
        if ( source == null )
        {
            final String message = String.format( "Invalid source format: '%s'. Use the form: '%s' instead.", from, sourceManager.getFormatHint() );
            logger.warn( message );
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST, message );
        }

        final DefaultAggregatorOptions options = createAggregationOptions( params, source );
        options.setProcessIncompleteSubgraphs( true );

        final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );

        try
        {
            final List<ProjectVersionRef> failed = ops.resolve( from, options, ref );

            return serializer.toString( Collections.singletonMap( "failures", failed ) );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to lookup incomplete subgraphs for: {} from: {}. Reason: {}", e, ref == null ? "all projects"
                            : ref, from, e.getMessage() );
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
