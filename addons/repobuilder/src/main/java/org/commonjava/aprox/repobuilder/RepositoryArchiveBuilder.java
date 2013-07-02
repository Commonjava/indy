package org.commonjava.aprox.repobuilder;

import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.copy;
import static org.commonjava.tensor.agg.AggregationUtils.collectProjectReferences;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.maven.graph.common.DependencyScope;
import org.apache.maven.graph.common.ref.ArtifactRef;
import org.apache.maven.graph.common.ref.ProjectRef;
import org.apache.maven.graph.effective.EProjectWeb;
import org.apache.maven.graph.effective.filter.DependencyFilter;
import org.apache.maven.graph.spi.GraphDriverException;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.filer.FileManager;
import org.commonjava.aprox.filer.PathUtils;
import org.commonjava.aprox.io.StorageItem;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.tensor.agg.AggregatorConfig;
import org.commonjava.tensor.agg.ProjectRefCollection;
import org.commonjava.tensor.data.TensorDataException;
import org.commonjava.tensor.data.TensorDataManager;
import org.commonjava.tensor.io.AggregatorConfigUtils;
import org.commonjava.util.logging.Logger;

@Path( "/repozip" )
@Produces( "application/zip" )
public class RepositoryArchiveBuilder
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private FileManager fileManager;

    @Inject
    private StoreDataManager storeManager;

    @Inject
    private TensorDataManager tensorManager;

    @GET
    @Path( "/{type}/{store}" )
    public Response getRuntimeRepository( @PathParam( "type" ) final String type,
                                          @PathParam( "store" ) final String store,
                                          @Context final HttpServletRequest req, @Context final HttpServletResponse resp )
    {
        Response response = Response.status( NO_CONTENT )
                                    .build();

        AggregatorConfig config = null;
        ZipOutputStream stream = null;
        EProjectWeb web = null;
        try
        {
            config = AggregatorConfigUtils.read( req.getInputStream() );
            //            final AggregationOptions options = createAggregationOptions( request );

            web = tensorManager.getProjectWeb( config.getRoots() );
            if ( web == null )
            {
                // TODO: discovery link
                response = Response.status( Status.NOT_FOUND )
                                   .build();
            }

            web = web.filteredInstance( new DependencyFilter( DependencyScope.runtime ) );

            final Map<ProjectRef, ProjectRefCollection> refMap = collectProjectReferences( web );

            final OutputStream os = resp.getOutputStream();
            stream = new ZipOutputStream( os );

            final List<ArtifactStore> stores = getStoresFor( type, store );

            for ( final ProjectRefCollection refs : refMap.values() )
            {
                for ( final ArtifactRef ar : refs.getArtifactRefs() )
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

                    final StorageItem item = fileManager.retrieveFirst( stores, path );
                    InputStream itemStream = null;
                    try
                    {
                        itemStream = item.openInputStream();
                        copy( itemStream, stream );
                    }
                    finally
                    {
                        closeQuietly( itemStream );
                    }
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
        catch ( final ProxyDataException e )
        {
            logger.error( "Failed to lookup Artifact stores based on %s:%s. Reason: %s", e, type, store, e.getMessage() );
            response = Response.serverError()
                               .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( "Failed to retrieve relevant artifacts for repository archive of %s:%s. Reason: %s", e, type,
                          store, e.getMessage() );

            response = e.getResponse();
            if ( response == null )
            {
                response = Response.serverError()
                                   .build();
            }
        }
        catch ( final GraphDriverException e )
        {
            logger.error( "Failed to filter for runtime artifacts of: %s. Reason: %s", e, config, e.getMessage() );

            response = Response.serverError()
                               .build();
        }
        finally
        {
            closeQuietly( stream );
        }

        boolean discoveryEligible = false;
        if ( web.getIncompleteSubgraphs() != null )
        {
            // TODO: missing-resolution link
            discoveryEligible = true;
        }

        if ( web.getVariableSubgraphs() != null )
        {
            // TODO: variable-resolution link
            discoveryEligible = true;
        }

        if ( discoveryEligible )
        {
            // TODO: discovery link
        }

        response = Response.ok()
                           .type( "application/zip" )
                           .build();

        return response;
    }

    private List<ArtifactStore> getStoresFor( final String type, final String store )
        throws ProxyDataException
    {
        final StoreType st = StoreType.get( type );

        List<ArtifactStore> stores = new ArrayList<ArtifactStore>();
        if ( st == StoreType.group )
        {
            stores = storeManager.getOrderedConcreteStoresInGroup( store );
        }
        else
        {
            final StoreKey sk = new StoreKey( st, store );
            stores.add( storeManager.getArtifactStore( sk ) );
        }

        return stores;
    }

}
