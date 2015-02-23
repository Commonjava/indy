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
package org.commonjava.aprox.depgraph.jaxrs.resolve;

import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatOkResponseWithJsonEntity;
import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatResponse;
import static org.commonjava.aprox.model.util.HttpUtils.parseQueryMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.jaxrs.AproxResources;
import org.commonjava.aprox.depgraph.rest.ResolverController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// FIXME: Inlining a source URL is a terrible idea, make these POSTs with config DTOs
@Path( "/api/depgraph/resolve/{from}" )
@ApplicationScoped
public class ResolverResource
    implements AproxResources
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ResolverController controller;

    @Path( "/{groupId}/{artifactId}/{version}" )
    @GET
    public Response resolveGraph( @PathParam( "from" ) final String f, @PathParam( "groupId" ) final String gid,
                                  @PathParam( "artifactId" ) final String aid,
                                  @PathParam( "version" ) final String ver, @QueryParam( "wsid" ) final String wsid,
                                  @QueryParam( "recurse" ) @DefaultValue( "false" ) final boolean recurse,
                                  @Context final HttpServletRequest request )
    {
        Response response = null;
        try
        {
            final String json =
                controller.resolveGraph( f, gid, aid, ver, recurse, wsid, parseQueryMap( request.getQueryString() ) );
            if ( json == null )
            {
                response = Response.ok()
                                   .build();
            }
            else
            {
                response = formatOkResponseWithJsonEntity( json );
            }

        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e, true );
        }
        return response;
    }

    // TODO: Return resolved rels
    @Path( "/{groupId}/{artifactId}/{version}/incomplete" )
    @GET
    public Response resolveIncomplete( @PathParam( "from" ) final String f, @PathParam( "groupId" ) final String gid,
                                       @PathParam( "artifactId" ) final String aid,
                                       @PathParam( "version" ) final String ver,
                                       @QueryParam( "wsid" ) final String wsid,
                                       @QueryParam( "recurse" ) @DefaultValue( "false" ) final boolean recurse,
                                       @Context final HttpServletRequest request )
    {
        Response response = null;
        try
        {
            controller.resolveIncomplete( f, gid, aid, ver, recurse, wsid, parseQueryMap( request.getQueryString() ) );
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

}
