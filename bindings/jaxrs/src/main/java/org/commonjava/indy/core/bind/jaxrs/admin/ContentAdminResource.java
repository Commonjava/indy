/**
 * Copyright (C) 2023 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.core.bind.jaxrs.admin;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.lang.StringUtils;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.bind.jaxrs.IndyResources;
import org.commonjava.indy.bind.jaxrs.util.REST;
import org.commonjava.indy.bind.jaxrs.util.ResponseHelper;
import org.commonjava.indy.core.bind.jaxrs.util.ContentAdminController;
import org.commonjava.indy.core.model.dto.TrackedContentDTO;
import org.commonjava.indy.core.model.TrackedContentEntry;
import org.commonjava.indy.core.model.dto.ContentTransferDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.commonjava.indy.util.ApplicationContent.application_zip;

@Api( value = "Tracking Record Admin Content Access", description = "Manages Content tracking records." )
@Path( "/api/admin/content" )
@ApplicationScoped
@REST
public class ContentAdminResource
                implements IndyResources
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ContentAdminController controller;

    @Inject
    private ResponseHelper responseHelper;

    // TODO need to disable folo {id}/repo/zip endpoint
    @ApiOperation( "Retrieve the content referenced in a tracking record as a ZIP-compressed Maven repository directory." )
    @ApiResponses( { @ApiResponse( code = 200, response = File.class, message = "ZIP repository content" ),
                    @ApiResponse( code = 404, message = "No such tracking record" ) } )
    @Path( "/repo/zip" )
    @POST
    @Produces( application_zip )
    public File getZipRepository( @Context final UriInfo uriInfo, final TrackedContentDTO record )
    {
        try
        {
            return controller.renderRepositoryZip( record );
        }
        catch ( IndyWorkflowException e )
        {
            responseHelper.throwError( e );
        }

        return null;
    }

    // TODO need to disable folo recalculate endpoint
    @ApiOperation( "Recalculate sizes and checksums for every file listed in a tracking record." )
    @ApiResponses( {
                    @ApiResponse( code = 200, response = ContentTransferDTO.class, message = "Recalculated tracking report" ),
                    @ApiResponse( code = 404, message = "No such tracking record can be found" ),
                    @ApiResponse( code = 400, message = "All entries must belong to the same tracking Id" ) } )
    @POST
    @Path( "/tracking/recalculate" )
    public Response recalculateEntrySet( @Context final UriInfo uriInfo, final Set<ContentTransferDTO> entries )
    {
        if ( entries == null )
        {
            return Response.status( Status.NOT_FOUND ).build();
        }
        String id = "";
        String another_id = "";
        for ( ContentTransferDTO entry : entries )
        {
            // Just get an id from an entry since all entries should belong to the same tracking id
            id = entry.getTrackingKey().getId();
            if ( !StringUtils.isNotBlank( another_id ) )
            {
                another_id = id;
            }
            else
            {
                if ( !another_id.equals( id ) )
                {
                    return Response.status( Status.BAD_REQUEST )
                                   .entity( "All entries must belong to the same tracking Id" )
                                   .build();
                }
                else
                {
                    break;
                }
            }
        }
        Response response;
        try
        {
            final Set<TrackedContentEntry> newEntries =
                            controller.recalculateEntrySet( entries, id, new AtomicBoolean() );

            if ( newEntries == null )
            {
                response = Response.status( Status.NOT_FOUND ).entity( "No such tracking record can be found" ).build();
            }
            else
            {
                response = responseHelper.formatOkResponseWithJsonEntity( newEntries );
            }
        }
        catch ( final IndyWorkflowException e )
        {
            logger.error( String.format( "Failed to serialize tracking report for: %s. Reason: %s", id,
                                         e.getMessage() ), e );

            response = responseHelper.formatResponse( e );
        }

        return response;
    }

}
