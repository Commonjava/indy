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
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.commonjava.indy.bind.jaxrs.IndyResources;
import org.commonjava.indy.bind.jaxrs.SecurityManager;
import org.commonjava.indy.bind.jaxrs.util.REST;
import org.commonjava.indy.bind.jaxrs.util.ResponseHelper;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.promote.data.PromotionException;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

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

    @ApiOperation( "Reload all rules for promotion validation" )
    @ApiResponse( code = 200, message = "", response = Response.class )
    @Path( "/validation/reload/rules" )
    @PUT
    @Consumes( ApplicationContent.application_json )
    public Response reloadRules( final @Context HttpServletRequest servletRequest,
                                 final @Context SecurityContext securityContext, final @Context UriInfo uriInfo )
    {
        return checkEnabledAnd( () -> {
            try
            {
                validationsManager.parseRules();
                return Response.ok( new ArrayList<>( validationsManager.toDTO().getRules().keySet() ) ).build();
            }
            catch ( PromotionException e )
            {
                logger.error( e.getMessage(), e );
                return responseHelper.formatResponse( e );
            }
        } );
    }

    @ApiOperation( "Reload all rule-sets for promotion validation" )
    @ApiResponse( code = 200, message = "", response = Response.class )
    @Path( "/validation/reload/rulesets" )
    @PUT
    @Consumes( ApplicationContent.application_json )
    public Response reloadRuleSets( final @Context HttpServletRequest servletRequest,
                                    final @Context SecurityContext securityContext, final @Context UriInfo uriInfo )
    {
        return checkEnabledAnd( () -> {
            try
            {
                validationsManager.parseRuleSets();
                return Response.ok( new ArrayList<>( validationsManager.toDTO().getRuleSets().keySet() ) ).build();
            }
            catch ( PromotionException e )
            {
                logger.error( e.getMessage(), e );
                return responseHelper.formatResponse( e );
            }
        } );
    }

    @ApiOperation( "Reload all rules & rule-sets for promotion validation" )
    @ApiResponse( code = 200, message = "", response = Response.class )
    @Path( "/validation/reload/all" )
    @PUT
    @Consumes( ApplicationContent.application_json )
    public Response reloadAll( final @Context HttpServletRequest servletRequest,
                               final @Context SecurityContext securityContext, final @Context UriInfo uriInfo )
    {
        return checkEnabledAnd( () -> {
            try
            {
                validationsManager.parseRuleBundles();
                Map<String, List<String>> bundles = new HashMap<>( 2 );
                bundles.put( "rules", new ArrayList<>( validationsManager.toDTO().getRules().keySet() ) );
                bundles.put( "rule-sets", new ArrayList<>( validationsManager.toDTO().getRuleSets().keySet() ) );
                return Response.ok(bundles).build();
            }
            catch ( PromotionException e )
            {
                logger.error( e.getMessage(), e );
                return responseHelper.formatResponse( e );
            }
        } );
    }

    @ApiOperation( "Get all rules' names" )
    @ApiResponses( { @ApiResponse( code = 200, response = Response.class,
                                   message = "All promotion validation rules' definition" ),
                           @ApiResponse( code = 404, message = "No rules are defined" ) } )
    @Path( "/validation/rules/all" )
    @GET
    @Consumes( ApplicationContent.application_json )
    public Response getAllRules( final @Context HttpServletRequest servletRequest,
                                 final @Context SecurityContext securityContext, final @Context UriInfo uriInfo )
    {
        return checkEnabledAnd( () -> {
            Map<String, ValidationRuleDTO> rules = validationsManager.toDTO().getRules();
            if ( !rules.isEmpty() )
            {
                return Response.ok( new ArrayList<>( rules.keySet() ) ).build();
            }
            else
            {
                return Response.status( Response.Status.NOT_FOUND ).entity( Collections.emptyList() ).build();
            }
        } );
    }

    @ApiOperation( "Get promotion rule by rule name" )
    @ApiResponses( { @ApiResponse( code = 200, response = Response.class,
                                   message = "The promotion validation rule definition" ),
                           @ApiResponse( code = 404, message = "The rule doesn't exist" ) } )
    @Path( "/validation/rules/named/{name}" )
    @GET
    @Consumes( ApplicationContent.application_json )
    public Response getRule( final @PathParam( "name" ) String ruleName,
                             final @Context HttpServletRequest servletRequest,
                             final @Context SecurityContext securityContext, final @Context UriInfo uriInfo )
    {
        return checkEnabledAnd( () -> {
            Optional<ValidationRuleDTO> dto = validationsManager.getNamedRuleAsDTO( ruleName );
            if ( dto.isPresent() )
            {
                return Response.ok( dto.get() ).build();
            }
            else
            {
                return Response.status( Response.Status.NOT_FOUND ).build();
            }
        } );
    }

    @ApiOperation( "Get promotion rule-set by store key" )
    @ApiResponses( { @ApiResponse( code = 200, response = Response.class,
                                   message = "The promotion validation rule-set definition" ),
                           @ApiResponse( code = 404, message = "The rule-set doesn't exist" ) } )
    @Path( "/validation/rulesets/all" )
    @GET
    @Consumes( ApplicationContent.application_json )
    public Response getAllRuleSets( final @Context HttpServletRequest servletRequest,
                                    final @Context SecurityContext securityContext, final @Context UriInfo uriInfo )
    {
        return checkEnabledAnd( () -> {
            Map<String, ValidationRuleSet> ruleSets = validationsManager.toDTO().getRuleSets();
            if ( !ruleSets.isEmpty() )
            {
                return Response.ok( new ArrayList<>( ruleSets.keySet() ) ).build();
            }
            else
            {
                return Response.status( Response.Status.NOT_FOUND ).entity( Collections.emptyList() ).build();
            }
        } );
    }

    @ApiOperation( "Get promotion rule-set by store key" )
    @ApiResponses( { @ApiResponse( code = 200, response = Response.class,
                                   message = "The promotion validation rule-set definition" ),
                           @ApiResponse( code = 404, message = "The rule-set doesn't exist" ) } )
    @Path( "/validation/rulesets/storekey/{storeKey}" )
    @GET
    @Consumes( ApplicationContent.application_json )
    public Response getRuleSetByStoreKey( final @PathParam( "storeKey" ) String storeKey,
                                          final @Context HttpServletRequest servletRequest,
                                          final @Context SecurityContext securityContext,
                                          final @Context UriInfo uriInfo )
    {
        return checkEnabledAnd( () -> {
            final StoreKey storekey;
            try
            {
                storekey = StoreKey.fromString( storeKey );
            }
            catch ( Exception e )
            {
                return responseHelper.formatBadRequestResponse( e.getMessage() );
            }
            ValidationRuleSet ruleSet = validationsManager.getRuleSetMatching( storekey );
            if ( ruleSet != null )
            {
                return Response.ok( ruleSet ).build();
            }
            else
            {
                return Response.status( Response.Status.NOT_FOUND ).build();
            }
        } );
    }

    @ApiOperation( "Get promotion rule-set by name" )
    @ApiResponses( { @ApiResponse( code = 200, response = Response.class,
                                   message = "The promotion validation rule-set definition" ),
                           @ApiResponse( code = 404, message = "The rule-set doesn't exist" ) } )
    @Path( "/validation/rulesets/named/{name}" )
    @GET
    @Consumes( ApplicationContent.application_json )
    public Response getRuleSetByName( final @PathParam( "name" ) String name,
                                      final @Context HttpServletRequest servletRequest,
                                      final @Context SecurityContext securityContext, final @Context UriInfo uriInfo )
    {
        return checkEnabledAnd( () -> {
            final StoreKey storekey;
            Optional<ValidationRuleSet> ruleSet = validationsManager.getNamedRuleSet( name );
            if ( ruleSet.isPresent() )
            {
                return Response.ok( ruleSet.get() ).build();
            }
            else
            {
                return Response.status( Response.Status.NOT_FOUND ).build();
            }
        } );
    }

    private Response checkEnabledAnd( Supplier<Response> responseSupplier )
    {
        if ( validationsManager.isEnabled() )
        {
            return responseSupplier.get();
        }
        else
        {
            return responseHelper.formatBadRequestResponse(
                    "Content promotion is disabled. Please check your indy configuration for more info." );
        }
    }

}
