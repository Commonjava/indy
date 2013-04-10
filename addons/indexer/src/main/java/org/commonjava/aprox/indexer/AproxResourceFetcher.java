package org.commonjava.aprox.indexer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.core.Response.Status;

import org.apache.maven.index.updater.ResourceFetcher;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.filer.FileManager;
import org.commonjava.aprox.io.StorageItem;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.rest.AproxWorkflowException;

public class AproxResourceFetcher
    implements ResourceFetcher
{

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
            store = storeDataManager.getArtifactStore( key );

            if ( store == null )
            {
                throw new IOException( String.format( "No such repository: %s.", id ) );
            }
        }
        catch ( final ProxyDataException e )
        {
            throw new IOException( String.format( "Failed to lookup repository: %s. Reason: %s", id, e.getMessage() ),
                                   e );
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
        try
        {
            final StorageItem item = fileManager.retrieve( store, name );

            return item == null ? null : item.openInputStream();
        }
        catch ( final AproxWorkflowException e )
        {
            if ( e.getResponse()
                  .getStatus() == Status.NOT_FOUND.getStatusCode() )
            {
                throw new FileNotFoundException( name );
            }
            else
            {
                throw new IOException( String.format( "Failed to retrieve: %s from: %s. Reason: %s", name,
                                                      store.getKey(), e.getMessage() ), e );
            }
        }
    }

}
