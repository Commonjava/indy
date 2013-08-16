package org.commonjava.aprox.depgraph.rest.calc;

import java.util.Collections;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.commonjava.aprox.depgraph.dto.GAVWithPreset;
import org.commonjava.aprox.depgraph.inject.DepgraphSpecific;
import org.commonjava.aprox.depgraph.json.DepgraphSerializationException;
import org.commonjava.aprox.depgraph.json.GAVWithPresetSer;
import org.commonjava.aprox.depgraph.util.GraphRetriever;
import org.commonjava.aprox.depgraph.util.RequestAdvisor;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.maven.cartographer.discover.DiscoverySourceManager;
import org.commonjava.maven.cartographer.dto.GraphCalculation;
import org.commonjava.maven.cartographer.dto.GraphDifference;
import org.commonjava.maven.cartographer.ops.CalculationOps;
import org.commonjava.util.logging.Logger;
import org.commonjava.web.json.ser.JsonSerializer;

@Path( "/depgraph/calc" )
@Produces( MediaType.APPLICATION_JSON )
@RequestScoped
public class CalculatorResource
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private CartoDataManager data;

    @Inject
    private CalculationOps ops;

    @Inject
    private RequestAdvisor advisor;

    @Inject
    @DepgraphSpecific
    private JsonSerializer serializer;

    @Inject
    private GraphRetriever retriever;

    @Inject
    private DiscoverySourceManager sourceFactory;

    @Path( "/diff/{gavp1: ([^:]+):([^:]+):([^:]+):([^/]+)}/{gavp2: ([^:]+):([^:]+):([^:]+):([^/]+)}" )
    @GET
    public Response difference( @PathParam( "gavp1" ) final String gavp1, @PathParam( "gavp2" ) final String gavp2,
                                @Context final HttpServletRequest request )
    {
        Response response;
        try
        {
            final GAVWithPreset first = GAVWithPresetSer.parsePathSegment( gavp1 );
            final ProjectRelationshipFilter firstFilter = advisor.getPresetFilter( first.getPreset() );

            final GAVWithPreset second = GAVWithPresetSer.parsePathSegment( gavp1 );
            final ProjectRelationshipFilter secondFilter = advisor.getPresetFilter( second.getPreset() );

            final GraphDifference difference =
                ops.difference( Collections.singleton( first.getGAV() ), firstFilter,
                                Collections.singleton( second.getGAV() ), secondFilter );

            final String json = serializer.toString( difference );

            response = Response.ok( json )
                               .build();
        }
        catch ( final CartoDataException e )
        {
            logger.error( "Failed to retrieve graph(s): %s", e, e.getMessage() );
            response = Response.serverError()
                               .build();
        }
        catch ( final DepgraphSerializationException e )
        {
            logger.error( "Failed to read input: %s", e, e.getMessage() );
            response = Response.serverError()
                               .build();
        }

        return response;
    }

    @Path( "/subtract/{gavp1: ([^:]+):([^:]+):([^:]+):([^/]+)}/{gavp2: ([^:]+):([^:]+):([^:]+):([^/]+)}" )
    @GET
    public Response subtract( @PathParam( "gavp1" ) final String gavp1, @PathParam( "gavp2" ) final String gavp2,
                              @Context final HttpServletRequest request )
    {
        Response response;
        try
        {
            final GAVWithPreset first = GAVWithPresetSer.parsePathSegment( gavp1 );
            final ProjectRelationshipFilter firstFilter = advisor.getPresetFilter( first.getPreset() );

            final GAVWithPreset second = GAVWithPresetSer.parsePathSegment( gavp1 );
            final ProjectRelationshipFilter secondFilter = advisor.getPresetFilter( second.getPreset() );

            final GraphCalculation result =
                ops.subtract( Collections.singleton( first.getGAV() ), firstFilter,
                              Collections.singleton( second.getGAV() ), secondFilter );

            final String json = serializer.toString( result );

            response = Response.ok( json )
                               .build();
        }
        catch ( final CartoDataException e )
        {
            logger.error( "Failed to retrieve graph(s): %s", e, e.getMessage() );
            response = Response.serverError()
                               .build();
        }
        catch ( final DepgraphSerializationException e )
        {
            logger.error( "Failed to read input: %s", e, e.getMessage() );
            response = Response.serverError()
                               .build();
        }

        return response;
    }

    @Path( "/add/{gavp1: ([^:]+):([^:]+):([^:]+):([^/]+)}/{gavp2: ([^:]+):([^:]+):([^:]+):([^/]+)}" )
    @GET
    public Response add( @PathParam( "gavp1" ) final String gavp1, @PathParam( "gavp2" ) final String gavp2,
                         @Context final HttpServletRequest request )
    {
        Response response;
        try
        {
            final GAVWithPreset first = GAVWithPresetSer.parsePathSegment( gavp1 );
            final ProjectRelationshipFilter firstFilter = advisor.getPresetFilter( first.getPreset() );

            final GAVWithPreset second = GAVWithPresetSer.parsePathSegment( gavp1 );
            final ProjectRelationshipFilter secondFilter = advisor.getPresetFilter( second.getPreset() );

            final GraphCalculation result =
                ops.add( Collections.singleton( first.getGAV() ), firstFilter,
                         Collections.singleton( second.getGAV() ), secondFilter );

            final String json = serializer.toString( result );

            response = Response.ok( json )
                               .build();
        }
        catch ( final CartoDataException e )
        {
            logger.error( "Failed to retrieve graph(s): %s", e, e.getMessage() );
            response = Response.serverError()
                               .build();
        }
        catch ( final DepgraphSerializationException e )
        {
            logger.error( "Failed to read input: %s", e, e.getMessage() );
            response = Response.serverError()
                               .build();
        }

        return response;
    }

    @Path( "/intersection/{gavp1: ([^:]+):([^:]+):([^:]+):([^/]+)}/{gavp2: ([^:]+):([^:]+):([^:]+):([^/]+)}" )
    @GET
    public Response intersection( @PathParam( "gavp1" ) final String gavp1, @PathParam( "gavp2" ) final String gavp2,
                                  @Context final HttpServletRequest request )
    {
        Response response;
        try
        {
            final GAVWithPreset first = GAVWithPresetSer.parsePathSegment( gavp1 );
            final ProjectRelationshipFilter firstFilter = advisor.getPresetFilter( first.getPreset() );

            final GAVWithPreset second = GAVWithPresetSer.parsePathSegment( gavp1 );
            final ProjectRelationshipFilter secondFilter = advisor.getPresetFilter( second.getPreset() );

            final GraphCalculation result =
                ops.intersection( Collections.singleton( first.getGAV() ), firstFilter,
                                  Collections.singleton( second.getGAV() ), secondFilter );

            final String json = serializer.toString( result );

            response = Response.ok( json )
                               .build();
        }
        catch ( final CartoDataException e )
        {
            logger.error( "Failed to retrieve graph(s): %s", e, e.getMessage() );
            response = Response.serverError()
                               .build();
        }
        catch ( final DepgraphSerializationException e )
        {
            logger.error( "Failed to read input: %s", e, e.getMessage() );
            response = Response.serverError()
                               .build();
        }

        return response;
    }

}
