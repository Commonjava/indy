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
package org.commonjava.aprox.indexer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.maven.index.updater.ResourceFetcher;
import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.filer.FileManager;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.util.ApplicationStatus;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AproxResourceFetcher
    implements ResourceFetcher
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final StoreDataManager storeDataManager;

    private final FileManager fileManager;

    private ArtifactStore store;

    public AproxResourceFetcher( final StoreDataManager storeDataManager, final FileManager fileManager )
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
        catch ( final ProxyDataException e )
        {
            logger.error( "Failed to lookup store: {}. Reason: {}", e, id, e.getMessage() );
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
            logger.error( "Failed to retrieve: {} from: {}. Reason: {}", e, path, store.getKey(), e );
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
