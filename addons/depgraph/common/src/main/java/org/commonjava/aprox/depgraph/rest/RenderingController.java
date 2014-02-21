package org.commonjava.aprox.depgraph.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.commonjava.aprox.depgraph.util.ConfigDTOHelper;
import org.commonjava.aprox.depgraph.util.PresetParameterParser;
import org.commonjava.aprox.depgraph.util.RequestAdvisor;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.aprox.rest.util.ApplicationStatus;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;
import org.commonjava.maven.cartographer.agg.AggregatorConfig;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.ops.GraphRenderingOps;

@ApplicationScoped
public class RenderingController
{

    @Inject
    private GraphRenderingOps ops;

    @Inject
    private RequestAdvisor requestAdvisor;

    @Inject
    private ConfigDTOHelper configHelper;

    @Inject
    private PresetParameterParser presetParamParser;

    public String bomFor( final String groupId, final String artifactId, final String version, final Map<String, String[]> params,
                          final InputStream configStream )
        throws AproxWorkflowException
    {
        final AggregatorConfig config = configHelper.readAggregatorConfig( configStream );
        return bomFor( groupId, artifactId, version, params, config );
    }

    public String bomFor( final String groupId, final String artifactId, final String version, final Map<String, String[]> params,
                          final String listing )
        throws AproxWorkflowException
    {
        final AggregatorConfig config = configHelper.readAggregatorConfig( listing );
        return bomFor( groupId, artifactId, version, params, config );
    }

    public String bomFor( final String groupId, final String artifactId, final String version, final Map<String, String[]> params,
                          final AggregatorConfig config )
        throws AproxWorkflowException
    {
        try
        {
            final ProjectRelationshipFilter filter = requestAdvisor.createRelationshipFilter( params, presetParamParser.parse( params ) );

            final Model model = ops.generateBOM( new ProjectVersionRef( groupId, artifactId, version ), filter, config.getRoots() );

            final StringWriter writer = new StringWriter();
            new MavenXpp3Writer().write( writer, model );

            return writer.toString();
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST, "Failed to read list of GAVs from config stream (body): %s", e,
                                              e.getMessage() );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.SERVER_ERROR, "Failed to retrieve web for: %s. Reason: %s", e, config, e.getMessage() );
        }
    }

    public String dotfile( final String groupId, final String artifactId, final String version, final Map<String, String[]> params )
        throws AproxWorkflowException
    {
        //        final DiscoveryConfig discovery = createDiscoveryConfig( request, null, sourceFactory );
        final ProjectRelationshipFilter filter = requestAdvisor.createRelationshipFilter( params, presetParamParser.parse( params ) );
        try
        {
            final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );
            final String dotfile = ops.dotfile( ref, filter, ref );

            if ( dotfile != null )
            {
                return dotfile;
            }
            else
            {
                throw new AproxWorkflowException( ApplicationStatus.NOT_FOUND, "Cannot find graph: %s:%s:%s", groupId, artifactId, version );
            }

        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.SERVER_ERROR, "Failed to retrieve web for: %s:%s:%s. Reason: %s", e, groupId,
                                              artifactId, version, e.getMessage() );
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST, "Invalid version in request: '%s'. Reason: %s", e, version,
                                              e.getMessage() );
        }
    }

    public String depTree( final String groupId, final String artifactId, final String version, final DependencyScope scope,
                           final Map<String, String[]> params )
        throws AproxWorkflowException
    {
        try
        {

            final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );

            final ProjectRelationshipFilter filter = requestAdvisor.createRelationshipFilter( params, presetParamParser.parse( params ) );

            final String tree =
                ops.depTree( ref, filter, scope == null ? DependencyScope.runtime : scope, false,
                             Collections.<String, Set<ProjectVersionRef>> emptyMap() );

            if ( tree != null )
            {
                return tree;
            }
            else
            {
                throw new AproxWorkflowException( ApplicationStatus.NOT_FOUND, "Cannot find graph: %s:%s:%s", groupId, artifactId, version );
            }
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.SERVER_ERROR, "Failed to retrieve web for: %s:%s:%s. Reason: %s", e, groupId,
                                              artifactId, version, e.getMessage() );
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST, "Invalid version in request: '%s'. Reason: %s", e, version,
                                              e.getMessage() );
        }
    }
}
