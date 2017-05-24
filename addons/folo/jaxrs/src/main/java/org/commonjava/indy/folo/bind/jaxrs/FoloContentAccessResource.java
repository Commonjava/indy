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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.commonjava.indy.bind.jaxrs.IndyDeployment;
import org.commonjava.indy.bind.jaxrs.IndyResources;
import org.commonjava.indy.core.bind.jaxrs.ContentAccessHandler;
import org.commonjava.indy.core.bind.jaxrs.ContentAccessResource;
import org.commonjava.indy.core.ctl.ContentController;
import org.commonjava.indy.folo.metrics.IndyFoloMetricsNames;
import org.commonjava.indy.folo.model.TrackingKey;
import org.commonjava.indy.measure.annotation.IndyMetrics;
import org.commonjava.indy.measure.annotation.Measure;
import org.commonjava.indy.measure.annotation.MetricNamed;
import org.commonjava.indy.model.core.AccessChannel;
import org.commonjava.maven.galley.event.EventMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import static org.commonjava.indy.IndyContentConstants.CHECK_CACHE_ONLY;
import static org.commonjava.indy.folo.ctl.FoloConstants.ACCESS_CHANNEL;
import static org.commonjava.indy.folo.ctl.FoloConstants.TRACKING_KEY;

/**
 * Modified copy of {@link ContentAccessResource} that collects a tracking ID in addition to store type and name, then hands this off to
 * {@link ContentController} (with {@link EventMetadata} containing the tracking ID), which records artifact accesses.
 *
 * NOTE: This is a result of copy/paste programming, so changes to {@link ContentAccessResource} will have to be ported over.
 *
 * @author jdcasey
 */
