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
package org.commonjava.aprox.indexer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.maven.index.updater.ResourceFetcher;
import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.content.DownloadManager;
import org.commonjava.aprox.data.AproxDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.util.ApplicationStatus;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AproxResourceFetcher
    implements ResourceFetcher
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final StoreDataManager storeDataManager;

    private final DownloadManager fileManager;

    private ArtifactStore store;

    public AproxResourceFetcher( final StoreDataManager storeDataManager, final DownloadManager fileManager )
    {
        this.storeDataManager = storeDataManager;
        this.fileManager = fileManager;
    }

    @Override
    public void connect( final String id, final String url )
        throws IOException
    {
        try
        {
            final StoreKey key = StoreKey.fromString( id );
            logger.info( "Looking up store: {} (from original id: {}, url was: {})", key, id, url );
            store = storeDataManager.getArtifactStore( key );

            if ( store == null )
            {
                throw new IOException( String.format( "No such repository: %s.", id ) );
            }
        }
        catch ( final AproxDataException e )
        {
            logger.error( String.format( "Failed to lookup store: %s. Reason: %s", id, e.getMessage() ), e );
            throw new IOException( String.format( "Failed to lookup store: %s. Reason: %s", id, e.getMessage() ), e );
        }
    }

    @Override
    public void disconnect()
        throws IOException
    {
    }

    @Override
    public InputStream retrieve( final String name )
        throws IOException, FileNotFoundException
    {
        final String path = "/.index/" + name;
        try
        {
            logger.info( "Retrieving: '{}' from store: {}", path, store.getKey() );
            final Transfer item = fileManager.retrieve( store, path );

            if ( item == null || !item.exists() )
            {
                throw new FileNotFoundException( path );
            }

            return item.openInputStream();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( String.format( "Failed to retrieve: %s from: %s. Reason: %s", path, store.getKey(), e ), e );
            if ( e.getStatus() == ApplicationStatus.NOT_FOUND.code() )
            {
                throw new FileNotFoundException( name );
            }
            else
            {
                throw new IOException( String.format( "Failed to retrieve: %s from: %s. Reason: %s", path, store.getKey(), e ), e );
            }
        }
    }

}
