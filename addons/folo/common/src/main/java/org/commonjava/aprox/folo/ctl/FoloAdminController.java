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
package org.commonjava.aprox.folo.ctl;

import java.net.MalformedURLException;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.content.ContentDigest;
import org.commonjava.aprox.content.ContentManager;
import org.commonjava.aprox.content.DownloadManager;
import org.commonjava.aprox.data.AproxDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.folo.data.FoloContentException;
import org.commonjava.aprox.folo.data.FoloRecordManager;
import org.commonjava.aprox.folo.dto.TrackedContentDTO;
import org.commonjava.aprox.folo.dto.TrackedContentEntryDTO;
import org.commonjava.aprox.folo.model.AffectedStoreRecord;
import org.commonjava.aprox.folo.model.TrackedContentRecord;
import org.commonjava.aprox.folo.model.TrackingKey;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.util.ApplicationStatus;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class FoloAdminController
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private FoloRecordManager recordManager;

    @Inject
    private DownloadManager downloadManager;

    @Inject
    private ContentManager contentManager;

    @Inject
    private StoreDataManager storeManager;

    protected FoloAdminController()
    {
    }

    public FoloAdminController( final FoloRecordManager recordManager, final DownloadManager downloadManager )
    {
        this.recordManager = recordManager;
        this.downloadManager = downloadManager;
    }

    public TrackedContentDTO renderReport( final String id, final String apiBaseUrl )
        throws AproxWorkflowException
    {
        final TrackingKey tk = new TrackingKey( id );
        try
        {
            logger.debug( "Retrieving tracking record for: {}", tk );
            final TrackedContentRecord record = recordManager.getRecord( tk );
            logger.debug( "Got: {}", record );

            if ( record == null )
            {
                throw new AproxWorkflowException( ApplicationStatus.NOT_FOUND.code(),
                                                  "No tracking record available for: %s", tk );
            }

            final Set<TrackedContentEntryDTO> uploads = new TreeSet<>();
            final Set<TrackedContentEntryDTO> downloads = new TreeSet<>();
            for ( final AffectedStoreRecord asr : record )
            {
                final StoreKey sk = asr.getKey();

                Set<String> paths = asr.getUploadedPaths();
                if ( paths != null )
                {
                    addEntries( uploads, sk, paths, apiBaseUrl );
                }

                paths = asr.getDownloadedPaths();
                if ( paths != null )
                {
                    addEntries( downloads, sk, paths, apiBaseUrl );
                }
            }

            return new TrackedContentDTO( tk, uploads, downloads );
        }
        catch ( final FoloContentException e )
        {
            throw new AproxWorkflowException( "Failed to retrieve record: %s. Reason: %s", e, tk, e.getMessage() );
        }
    }

    private void addEntries( final Set<TrackedContentEntryDTO> entries, final StoreKey key, final Set<String> paths,
                             final String apiBaseUrl )
        throws AproxWorkflowException
    {
        for ( final String path : paths )
        {
            final Transfer txfr = downloadManager.getStorageReference( key, path );
            if ( txfr != null )
            {
                final TrackedContentEntryDTO entry = new TrackedContentEntryDTO( key, path );

                try
                {
                    final String localUrl =
                        UrlUtils.buildUrl( apiBaseUrl, key.getType()
                                                                    .singularEndpointName(), key.getName(), path );

                    String remoteUrl = null;
                    if ( StoreType.remote == key.getType() )
                    {
                        final RemoteRepository repo = storeManager.getRemoteRepository( key.getName() );
                        if ( repo != null )
                        {
                            remoteUrl = UrlUtils.buildUrl( repo.getUrl(), path );
                        }
                    }

                    entry.setLocalUrl( localUrl );
                    entry.setOriginUrl( remoteUrl );

                    final Map<ContentDigest, String> digests =
                        contentManager.digest( key, path, ContentDigest.MD5, ContentDigest.SHA_256 );

                    entry.setMd5( digests.get( ContentDigest.MD5 ) );
                    entry.setSha256( digests.get( ContentDigest.SHA_256 ) );

                    entries.add( entry );
                }
                catch ( final AproxDataException e )
                {
                    throw new AproxWorkflowException(
                                                      "Cannot retrieve RemoteRepository: %s to calculate remote URL for: %s. Reason: %s",
                                                      e, key, path, e.getMessage() );
                }
                catch ( final MalformedURLException e )
                {
                    throw new AproxWorkflowException( "Cannot format URL. Reason: %s", e, e.getMessage() );
                }
            }
        }
    }

    public TrackedContentRecord getRecord( final String id )
        throws AproxWorkflowException
    {
        final TrackingKey tk = new TrackingKey( id );
        try
        {
            return recordManager.getRecord( tk );
        }
        catch ( final FoloContentException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.NOT_FOUND.code(),
                                              "Failed to retrieve record: %s. Reason: %s", e, tk, e.getMessage() );
        }
    }

    public void clearRecord( final String id )
    {
        final TrackingKey tk = new TrackingKey( id );
        recordManager.clearRecord( tk );
    }

    public boolean hasRecord( final String id )
    {
        return recordManager.hasRecord( new TrackingKey( id ) );
    }

    public void initRecord( final String id )
        throws AproxWorkflowException
    {
        try
        {
            recordManager.initRecord( new TrackingKey( id ) );
        }
        catch ( final FoloContentException e )
        {
            throw new AproxWorkflowException( "Failed to initialize tracking record for: %s. Reason: %s", e, id,
                                              e.getMessage() );
        }
    }

}
