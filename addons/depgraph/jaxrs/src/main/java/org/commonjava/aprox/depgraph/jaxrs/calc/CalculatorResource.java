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
package org.commonjava.aprox.depgraph.jaxrs.calc;

import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatOkResponseWithJsonEntity;
import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatResponse;
import static org.commonjava.aprox.util.ApplicationContent.application_json;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.jaxrs.AproxResources;
import org.commonjava.aprox.depgraph.rest.CalculatorController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path( "/api/depgraph/calc" )
@ApplicationScoped
public class CalculatorResource
    implements AproxResources
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private CalculatorController controller;

    @Path( "/diff" )
    @GET
    @Produces( application_json )
    public Response difference( final @QueryParam( "wsid" ) String wsid, @Context final HttpServletRequest request )
    {
        Response response = null;
        try
        {
            final String json = controller.difference( request.getInputStream(), request.getCharacterEncoding(), wsid );
            response = formatOkResponseWithJsonEntity( json );
        }
        catch ( final AproxWorkflowException | IOException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e, true );
        }
        return response;
    }

    @GET
    @Produces( application_json )
    public Response calculate( final @QueryParam( "wsid" ) String wsid, @Context final HttpServletRequest request )
    {
        Response response = null;
        try
        {
            final String json = controller.calculate( request.getInputStream(), request.getCharacterEncoding(), wsid );
            response = formatOkResponseWithJsonEntity( json );
        }
        catch ( final AproxWorkflowException | IOException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e, true );
        }
        return response;
    }
}
