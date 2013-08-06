package org.commonjava.aprox.indexer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.core.Response.Status;

import org.apache.maven.index.updater.ResourceFetcher;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.filer.FileManager;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.util.logging.Logger;

public class AproxResourceFetcher
    implements ResourceFetcher
{

    private final Logger logger = new Logger( getClass() );

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
            logger.info( "Looking up store: %s (from original id: %s, url was: %s)", key, id, url );
            store = storeDataManager.getArtifactStore( key );

            if ( store == null )
            {
                throw new IOException( String.format( "No such repository: %s.", id ) );
            }
        }
        catch ( final ProxyDataException e )
        {
            logger.error( "Failed to lookup store: %s. Reason: %s", e, id, e.getMessage() );
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
            logger.info( "Retrieving: '%s' from store: %s", path, store.getKey() );
            final Transfer item = fileManager.retrieve( store, path );

            if ( item == null || !item.exists() )
            {
                throw new FileNotFoundException( path );
            }

            return item.openInputStream();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( "Failed to retrieve: %s from: %s. Reason: %s", e, path, store.getKey(), e );
            if ( e.getResponse()
                  .getStatus() == Status.NOT_FOUND.getStatusCode() )
            {
                throw new FileNotFoundException( name );
            }
            else
            {
                throw new IOException( String.format( "Failed to retrieve: %s from: %s. Reason: %s", path,
                                                      store.getKey(), e ), e );
            }
        }
    }

}
