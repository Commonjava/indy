package org.commonjava.indy.diag.bind.jaxrs;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.commonjava.indy.bind.jaxrs.IndyResources;
import org.commonjava.indy.diag.data.DiagnosticsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import java.io.File;
import java.io.IOException;

import static org.commonjava.indy.util.ApplicationContent.application_zip;

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
            "Retrieve a ZIP-compressed file containing log files and a thread dump for Indy." )
    @ApiResponses( { @ApiResponse( code = 200, response = File.class, message = "ZIP bundle" ),
                           @ApiResponse( code = 500, message = "Log files could not be found / accessed" ) } )
    @GET
    @Path( "/bundle" )
    @Produces(application_zip)
    public File getBundle()
    {
        try
        {
            File bundle = diagnosticsManager.getDiagnosticBundle();
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.info( "Returning diagnostic bundle: {}", bundle );

            return bundle;
        }
        catch ( IOException e )
        {
            throw new WebApplicationException(
                    "Cannot retrieve log files, or failed to write logs + thread dump to bundle zip.", e );
        }
    }
}
