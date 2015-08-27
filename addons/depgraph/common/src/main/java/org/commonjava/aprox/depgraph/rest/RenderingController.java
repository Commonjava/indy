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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.depgraph.conf.AproxDepgraphConfig;
import org.commonjava.aprox.depgraph.util.PresetParameterParser;
import org.commonjava.aprox.depgraph.util.RecipeHelper;
import org.commonjava.aprox.depgraph.util.RequestAdvisor;
import org.commonjava.aprox.util.ApplicationStatus;
import org.commonjava.cartographer.CartoDataException;
import org.commonjava.cartographer.CartoRequestException;
import org.commonjava.cartographer.graph.preset.CommonPresetParameters;
import org.commonjava.cartographer.ops.GraphRenderingOps;
import org.commonjava.cartographer.request.*;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.*;
import java.util.Collections;
import java.util.Map;

@ApplicationScoped
// FIXME: DTO Validations!!
public class RenderingController
{

    @Inject
    private GraphRenderingOps ops;

    @Inject
    private RequestAdvisor requestAdvisor;

    @Inject
    private AproxDepgraphConfig config;

    @Inject
    private RecipeHelper configHelper;

    @Inject
    private PresetParameterParser presetParamParser;

    @Inject
    private ObjectMapper serializer;

    public File tree( final InputStream configStream )
        throws AproxWorkflowException
    {
        final RepositoryContentRequest dto = configHelper.readRecipe( configStream, RepositoryContentRequest.class );
        return tree( dto );
    }

    public File tree( final String json )
        throws AproxWorkflowException
    {
        final RepositoryContentRequest dto = configHelper.readRecipe( json, RepositoryContentRequest.class );
        return tree( dto );
    }

    public File tree( final RepositoryContentRequest recipe )
        throws AproxWorkflowException
    {
        configHelper.setRecipeDefaults( recipe );

        final File workBasedir = config.getWorkBasedir();
        String dtoJson;
        try
        {
            dtoJson = serializer.writeValueAsString( recipe );
        }
        catch ( final JsonProcessingException e )
        {
            throw new AproxWorkflowException( "Failed to serialize to JSON: %s", e, e.getMessage() );
        }

        final File out = new File( workBasedir, DigestUtils.md5Hex( dtoJson ) );
        workBasedir.mkdirs();

        FileWriter w = null;
        try
        {
            w = new FileWriter( out );
            ops.depTree( recipe, false, new PrintWriter( w ) );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to generate dependency tree. Reason: {}", e, e.getMessage() );
        }
        catch ( CartoRequestException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(), "Invalid request: %s. Reason: %s", e,
                                              recipe, e.getMessage() );
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

    public File list( final RepositoryContentRequest recipe )
            throws AproxWorkflowException
    {
        configHelper.setRecipeDefaults( recipe );

        final File workBasedir = config.getWorkBasedir();
        String dtoJson;
        try
        {
            dtoJson = serializer.writeValueAsString( recipe );
        }
        catch ( final JsonProcessingException e )
        {
            throw new AproxWorkflowException( "Failed to serialize to JSON: %s", e, e.getMessage() );
        }

        final File out = new File( workBasedir, DigestUtils.md5Hex( dtoJson ) );
        workBasedir.mkdirs();

        FileWriter w = null;
        try
        {
            w = new FileWriter( out );
            ops.depList( recipe, new PrintWriter( w ) );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to generate dependency tree. Reason: {}", e, e.getMessage() );
        }
        catch ( CartoRequestException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(), "Invalid request: %s. Reason: %s", e,
                                              recipe, e.getMessage() );
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
        final ProjectVersionRef ref = new SimpleProjectVersionRef( groupId, artifactId, version );

        final Map<String, Object> parsed = presetParamParser.parse( params );
        if ( !parsed.containsKey( CommonPresetParameters.SCOPE ) )
        {
            parsed.put( CommonPresetParameters.SCOPE, scope == null ? DependencyScope.runtime : scope );
        }

        final ProjectRelationshipFilter filter = requestAdvisor.createRelationshipFilter( params, parsed );

        final RepositoryContentRequest dto = new RepositoryContentRequest();
        dto.setWorkspaceId( workspaceId );

        final GraphDescription desc = new GraphDescription( filter, ref );
        dto.setGraphComposition( new GraphComposition( GraphCalculationType.ADD, Collections.singletonList( desc ) ) );

        return tree( dto );
    }

    @Deprecated
    public String pomFor( final String groupId, final String artifactId, final String version,
                          final String workspaceId, final Map<String, String[]> params, final InputStream configStream )
        throws AproxWorkflowException
    {
        final PomRequest config = configHelper.readRecipe( configStream, PomRequest.class );
        return pomFor( groupId, artifactId, version, workspaceId, params, config );
    }

    @Deprecated
    public String pomFor( final String groupId, final String artifactId, final String version,
                          final String workspaceId, final Map<String, String[]> params, final String configJson )
        throws AproxWorkflowException
    {
        final PomRequest config = configHelper.readRecipe( configJson, PomRequest.class );
        return pomFor( groupId, artifactId, version, workspaceId, params, config );
    }

    @Deprecated
    public String pomFor( final String groupId, final String artifactId, final String version,
                          final String workspaceId, final Map<String, String[]> params, final PomRequest config )
        throws AproxWorkflowException
    {
        final ProjectVersionRef pvr = new SimpleProjectVersionRef( groupId, artifactId, version );
        config.setOutput( pvr );
        return pomFor( config );
    }

    public String pomFor( final InputStream configStream )
        throws AproxWorkflowException
    {
        final PomRequest config = configHelper.readRecipe( configStream, PomRequest.class );
        return pomFor( config );
    }

    public String pomFor( final String configJson )
        throws AproxWorkflowException
    {
        final PomRequest config = configHelper.readRecipe( configJson, PomRequest.class );
        return pomFor( config );
    }

    public String pomFor( final PomRequest recipe )
        throws AproxWorkflowException
    {
        configHelper.setRecipeDefaults( recipe );

        try
        {
            final Model model = ops.generatePOM( recipe );

            final StringWriter writer = new StringWriter();
            new MavenXpp3Writer().write( writer, model );

            return writer.toString();
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(), "Failed to render POM: {}", e,
                                              e.getMessage() );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.SERVER_ERROR.code(),
                                              "Failed to generate POM for: {} using config: {}. Reason: {}", e, recipe,
                                              e.getMessage() );
        }
        catch ( CartoRequestException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(), "Invalid request: %s. Reason: %s", e,
                                              recipe, e.getMessage() );
        }
    }

    public String dotfile( final InputStream configStream )
        throws AproxWorkflowException
    {
        final MultiRenderRequest config = configHelper.readRecipe( configStream, MultiRenderRequest.class );
        return dotfile( config );
    }

    public String dotfile( final String configJson )
        throws AproxWorkflowException
    {
        final MultiRenderRequest config = configHelper.readRecipe( configJson, MultiRenderRequest.class );
        return dotfile( config );
    }

    public String dotfile( final MultiRenderRequest recipe )
        throws AproxWorkflowException
    {
        configHelper.setRecipeDefaults( recipe );

        try
        {
            return ops.dotfile( recipe );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to render Graphviz dotfile for: %s. Reason: %s", e, recipe,
                                              e.getMessage() );
        }
        catch ( CartoRequestException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(), "Invalid request: %s. Reason: %s", e,
                                              recipe, e.getMessage() );
        }
    }

}
