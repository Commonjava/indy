package org.commonjava.aprox.depgraph.rest.render;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.copy;
import static org.apache.commons.lang.StringUtils.join;
import static org.commonjava.maven.galley.util.UrlUtils.buildUrl;
import static org.commonjava.web.json.ser.ServletSerializerUtils.fromRequestBody;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
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

import org.commonjava.aprox.depgraph.dto.WebOperationConfigDTO;
import org.commonjava.aprox.depgraph.inject.DepgraphSpecific;
import org.commonjava.aprox.depgraph.util.RequestAdvisor;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.galley.CacheOnlyLocation;
import org.commonjava.aprox.model.galley.KeyedLocation;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.ops.ResolveOps;
import org.commonjava.maven.galley.TransferException;
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
    private RequestAdvisor requestAdvisor;

    @Inject
    private LocationExpander locationExpander;

    @POST
    @Path( "/urlmap" )
    @Produces( "application/json" )
    public Response getUrlMap( @Context final HttpServletRequest req, @Context final HttpServletResponse resp, @Context final UriInfo info )
    {
        final Map<ProjectVersionRef, Map<String, Object>> result = new HashMap<>();

        try
        {
            final WebOperationConfigDTO dto = readDTO( req );

            final Map<ProjectVersionRef, Map<ArtifactRef, Transfer>> contents = resolveContents( dto, req );

            for ( final Entry<ProjectVersionRef, Map<ArtifactRef, Transfer>> entry : contents.entrySet() )
            {
                final ProjectVersionRef gav = entry.getKey();
                final Map<ArtifactRef, Transfer> items = entry.getValue();

                final Map<String, Object> data = new HashMap<>();
                result.put( gav, data );

                final Set<String> files = new HashSet<>();
                data.put( URLMAP_DATA_FILES, files );

                for ( final Transfer item : items.values() )
                {
                    if ( !data.containsKey( URLMAP_DATA_REPO_URL ) )
                    {
                        data.put( URLMAP_DATA_REPO_URL, formatUrlMapRepositoryUrl( item, info, dto.getLocalUrls() ) );
                    }

                    files.add( item.getDetachedFile()
                                   .getName() );
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

            final Map<ProjectVersionRef, Map<ArtifactRef, Transfer>> contents = resolveContents( dto, req );

            for ( final Map<ArtifactRef, Transfer> items : contents.values() )
            {
                for ( final Transfer item : items.values() )
                {
                    logger.info( "Adding: '%s'", item.getPath() );
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

        final String output = join( downLog, "\n" );

        return Response.ok( output )
                       .type( "text/plain" )
                       .build();
    }

    @POST
    @Path( "/zip" )
    @Produces( "application/zip" )
    public Response getZipRepository( @Context final HttpServletRequest req, @Context final HttpServletResponse resp )
    {
        Response response = Response.noContent()
                                    .build();

        ZipOutputStream stream = null;
        try
        {
            final WebOperationConfigDTO dto = readDTO( req );

            final Map<ProjectVersionRef, Map<ArtifactRef, Transfer>> contents = resolveContents( dto, req );

            final OutputStream os = resp.getOutputStream();
            stream = new ZipOutputStream( os );

            final Set<String> seenPaths = new HashSet<>();

            for ( final Map<ArtifactRef, Transfer> items : contents.values() )
            {

                for ( final Transfer item : items.values() )
                {
                    final String path = item.getPath();
                    if ( seenPaths.contains( path ) )
                    {
                        continue;
                    }

                    seenPaths.add( path );

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
            logger.error( "Failed to generate runtime repository. Reason: %s", e, e.getMessage() );

            return Response.serverError()
                           .build();
        }
        catch ( final AproxWorkflowException e )
        {
            return e.getResponse();
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

    private String formatDownlogEntry( final Transfer item, final UriInfo info, final boolean localUrls )
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

    private String formatUrlMapRepositoryUrl( final Transfer item, final UriInfo info, final boolean localUrls )
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
                                .build();

            return uri.toURL()
                      .toExternalForm();
        }
        else
        {
            return item.getLocation()
                       .getUri();
        }
    }

    private Map<ProjectVersionRef, Map<ArtifactRef, Transfer>> resolveContents( final WebOperationConfigDTO dto, final HttpServletRequest req )
        throws AproxWorkflowException
    {
        if ( dto == null )
        {
            logger.warn( "Repository archive configuration is missing." );
            throw new AproxWorkflowException( Status.BAD_REQUEST, "JSON configuration not supplied" );
        }

        final ProjectRelationshipFilter presetFilter = requestAdvisor.getPresetFilter( dto.getPreset() );
        dto.setFilter( presetFilter );

        if ( !dto.isValid() )
        {
            logger.warn( "Repository archive configuration is invalid: %s", dto );
            throw new AproxWorkflowException( Status.BAD_REQUEST, "Invalid configuration: %s", dto );
        }

        Map<ProjectVersionRef, Map<ArtifactRef, Transfer>> contents;
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
        final WebOperationConfigDTO dto = fromRequestBody( req, serializer, WebOperationConfigDTO.class );
        logger.info( "Got configuration:\n\n%s\n\n", serializer.toString( dto ) );

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