@Api( value = "FOLO Tracked Content Access and Storage",
      description = "Tracks retrieval and management of file/artifact content." )
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

    @ApiOperation( "Store and track file/artifact content under the given artifact store (type/name) and path." )
    @ApiResponses( { @ApiResponse( code = 201, message = "Content was stored successfully" ), @ApiResponse( code = 400,
                                                                                                            message = "No appropriate storage location was found in the specified store (this store, or a member if a group is specified)." ) } )

    @IndyMetrics( measure = @Measure( timers = @MetricNamed( name =
                    IndyFoloMetricsNames.FoloContentAccessResourceMetricsNames.METHOD_DOCREATE
                                    + IndyFoloMetricsNames.TIMER ), meters = @MetricNamed( name =
                    IndyFoloMetricsNames.FoloContentAccessResourceMetricsNames.METHOD_DOCREATE
                                    + IndyFoloMetricsNames.METER ) ), exceptions = @Measure( meters = @MetricNamed( name =
                    IndyFoloMetricsNames.FoloContentAccessResourceMetricsNames.METHOD_DOCREATE
                                    + IndyFoloMetricsNames.EXCEPTION ) ) )
    @PUT
    @Path( "/{path: (.*)}" )
    public Response doCreate( @ApiParam( "User-assigned tracking session key" ) @PathParam( "id" ) final String id,
                              @ApiParam( allowableValues = "hosted,group,remote", required = true ) @PathParam( "type" )
                              final String type, @PathParam( "name" ) final String name,
                              @PathParam( "path" ) final String path, @Context final HttpServletRequest request,
                              @Context final UriInfo uriInfo )
    {
        final TrackingKey tk = new TrackingKey( id );

        EventMetadata metadata =
                new EventMetadata().set( TRACKING_KEY, tk ).set( ACCESS_CHANNEL, AccessChannel.MAVEN_REPO );
        return handler.doCreate( type, name, path, request, metadata, () -> uriInfo.getBaseUriBuilder()
                                                                                   .path( getClass() )
                                                                                   .path( path )
                                                                                   .build( id, type, name ) );
    }

    @ApiOperation( "Store and track file/artifact content under the given artifact store (type/name) and path." )
    @ApiResponses( { @ApiResponse( code=404, message = "Content is not available" ), @ApiResponse( code = 200,
                                   message = "Header metadata for content (or rendered listing when path ends with '/index.html' or '/'" ), } )

    @IndyMetrics( measure = @Measure( timers = @MetricNamed( name =
                    IndyFoloMetricsNames.FoloContentAccessResourceMetricsNames.METHOD_DOHEAD
                                    + IndyFoloMetricsNames.TIMER ), meters = @MetricNamed( name =
                    IndyFoloMetricsNames.FoloContentAccessResourceMetricsNames.METHOD_DOHEAD
                                    + IndyFoloMetricsNames.METER ) ), exceptions = @Measure( meters = @MetricNamed( name =
                    IndyFoloMetricsNames.FoloContentAccessResourceMetricsNames.METHOD_DOHEAD
                                    + IndyFoloMetricsNames.EXCEPTION ) ) )
    @HEAD
    @Path( "/{path: (.*)}" )
    public Response doHead( @ApiParam( "User-assigned tracking session key" ) @PathParam( "id" ) final String id,
                            @ApiParam( allowableValues = "hosted,group,remote", required = true ) @PathParam( "type" )
                            final String type, @PathParam( "name" ) final String name,
                            @PathParam( "path" ) final String path,
                            @QueryParam( CHECK_CACHE_ONLY ) final Boolean cacheOnly,
                            @Context final HttpServletRequest request, @Context final UriInfo uriInfo )
    {
        final TrackingKey tk = new TrackingKey( id );

        final String baseUri = uriInfo.getBaseUriBuilder().path( BASE_PATH ).path( id ).build().toString();

        EventMetadata metadata =
                new EventMetadata().set( TRACKING_KEY, tk ).set( ACCESS_CHANNEL, AccessChannel.MAVEN_REPO );
        return handler.doHead( type, name, path, cacheOnly, baseUri, request, metadata );
    }

    @ApiOperation( "Retrieve and track file/artifact content under the given artifact store (type/name) and path." )
    @ApiResponses( { @ApiResponse( code=404, message = "Content is not available" ), @ApiResponse( code = 200, response = String.class,
                                   message = "Rendered content listing (when path ends with '/index.html' or '/')" ),
                           @ApiResponse( code = 200, response = StreamingOutput.class, message = "Content stream" ), } )

    @IndyMetrics( measure = @Measure( timers = @MetricNamed( name =
                    IndyFoloMetricsNames.FoloContentAccessResourceMetricsNames.METHOD_DOGET
                                    + IndyFoloMetricsNames.TIMER ), meters = @MetricNamed( name =
                    IndyFoloMetricsNames.FoloContentAccessResourceMetricsNames.METHOD_DOGET
                                    + IndyFoloMetricsNames.METER ) ), exceptions = @Measure( meters = @MetricNamed( name =
                    IndyFoloMetricsNames.FoloContentAccessResourceMetricsNames.METHOD_DOGET
                                    + IndyFoloMetricsNames.EXCEPTION ) ) )
    @GET
    @Path( "/{path: (.*)}" )
    public Response doGet( @ApiParam( "User-assigned tracking session key" ) @PathParam( "id" ) final String id,
                           @ApiParam( allowableValues = "hosted,group,remote", required = true ) @PathParam( "type" )
                           final String type, @PathParam( "name" ) final String name,
                           @PathParam( "path" ) final String path, @Context final HttpServletRequest request,
                           @Context final UriInfo uriInfo )
    {
        final TrackingKey tk = new TrackingKey( id );
        final String baseUri = uriInfo.getBaseUriBuilder().path( BASE_PATH ).path( id ).build().toString();

        EventMetadata metadata =
                new EventMetadata().set( TRACKING_KEY, tk ).set( ACCESS_CHANNEL, AccessChannel.MAVEN_REPO );
        return handler.doGet( type, name, path, baseUri, request, metadata );
    }

}
