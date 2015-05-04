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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.depgraph.dto.ProjectListing;
import org.commonjava.aprox.depgraph.dto.ProjectRelationshipListing;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class ProjectController
{
    @Inject
    private GraphOps ops;

    @Inject
    private ObjectMapper serializer;

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

            return serializer.writeValueAsString( Collections.singletonMap( "items", Collections.singleton( error ) ) );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to lookup errors for: {} in: {}. Reason: {}", e,
                                              ref == null ? "all projects" : ref, params, e.getMessage() );
        }
        catch ( final JsonProcessingException e )
        {
            throw new AproxWorkflowException( "Failed to serialize to JSON: %s", e, e.getMessage() );
        }
    }

    public String list( final String groupIdPattern, final String artifactIdPattern, final String workspaceId )
        throws AproxWorkflowException
    {
        final ViewParams params = new ViewParams( workspaceId );

        try
        {
            final List<ProjectVersionRef> matching = ops.listProjects( groupIdPattern, artifactIdPattern, params );
            return matching == null ? null
                            : serializer.writeValueAsString( new ProjectListing<ProjectVersionRef>( matching ) );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException(
                                              "Failed to lookup project listing matching groupId pattern: '{}' and artifactId pattern: '{}'. Reason: {}",
                                              e, groupIdPattern, artifactIdPattern, e.getMessage() );
        }
        catch ( final JsonProcessingException e )
        {
            throw new AproxWorkflowException( "Failed to serialize to JSON: %s", e, e.getMessage() );
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

            return parent == null ? null : serializer.writeValueAsString( parent );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to lookup parent for: {}:{}:{}. Reason: {}", e, groupId,
                                              artifactId, version, e.getMessage() );
        }
        catch ( final JsonProcessingException e )
        {
            throw new AproxWorkflowException( "Failed to serialize to JSON: %s", e, e.getMessage() );
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

            return rels == null ? null
 : serializer.writeValueAsString( new ProjectRelationshipListing( rels ) );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to lookup dependencies for: {}:{}:{}. Reason: {}", e, groupId,
                                              artifactId, version, e.getMessage() );
        }
        catch ( final JsonProcessingException e )
        {
            throw new AproxWorkflowException( "Failed to serialize to JSON: %s", e, e.getMessage() );
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
            return rels == null ? null : serializer.writeValueAsString( new ProjectRelationshipListing( rels ) );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to lookup relationships of type: {} for: {}:{}:{}. Reason: {}",
                                              e, new JoinString( ", ", types ), groupId, artifactId, version,
                                              e.getMessage() );
        }
        catch ( final JsonProcessingException e )
        {
            throw new AproxWorkflowException( "Failed to serialize to JSON: %s", e, e.getMessage() );
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
            return rels == null ? null : serializer.writeValueAsString( new ProjectRelationshipListing( rels ) );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to lookup relationships specified by: {}:{}:{}. Reason: {}", e,
                                              groupId, artifactId, version, e.getMessage() );
        }
        catch ( final JsonProcessingException e )
        {
            throw new AproxWorkflowException( "Failed to serialize to JSON: %s", e, e.getMessage() );
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
            return rels == null ? null : serializer.writeValueAsString( new ProjectRelationshipListing( rels ) );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to lookup relationships of type: {} for: {}:{}:{}. Reason: {}",
                                              e, new JoinString( ", ", types ), groupId, artifactId, version,
                                              e.getMessage() );
        }
        catch ( final JsonProcessingException e )
        {
            throw new AproxWorkflowException( "Failed to serialize to JSON: %s", e, e.getMessage() );
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
            return rels == null ? null : serializer.writeValueAsString( new ProjectRelationshipListing( rels ) );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to lookup relationships specified by: {}:{}:{}. Reason: {}", e,
                                              groupId, artifactId, version, e.getMessage() );
        }
        catch ( final JsonProcessingException e )
        {
            throw new AproxWorkflowException( "Failed to serialize to JSON: %s", e, e.getMessage() );
        }
    }

}
