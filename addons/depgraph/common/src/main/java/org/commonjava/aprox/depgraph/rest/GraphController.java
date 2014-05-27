/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.depgraph.rest;

import static org.commonjava.maven.atlas.ident.util.IdentityUtils.projectVersion;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.depgraph.inject.DepgraphSpecific;
import org.commonjava.aprox.depgraph.util.PresetParameterParser;
import org.commonjava.aprox.depgraph.util.RequestAdvisor;
import org.commonjava.aprox.util.ApplicationStatus;
import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.mutate.ManagedDependencyMutator;
import org.commonjava.maven.atlas.graph.traverse.model.BuildOrder;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.dto.GraphExport;
import org.commonjava.maven.cartographer.ops.GraphOps;
import org.commonjava.web.json.model.Listing;
import org.commonjava.web.json.ser.JsonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class GraphController
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private GraphOps ops;

    @Inject
    @DepgraphSpecific
    private JsonSerializer serializer;

    @Inject
    private RequestAdvisor requestAdvisor;

    @Inject
    private PresetParameterParser presetParamParser;

    public void reindex( final String gav, final String workspaceId )
        throws AproxWorkflowException
    {
        final ViewParams params = new ViewParams( workspaceId );
        ProjectVersionRef ref = null;
        try
        {
            if ( gav != null )
            {
                ref = projectVersion( gav );
            }

            if ( ref != null )
            {
                ops.reindex( ref, params );
            }
            else
            {
                ops.reindexAll( params );
            }
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to reindex: {}. Reason: {}", e, ref == null ? "all projects"
                            : ref, e.getMessage() );
        }
    }

    public String errors( final String gav, final String workspaceId )
        throws AproxWorkflowException
    {
        final ViewParams params = new ViewParams( workspaceId );
        ProjectVersionRef ref = null;
        try
        {
            if ( gav != null )
            {
                ref = projectVersion( gav );
            }

            Map<ProjectVersionRef, Throwable> errors;
            if ( ref != null )
            {
                logger.debug( "Retrieving project errors in graph: {}", ref );
                errors = Collections.singletonMap( ref, ops.getProjectError( ref, params ) );
            }
            else
            {
                logger.debug( "Retrieving ALL project errors" );
                errors = ops.getAllProjectErrors( params );
            }

            return errors == null ? null : serializer.toString( errors );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to lookup resolution errors for: {}. Reason: {}", e,
                                              ref == null ? "all projects" : ref, e.getMessage() );
        }
    }

    public String incomplete( final String gav, final String workspaceId, final Map<String, String[]> params )
        throws AproxWorkflowException
    {
        final ProjectVersionRef ref = gav == null ? null : projectVersion( gav );

        try
        {
            final ProjectRelationshipFilter filter =
                requestAdvisor.createRelationshipFilter( params, presetParamParser.parse( params ) );

            ViewParams viewParams;
            if ( ref == null )
            {
                viewParams = new ViewParams( workspaceId, filter, new ManagedDependencyMutator() );
            }
            else
            {
                viewParams = new ViewParams( workspaceId, filter, new ManagedDependencyMutator(), ref );
            }

            final Set<ProjectVersionRef> result = ops.getIncomplete( viewParams );

            return result == null ? null : serializer.toString( new Listing<ProjectVersionRef>( result ) );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to lookup incomplete subgraphs for: {}. Reason: {}", e,
                                              ref == null ? "all projects" : ref, e.getMessage() );
        }
    }

    public String variable( final String gav, final String workspaceId, final Map<String, String[]> params )
        throws AproxWorkflowException
    {
        final ProjectVersionRef ref = gav == null ? null : projectVersion( gav );

        try
        {
            final ProjectRelationshipFilter filter =
                requestAdvisor.createRelationshipFilter( params, presetParamParser.parse( params ) );

            ViewParams viewParams;
            if ( ref == null )
            {
                viewParams = new ViewParams( workspaceId, filter, new ManagedDependencyMutator() );
            }
            else
            {
                viewParams = new ViewParams( workspaceId, filter, new ManagedDependencyMutator(), ref );
            }

            final Set<ProjectVersionRef> result = ops.getVariable( viewParams );

            return result == null ? null : serializer.toString( new Listing<ProjectVersionRef>( result ) );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to lookup variable subgraphs for: {}. Reason: {}", e,
                                              ref == null ? "all projects" : ref, e.getMessage() );
        }
    }

    public String ancestryOf( final String groupId, final String artifactId, final String version,
                              final String workspaceId )
        throws AproxWorkflowException
    {
        final ProjectVersionRef root = new ProjectVersionRef( groupId, artifactId, version );
        final ViewParams params = new ViewParams( workspaceId, root );
        try
        {
            final List<ProjectVersionRef> ancestry = ops.getAncestry( root, params );

            return ancestry == null ? null : serializer.toString( new Listing<ProjectVersionRef>( ancestry ) );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to lookup ancestry for: {}:{}:{}. Reason: {}", e, groupId,
                                              artifactId, version, e.getMessage() );
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST,
                                              "Invalid version in request: '{}'. Reason: {}", e, version,
                                              e.getMessage() );
        }
    }

    public String buildOrder( final String groupId, final String artifactId, final String version,
                              final String workspaceId, final Map<String, String[]> filterParams )
        throws AproxWorkflowException
    {
        //        final DiscoveryConfig discovery = createDiscoveryConfig( request, null, sourceFactory );
        final ProjectRelationshipFilter filter =
            requestAdvisor.createRelationshipFilter( filterParams, presetParamParser.parse( filterParams ) );

        try
        {
            final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );

            final ViewParams params = new ViewParams( workspaceId, filter, new ManagedDependencyMutator(), ref );

            final BuildOrder buildOrder = ops.getBuildOrder( ref, params );

            return buildOrder == null ? null : serializer.toString( buildOrder );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to lookup project graph for: {}:{}:{}. Reason: {}", e, groupId,
                                              artifactId, version, e.getMessage() );
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST,
                                              "Invalid version in request: '{}'. Reason: {}", e, version,
                                              e.getMessage() );
        }
    }

    public String projectGraph( final String groupId, final String artifactId, final String version,
                                final String workspaceId, final Map<String, String[]> filterParams )
        throws AproxWorkflowException
    {
        try
        {
            //            final DiscoveryConfig discovery = createDiscoveryConfig( request, null, sourceFactory );
            final ProjectRelationshipFilter filter =
                requestAdvisor.createRelationshipFilter( filterParams, presetParamParser.parse( filterParams ) );

            final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );

            final ViewParams params = new ViewParams( workspaceId, filter, new ManagedDependencyMutator(), ref );
            final GraphExport graph = ops.exportGraph( params );

            if ( graph != null )
            {
                return serializer.toString( graph );
            }
            else
            {
                throw new AproxWorkflowException( ApplicationStatus.NOT_FOUND, "Could not find graph: {}", ref );
            }
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to lookup project graph for: {}:{}:{}. Reason: {}", e, groupId,
                                              artifactId, version, e.getMessage() );
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST,
                                              "Invalid version in request: '{}'. Reason: {}", e, version,
                                              e.getMessage() );
        }
    }

}
