package org.commonjava.aprox.depgraph.rest.calc;

import java.io.IOException;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.depgraph.conf.AproxDepgraphConfig;
import org.commonjava.aprox.depgraph.inject.DepgraphSpecific;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.dto.GraphCalculation;
import org.commonjava.maven.cartographer.dto.GraphComposition;
import org.commonjava.maven.cartographer.dto.GraphDescription;
import org.commonjava.maven.cartographer.dto.GraphDifference;
import org.commonjava.maven.cartographer.ops.CalculationOps;
import org.commonjava.maven.cartographer.preset.PresetSelector;
import org.commonjava.util.logging.Logger;
import org.commonjava.web.json.ser.JsonSerializer;

@Path( "/depgraph/calc" )
@Produces( MediaType.APPLICATION_JSON )
@RequestScoped
public class CalculatorResource
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private CalculationOps ops;

    @Inject
    @DepgraphSpecific
    private JsonSerializer serializer;

    @Inject
    private PresetSelector presets;

    @Inject
    private AproxDepgraphConfig config;

    @Path( "/diff" )
    @GET
    public Response difference( @Context final HttpServletRequest request )
    {
        Response response;
        try
        {
            final GraphComposition dto = readDTO( request );
            final List<GraphDescription> graphs = dto.getGraphs();

            if ( graphs.size() != 2 )
            {
                response =
                    Response.status( Status.BAD_REQUEST )
                            .entity( "You must specify EXACTLY two graph descriptions (GAV-set with optional filter preset) in order to perform a diff." )
                            .build();
            }
            else
            {
                final GraphDifference difference = ops.difference( graphs.get( 0 ), graphs.get( 1 ) );
                final String json = serializer.toString( difference );

                response = Response.ok( json )
                                   .build();
            }
        }
        catch ( final CartoDataException e )
        {
            logger.error( "Failed to retrieve graph(s): %s", e, e.getMessage() );
            response = Response.serverError()
                               .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( "Failed to read configuration for diff: %s", e, e.getMessage() );
            response = e.getResponse();
        }

        return response;
    }

    @GET
    public Response calculate( @Context final HttpServletRequest request )
    {
        Response response;
        try
        {
            final GraphComposition dto = readDTO( request );
            final List<GraphDescription> graphs = dto.getGraphs();

            if ( graphs.size() < 2 )
            {
                response =
                    Response.status( Status.BAD_REQUEST )
                            .entity( "You must specify AT LEAST two graph descriptions (GAV-set with optional filter preset) in order to perform a calculation." )
                            .build();
            }
            else if ( dto.getCalculation() == null )
            {
                response = Response.status( Status.BAD_REQUEST )
                                   .entity( "You must specify a calculation type." )
                                   .build();
            }
            else
            {
                final GraphCalculation result = ops.calculate( dto );

                final String json = serializer.toString( result );

                response = Response.ok( json )
                                   .build();
            }
        }
        catch ( final CartoDataException e )
        {
            logger.error( "Failed to retrieve graph(s): %s", e, e.getMessage() );
            response = Response.serverError()
                               .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( "Failed to read configuration for diff: %s", e, e.getMessage() );
            response = e.getResponse();
        }

        return response;
    }

    private GraphComposition readDTO( final HttpServletRequest req )
        throws AproxWorkflowException
    {
        String json;
        try
        {
            json = IOUtils.toString( req.getInputStream() );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( "Failed to read configuration JSON from request body. Reason: %s", e, e.getMessage() );
        }

        logger.info( "Got configuration JSON:\n\n%s\n\n", json );
        final GraphComposition dto = serializer.fromString( json, GraphComposition.class );

        dto.resolveFilters( presets, config.getDefaultWebFilterPreset() );

        return dto;
    }
}
