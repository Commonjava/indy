package org.commonjava.aprox.depgraph.util;

import java.util.LinkedList;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.commonjava.aprox.depgraph.dto.GAVWithPreset;
import org.commonjava.aprox.depgraph.dto.GraphRetrievalResult;
import org.commonjava.aprox.depgraph.json.DepgraphSerializationException;
import org.commonjava.aprox.depgraph.json.GAVWithPresetSer;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.model.EProjectGraph;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.util.logging.Logger;

@ApplicationScoped
public class GraphRetriever
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private CartoDataManager data;

    @Inject
    private RequestAdvisor advisor;

    public GraphRetrievalResult retrieveGraphs( final HttpServletRequest request, final String... gavps )
    {
        final LinkedList<GAVWithPreset> specs = new LinkedList<>();

        for ( final String gavp : gavps )
        {
            try
            {
                specs.add( GAVWithPresetSer.parsePathSegment( gavp ) );
            }
            catch ( final DepgraphSerializationException e )
            {
                logger.error( e.getMessage(), e );
                return new GraphRetrievalResult( Response.status( Status.BAD_REQUEST )
                                                         .entity( e.getMessage() )
                                                         .build() );
            }
        }

        final LinkedList<EProjectGraph> graphs = new LinkedList<>();

        for ( final GAVWithPreset spec : specs )
        {
            final ProjectRelationshipFilter filter = advisor.getPresetFilter( spec.getPreset() );

            EProjectGraph graph;
            try
            {
                graph = data.getProjectGraph( filter, spec.getGAV() );
                if ( graph == null )
                {
                    return new GraphRetrievalResult( Response.status( Status.BAD_REQUEST )
                                                             .entity( "Nothing known about graph: " + spec )
                                                             .build() );
                }

                graphs.add( graph );
            }
            catch ( final CartoDataException e )
            {
                logger.error( "Failed to lookup graph for: %s. Reason: %s", e, spec, e.getMessage() );
                return new GraphRetrievalResult( Response.serverError()
                                                         .build() );
            }
        }

        return new GraphRetrievalResult( specs, graphs );
    }
}
