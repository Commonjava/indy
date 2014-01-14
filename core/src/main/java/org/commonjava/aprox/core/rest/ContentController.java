package org.commonjava.aprox.core.rest;

import java.io.InputStream;
import java.util.List;

import javax.activation.MimetypesFileTypeMap;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.filer.FileManager;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.aprox.rest.util.ApplicationStatus;
import org.commonjava.maven.galley.model.Transfer;

@ApplicationScoped
public class ContentController
{

    @Inject
    private StoreDataManager storeManager;

    @Inject
    private FileManager fileManager;

    protected ContentController()
    {
    }

    public ContentController( final StoreDataManager storeManager, final FileManager fileManager )
    {
        this.storeManager = storeManager;
        this.fileManager = fileManager;
    }

    public ApplicationStatus delete( final StoreType type, final String name, final String path )
        throws AproxWorkflowException
    {
        final ArtifactStore store = getStore( type, name );

        final boolean deleted = fileManager.delete( store, path );
        return deleted ? ApplicationStatus.OK : ApplicationStatus.NOT_FOUND;
    }

    public Transfer get( final StoreType type, final String name, final String path )
        throws AproxWorkflowException
    {
        final ArtifactStore store = getStore( type, name );
        final Transfer item = fileManager.retrieve( store, path );

        if ( item == null || item.isDirectory() )
        {
            throw new AproxWorkflowException( ApplicationStatus.NOT_FOUND, "%s", ( path + ( item == null ? " was not found." : "is a directory" ) ) );
        }

        return item;
    }

    public String getContentType( final String path )
    {
        return new MimetypesFileTypeMap().getContentType( path );
    }

    public Transfer store( final StoreType type, final String name, final String path, final InputStream stream )
        throws AproxWorkflowException
    {
        final ArtifactStore store = getStore( type, name );
        final Transfer item = fileManager.store( store, path, stream );

        return item;
    }

    public void rescan( final StoreKey key )
        throws AproxWorkflowException
    {
        final ArtifactStore artifactStore = getStore( key );
        fileManager.rescan( artifactStore );
    }

    public void rescanAll()
        throws AproxWorkflowException
    {
        try
        {
            final List<ArtifactStore> stores = storeManager.getAllConcreteArtifactStores();
            fileManager.rescanAll( stores );
        }
        catch ( final ProxyDataException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.SERVER_ERROR, "Failed to retrieve list of concrete stores. Reason: %s", e,
                                              e.getMessage() );
        }
    }

    public void deleteAll( final String path )
        throws AproxWorkflowException
    {
        try
        {
            final List<ArtifactStore> stores = storeManager.getAllConcreteArtifactStores();
            fileManager.deleteAll( stores, path );
        }
        catch ( final ProxyDataException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.SERVER_ERROR, "Failed to retrieve list of concrete stores. Reason: %s", e,
                                              e.getMessage() );
        }
    }

    private ArtifactStore getStore( final StoreType type, final String name )
        throws AproxWorkflowException
    {
        final StoreKey key = new StoreKey( type, name );
        return getStore( key );
    }

    private ArtifactStore getStore( final StoreKey key )
        throws AproxWorkflowException
    {
        ArtifactStore store = null;
        try
        {
            store = storeManager.getArtifactStore( key );
        }
        catch ( final ProxyDataException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.SERVER_ERROR, "Cannot retrieve store: %s. Reason: %s", e, key, e.getMessage() );
        }

        if ( store == null )
        {
            throw new AproxWorkflowException( ApplicationStatus.NOT_FOUND, "Cannot find store: %s", key );
        }

        return store;
    }

}
