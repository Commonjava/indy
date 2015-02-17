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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.depgraph.conf.AproxDepgraphConfig;
import org.commonjava.aprox.depgraph.dto.WebBomDTO;
import org.commonjava.aprox.depgraph.dto.WebOperationConfigDTO;
import org.commonjava.aprox.depgraph.util.ConfigDTOHelper;
import org.commonjava.aprox.depgraph.util.PresetParameterParser;
import org.commonjava.aprox.depgraph.util.RequestAdvisor;
import org.commonjava.aprox.util.ApplicationStatus;
import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.RelationshipGraphException;
import org.commonjava.maven.atlas.graph.RelationshipGraphFactory;
import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.mutate.GraphMutator;
import org.commonjava.maven.atlas.graph.mutate.ManagedDependencyMutator;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoGraphUtils;
import org.commonjava.maven.cartographer.dto.GraphCalculation.Type;
import org.commonjava.maven.cartographer.dto.GraphComposition;
import org.commonjava.maven.cartographer.dto.GraphDescription;
import org.commonjava.maven.cartographer.ops.GraphRenderingOps;
import org.commonjava.maven.cartographer.ops.ResolveOps;
import org.commonjava.maven.cartographer.preset.CommonPresetParameters;
import org.commonjava.maven.cartographer.preset.PresetSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
// FIXME: DTO Validations!!
public class RenderingController
{

    @Inject
    private GraphRenderingOps ops;

    @Inject
    private RequestAdvisor requestAdvisor;

    @Inject
    private ConfigDTOHelper configHelper;

    @Inject
    private PresetSelector presets;

    @Inject
    private AproxDepgraphConfig config;

    @Inject
    private PresetParameterParser presetParamParser;

    @Inject
    private ObjectMapper serializer;

    @Inject
    private ResolveOps resolveOps;

    @Inject
    private RelationshipGraphFactory graphFactory;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public File tree( final InputStream configStream )
        throws AproxWorkflowException
    {
        final WebOperationConfigDTO dto = configHelper.readWebOperationDTO( configStream );
        return tree( dto );
    }

    public File tree( final String json )
        throws AproxWorkflowException
    {
        final WebOperationConfigDTO dto = configHelper.readWebOperationDTO( json );
        return tree( dto );
    }

    private File tree( final WebOperationConfigDTO dto )
        throws AproxWorkflowException
    {
        final File workBasedir = config.getWorkBasedir();
        String dtoJson;
        try
        {
            dtoJson = serializer.writeValueAsString( dto );
        }
        catch ( final JsonProcessingException e )
        {
            throw new AproxWorkflowException( "Failed to serialize to JSON: %s", e, e.getMessage() );
        }

        final File out = new File( workBasedir, DigestUtils.md5Hex( dtoJson ) );
        workBasedir.mkdirs();

        final GraphComposition comp = resolve( dto );
        FileWriter w = null;
        try
        {
            w = new FileWriter( out );
            ops.depTree( dto.getWorkspaceId(), comp, false, new PrintWriter( w ) );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to generate dependency tree. Reason: {}", e, e.getMessage() );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( "Failed to open work file for caching output: {}. Reason: {}", e, out,
                                              e.getMessage() );
        }
        finally
        {
            IOUtils.closeQuietly( w );
        }

