/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.changelog.bind.jaxrs;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.commonjava.auditquery.history.ChangeEvent;
import org.commonjava.indy.bind.jaxrs.IndyResources;
import org.commonjava.indy.bind.jaxrs.util.REST;
import org.commonjava.indy.changelog.cache.RepoChangelogCache;
import org.commonjava.indy.changelog.conf.RepoChangelogConfiguration;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.indy.util.ApplicationContent;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;

/**
 * <strong>NOTE:</strong>
 * This rest endpoint is just used for testing! Do not use it for real any real operations!
 */
@Api( value = "Indy Repository change logs searching."
        + "/n *Warning*: This rest endpoint is only used for testing, "
        + "and will not return all results. It should not be used for any real operations" )
@Path( "/api/repo/changelog" )
@ApplicationScoped
@REST
public class RepoChangelogResource
        implements IndyResources
{
    @Inject
    @RepoChangelogCache
    private CacheHandle<String, ChangeEvent> changeLogCache;

    @Inject
    private RepoChangelogConfiguration config;

    @ApiOperation( "Retrieve change logs by specified store key" )
    @ApiResponses( { @ApiResponse( code = 404, message = "Change logs are not available" ),
                           @ApiResponse( code = 200, response = String.class,
                                         message = "Change logs for store key" ) } )
    @GET
    @Path( "/{packageType}/{type: (hosted|group|remote)}/{name}" )
    @Produces( ApplicationContent.application_json )
    public Response getChangelogByStoreKey(
            final @ApiParam( required = true ) @PathParam( "packageType" ) String packageType,
            final @ApiParam( required = true ) @PathParam( "type" ) String type,
            final @ApiParam( required = true ) @PathParam( "name" ) String name, @Context final UriInfo uriInfo )
    {
        if ( !config.isEnabled() )
        {
            return Response.status( 404 ).entity( "{\"error\":\"Change log module not enabled\"}" ).build();
        }
        StoreKey key = new StoreKey( packageType, StoreType.valueOf( type ), name );
        return Response.status( 200 ).entity( getLogsByStoreKey( key ) ).build();
    }

    @ApiOperation( "Retrieve all change logs./n "
                           + "*Warning*: This rest endpoint is only used for testing, "
                           + "and will not return all results. It should not be used for any real operations" )
    @ApiResponses( { @ApiResponse( code = 404, message = "Change logs are not available" ),
                           @ApiResponse( code = 200, response = String.class,
                                         message = "change logs for store key" ) } )
    @GET
    @Path( "all" )
    @Produces( ApplicationContent.application_json )
    public Response getAllChangelogs()
    {
        if ( !config.isEnabled() )
        {
            return Response.status( 404 ).entity( "{\"error\":\"Change log module not enabled\"}" ).build();
        }
        return Response.status( 200 ).entity( getAllLogs() ).build();
    }

    private final int RESULT_LIMITATION_FOR_TESTING = 10;

    private List<ChangeEvent> getLogsByStoreKey( StoreKey storeKey )
    {
        final ArrayList<ChangeEvent> results = new ArrayList<>( RESULT_LIMITATION_FOR_TESTING );

        return changeLogCache.execute( c -> {
            for ( ChangeEvent log : c.values() )
            {
                if ( log.getStoreKey().equals( storeKey.toString() ) )
                {
                    results.add( log );
                }
                if ( results.size() >= RESULT_LIMITATION_FOR_TESTING )
                {
                    return results;
                }
            }
            return results;
        } );

    }

    private List<ChangeEvent> getAllLogs()
    {
        final ArrayList<ChangeEvent> results = new ArrayList<>( RESULT_LIMITATION_FOR_TESTING );
        return changeLogCache.execute( c -> {
            for ( ChangeEvent log : c.values() )
            {
                results.add( log );
                if ( results.size() >= RESULT_LIMITATION_FOR_TESTING )
                {
                    return results;
                }
            }
            return results;
        } );
    }
}
