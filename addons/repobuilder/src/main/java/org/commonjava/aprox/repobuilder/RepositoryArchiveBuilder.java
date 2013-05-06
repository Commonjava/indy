package org.commonjava.aprox.repobuilder;

import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static org.commonjava.tensor.web.base.rest.RequestUtils.createAggregationOptions;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.maven.graph.common.ref.ArtifactRef;
import org.apache.maven.graph.common.ref.ProjectRef;
import org.commonjava.aprox.filer.FileManager;
import org.commonjava.aprox.filer.PathUtils;
import org.commonjava.tensor.agg.AggregationOptions;
import org.commonjava.tensor.agg.AggregatorConfig;
import org.commonjava.tensor.agg.GraphAggregator;
import org.commonjava.tensor.agg.ProjectRefCollection;
import org.commonjava.tensor.data.TensorDataException;
import org.commonjava.tensor.io.AggregatorConfigUtils;
import org.commonjava.util.logging.Logger;

@Path( "/repozip" )
@Produces( "application/zip" )
public class RepositoryArchiveBuilder
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private GraphAggregator aggregator;

    @Inject
    private FileManager fileManager;

    @GET
    @Path( "/runtime/{store}" )
    public Response getRuntimeRepository( @PathParam( "store" ) final String store,
                                          @Context final HttpServletRequest request )
    {
        Response response = Response.status( NO_CONTENT )
                                    .build();

        AggregatorConfig config = null;
        try
        {
            config = AggregatorConfigUtils.read( request.getInputStream() );
            final AggregationOptions options = createAggregationOptions( request );

            final Map<ProjectRef, ProjectRefCollection> refMap = aggregator.collectProjectReferences( options, config );

            final OutputStream os = null;
            final ZipOutputStream stream = new ZipOutputStream( os );
            for ( final Entry<ProjectRef, ProjectRefCollection> entry : refMap.entrySet() )
            {
                final ProjectRef key = entry.getKey();
                final ProjectRefCollection value = entry.getValue();
                for ( final ArtifactRef ar : value.getArtifactRefs() )
                {
                    final String version = ar.getVersionString();
                    if ( ar.isVariableVersion() )
                    {
                        // resolve;
                    }

                    final StringBuilder sb = new StringBuilder();
                    sb.append( ar.getArtifactId() )
                      .append( version );
                    if ( ar.getClassifier() != null )
                    {
                        sb.append( '-' )
                          .append( ar.getClassifier() );
                    }

                    sb.append( '.' )
                      .append( ar.getType() );

                    final String path =
                        PathUtils.join( ar.getGroupId()
                                          .replace( '.', '/' ), ar.getArtifactId(), version, sb.toString() );
                    final ZipEntry ze = new ZipEntry( path );
                    stream.putNextEntry( ze );

                    fileManager.retrieveFirst( stores, path );
                }
            }
        }
        catch ( final IOException e )
        {
            logger.error( "Failed to generate runtime repository for: %s. Reason: %s", e, config, e.getMessage() );

            response = Response.serverError()
                               .build();
        }
        catch ( final TensorDataException e )
        {
            logger.error( "Failed to generate runtime repository for: %s. Reason: %s", e, config, e.getMessage() );

            response = Response.serverError()
                               .build();
        }

        return response;
    }

}
