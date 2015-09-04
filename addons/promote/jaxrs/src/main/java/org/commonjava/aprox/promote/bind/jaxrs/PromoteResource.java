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
package org.commonjava.aprox.promote.bind.jaxrs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.jaxrs.AproxResources;
import org.commonjava.aprox.bind.jaxrs.SecurityManager;
import org.commonjava.aprox.promote.data.PromotionException;
import org.commonjava.aprox.promote.data.PromotionManager;
import org.commonjava.aprox.promote.model.GroupPromoteRequest;
import org.commonjava.aprox.promote.model.GroupPromoteResult;
import org.commonjava.aprox.promote.model.PathsPromoteRequest;
import org.commonjava.aprox.promote.model.PathsPromoteResult;
import org.commonjava.aprox.util.ApplicationContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;

import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatOkResponseWithJsonEntity;
import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatResponse;
import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.throwError;

@Path( "/api/promotion" )
public class PromoteResource
        implements AproxResources
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private PromotionManager manager;

    @Inject
    private ObjectMapper mapper;

    @Inject
    private SecurityManager securityManager;

    @Path( "/groups/promote" )
    @POST
    @Consumes( ApplicationContent.application_json )
    public GroupPromoteResult promoteToGroup( final GroupPromoteRequest request,
                                              final @Context HttpServletRequest servletRequest,
                                              final @Context SecurityContext securityContext )
    {
        Response response = null;
        try
        {
            String user = securityManager.getUser( securityContext, servletRequest );
            return manager.promoteToGroup( request, user );
        }
        catch ( PromotionException e )
        {
            logger.error( e.getMessage(), e );
            throwError( e );
        }

        return null;
    }

    @Path( "/groups/rollback" )
    @POST
    @Consumes( ApplicationContent.application_json )
    public GroupPromoteResult rollbackGroupPromote( GroupPromoteResult result,
                                                    @Context final HttpServletRequest servletRequest,
                                                    @Context final SecurityContext securityContext )
    {
        try
        {
            String user = securityManager.getUser( securityContext, servletRequest );
            return manager.rollbackGroupPromote( result, user );
        }
        catch ( PromotionException e )
        {
            logger.error( e.getMessage(), e );
            throwError( e );
        }

        return null;
    }

    @Path( "/paths/promote" )
    @POST
    @Consumes( ApplicationContent.application_json )
    public Response promotePaths( final @Context HttpServletRequest request )
    {
        PathsPromoteRequest req = null;
        Response response = null;
        try
        {
            final String json = IOUtils.toString( request.getInputStream() );
            logger.info( "Got promotion request:\n{}", json );
            req = mapper.readValue( json, PathsPromoteRequest.class );
        }
        catch ( final IOException e )
        {
            response = formatResponse( e, "Failed to read DTO from request body." );
        }

        if ( response != null )
        {
            return response;
        }

        try
        {
            final PathsPromoteResult result = manager.promotePaths( req );

            // TODO: Amend response status code based on presence of error? This would have consequences for client API...
            response = formatOkResponseWithJsonEntity( result, mapper );
        }
        catch ( PromotionException | AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e );
        }

        return response;
    }

    @Path( "/paths/resume" )
    @POST
    @Consumes( ApplicationContent.application_json )
    public Response resumePaths( @Context final HttpServletRequest request )
    {
        PathsPromoteResult result = null;
        Response response = null;
        try
        {
            result = mapper.readValue( request.getInputStream(), PathsPromoteResult.class );
        }
        catch ( final IOException e )
        {
            response = formatResponse( e, "Failed to read DTO from request body." );
        }

        if ( response != null )
        {
            return response;
        }

        try
        {
            result = manager.resumePathsPromote( result );

            // TODO: Amend response status code based on presence of error? This would have consequences for client API...
            response = formatOkResponseWithJsonEntity( result, mapper );
        }
        catch ( PromotionException | AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e );
        }

        return response;
    }

    @Path( "/paths/rollback" )
    @POST
    @Consumes( ApplicationContent.application_json )
    public Response rollbackPaths( @Context final HttpServletRequest request )
    {
        PathsPromoteResult result = null;
        Response response = null;
        try
        {
            result = mapper.readValue( request.getInputStream(), PathsPromoteResult.class );
        }
        catch ( final IOException e )
        {
            response = formatResponse( e, "Failed to read DTO from request body." );
        }

        if ( response != null )
        {
            return response;
        }

        try
        {
            result = manager.rollbackPathsPromote( result );

            // TODO: Amend response status code based on presence of error? This would have consequences for client API...
            response = formatOkResponseWithJsonEntity( result, mapper );
        }
        catch ( PromotionException | AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e );
        }

        return response;
    }

}
