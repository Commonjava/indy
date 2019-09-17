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
package org.commonjava.indy.promote.bind.jaxrs;

import static org.commonjava.indy.bind.jaxrs.util.JaxRsUriFormatter.getBaseUrlByStoreKey;
import static org.commonjava.indy.util.ApplicationContent.application_json;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import org.apache.commons.io.IOUtils;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.bind.jaxrs.IndyResources;
import org.commonjava.indy.bind.jaxrs.SecurityManager;
import org.commonjava.indy.bind.jaxrs.util.REST;
import org.commonjava.indy.bind.jaxrs.util.ResponseHelper;
import org.commonjava.indy.promote.data.PromotionException;
import org.commonjava.indy.promote.data.PromotionManager;
import org.commonjava.indy.promote.model.GroupPromoteRequest;
import org.commonjava.indy.promote.model.GroupPromoteResult;
import org.commonjava.indy.promote.model.PathsPromoteRequest;
import org.commonjava.indy.promote.model.PathsPromoteResult;
import org.commonjava.indy.util.ApplicationContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;

@Api( value="Content Promotion", description = "Promote content from a source repository to a target repository or group." )
@Path( "/api/promotion" )
@Produces( { application_json } )
@REST
public class PromoteResource
        implements IndyResources
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private PromotionManager manager;

    @Inject
    private ObjectMapper mapper;

    @Inject
    private SecurityManager securityManager;

    @Inject
    private ResponseHelper responseHelper;

    @ApiOperation( "Promote a source repository into the membership of a target group (subject to validation)." )
    @ApiResponse( code = 200, message = "Promotion operation finished (consult response content for success/failure).",
                  response = GroupPromoteResult.class )
    @ApiImplicitParam( name = "body", paramType = "body",
                       value = "JSON request specifying source and target, with other configuration options",
                       allowMultiple = false, required = true,
                       dataType = "org.commonjava.indy.promote.model.GroupPromoteRequest" )
    @Path( "/groups/promote" )
    @POST
    @Consumes( ApplicationContent.application_json )
    public GroupPromoteResult promoteToGroup( final GroupPromoteRequest request,
                                              final @Context HttpServletRequest servletRequest,
                                              final @Context SecurityContext securityContext,
                                              final @Context UriInfo uriInfo)
    {
        Response response;
        try
        {
            String user = securityManager.getUser( securityContext, servletRequest );
            final String baseUrl = getBaseUrlByStoreKey( uriInfo, request.getSource() );

            return manager.promoteToGroup( request, user, baseUrl );
        }
        catch ( PromotionException | IndyWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            responseHelper.throwError( e );
        }

        return null;
    }

    @ApiOperation( "Rollback (remove) a previously promoted source repository from the membership of a target group." )
    @ApiResponse( code=200, message = "Promotion operation finished (consult response content for success/failure).", response=GroupPromoteResult.class )
    @ApiImplicitParam( name = "body", paramType = "body",
                       value = "JSON result from previous call, specifying source and target, with other configuration options",
                       allowMultiple = false, required = true,
                       dataType = "org.commonjava.indy.promote.model.GroupPromoteResult" )
    @Path( "/groups/rollback" )
    @POST
    @Consumes( ApplicationContent.application_json )
    public GroupPromoteResult rollbackGroupPromote( final GroupPromoteResult result,
                                                    @Context final HttpServletRequest servletRequest,
                                                    @Context final SecurityContext securityContext )
    {
        try
        {
            String user = securityManager.getUser( securityContext, servletRequest );
            return manager.rollbackGroupPromote( result, user );
        }
        catch ( PromotionException | IndyWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            responseHelper.throwError( e );
        }

        return null;
    }

    @ApiOperation( "Promote paths from a source repository into a target repository/group (subject to validation)." )
    @ApiResponse( code=200, message = "Promotion operation finished (consult response content for success/failure).", response=PathsPromoteResult.class )
    @ApiImplicitParam( name = "body", paramType = "body",
                       value = "JSON request specifying source and target, with other configuration options",
                       allowMultiple = false, required = true,
                       dataType = "org.commonjava.indy.promote.model.PathsPromoteRequest" )
    @Path( "/paths/promote" )
    @POST
    @Consumes( ApplicationContent.application_json )
    public Response promotePaths( final @Context HttpServletRequest request, final @Context UriInfo uriInfo )
    {
        PathsPromoteRequest req;
        Response response;
        try
        {
            final String json = IOUtils.toString( request.getInputStream() );
            logger.info( "Got promotion request:\n{}", json );
            req = mapper.readValue( json, PathsPromoteRequest.class );
        }
        catch ( final IOException e )
        {
            response = responseHelper.formatResponse( e, "Failed to read DTO from request body." );
            return response;
        }

        try
        {
            final String baseUrl = getBaseUrlByStoreKey( uriInfo, req.getSource() );
            final PathsPromoteResult result = manager.promotePaths( req, baseUrl );

            response = responseHelper.formatOkResponseWithJsonEntity( result );
            logger.info( "Send promotion result:\n{}", response.getEntity() );
        }
        catch ( PromotionException | IndyWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            response = responseHelper.formatResponse( e );
        }

        return response;
    }

    @ApiOperation( "Rollback promotion of any completed paths to a source repository from a target repository/group." )
    @ApiResponse( code=200, message = "Promotion operation finished (consult response content for success/failure).", response=PathsPromoteResult.class )
    @ApiImplicitParam( name = "body", paramType = "body",
                       value = "JSON result from previous attempt, specifying source and target, with other configuration options",
                       allowMultiple = false, required = true,
                       dataType = "org.commonjava.indy.promote.model.PathsPromoteResult" )
    @Path( "/paths/rollback" )
    @POST
    @Consumes( ApplicationContent.application_json )
    public Response rollbackPaths( @Context final HttpServletRequest request, @Context final UriInfo uriInfo  )
    {
        PathsPromoteResult result;
        Response response;
        try
        {
            result = mapper.readValue( request.getInputStream(), PathsPromoteResult.class );
        }
        catch ( final IOException e )
        {
            response = responseHelper.formatResponse( e, "Failed to read DTO from request body." );
            return response;
        }

        try
        {
            result = manager.rollbackPathsPromote( result );
            response = responseHelper.formatOkResponseWithJsonEntity( result );
        }
        catch ( PromotionException | IndyWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            response = responseHelper.formatResponse( e );
        }

        return response;
    }

}
