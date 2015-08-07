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

import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatOkResponseWithJsonEntity;
import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatResponse;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.jaxrs.AproxResources;
import org.commonjava.aprox.promote.data.PromotionException;
import org.commonjava.aprox.promote.data.PromotionManager;
import org.commonjava.aprox.promote.model.PromoteRequest;
import org.commonjava.aprox.promote.model.PromoteResult;
import org.commonjava.aprox.util.ApplicationContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

@Path( "/api/promotion" )
public class PromoteResource
    implements AproxResources
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private PromotionManager manager;

    @Inject
    private ObjectMapper mapper;

    @Path( "/promote" )
    @POST
    @Consumes( ApplicationContent.application_json )
    public Response promote( final @Context HttpServletRequest request )
    {
        PromoteRequest req = null;
        Response response = null;
        try
        {
            final String json = IOUtils.toString( request.getInputStream() );
            logger.info( "Got promotion request:\n{}", json );
            req = mapper.readValue( json, PromoteRequest.class );
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
            final PromoteResult result = manager.promote( req );

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

    @Path( "/resume" )
    @POST
    @Consumes( ApplicationContent.application_json )
    public Response resume( @Context final HttpServletRequest request )
    {
        PromoteResult result = null;
        Response response = null;
        try
        {
            result = mapper.readValue( request.getInputStream(), PromoteResult.class );
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
            result = manager.resume( result );

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

    @Path( "/rollback" )
    @POST
    @Consumes( ApplicationContent.application_json )
    public Response rollback( @Context final HttpServletRequest request )
    {
        PromoteResult result = null;
        Response response = null;
        try
        {
            result = mapper.readValue( request.getInputStream(), PromoteResult.class );
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
            result = manager.rollback( result );

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
