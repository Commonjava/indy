/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.depgraph.rest;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.copy;
import static org.apache.commons.lang.StringUtils.join;
import static org.commonjava.maven.galley.util.UrlUtils.buildUrl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.depgraph.conf.AproxDepgraphConfig;
import org.commonjava.aprox.depgraph.dto.WebOperationConfigDTO;
import org.commonjava.aprox.depgraph.inject.DepgraphSpecific;
import org.commonjava.aprox.depgraph.util.ConfigDTOHelper;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.galley.CacheOnlyLocation;
import org.commonjava.aprox.model.galley.KeyedLocation;
import org.commonjava.aprox.util.ApplicationStatus;
import org.commonjava.aprox.util.UriFormatter;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.ops.ResolveOps;
import org.commonjava.maven.cartographer.preset.PresetSelector;
import org.commonjava.maven.cartographer.util.ProjectVersionRefComparator;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.TransferManager;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferBatch;
import org.commonjava.util.logging.Logger;
import org.commonjava.web.json.ser.JsonSerializer;

@ApplicationScoped
public class RepositoryController
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
    private TransferManager transferManager;

    @Inject
    private PresetSelector presets;

    @Inject
    private AproxDepgraphConfig config;

    @Inject
    private ConfigDTOHelper configHelper;

    public String getUrlMap( final InputStream configStream, final String baseUri, final UriFormatter uriFormatter )
        throws AproxWorkflowException
    {
        final WebOperationConfigDTO dto = configHelper.readWebOperationDTO( configStream );
        return getUrlMap( dto, baseUri, uriFormatter );
    }

    public String getUrlMap( final String json, final String baseUri, final UriFormatter uriFormatter )
        throws AproxWorkflowException
    {
        final WebOperationConfigDTO dto = configHelper.readWebOperationDTO( json );
        return getUrlMap( dto, baseUri, uriFormatter );
    }

    private String getUrlMap( final WebOperationConfigDTO dto, final String baseUri, final UriFormatter uriFormatter )
        throws AproxWorkflowException
    {
        final Map<ProjectVersionRef, Map<String, Object>> result = new LinkedHashMap<ProjectVersionRef, Map<String, Object>>();

        try
        {

            final Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> contents = resolveContents( dto );

            final List<ProjectVersionRef> topKeys = new ArrayList<ProjectVersionRef>( contents.keySet() );
            Collections.sort( topKeys, new ProjectVersionRefComparator() );

            for ( final ProjectVersionRef gav : topKeys )
            {
                final Map<ArtifactRef, ConcreteResource> items = contents.get( gav );

                final Map<String, Object> data = new HashMap<String, Object>();
                result.put( gav, data );

                final Set<String> files = new HashSet<String>();
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

                final List<String> sortedFiles = new ArrayList<String>( files );
                Collections.sort( sortedFiles );
                data.put( URLMAP_DATA_REPO_URL, formatUrlMapRepositoryUrl( kl, dto.getLocalUrls(), baseUri, uriFormatter ) );
                data.put( URLMAP_DATA_FILES, sortedFiles );
            }
        }
        catch ( final MalformedURLException e )
        {
            throw new AproxWorkflowException( "Failed to generate runtime repository. Reason: %s", e, e.getMessage() );
        }

        return serializer.toString( result );
    }

    public String getDownloadLog( final InputStream configStream, final String baseUri, final UriFormatter uriFormatter )
        throws AproxWorkflowException
    {
        final WebOperationConfigDTO dto = configHelper.readWebOperationDTO( configStream );
        return getDownloadLog( dto, baseUri, uriFormatter );
    }

    public String getDownloadLog( final String json, final String baseUri, final UriFormatter uriFormatter )
        throws AproxWorkflowException
    {
        final WebOperationConfigDTO dto = configHelper.readWebOperationDTO( json );
        return getDownloadLog( dto, baseUri, uriFormatter );
    }

    public String getDownloadLog( final WebOperationConfigDTO dto, final String baseUri, final UriFormatter uriFormatter )
        throws AproxWorkflowException
    {
        final Set<String> downLog = new HashSet<String>();
        try
        {
            final Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> contents = resolveContents( dto );

            final List<ProjectVersionRef> refs = new ArrayList<ProjectVersionRef>( contents.keySet() );
            Collections.sort( refs );

            for ( final ProjectVersionRef ref : refs )
            {
                final Map<ArtifactRef, ConcreteResource> items = contents.get( ref );
                for ( final ConcreteResource item : items.values() )
                {
                    logger.info( "Adding: '%s'", item );
                    downLog.add( formatDownlogEntry( item, dto.getLocalUrls(), baseUri, uriFormatter ) );
                }
            }
        }
        catch ( final MalformedURLException e )
        {
            throw new AproxWorkflowException( "Failed to generate runtime repository. Reason: %s", e, e.getMessage() );
        }

        final List<String> sorted = new ArrayList<String>( downLog );
        Collections.sort( sorted );

        return join( sorted, "\n" );
    }

    public void getZipRepository( final InputStream configStream, final OutputStream zipStream )
        throws AproxWorkflowException
    {
        final WebOperationConfigDTO dto = configHelper.readWebOperationDTO( configStream );
        getZipRepository( dto, zipStream );
    }

    public void getZipRepository( final String json, final OutputStream zipStream )
        throws AproxWorkflowException
    {
        final WebOperationConfigDTO dto = configHelper.readWebOperationDTO( json );
        getZipRepository( dto, zipStream );
    }

    public void getZipRepository( final WebOperationConfigDTO dto, final OutputStream zipStream )
        throws AproxWorkflowException
    {
        ZipOutputStream stream = null;
        try
        {
            final Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> contents = resolveContents( dto );

            final Set<ConcreteResource> entries = new HashSet<ConcreteResource>();
            final Set<String> seenPaths = new HashSet<String>();

            logger.info( "Iterating contents with %d GAVs.", contents.size() );
            for ( final Map<ArtifactRef, ConcreteResource> artifactResources : contents.values() )
            {
                for ( final Entry<ArtifactRef, ConcreteResource> entry : artifactResources.entrySet() )
                {
                    final ArtifactRef ref = entry.getKey();
                    final ConcreteResource resource = entry.getValue();

                    //                        logger.info( "Checking %s (%s) for inclusion...", ref, resource );

                    final String path = resource.getPath();
                    if ( seenPaths.contains( path ) )
                    {
                        logger.info( "Conflicting path: %s. Skipping %s.", path, ref );
                        continue;
                    }

                    seenPaths.add( path );

                    //                        logger.info( "Adding to batch: %s via resource: %s", ref, resource );
                    entries.add( resource );
                }
            }

            logger.info( "Starting batch retrieval of %d artifacts.", entries.size() );
            TransferBatch batch = new TransferBatch( entries );
            batch = transferManager.batchRetrieve( batch );

            logger.info( "Retrieved %d artifacts. Creating zip.", batch.getTransfers()
                                                                       .size() );

            // FIXME: Stream to a temp file, then pass that to the Response.ok() handler...
            stream = new ZipOutputStream( zipStream );

            final List<Transfer> items = new ArrayList<Transfer>( batch.getTransfers()
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
                //                    logger.info( "Adding: %s", item );
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
    }

    private String formatDownlogEntry( final ConcreteResource item, final boolean localUrls, final String baseUri, final UriFormatter uriFormatter )
        throws MalformedURLException
    {
        final KeyedLocation kl = (KeyedLocation) item.getLocation();
        final StoreKey key = kl.getKey();

        if ( localUrls || kl instanceof CacheOnlyLocation )
        {
            final String uri = uriFormatter.formatAbsolutePathTo( baseUri, key.getType()
                                                                              .singularEndpointName(), key.getName() );
            return String.format( "Downloading: %s", uri );
        }
        else
        {
            return "Downloading: " + buildUrl( item.getLocation()
                                                   .getUri(), item.getPath() );
        }
    }

    private String formatUrlMapRepositoryUrl( final KeyedLocation kl, final boolean localUrls, final String baseUri, final UriFormatter uriFormatter )
        throws MalformedURLException
    {
        if ( localUrls || kl instanceof CacheOnlyLocation )
        {
            final StoreKey key = kl.getKey();
            return uriFormatter.formatAbsolutePathTo( baseUri, key.getType()
                                                                  .singularEndpointName(), key.getName() );
        }
        else
        {
            return kl.getUri();
        }
    }

    private Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> resolveContents( final WebOperationConfigDTO dto )
        throws AproxWorkflowException
    {
        if ( dto == null )
        {
            logger.warn( "Repository archive configuration is missing." );
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST, "JSON configuration not supplied" );
        }

        dto.resolveFilters( presets, config.getDefaultWebFilterPreset() );

        if ( !dto.isValid() )
        {
            logger.warn( "Repository archive configuration is invalid: %s", dto );
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST, "Invalid configuration: %s", dto );
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

}