        return out;
    }

    @Deprecated
    public File tree( final String groupId, final String artifactId, final String version, final String workspaceId,
                      final DependencyScope scope, final Map<String, String[]> params )
        throws AproxWorkflowException
    {
        final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );

        final Map<String, Object> parsed = presetParamParser.parse( params );
        if ( !parsed.containsKey( CommonPresetParameters.SCOPE ) )
        {
            parsed.put( CommonPresetParameters.SCOPE, scope == null ? DependencyScope.runtime : scope );
        }

        final ProjectRelationshipFilter filter = requestAdvisor.createRelationshipFilter( params, parsed );

        final WebOperationConfigDTO dto = new WebOperationConfigDTO();
        dto.setWorkspaceId( workspaceId );

        final GraphDescription desc = new GraphDescription( filter, ref );
        dto.setGraphComposition( new GraphComposition( Type.ADD, Collections.singletonList( desc ) ) );

        return tree( dto );
    }

    @Deprecated
    public String bomFor( final String groupId, final String artifactId, final String version,
                          final String workspaceId, final Map<String, String[]> params, final InputStream configStream )
        throws AproxWorkflowException
    {
        final WebBomDTO config = configHelper.readBomDTO( configStream );
        return bomFor( groupId, artifactId, version, workspaceId, params, config );
    }

    @Deprecated
    public String bomFor( final String groupId, final String artifactId, final String version,
                          final String workspaceId, final Map<String, String[]> params, final String configJson )
        throws AproxWorkflowException
    {
        final WebBomDTO config = configHelper.readBomDTO( configJson );
        return bomFor( groupId, artifactId, version, workspaceId, params, config );
    }

    @Deprecated
    public String bomFor( final String groupId, final String artifactId, final String version,
                          final String workspaceId, final Map<String, String[]> params, final WebBomDTO config )
        throws AproxWorkflowException
    {
        final ProjectVersionRef pvr = new ProjectVersionRef( groupId, artifactId, version );
        config.setOutput( pvr );
        return bomFor( config );
    }

    public String bomFor( final InputStream configStream )
        throws AproxWorkflowException
    {
        final WebBomDTO config = configHelper.readBomDTO( configStream );
        return bomFor( config );
    }

    public String bomFor( final String configJson )
        throws AproxWorkflowException
    {
        final WebBomDTO config = configHelper.readBomDTO( configJson );
        return bomFor( config );
    }

    public String bomFor( final WebBomDTO config )
        throws AproxWorkflowException
    {
        try
        {
            final Model model = ops.generateBOM( config );

            final StringWriter writer = new StringWriter();
            new MavenXpp3Writer().write( writer, model );

            return writer.toString();
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(), "Failed to render BOM: {}", e,
                                              e.getMessage() );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.SERVER_ERROR.code(),
                                              "Failed to generate BOM for: {} using config: {}. Reason: {}", e, config,
                                              e.getMessage() );
        }
    }

    public String dotfile( final String groupId, final String artifactId, final String version,
                           final String workspaceId, final Map<String, String[]> params )
        throws AproxWorkflowException
    {
        //        final DiscoveryConfig discovery = createDiscoveryConfig( request, null, sourceFactory );
        final ProjectRelationshipFilter filter =
            requestAdvisor.createRelationshipFilter( params, presetParamParser.parse( params ) );

        final GraphMutator mutator = new ManagedDependencyMutator();

        RelationshipGraph graph = null;
        try
        {
            final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );

            graph = graphFactory.open( new ViewParams( workspaceId, filter, mutator, ref ), true );
            final String dotfile = ops.dotfile( ref, graph );

            if ( dotfile != null )
            {
                return dotfile;
            }
            else
            {
                throw new AproxWorkflowException( ApplicationStatus.NOT_FOUND.code(),
                                                  "Cannot find graph: {}:{}:{} in workspace: {}", groupId, artifactId,
                                                  version, workspaceId );
            }

        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.SERVER_ERROR.code(),
                                              "Failed to render DOT file for: {}:{}:{} in workspace: {}. Reason: {}",
                                              e, groupId, artifactId, version, workspaceId, e.getMessage(), e );
        }
        catch ( final RelationshipGraphException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.SERVER_ERROR.code(),
                                              "Failed to render DOT file for: {}:{}:{} in workspace: {}. Reason: {}",
                                              e, groupId, artifactId, version, workspaceId, e.getMessage(), e );
        }
        finally
        {
            CartoGraphUtils.closeGraphQuietly( graph );
        }
    }

    private GraphComposition resolve( final WebOperationConfigDTO dto )
        throws AproxWorkflowException
    {
        if ( dto == null )
        {
            logger.warn( "Repository archive configuration is missing." );
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(), "JSON configuration not supplied" );
        }

        dto.resolveFilters( presets, config.getDefaultWebFilterPreset() );

        if ( !dto.isValid() )
        {
            logger.warn( "Repository archive configuration is invalid: {}", dto );
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(), "Invalid configuration: {}", dto );
        }

        GraphComposition result;
        try
        {
            result = resolveOps.resolve( dto );
        }
        catch ( final CartoDataException e )
        {
            logger.error( String.format( "Failed to resolve repository contents for: %s. Reason: %s", dto,
                                         e.getMessage() ), e );
            throw new AproxWorkflowException( "Failed to resolve repository contents for: {}. Reason: {}", e, dto,
                                              e.getMessage() );
        }

        return result;
    }
}
