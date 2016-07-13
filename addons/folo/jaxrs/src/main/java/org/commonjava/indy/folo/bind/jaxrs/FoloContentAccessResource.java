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
package org.commonjava.indy.folo.bind.jaxrs;

import static org.commonjava.indy.folo.ctl.FoloConstants.TRACKING_KEY;
import static org.commonjava.indy.IndyContentConstants.CHECK_CACHE_ONLY;

import static org.commonjava.indy.bind.jaxrs.util.ResponseUtils.formatResponse;
import static org.commonjava.indy.bind.jaxrs.util.ResponseUtils.setInfoHeaders;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.commonjava.indy.bind.jaxrs.IndyDeployment;
import org.commonjava.indy.bind.jaxrs.IndyResources;
import org.commonjava.indy.core.bind.jaxrs.ContentAccessHandler;
import org.commonjava.indy.core.bind.jaxrs.ContentAccessResource;
import org.commonjava.indy.core.ctl.ContentController;
import org.commonjava.indy.folo.model.TrackingKey;
import org.commonjava.maven.galley.event.EventMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Modified copy of {@link ContentAccessResource} that collects a tracking ID in addition to store type and name, then hands this off to
 * {@link ContentController} (with {@link EventMetadata} containing the tracking ID), which records artifact accesses.
 * 
 * NOTE: This is a result of copy/paste programming, so changes to {@link ContentAccessResource} will have to be ported over.
 * 
 * @author jdcasey
 */
@Path( "/api/folo/track/{id}/{type: (hosted|group|remote)}/{name}" )
public class FoloContentAccessResource
    implements IndyResources
{

    private static final String BASE_PATH = IndyDeployment.API_PREFIX + "/folo/track";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ContentAccessHandler handler;

    public FoloContentAccessResource()
    {
    }

    public FoloContentAccessResource( final ContentAccessHandler handler )
    {
        this.handler = handler;
    }

    @PUT
    @Path( "/{path: (.*)}" )
    public Response doCreate( @PathParam( "id" ) final String id, @PathParam( "type" ) final String type,
                              @PathParam( "name" ) final String name, @PathParam( "path" ) final String path,
                              @Context final HttpServletRequest request, @Context final UriInfo uriInfo )
    {
        final TrackingKey tk = new TrackingKey( id );

        return handler.doCreate( type, name, path, request, new EventMetadata().set( TRACKING_KEY, tk ), ()->uriInfo.getBaseUriBuilder()
                                                                                                                                    .path( getClass() )
                                                                                                                                    .path( path )
                                                                                                                                    .build( id, type, name ) );
    }

    @HEAD
    @Path( "/{path: (.*)}" )
    public Response doHead( @PathParam( "id" ) final String id, @PathParam( "type" ) final String type,
                            @PathParam( "name" ) final String name, @PathParam( "path" ) final String path,
                            @QueryParam( CHECK_CACHE_ONLY ) Boolean cacheOnly, @Context final HttpServletRequest request, @Context final UriInfo uriInfo )
    {
        final TrackingKey tk = new TrackingKey( id );

        final String baseUri = uriInfo.getBaseUriBuilder()
                                      .path( BASE_PATH )
                                      .path( id )
                                      .build()
                                      .toString();

        return handler.doHead( type, name, path, cacheOnly, baseUri, request,
                               new EventMetadata().set( TRACKING_KEY, tk ) );
    }

    @GET
    @Path( "/{path: (.*)}" )
    public Response doGet( @PathParam( "id" ) final String id, @PathParam( "type" ) final String type,
                           @PathParam( "name" ) final String name, @PathParam( "path" ) final String path,
                           @Context final HttpServletRequest request, @Context final UriInfo uriInfo )
    {
        final TrackingKey tk = new TrackingKey( id );
        final String baseUri = uriInfo.getBaseUriBuilder()
                                      .path( BASE_PATH )
                                      .path( id )
                                      .build()
                                      .toString();

        return handler.doGet( type, name, path, baseUri, request, new EventMetadata().set( TRACKING_KEY, tk ) );
    }

}
