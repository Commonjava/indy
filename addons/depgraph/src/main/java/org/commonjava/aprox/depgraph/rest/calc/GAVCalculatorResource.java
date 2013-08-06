package org.commonjava.aprox.depgraph.rest.calc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

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
import org.commonjava.aprox.depgraph.dto.GraphRetrievalResult;
import org.commonjava.aprox.depgraph.util.GraphRetriever;
import org.commonjava.maven.atlas.common.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.effective.EProjectGraph;
import org.commonjava.tensor.data.CartoDataManager;
import org.commonjava.tensor.discover.DiscoverySourceManager;
import org.commonjava.tensor.inject.TensorData;
import org.commonjava.util.logging.Logger;
import org.commonjava.web.json.ser.JsonSerializer;

@Path( "/depgraph/calc/gav" )
@Produces( MediaType.APPLICATION_JSON )
@RequestScoped
public class GAVCalculatorResource
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private CartoDataManager data;

    @Inject
    @TensorData
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
        final GraphRetrievalResult result = retriever.retrieveGraphs( request, gavp1, gavp2 );
        if ( result.getResponse() != null )
        {
            return result.getResponse();
        }

        final LinkedList<GAVWithPreset> specs = result.getSpecs();
        final LinkedList<EProjectGraph> graphs = result.getGraphs();

        final Set<ProjectVersionRef> firstAll = graphs.getFirst()
                                                      .getAllProjects();
        final Set<ProjectVersionRef> secondAll = graphs.getLast()
                                                       .getAllProjects();

        final Set<ProjectVersionRef> added = new HashSet<>( secondAll );
        added.removeAll( firstAll );

        final Set<ProjectVersionRef> removed = new HashSet<>( firstAll );
        removed.removeAll( secondAll );

        final Map<String, Object> out = new HashMap<String, Object>();
        out.put( "from", specs.getFirst()
                              .getGAV() );

        out.put( "from-preset", specs.getFirst()
                                     .getPreset() );

        out.put( "to", specs.getLast()
                            .getGAV() );

        out.put( "to-preset", specs.getLast()
                                   .getPreset() );

        out.put( "added", added );
        out.put( "removed", removed );

        final String json = serializer.toString( out );

        return Response.ok( json )
                       .build();
    }

    @Path( "/subtract/{gavp1: ([^:]+):([^:]+):([^:]+):([^/]+)}/{gavp2: ([^:]+):([^:]+):([^:]+):([^/]+)}" )
    @GET
    public Response subtract( @PathParam( "gavp1" ) final String gavp1, @PathParam( "gavp2" ) final String gavp2,
                              @Context final HttpServletRequest request )
    {
        logger.info( "Got the following GAV+P's for subtraction:\n  First GAV+P: %s\n  Second GAV+P: %s", gavp1, gavp2 );
        final GraphRetrievalResult result = retriever.retrieveGraphs( request, gavp1, gavp2 );
        if ( result.getResponse() != null )
        {
            return result.getResponse();
        }

        final LinkedList<GAVWithPreset> specs = result.getSpecs();
        final LinkedList<EProjectGraph> graphs = result.getGraphs();

        final Set<ProjectVersionRef> firstAll = graphs.getFirst()
                                                      .getAllProjects();
        final Set<ProjectVersionRef> secondAll = graphs.getLast()
                                                       .getAllProjects();

        final Set<ProjectVersionRef> removed = new HashSet<>( firstAll );
        removed.removeAll( secondAll );

        final Map<String, Object> out = new HashMap<String, Object>();
        out.put( "from", specs.getFirst()
                              .getGAV() );

        out.put( "from-preset", specs.getFirst()
                                     .getPreset() );

        out.put( "subtracted", specs.getLast()
                                    .getGAV() );

        out.put( "subtracted-preset", specs.getLast()
                                           .getPreset() );

        out.put( "gavs", removed );

        final String json = serializer.toString( out );

        return Response.ok( json )
                       .build();
    }

    @Path( "/add/{gavp1: ([^:]+):([^:]+):([^:]+):([^/]+)}/{gavp2: ([^:]+):([^:]+):([^:]+):([^/]+)}" )
    @GET
    public Response add( @PathParam( "gavp1" ) final String gavp1, @PathParam( "gavp2" ) final String gavp2,
                         @Context final HttpServletRequest request )
    {
        final GraphRetrievalResult result = retriever.retrieveGraphs( request, gavp1, gavp2 );
        if ( result.getResponse() != null )
        {
            return result.getResponse();
        }

        final LinkedList<GAVWithPreset> specs = result.getSpecs();
        final LinkedList<EProjectGraph> graphs = result.getGraphs();

        final Set<ProjectVersionRef> firstAll = graphs.getFirst()
                                                      .getAllProjects();
        final Set<ProjectVersionRef> secondAll = graphs.getLast()
                                                       .getAllProjects();

        final Set<ProjectVersionRef> added = new HashSet<>( secondAll );
        added.addAll( firstAll );

        final Map<String, Object> out = new HashMap<String, Object>();
        out.put( "first", specs.getFirst()
                               .getGAV() );

        out.put( "first-preset", specs.getFirst()
                                      .getPreset() );

        out.put( "second", specs.getLast()
                                .getGAV() );

        out.put( "second-preset", specs.getLast()
                                       .getPreset() );

        out.put( "gavs", added );

        final String json = serializer.toString( out );

        return Response.ok( json )
                       .build();
    }

    @Path( "/intersection/{gavp1: ([^:]+):([^:]+):([^:]+):([^/]+)}/{gavp2: ([^:]+):([^:]+):([^:]+):([^/]+)}" )
    @GET
    public Response intersection( @PathParam( "gavp1" ) final String gavp1, @PathParam( "gavp2" ) final String gavp2,
                                  @Context final HttpServletRequest request )
    {
        final GraphRetrievalResult result = retriever.retrieveGraphs( request, gavp1, gavp2 );
        if ( result.getResponse() != null )
        {
            return result.getResponse();
        }

        final LinkedList<GAVWithPreset> specs = result.getSpecs();
        final LinkedList<EProjectGraph> graphs = result.getGraphs();

        final Set<ProjectVersionRef> firstAll = graphs.getFirst()
                                                      .getAllProjects();
        final Set<ProjectVersionRef> secondAll = graphs.getLast()
                                                       .getAllProjects();

        final Set<ProjectVersionRef> intersect = new HashSet<>( firstAll );
        intersect.retainAll( secondAll );

        final Map<String, Object> out = new HashMap<String, Object>();
        out.put( "first", specs.getFirst()
                               .getGAV() );

        out.put( "first-preset", specs.getFirst()
                                      .getPreset() );

        out.put( "second", specs.getLast()
                                .getGAV() );

        out.put( "second-preset", specs.getLast()
                                       .getPreset() );

        out.put( "gavs", intersect );

        final String json = serializer.toString( out );

        return Response.ok( json )
                       .build();
    }

}
