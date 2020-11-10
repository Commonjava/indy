/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import org.commonjava.indy.bind.jaxrs.IndyResources;
import org.commonjava.indy.bind.jaxrs.SecurityManager;
import org.commonjava.indy.bind.jaxrs.util.REST;
import org.commonjava.indy.bind.jaxrs.util.ResponseHelper;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.promote.data.PromotionException;
import org.commonjava.indy.promote.model.GroupPromoteResult;
import org.commonjava.indy.promote.model.ValidationRuleDTO;
import org.commonjava.indy.promote.model.ValidationRuleSet;
import org.commonjava.indy.promote.validate.PromoteValidationsManager;
import org.commonjava.indy.util.ApplicationContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import static org.commonjava.indy.util.ApplicationContent.application_json;

@Api( value = "Promote administration resource to manage configurations for content promotion." )
@Path( "/api/admin/promotion" )
@Produces( { application_json } )
@REST
public class PromoteAdminResource
        implements IndyResources
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private PromoteValidationsManager validationsManager;

    @Inject
    private ObjectMapper mapper;

    @Inject
    private SecurityManager securityManager;

    @Inject
    private ResponseHelper responseHelper;

    @ApiOperation( "Promote a source repository into the membership of a target group (subject to validation)." )
    @ApiResponse( code = 200, message = "Promotion operation finished (consult response content for success/failure).",
                  response = Response.class )
    @ApiImplicitParam( name = "body", paramType = "body",
                       value = "JSON request specifying source and target, with other configuration options",
                       required = true, dataType = "org.commonjava.indy.promote.model.GroupPromoteRequest" )
    @Path( "/validation/reload/rules" )
    @PUT
    @Consumes( ApplicationContent.application_json )
    public Response reloadRules( final @Context HttpServletRequest servletRequest,
                                 final @Context SecurityContext securityContext, final @Context UriInfo uriInfo )
    {
        Response response;
        if ( !validationsManager.isEnabled() )
        {
            response = responseHelper.formatBadRequestResponse(
                    "Content promotion is disabled. Please check your indy configuration for more info." );
        }
        else
        {
            try
            {
                validationsManager.parseRules();
                response = Response.ok().build();
            }
            catch ( PromotionException e )
            {
                logger.error( e.getMessage(), e );
                response = responseHelper.formatResponse( e );
            }
        }
        return response;
    }

    @ApiOperation( "" )
    @ApiResponse( code = 200, message = "Promotion operation finished (consult response content for success/failure).",
                  response = Response.class )
    @ApiImplicitParam( name = "body", paramType = "body",
                       value = "JSON request specifying source and target, with other configuration options",
                       allowMultiple = false, required = true,
                       dataType = "org.commonjava.indy.promote.model.GroupPromoteRequest" )
    @Path( "/validation/reload/rulesets" )
    @PUT
    @Consumes( ApplicationContent.application_json )
    public Response reloadRuleSets( final @Context HttpServletRequest servletRequest,
                                    final @Context SecurityContext securityContext, final @Context UriInfo uriInfo )
    {
        Response response;
        if ( !validationsManager.isEnabled() )
        {
            response = responseHelper.formatBadRequestResponse(
                    "Content promotion is disabled. Please check your indy configuration for more info." );
        }
        else
        {
            try
            {
                validationsManager.parseRuleSets();
                response = Response.ok().build();
            }
            catch ( PromotionException e )
            {
                logger.error( e.getMessage(), e );
                response = responseHelper.formatResponse( e );
            }
        }
        return response;
    }

    @ApiOperation( "" )
    @ApiResponse( code = 200, message = "Promotion operation finished (consult response content for success/failure).",
                  response = GroupPromoteResult.class )
    @ApiImplicitParam( name = "body", paramType = "body", value = "", required = true,
                       dataType = "org.commonjava.indy.promote.model.GroupPromoteRequest" )
    @Path( "/validation/rules/{name}" )
    @GET
    @Consumes( ApplicationContent.application_json )
    public Response getRule( final @PathParam( "name" ) String ruleName,
                             final @Context HttpServletRequest servletRequest,
                             final @Context SecurityContext securityContext, final @Context UriInfo uriInfo )
    {
        if ( validationsManager.isEnabled() )
        {
            return Response.ok( validationsManager.getNamedRuleAsDTO( ruleName ) ).build();
        }
        else
        {
            return responseHelper.formatBadRequestResponse(
                    "Content promotion is disabled. Please check your indy configuration for more info." );
        }
    }

    @ApiOperation( "" )
    @ApiResponse( code = 200, message = "Promotion operation finished (consult response content for success/failure).",
                  response = Response.class )
    @ApiImplicitParam( name = "body", paramType = "body", value = "", required = true,
                       dataType = "org.commonjava.indy.promote.model.GroupPromoteRequest" )
    @Path( "/validation/rulesets/{storeKey}" )
    @GET
    @Consumes( ApplicationContent.application_json )
    public Response getRuleSet( final @PathParam( "storeKey" ) StoreKey storeKey,
                                final @Context HttpServletRequest servletRequest,
                                final @Context SecurityContext securityContext, final @Context UriInfo uriInfo )
    {
        if ( validationsManager.isEnabled() )
        {
            return Response.ok( validationsManager.getRuleSetMatching( storeKey ) ).build();
        }
        else
        {
            return responseHelper.formatBadRequestResponse(
                    "Content promotion is disabled. Please check your indy configuration for more info." );
        }
    }

}
