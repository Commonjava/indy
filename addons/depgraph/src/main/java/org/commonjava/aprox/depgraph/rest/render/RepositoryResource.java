package org.commonjava.aprox.depgraph.rest.render;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.copy;
import static org.apache.commons.lang.StringUtils.join;
import static org.commonjava.maven.galley.util.UrlUtils.buildUrl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.depgraph.conf.AproxDepgraphConfig;
import org.commonjava.aprox.depgraph.dto.WebOperationConfigDTO;
import org.commonjava.aprox.depgraph.inject.DepgraphSpecific;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.galley.CacheOnlyLocation;
import org.commonjava.aprox.model.galley.KeyedLocation;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.ops.ResolveOps;
import org.commonjava.maven.cartographer.preset.PresetSelector;
import org.commonjava.maven.cartographer.util.ProjectVersionRefComparator;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.TransferManager;
import org.commonjava.maven.galley.model.ArtifactBatch;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.spi.transport.LocationExpander;
import org.commonjava.util.logging.Logger;
import org.commonjava.web.json.ser.JsonSerializer;

@Path( "/depgraph/repo" )
public class RepositoryResource
{

    private static final String URLMAP_DATA_REPO_URL = "repoUrl";

    private static final String URLMAP_DATA_FILES = "files";

    private final Logger logger = new Logger( getClass() );

    @Inject
    private ResolveOps ops;

    @Inject
    @DepgraphSpecific
    private JsonSerializer serializer;

    @Inject
    private LocationExpander locationExpander;

    @Inject
    private TransferManager transferManager;

    @Inject
    private PresetSelector presets;

    @Inject
    private AproxDepgraphConfig config;

    @POST
    @Path( "/urlmap" )
    @Produces( "application/json" )
    public Response getUrlMap( @Context final HttpServletRequest req, @Context final HttpServletResponse resp, @Context final UriInfo info )
    {
        final Map<ProjectVersionRef, Map<String, Object>> result = new LinkedHashMap<>();

        try
        {
            final WebOperationConfigDTO dto = readDTO( req );

            final Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> contents = resolveContents( dto, req );

            final List<ProjectVersionRef> topKeys = new ArrayList<>( contents.keySet() );
            Collections.sort( topKeys, new ProjectVersionRefComparator() );

            for ( final ProjectVersionRef gav : topKeys )
            {
                final Map<ArtifactRef, ConcreteResource> items = contents.get( gav );

                final Map<String, Object> data = new HashMap<>();
                result.put( gav, data );

                final Set<String> files = new HashSet<>();
                KeyedLocation kl = null;

                for ( final ConcreteResource item : items.values() )
                {
                    final KeyedLocation loc = (KeyedLocation) item.getLocation();

                    // FIXME: we're squashing some potential variation in the locations here!
                    // if we're not looking for local urls, allow any cache-only location to be overridden...
                    if ( kl == null || ( !dto.getLocalUrls() && ( kl instanceof CacheOnlyLocation ) ) )
                    {
                        kl = loc;
                    }

                    logger.info( "Adding %s (keyLocation: %s)", item, kl );
                    files.add( new File( item.getPath() ).getName() );
                }

                final List<String> sortedFiles = new ArrayList<>( files );
                Collections.sort( sortedFiles );
                data.put( URLMAP_DATA_REPO_URL, formatUrlMapRepositoryUrl( kl, info, dto.getLocalUrls() ) );
                data.put( URLMAP_DATA_FILES, sortedFiles );
            }
        }
        catch ( final MalformedURLException e )
        {
            logger.error( "Failed to generate runtime repository. Reason: %s", e, e.getMessage() );

            return Response.serverError()
                           .build();
        }
        catch ( final AproxWorkflowException e )
        {
            return e.getResponse();
        }

        final String json = serializer.toString( result );

        return Response.ok( json )
                       .type( "application/json" )
                       .build();
    }

    @POST
    @Path( "/downlog" )
    @Produces( "text/plain" )
    public Response getDownloadLog( @Context final HttpServletRequest req, @Context final HttpServletResponse resp, @Context final UriInfo info )
    {
        final Set<String> downLog = new HashSet<>();
        try
        {
            final WebOperationConfigDTO dto = readDTO( req );

            final Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> contents = resolveContents( dto, req );

            final List<ProjectVersionRef> refs = new ArrayList<>( contents.keySet() );
            Collections.sort( refs );

            for ( final ProjectVersionRef ref : refs )
            {
                final Map<ArtifactRef, ConcreteResource> items = contents.get( ref );
                for ( final ConcreteResource item : items.values() )
                {
                    logger.info( "Adding: '%s'", item );
                    downLog.add( formatDownlogEntry( item, info, dto.getLocalUrls() ) );
                }
            }
        }
        catch ( final MalformedURLException e )
        {
            logger.error( "Failed to generate runtime repository. Reason: %s", e, e.getMessage() );

            return Response.serverError()
                           .build();
        }
        catch ( final AproxWorkflowException e )
        {
            return e.getResponse();
        }

        final List<String> sorted = new ArrayList<>( downLog );
        Collections.sort( sorted );

        final String output = join( sorted, "\n" );

        return Response.ok( output )
                       .type( "text/plain" )
                       .build();
    }

