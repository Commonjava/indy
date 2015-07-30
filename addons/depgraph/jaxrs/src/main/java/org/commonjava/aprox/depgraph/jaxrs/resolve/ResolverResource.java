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
package org.commonjava.aprox.depgraph.jaxrs.resolve;

import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatOkResponseWithJsonEntity;
import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatResponse;
import static org.commonjava.aprox.model.util.HttpUtils.parseQueryMap;

import java.util.Map;

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
            final Map<String, String[]> queryMap = parseQueryMap( request.getQueryString() );

            logger.debug( "Resolving graph given the following information:\nFrom: {}\nGroup-Id: {}\nArtifact-Id: {}\nVersion: {}\nWorkspace-Id: {}\nRecurse: {}\nQuery params: {}",
                          f, gid, aid, ver, wsid, recurse, queryMap );

            final String json = controller.resolveGraph( f, gid, aid, ver, recurse, wsid, queryMap );

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
