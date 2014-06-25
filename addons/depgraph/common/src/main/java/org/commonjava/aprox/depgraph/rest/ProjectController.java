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

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.depgraph.inject.DepgraphSpecific;
import org.commonjava.aprox.depgraph.util.PresetParameterParser;
import org.commonjava.aprox.depgraph.util.RequestAdvisor;
import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.atlas.graph.filter.DependencyOnlyFilter;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.mutate.NoOpGraphMutator;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.util.JoinString;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.ops.GraphOps;
import org.commonjava.web.json.model.Listing;
import org.commonjava.web.json.ser.JsonSerializer;

@ApplicationScoped
public class ProjectController
{
    @Inject
    private GraphOps ops;

    @Inject
    @DepgraphSpecific
    private JsonSerializer serializer;

    @Inject
    private RequestAdvisor requestAdvisor;

    @Inject
    private PresetParameterParser presetParamParser;

    public String errors( final String groupId, final String artifactId, final String version, final String workspaceId )
        throws AproxWorkflowException
    {
        final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );
        final ViewParams params = new ViewParams( workspaceId, ref );
        try
        {
            final String error = ops.getProjectError( ref, params );
            if ( error == null )
            {
                return null;
            }

            return serializer.toString( new Listing<String>( error ) );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to lookup errors for: {} in: {}. Reason: {}", e,
                                              ref == null ? "all projects" : ref, params, e.getMessage() );
        }
    }

    public String list( final String groupIdPattern, final String artifactIdPattern, final String workspaceId )
        throws AproxWorkflowException
    {
        final ViewParams params = new ViewParams( workspaceId );

        try
        {
            final List<ProjectVersionRef> matching = ops.listProjects( groupIdPattern, artifactIdPattern, params );
            return matching == null ? null : serializer.toString( new Listing<ProjectVersionRef>( matching ) );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException(
                                              "Failed to lookup project listing matching groupId pattern: '{}' and artifactId pattern: '{}'. Reason: {}",
                                              e, groupIdPattern, artifactIdPattern, e.getMessage() );
        }
    }

    public String parentOf( final String groupId, final String artifactId, final String version,
                            final String workspaceId )
        throws AproxWorkflowException
    {
        final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );
        final ViewParams params = new ViewParams( workspaceId, ref );

        try
        {
            final ProjectVersionRef parent = ops.getProjectParent( ref, params );

            return parent == null ? null : serializer.toString( parent );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to lookup parent for: {}:{}:{}. Reason: {}", e, groupId,
                                              artifactId, version, e.getMessage() );
        }
    }

    public String dependenciesOf( final String groupId, final String artifactId, final String version,
                                  final String workspaceId, final DependencyScope... scopes )
        throws AproxWorkflowException
    {
        final DependencyOnlyFilter filter = new DependencyOnlyFilter( false, true, true, scopes );
        final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );
        final ViewParams params = new ViewParams( workspaceId, filter, NoOpGraphMutator.INSTANCE, ref );

        try
        {
            final Set<ProjectRelationship<?>> rels =
                ops.getDirectRelationshipsFrom( ref, params, RelationshipType.DEPENDENCY );

            return rels == null ? null : serializer.toString( new Listing<ProjectRelationship<?>>( rels ) );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to lookup dependencies for: {}:{}:{}. Reason: {}", e, groupId,
                                              artifactId, version, e.getMessage() );
        }
    }

    public String relationshipsDeclaredBy( final String groupId, final String artifactId, final String version,
                                           final String workspaceId, final RelationshipType... types )
        throws AproxWorkflowException
    {
        final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );
        final ViewParams params = new ViewParams( workspaceId, ref );
        try
        {
            final Set<ProjectRelationship<?>> rels = ops.getDirectRelationshipsFrom( ref, params, types );
            return rels == null ? null : serializer.toString( new Listing<ProjectRelationship<?>>( rels ) );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to lookup relationships of type: {} for: {}:{}:{}. Reason: {}",
                                              e, new JoinString( ", ", types ), groupId, artifactId, version,
                                              e.getMessage() );
        }
    }

    public String relationshipsDeclaredBy( final String groupId, final String artifactId, final String version,
                                           final String workspaceId, final Map<String, String[]> filterParams )
        throws AproxWorkflowException
    {
        final ProjectRelationshipFilter filter =
            requestAdvisor.createRelationshipFilter( filterParams, presetParamParser.parse( filterParams ) );

        final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );
        final ViewParams params = new ViewParams( workspaceId, filter, NoOpGraphMutator.INSTANCE, ref );
        try
        {
            final Set<RelationshipType> types = filter.getAllowedTypes();
            final Set<ProjectRelationship<?>> rels =
                ops.getDirectRelationshipsFrom( ref, params, types.toArray( new RelationshipType[types.size()] ) );
            return rels == null ? null : serializer.toString( new Listing<ProjectRelationship<?>>( rels ) );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to lookup relationships specified by: {}:{}:{}. Reason: {}", e,
                                              groupId, artifactId, version, e.getMessage() );
        }
    }

    public String relationshipsTargeting( final String groupId, final String artifactId, final String version,
                                          final String workspaceId, final RelationshipType... types )
        throws AproxWorkflowException
    {
        final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );
        final ViewParams params = new ViewParams( workspaceId, ref );
        try
        {
            final Set<ProjectRelationship<?>> rels = ops.getDirectRelationshipsTo( ref, params, types );
            return rels == null ? null : serializer.toString( new Listing<ProjectRelationship<?>>( rels ) );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to lookup relationships of type: {} for: {}:{}:{}. Reason: {}",
                                              e, new JoinString( ", ", types ), groupId, artifactId, version,
                                              e.getMessage() );
        }
    }

    public String relationshipsTargeting( final String groupId, final String artifactId, final String version,
                                          final String workspaceId, final Map<String, String[]> filterParams )
        throws AproxWorkflowException
    {
        final ProjectRelationshipFilter filter =
            requestAdvisor.createRelationshipFilter( filterParams, presetParamParser.parse( filterParams ) );

        final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );
        final ViewParams params = new ViewParams( workspaceId, filter, NoOpGraphMutator.INSTANCE, ref );
        try
        {
            final Set<RelationshipType> types = filter.getAllowedTypes();
            final Set<ProjectRelationship<?>> rels =
                ops.getDirectRelationshipsTo( ref, params, types.toArray( new RelationshipType[types.size()] ) );
            return rels == null ? null : serializer.toString( new Listing<ProjectRelationship<?>>( rels ) );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to lookup relationships specified by: {}:{}:{}. Reason: {}", e,
                                              groupId, artifactId, version, e.getMessage() );
        }
    }

}
