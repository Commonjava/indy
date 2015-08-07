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
import org.commonjava.aprox.depgraph.util.PresetParameterParser;
import org.commonjava.aprox.depgraph.util.RecipeHelper;
import org.commonjava.aprox.depgraph.util.RequestAdvisor;
import org.commonjava.aprox.util.ApplicationStatus;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.dto.GraphCalculation.Type;
import org.commonjava.maven.cartographer.dto.GraphComposition;
import org.commonjava.maven.cartographer.dto.GraphDescription;
import org.commonjava.maven.cartographer.ops.GraphRenderingOps;
import org.commonjava.maven.cartographer.preset.CommonPresetParameters;
import org.commonjava.maven.cartographer.recipe.MultiRenderRecipe;
import org.commonjava.maven.cartographer.recipe.PomRecipe;
import org.commonjava.maven.cartographer.recipe.RepositoryContentRecipe;

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
        final RepositoryContentRecipe dto = configHelper.readRecipe( configStream, RepositoryContentRecipe.class );
        return tree( dto );
    }

    public File tree( final String json )
        throws AproxWorkflowException
    {
        final RepositoryContentRecipe dto = configHelper.readRecipe( json, RepositoryContentRecipe.class );
        return tree( dto );
    }

    public File tree( final RepositoryContentRecipe dto )
        throws AproxWorkflowException
    {
        configHelper.setRecipeDefaults( dto );

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

        FileWriter w = null;
        try
        {
            w = new FileWriter( out );
            ops.depTree( dto, false, new PrintWriter( w ) );
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

        final RepositoryContentRecipe dto = new RepositoryContentRecipe();
        dto.setWorkspaceId( workspaceId );

        final GraphDescription desc = new GraphDescription( filter, ref );
        dto.setGraphComposition( new GraphComposition( Type.ADD, Collections.singletonList( desc ) ) );

        return tree( dto );
    }

    @Deprecated
    public String pomFor( final String groupId, final String artifactId, final String version,
                          final String workspaceId, final Map<String, String[]> params, final InputStream configStream )
        throws AproxWorkflowException
    {
        final PomRecipe config = configHelper.readRecipe( configStream, PomRecipe.class );
        return pomFor( groupId, artifactId, version, workspaceId, params, config );
    }

    @Deprecated
    public String pomFor( final String groupId, final String artifactId, final String version,
                          final String workspaceId, final Map<String, String[]> params, final String configJson )
        throws AproxWorkflowException
    {
        final PomRecipe config = configHelper.readRecipe( configJson, PomRecipe.class );
        return pomFor( groupId, artifactId, version, workspaceId, params, config );
    }

    @Deprecated
    public String pomFor( final String groupId, final String artifactId, final String version,
                          final String workspaceId, final Map<String, String[]> params, final PomRecipe config )
        throws AproxWorkflowException
    {
        final ProjectVersionRef pvr = new ProjectVersionRef( groupId, artifactId, version );
        config.setOutput( pvr );
        return pomFor( config );
    }

    public String pomFor( final InputStream configStream )
        throws AproxWorkflowException
    {
        final PomRecipe config = configHelper.readRecipe( configStream, PomRecipe.class );
        return pomFor( config );
    }

    public String pomFor( final String configJson )
        throws AproxWorkflowException
    {
        final PomRecipe config = configHelper.readRecipe( configJson, PomRecipe.class );
        return pomFor( config );
    }

    public String pomFor( final PomRecipe config )
        throws AproxWorkflowException
    {
        configHelper.setRecipeDefaults( config );

        try
        {
            final Model model = ops.generatePOM( config );

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
                                              "Failed to generate POM for: {} using config: {}. Reason: {}", e, config,
                                              e.getMessage() );
        }
    }

    public String dotfile( final InputStream configStream )
        throws AproxWorkflowException
    {
        final MultiRenderRecipe config = configHelper.readRecipe( configStream, MultiRenderRecipe.class );
        return dotfile( config );
    }

    public String dotfile( final String configJson )
        throws AproxWorkflowException
    {
        final MultiRenderRecipe config = configHelper.readRecipe( configJson, MultiRenderRecipe.class );
        return dotfile( config );
    }

    public String dotfile( final MultiRenderRecipe recipe )
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
    }

}
