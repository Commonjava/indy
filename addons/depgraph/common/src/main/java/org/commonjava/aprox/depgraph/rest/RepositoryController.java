/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.aprox.depgraph.rest;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.copy;
import static org.commonjava.maven.galley.util.UrlUtils.buildUrl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.depgraph.dto.DownlogDTO;
import org.commonjava.aprox.depgraph.dto.DownlogRequest;
import org.commonjava.aprox.depgraph.dto.UrlMapDTO;
import org.commonjava.aprox.depgraph.util.RecipeHelper;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.galley.CacheOnlyLocation;
import org.commonjava.aprox.model.galley.KeyedLocation;
import org.commonjava.aprox.util.ApplicationStatus;
import org.commonjava.aprox.util.UriFormatter;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.CartoRequestException;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.ops.ResolveOps;
import org.commonjava.maven.cartographer.request.RepositoryContentRequest;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.TransferManager;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferBatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class RepositoryController
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ResolveOps ops;

    @Inject
    private TransferManager transferManager;

    @Inject
    private RecipeHelper configHelper;


    public UrlMapDTO getUrlMap( final InputStream configStream, final String baseUri, final UriFormatter uriFormatter )
        throws AproxWorkflowException
    {
        final RepositoryContentRequest dto = configHelper.readRecipe( configStream, RepositoryContentRequest.class );
        return getUrlMap( dto, baseUri, uriFormatter );
    }

    public UrlMapDTO getUrlMap( final String json, final String baseUri, final UriFormatter uriFormatter )
        throws AproxWorkflowException
    {
        final RepositoryContentRequest dto = configHelper.readRecipe( json, RepositoryContentRequest.class );
        return getUrlMap( dto, baseUri, uriFormatter );
    }

    public UrlMapDTO getUrlMap( final RepositoryContentRequest recipe, final String baseUri,
                                final UriFormatter uriFormatter )
        throws AproxWorkflowException
    {
        final Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> contents = resolveContents( recipe );
        return new UrlMapDTO( contents, recipe, baseUri, uriFormatter );
    }

    public DownlogDTO getDownloadLog(final InputStream configStream, final String baseUri, final UriFormatter uriFormatter)
        throws AproxWorkflowException
    {
        final DownlogRequest dto = configHelper.readDownlogDTO( configStream );
        return getDownloadLog( dto, baseUri, uriFormatter );
    }

    public DownlogDTO getDownloadLog(final String json, final String baseUri, final UriFormatter uriFormatter)
        throws AproxWorkflowException
    {
        final DownlogRequest dto = configHelper.readDownlogDTO( json );
        return getDownloadLog( dto, baseUri, uriFormatter );
    }

    public DownlogDTO getDownloadLog( final DownlogRequest recipe, final String baseUri, final UriFormatter uriFormatter )
        throws AproxWorkflowException
    {
        final Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> contents = resolveContents( recipe );
        return new DownlogDTO(contents, recipe, baseUri, uriFormatter);
    }

    public void getZipRepository( final InputStream configStream, final OutputStream zipStream )
        throws AproxWorkflowException
    {
        final RepositoryContentRequest dto = configHelper.readRecipe( configStream, RepositoryContentRequest.class );
        getZipRepository( dto, zipStream );
    }

    public void getZipRepository( final String json, final OutputStream zipStream )
        throws AproxWorkflowException
    {
        final RepositoryContentRequest dto = configHelper.readRecipe( json, RepositoryContentRequest.class );
        getZipRepository( dto, zipStream );
    }

    public void getZipRepository( final RepositoryContentRequest dto, final OutputStream zipStream )
        throws AproxWorkflowException
    {
        ZipOutputStream stream = null;
        try
        {
            final Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> contents = resolveContents( dto );

            final Set<ConcreteResource> entries = new HashSet<ConcreteResource>();
            final Set<String> seenPaths = new HashSet<String>();

            logger.info( "Iterating contents with {} GAVs.", contents.size() );
            for ( final Map<ArtifactRef, ConcreteResource> artifactResources : contents.values() )
            {
                for ( final Entry<ArtifactRef, ConcreteResource> entry : artifactResources.entrySet() )
                {
                    final ArtifactRef ref = entry.getKey();
                    final ConcreteResource resource = entry.getValue();

                    //                        logger.info( "Checking {} ({}) for inclusion...", ref, resource );

                    final String path = resource.getPath();
                    if ( seenPaths.contains( path ) )
                    {
                        logger.warn( "Conflicting path: {}. Skipping {}.", path, ref );
                        continue;
                    }

                    seenPaths.add( path );

                    //                        logger.info( "Adding to batch: {} via resource: {}", ref, resource );
                    entries.add( resource );
                }
            }

            logger.info( "Starting batch retrieval of {} artifacts.", entries.size() );
            TransferBatch batch = new TransferBatch( entries );
            batch = transferManager.batchRetrieve( batch, new EventMetadata() );

            logger.info( "Retrieved {} artifacts. Creating zip.", batch.getTransfers()
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
                //                    logger.info( "Adding: {}", item );
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
        catch ( final IOException | TransferException e )
        {
            throw new AproxWorkflowException( "Failed to generate runtime repository. Reason: {}", e, e.getMessage() );
        }
        finally
        {
            closeQuietly( stream );
        }
    }

    private String formatDownlogEntry( final ConcreteResource item, final DownlogRequest dto,
                                       final String baseUri, final UriFormatter uriFormatter )
        throws MalformedURLException
    {
        final KeyedLocation kl = (KeyedLocation) item.getLocation();
        final StoreKey key = kl.getKey();

        if ( dto.isPathOnly() )
        {
            final String prefix = dto.getLinePrefix();
            if ( prefix != null )
            {
                return prefix + item.getPath();
            }
            else
            {
                return item.getPath();
            }
        }

        if ( dto.getLocalUrls() || kl instanceof CacheOnlyLocation )
        {
            final String uri = uriFormatter.formatAbsolutePathTo( baseUri, key.getType()
                                                               .singularEndpointName(), key.getName(), item.getPath() );
            final String prefix = dto.getLinePrefix();
            if ( prefix != null )
            {
                return prefix + uri;
            }
            else
            {
                return uri;
            }
        }
        else
        {
            final String prefix = dto.getLinePrefix();
            if ( prefix != null )
            {
                return prefix + buildUrl( item.getLocation()
                                                   .getUri(), item.getPath() );
            }
            else
            {
                return buildUrl( item.getLocation()
                                     .getUri(), item.getPath() );
            }
        }
    }

    private Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> resolveContents( final RepositoryContentRequest recipe )
        throws AproxWorkflowException
    {
        configHelper.setRecipeDefaults( recipe );

        Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> contents;
        try
        {
            contents = ops.resolveRepositoryContents( recipe );
        }
        catch ( final CartoDataException e )
        {
            logger.error( String.format( "Failed to resolve repository contents for: %s. Reason: %s", recipe, e.getMessage() ), e );
            throw new AproxWorkflowException( "Failed to resolve repository contents for: {}. Reason: {}", e, recipe, e.getMessage() );
        }
        catch ( CartoRequestException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(), "Invalid request: %s. Reason: %s", e,
                                              recipe, e.getMessage() );
        }

        return contents;
    }

}
