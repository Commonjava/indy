/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.diag.bind.jaxrs;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.commonjava.indy.bind.jaxrs.IndyResources;
import org.commonjava.indy.diag.data.DiagnosticsManager;
import org.commonjava.indy.util.ApplicationHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;

import static org.commonjava.indy.util.ApplicationContent.application_zip;
import static org.commonjava.indy.util.ApplicationContent.text_plain;

/**
 * Created by jdcasey on 1/11/17.
 *
 * REST resource to expose diagnostic retrieval options from {@link org.commonjava.indy.diag.data.DiagnosticsManager}
 */
@ApplicationScoped
@Api( value = "Diagnostics",
      description = "Tools to aid users when something goes wrong on the server, and you don't have access to logs." )
@Path( "/api/diag" )
public class DiagnosticsResource
        implements IndyResources
{
    @Inject
    private DiagnosticsManager diagnosticsManager;

    @ApiOperation(
            "Retrieve a thread dump for Indy." )
    @ApiResponses( { @ApiResponse( code = 200, response = String.class, message = "Thread dump content" ) } )
    @GET
    @Path( "/threads" )
    @Produces(text_plain)
    public Response getThreadDump()
    {
        String threadDump = diagnosticsManager.getThreadDumpString();
        return Response.ok( threadDump )
                       .header( ApplicationHeader.content_disposition.key(),
                                "attachment; filename=indy-threads-" + System.currentTimeMillis() + ".txt" )
                       .build();
    }

    @ApiOperation(
            "Retrieve a ZIP-compressed file containing log files and a thread dump for Indy." )
    @ApiResponses( { @ApiResponse( code = 200, response = File.class, message = "ZIP bundle" ),
                           @ApiResponse( code = 500, message = "Log files could not be found / accessed" ) } )
    @GET
    @Path( "/bundle" )
    @Produces(application_zip)
    public Response getBundle()
    {
        try
        {
            File bundle = diagnosticsManager.getDiagnosticBundle();
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.info( "Returning diagnostic bundle: {}", bundle );

            return Response.ok( bundle )
                           .header( ApplicationHeader.content_disposition.key(),
                                    "attachment; filename=indy-diagnostic-bundle-" + System.currentTimeMillis() + ".zip" )
                           .build();
        }
        catch ( IOException e )
        {
            throw new WebApplicationException(
                    "Cannot retrieve log files, or failed to write logs + thread dump to bundle zip.", e );
        }
    }

    @ApiOperation(
                    "Retrieve a ZIP-compressed file containing all repository definitions." )
    @ApiResponses( { @ApiResponse( code = 200, response = File.class, message = "ZIP bundle" ),
                    @ApiResponse( code = 500, message = "Repository files could not be found / accessed" ) } )
    @GET
    @Path( "/repo" )
    @Produces(application_zip)
    public Response getRepoBundle()
    {
        try
        {
            File bundle = diagnosticsManager.getRepoBundle();
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.info( "Returning repo bundle: {}", bundle );

            return Response.ok( bundle )
                           .header( ApplicationHeader.content_disposition.key(),
                                    "attachment; filename=indy-repo-bundle-" + System.currentTimeMillis() + ".zip" )
                           .build();
        }
        catch ( IOException e )
        {
            throw new WebApplicationException(
                            "Cannot retrieve repository files, or failed to write to bundle zip.", e );
        }
    }
}
