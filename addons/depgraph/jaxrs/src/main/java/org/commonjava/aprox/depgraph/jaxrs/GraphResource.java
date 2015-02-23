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
package org.commonjava.aprox.depgraph.jaxrs;

import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatOkResponseWithJsonEntity;
import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatResponse;
import static org.commonjava.aprox.model.util.HttpUtils.parseQueryMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.jaxrs.AproxResources;
import org.commonjava.aprox.depgraph.rest.GraphController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path( "/api/depgraph/rel" )
@ApplicationScoped
public class GraphResource
    implements AproxResources
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private GraphController controller;

    @Path( "/reindex{gav: ([^/]+/[^/]+/[^/]+)?}" )
    @GET
    public Response reindex( final @PathParam( "gav" ) String coord, @QueryParam( "wsid" ) final String wsid )
    {
        Response response = null;
        try
        {
            controller.reindex( coord, wsid );
            response = Response.ok()
                               .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e, true );
        }
        return response;
    }

    @Path( "/errors{gav: ([^/]+/[^/]+/[^/]+)?}" )
    @GET
    public Response errors( final @PathParam( "gav" ) String coord, @QueryParam( "wsid" ) final String wsid )
    {
        Response response = null;
        try
        {
            final String json = controller.errors( coord, wsid );
            if ( json != null )
            {
                response = formatOkResponseWithJsonEntity( json );
            }
            else
            {
                response = Response.ok()
                                   .build();
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e, true );
        }
        return response;
    }

    @Path( "/incomplete{gav: ([^/]+/[^/]+/[^/]+)?}" )
    @GET
    public Response incomplete( final @PathParam( "gav" ) String coord, @QueryParam( "wsid" ) final String wsid,
                            @Context final HttpServletRequest request )
    {
        Response response = null;
        try
        {
            final String json = controller.incomplete( coord, wsid, parseQueryMap( request.getQueryString() ) );

            if ( json != null )
            {
                response = formatOkResponseWithJsonEntity( json );
            }
            else
            {
                response = Response.ok()
                                   .build();
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e, true );
        }
        return response;
    }

    @Path( "/variable{gav: ([^/]+/[^/]+/[^/]+)?}" )
    @GET
    public Response variable( final @PathParam( "gav" ) String coord, @QueryParam( "wsid" ) final String wsid,
                              @Context final HttpServletRequest request )
    {
        Response response = null;
        try
        {
            final String json = controller.variable( coord, wsid, parseQueryMap( request.getQueryString() ) );

            if ( json != null )
            {
                response = formatOkResponseWithJsonEntity( json );
            }
            else
            {
                response = Response.ok()
                                   .build();
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e, true );
        }
        return response;
    }

    @Path( "/ancestry/{groupId}/{artifactId}/{version}" )
    @GET
    public Response ancestryOf( final @PathParam( "groupId" ) String gid, @PathParam( "artifactId" ) final String aid,
                                @PathParam( "version" ) final String ver, @QueryParam( "wsid" ) final String wsid )
    {
        Response response = null;
        try
        {
            final String json = controller.ancestryOf( gid, aid, ver, wsid );

            if ( json != null )
            {
                response = formatOkResponseWithJsonEntity( json );
            }
            else
            {
                response = Response.ok()
                                   .build();
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e, true );
        }
        return response;
    }

    @Path( "/build-order/{groupId}/{artifactId}/{version}" )
    @GET
    public Response buildOrder( @PathParam( "groupId" ) final String gid, @PathParam( "artifactId" ) final String aid,
                                @PathParam( "version" ) final String ver, @QueryParam( "wsid" ) final String wsid,
                                @Context final HttpServletRequest request )
    {
        Response response = null;
        try
        {
            final String json = controller.buildOrder( gid, aid, ver, wsid, parseQueryMap( request.getQueryString() ) );

            if ( json != null )
            {
                response = formatOkResponseWithJsonEntity( json );
            }
            else
            {
                response = Response.ok()
                                   .build();
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e, true );
        }
        return response;
    }

    @Path( "/project/{groupId}/{artifactId}/{version}" )
    @GET
    public Response projectGraph( @PathParam( "groupId" ) final String gid,
                                  @PathParam( "artifactId" ) final String aid,
                                  @PathParam( "version" ) final String ver, @QueryParam( "wsid" ) final String wsid,
                                  @Context final HttpServletRequest request )
    {
        Response response = null;
        try
        {
            final String json =
                controller.projectGraph( gid, aid, ver, wsid, parseQueryMap( request.getQueryString() ) );

            if ( json != null )
            {
                response = formatOkResponseWithJsonEntity( json );
            }
            else
            {
                response = Response.ok()
                                   .build();
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e, true );
        }
        return response;
    }

}
