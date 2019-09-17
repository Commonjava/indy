/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.hostedbyarc.bind.jaxrs;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.bind.jaxrs.IndyResources;
import org.commonjava.indy.bind.jaxrs.SecurityManager;
import org.commonjava.indy.bind.jaxrs.util.REST;
import org.commonjava.indy.bind.jaxrs.util.ResponseHelper;
import org.commonjava.indy.core.ctl.AdminController;
import org.commonjava.indy.hostedbyarc.HostedByArchiveManager;
import org.commonjava.indy.hostedbyarc.config.HostedByArchiveConfig;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.util.ApplicationContent;
import org.commonjava.indy.util.ApplicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.io.InputStream;
import java.net.URI;

import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;

@Api( value = "Hosted by archive", description = "Create a new maven hosted store by zip file" )
@Path( "/api/admin/stores/maven/hosted/{name}/compressed-content" )
@ApplicationScoped
@REST
public class IndyHostedByArchiveResource
        implements IndyResources
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private HostedByArchiveManager hostedByArchiveManager;

    @Inject
    private AdminController adminController;

    @Inject
    private SecurityManager securityManager;

    @Inject
    private HostedByArchiveConfig config;

    @Inject
    private IndyObjectMapper objectMapper;

    @Inject
    private ResponseHelper responseHelper;

    @ApiOperation( "Create a new maven hosted store by a zip file" )
    @ApiResponses( { @ApiResponse( code = 201, response = ArtifactStore.class, message = "The store was created" ),
                           @ApiResponse( code = 409,
                                         message = "A store with the specified type and name already exists" ) } )
    @ApiImplicitParams( { @ApiImplicitParam( paramType = "body", name = "body", required = true,
                                             dataType = "org.commonjava.indy.model.core.ArtifactStore",
                                             value = "The artifact store definition JSON" ) } )
    @POST
    @Consumes( ApplicationContent.application_zip )
    @Produces( ApplicationContent.application_json )
    public Response postCreateHostedByZip( final @PathParam( "name" ) String name, final @Context UriInfo uriInfo,
                                           final @QueryParam( "pathPrefixToIgnore" ) String ignorePathPrefix,
                                           final InputStream fileInputStream, final @Context HttpServletRequest request,
                                           final @Context SecurityContext securityContext )
    {
        return createHostedByZip( name, uriInfo, ignorePathPrefix, fileInputStream, request, securityContext );
    }

    @ApiOperation( "Create a new maven hosted store by a zip file" )
    @ApiResponses( { @ApiResponse( code = 201, response = ArtifactStore.class, message = "The store was created" ),
                           @ApiResponse( code = 409,
                                         message = "A store with the specified type and name already exists" ) } )
    @ApiImplicitParams( { @ApiImplicitParam( paramType = "body", name = "body", required = true,
                                             dataType = "org.commonjava.indy.model.core.ArtifactStore",
                                             value = "The artifact store definition JSON" ) } )
    @PUT
    @Consumes( ApplicationContent.application_zip )
    @Produces( ApplicationContent.application_json )
    public Response putCreateHostedByZip( final @PathParam( "name" ) String name, final @Context UriInfo uriInfo,
                                          final @QueryParam( "pathPrefixToIgnore" ) String ignorePathPrefix,
                                          final InputStream fileInput, final @Context HttpServletRequest request,
                                          final @Context SecurityContext securityContext )
    {
        return createHostedByZip( name, uriInfo, ignorePathPrefix, fileInput, request, securityContext );
    }

    private Response createHostedByZip( final String name, final UriInfo uriInfo, final String ignorePathPrefix,
                                        final InputStream fileInput, final HttpServletRequest request,
                                        final SecurityContext securityContext )
    {
        if ( !config.isEnabled() )
        {
            return responseHelper.formatResponse( ApplicationStatus.METHOD_NOT_ALLOWED,
                                   "This REST end point is disabled, please enable it first to use" );
        }

        logger.info( "Checking for existence of: {}:{}:{}", MAVEN_PKG_KEY, StoreType.hosted, name );
        StoreKey storeKey = new StoreKey( MAVEN_PKG_KEY, StoreType.hosted, name );
        if ( adminController.exists( storeKey ) )
        {
            return responseHelper.formatResponse( ApplicationStatus.CONFLICT,
                                   String.format( "Hosted repository %s already exists, can not create it again.",
                                                  name ) );
        }

        final String user = securityManager.getUser( securityContext, request );

        final String IGNORED_PATH_PREFIX;
        if ( StringUtils.isBlank( ignorePathPrefix ) )
        {
            IGNORED_PATH_PREFIX = "";
        }
        else
        {
            IGNORED_PATH_PREFIX = ignorePathPrefix.startsWith( "/" ) ? ignorePathPrefix : "/" + ignorePathPrefix;
        }

        HostedRepository repo;
        try
        {
            repo = hostedByArchiveManager.createStoreByArc( fileInput, name, user, IGNORED_PATH_PREFIX );
        }
        catch ( IndyWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return responseHelper.formatResponse( e );
        }
        finally
        {
            IOUtils.closeQuietly( fileInput );
        }

        if ( repo != null )
        {
            final URI uri = uriInfo.getBaseUriBuilder()
                                   .path( "/api/admin/stores" )
                                   .path( repo.getPackageType() )
                                   .path( repo.getType().singularEndpointName() )
                                   .build( repo.getName() );

            return responseHelper.formatCreatedResponseWithJsonEntity( uri, repo );
        }
        else
        {
            return responseHelper.formatResponse( new IndyWorkflowException( "Hosted creation failed with some unknown error!" ) );
        }

    }
}
