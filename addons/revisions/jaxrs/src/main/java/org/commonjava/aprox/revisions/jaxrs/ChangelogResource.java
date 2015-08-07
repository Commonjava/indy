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
package org.commonjava.aprox.revisions.jaxrs;

import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatOkResponseWithJsonEntity;
import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatResponse;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.commonjava.aprox.audit.ChangeSummary;
import org.commonjava.aprox.bind.jaxrs.AproxResources;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.revisions.RevisionsManager;
import org.commonjava.aprox.revisions.jaxrs.dto.ChangeSummaryDTO;
import org.commonjava.aprox.subsys.git.GitSubsystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

@Path( "/api/revisions/changelog" )
public class ChangelogResource
    implements AproxResources
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private static final int DEFAULT_CHANGELOG_COUNT = 10;

    @Inject
    private RevisionsManager revisions;

    @Inject
    private ObjectMapper objectMapper;

    @Path( "/store/{type}/{name}" )
    public Response getStoreChangelog( final @PathParam( "type" ) String t,
                                       @PathParam( "name" ) final String storeName, @QueryParam( "start" ) int start,
                                       @QueryParam( "count" ) int count )
    {
        final StoreType storeType = StoreType.get( t );
        if ( storeType == null )
        {
            return Response.status( Status.BAD_REQUEST )
                           .entity( "Invalid store type: '" + t + "'" )
                           .build();
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
            response = formatOkResponseWithJsonEntity( new ChangeSummaryDTO( dataChangeLog ), objectMapper );

            logger.info( "\n\n\n\n\n\n{} Sent changelog for: {}\n\n{}\n\n\n\n\n\n\n", new Date(), key, dataChangeLog );
        }
        catch ( final GitSubsystemException e )
        {
            final String message =
                String.format( "Failed to lookup changelog for: %s. Reason: %s", key, e.getMessage() );
            logger.error( message, e );

            response = formatResponse( e );
        }

        return response;
    }

}
