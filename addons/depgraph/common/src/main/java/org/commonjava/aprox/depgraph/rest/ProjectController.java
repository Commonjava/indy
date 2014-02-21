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

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.depgraph.inject.DepgraphSpecific;
import org.commonjava.aprox.depgraph.util.PresetParameterParser;
import org.commonjava.aprox.depgraph.util.RequestAdvisor;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.maven.atlas.graph.filter.DependencyOnlyFilter;
import org.commonjava.maven.atlas.graph.filter.ExtensionOnlyFilter;
import org.commonjava.maven.atlas.graph.filter.PluginOnlyFilter;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
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

    public String errors( final String groupId, final String artifactId, final String version )
        throws AproxWorkflowException
    {
        final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );
        try
        {
            final Set<String> errors = ops.getProjectErrors( ref );
            return errors == null ? null : serializer.toString( new Listing<String>( errors ) );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to lookup errors for: %s. Reason: %s", e, ref == null ? "all projects" : ref, e.getMessage() );
        }
    }

    public String list( final String groupIdPattern, final String artifactIdPattern )
        throws AproxWorkflowException
    {
        try
        {
            final List<ProjectVersionRef> matching = ops.listProjects( groupIdPattern, artifactIdPattern );
            return matching == null ? null : serializer.toString( new Listing<ProjectVersionRef>( matching ) );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException(
                                              "Failed to lookup project listing matching groupId pattern: '%s' and artifactId pattern: '%s'. Reason: %s",
                                              e, groupIdPattern, artifactIdPattern, e.getMessage() );
        }
    }

    public String parentOf( final String groupId, final String artifactId, final String version )
        throws AproxWorkflowException
    {
        try
        {
            final ProjectVersionRef parent = ops.getProjectParent( new ProjectVersionRef( groupId, artifactId, version ) );
            return parent == null ? null : serializer.toString( parent );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to lookup parent for: %s:%s:%s. Reason: %s", e, groupId, artifactId, version, e.getMessage() );
        }
    }

    public String dependenciesOf( final String groupId, final String artifactId, final String version, final DependencyScope... scopes )
        throws AproxWorkflowException
    {
        final DependencyOnlyFilter filter = new DependencyOnlyFilter( false, true, true, scopes );
        final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );
        try
        {
            final Set<ProjectRelationship<?>> rels = ops.getDirectRelationshipsFrom( ref, filter );
            return rels == null ? null : serializer.toString( new Listing<ProjectRelationship<?>>( rels ) );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to lookup dependencies for: %s:%s:%s. Reason: %s", e, groupId, artifactId, version,
                                              e.getMessage() );
        }
    }

    public String pluginsOf( final String groupId, final String artifactId, final String version )
        throws AproxWorkflowException
    {
        final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );
        try
        {
            final Set<ProjectRelationship<?>> rels = ops.getDirectRelationshipsFrom( ref, new PluginOnlyFilter() );
            return rels == null ? null : serializer.toString( new Listing<ProjectRelationship<?>>( rels ) );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to lookup plugins for: %s:%s:%s. Reason: %s", e, groupId, artifactId, version, e.getMessage() );
        }
    }

    public String extensionsOf( final String groupId, final String artifactId, final String version )
        throws AproxWorkflowException
    {
        final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );
        try
        {
            final Set<ProjectRelationship<?>> rels = ops.getDirectRelationshipsFrom( ref, new ExtensionOnlyFilter() );
            return rels == null ? null : serializer.toString( new Listing<ProjectRelationship<?>>( rels ) );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to lookup extensions for: %s:%s:%s. Reason: %s", e, groupId, artifactId, version,
                                              e.getMessage() );
        }
    }

    public String relationshipsSpecifiedBy( final String groupId, final String artifactId, final String version, final Map<String, String[]> params )
        throws AproxWorkflowException
    {
        final ProjectRelationshipFilter filter = requestAdvisor.createRelationshipFilter( params, presetParamParser.parse( params ) );

        final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );
        try
        {
            final Set<ProjectRelationship<?>> rels = ops.getDirectRelationshipsFrom( ref, filter );
            return rels == null ? null : serializer.toString( new Listing<ProjectRelationship<?>>( rels ) );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to lookup relationships specified by: %s:%s:%s. Reason: %s", e, groupId, artifactId, version,
                                              e.getMessage() );
        }
    }

    public String relationshipsTargeting( final String groupId, final String artifactId, final String version, final Map<String, String[]> params )
        throws AproxWorkflowException
    {
        final ProjectRelationshipFilter filter = requestAdvisor.createRelationshipFilter( params, presetParamParser.parse( params ) );
        final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );
        try
        {
            final Set<ProjectRelationship<?>> rels = ops.getDirectRelationshipsTo( ref, filter );
            return rels == null ? null : serializer.toString( new Listing<ProjectRelationship<?>>( rels ) );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to lookup relationships targeting: %s:%s:%s. Reason: %s", e, groupId, artifactId, version,
                                              e.getMessage() );
        }
    }

}
