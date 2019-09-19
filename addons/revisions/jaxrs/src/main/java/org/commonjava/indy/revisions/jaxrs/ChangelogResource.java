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
package org.commonjava.indy.revisions.jaxrs;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.bind.jaxrs.IndyResources;
import org.commonjava.indy.bind.jaxrs.util.REST;
import org.commonjava.indy.bind.jaxrs.util.ResponseHelper;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.revisions.RevisionsManager;
import org.commonjava.indy.revisions.jaxrs.dto.ChangeSummaryDTO;
import org.commonjava.indy.subsys.git.GitSubsystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Date;
import java.util.List;

@Path( "/api/revisions/changelog" )
@REST
public class ChangelogResource
        implements IndyResources
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private static final int DEFAULT_CHANGELOG_COUNT = 10;

    @Inject
    private RevisionsManager revisions;

    @Inject
    private ObjectMapper objectMapper;

    @Inject
    private ResponseHelper responseHelper;

    @ApiOperation(
            "Retrieve the changelog for the Indy group/repository definition with the start-index and number of results" )
    @ApiResponses( { @ApiResponse( code = 200, message = "JSON containing changelog entries",
                                   response = ChangeSummaryDTO.class ), @ApiResponse( code = 400,
                                                                                      message = "Requested group/repository type is not one of: {remote, hosted, group}" ) } )
    @Path( "/store/{type}/{name}" )
    public Response getStoreChangelog(
            @ApiParam( allowableValues = "hosted,group,remote", required = true ) final @PathParam( "type" ) String t,
            @PathParam( "name" ) final String storeName, @QueryParam( "start" ) int start,
            @QueryParam( "count" ) int count )
    {
        final StoreType storeType = StoreType.get( t );
        if ( storeType == null )
        {
            return Response.status( Status.BAD_REQUEST ).entity( "Invalid store type: '" + t + "'" ).build();
        }

        final StoreKey key = new StoreKey( storeType, storeName );

        if ( start < 0 )
        {
            start = 0;
        }

        if ( count < 1 )
        {
            count = DEFAULT_CHANGELOG_COUNT;
        }

        Response response;
        try
        {
            final List<ChangeSummary> dataChangeLog = revisions.getDataChangeLog( key, start, count );
            response = responseHelper.formatOkResponseWithJsonEntity( new ChangeSummaryDTO( dataChangeLog ) );

            logger.info( "\n\n\n\n\n\n{} Sent changelog for: {}\n\n{}\n\n\n\n\n\n\n", new Date(), key, dataChangeLog );
        }
        catch ( final GitSubsystemException e )
        {
            final String message =
                    String.format( "Failed to lookup changelog for: %s. Reason: %s", key, e.getMessage() );
            logger.error( message, e );

            response = responseHelper.formatResponse( e );
        }

        return response;
    }

}