    @POST
    @Path( "/zip" )
    @Produces( "application/zip" )
    public Response getZipRepository( @Context final HttpServletRequest req, @Context final HttpServletResponse resp )
    {
        try
        {
            Response response = Response.noContent()
                                        .build();

            ZipOutputStream stream = null;
            try
            {
                final WebOperationConfigDTO dto = readDTO( req );

                final Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> contents = resolveContents( dto, req );

                final Map<ArtifactRef, List<? extends Location>> entries = new HashMap<>();
                final Set<String> seenPaths = new HashSet<>();

                for ( final Map<ArtifactRef, ConcreteResource> artifactResources : contents.values() )
                {
                    for ( final Entry<ArtifactRef, ConcreteResource> entry : artifactResources.entrySet() )
                    {
                        final ArtifactRef ref = entry.getKey();
                        final ConcreteResource resource = entry.getValue();

                        final String path = resource.getPath();
                        if ( seenPaths.contains( path ) )
                        {
                            continue;
                        }

                        seenPaths.add( path );

                        entries.put( ref, Arrays.asList( resource.getLocation() ) );
                    }
                }

                ArtifactBatch batch = new ArtifactBatch( entries );
                batch = transferManager.batchRetrieve( batch );

                final OutputStream os = resp.getOutputStream();
                stream = new ZipOutputStream( os );

                final List<Transfer> items = new ArrayList<>( batch.getTransfers()
                                                                   .values() );
                Collections.sort( items, new Comparator<Transfer>()
                {
                    @Override
                    public int compare( final Transfer f, final Transfer s )
                    {
                        return f.getPath()
                                .compareTo( s.getPath() );
                    }
                } );

                for ( final Transfer item : items )
                {
                    final String path = item.getPath();
                    if ( item != null )
                    {
                        final ZipEntry ze = new ZipEntry( path );
                        stream.putNextEntry( ze );

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
                throw new AproxWorkflowException( "Failed to generate runtime repository. Reason: %s", e, e.getMessage() );
            }
            catch ( final TransferException e )
            {
                throw new AproxWorkflowException( "Failed to generate runtime repository. Reason: %s", e, e.getMessage() );
            }
            finally
            {
                closeQuietly( stream );
            }

            response = Response.ok()
                               .type( "application/zip" )
                               .build();

            return response;
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return e.getResponse();
        }

    }

    private String formatDownlogEntry( final ConcreteResource item, final UriInfo info, final boolean localUrls )
        throws MalformedURLException
    {
        final KeyedLocation kl = (KeyedLocation) item.getLocation();
        final StoreKey key = kl.getKey();

        if ( localUrls || kl instanceof CacheOnlyLocation )
        {
            final URI uri = info.getBaseUriBuilder()
                                .path( key.getType()
                                          .singularEndpointName() )
                                .path( key.getName() )
                                .path( item.getPath() )
                                .build();

            return String.format( "Downloading: %s", uri.toURL()
                                                        .toExternalForm() );
        }
        else
        {
            return "Downloading: " + buildUrl( item.getLocation()
                                                   .getUri(), item.getPath() );
        }
    }

    private String formatUrlMapRepositoryUrl( final KeyedLocation kl, final UriInfo info, final boolean localUrls )
        throws MalformedURLException
    {
        if ( localUrls || kl instanceof CacheOnlyLocation )
        {
            final StoreKey key = kl.getKey();
            final URI uri = info.getBaseUriBuilder()
                                .path( key.getType()
                                          .singularEndpointName() )
                                .path( key.getName() )
                                .build();

            return uri.toURL()
                      .toExternalForm();
        }
        else
        {
            return kl.getUri();
        }
    }

    private Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> resolveContents( final WebOperationConfigDTO dto, final HttpServletRequest req )
        throws AproxWorkflowException
    {
        if ( dto == null )
        {
            logger.warn( "Repository archive configuration is missing." );
            throw new AproxWorkflowException( Status.BAD_REQUEST, "JSON configuration not supplied" );
        }

        dto.resolveFilters( presets, config.getDefaultWebFilterPreset() );

        if ( !dto.isValid() )
        {
            logger.warn( "Repository archive configuration is invalid: %s", dto );
            throw new AproxWorkflowException( Status.BAD_REQUEST, "Invalid configuration: %s", dto );
        }

        Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> contents;
        try
        {
            contents = ops.resolveRepositoryContents( dto );
        }
        catch ( final CartoDataException e )
        {
            logger.error( "Failed to resolve repository contents for: %s. Reason: %s", e, dto, e.getMessage() );
            throw new AproxWorkflowException( "Failed to resolve repository contents for: %s. Reason: %s", e, dto, e.getMessage() );
        }

        return contents;
    }

    private WebOperationConfigDTO readDTO( final HttpServletRequest req )
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
        final WebOperationConfigDTO dto = serializer.fromString( json, WebOperationConfigDTO.class );

        try
        {
            dto.calculateLocations( locationExpander );
        }
        catch ( final TransferException e )
        {
            throw new AproxWorkflowException( Status.BAD_REQUEST, "One or more sources/excluded sources is invalid: %s", e, e.getMessage() );
        }

        return dto;
    }

}
