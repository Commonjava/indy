package org.commonjava.aprox.core.rest;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.aprox.rest.util.ApplicationStatus;
import org.commonjava.aprox.stats.AProxVersioning;
import org.commonjava.shelflife.ExpirationManager;
import org.commonjava.shelflife.ExpirationManagerException;
import org.commonjava.util.logging.Logger;

@ApplicationScoped
public class AdminController
{
    private final Logger logger = new Logger( getClass() );

    @Inject
    private StoreDataManager storeManager;

    @Inject
    private ExpirationManager expirationManager;

    @Inject
    private AProxVersioning versioning;

    protected AdminController()
    {
    }

    public AdminController( final StoreDataManager storeManager, final ExpirationManager expirationManager, final AProxVersioning versioning )
    {
        this.storeManager = storeManager;
        this.expirationManager = expirationManager;
        this.versioning = versioning;
    }

    public boolean store( final ArtifactStore store, final boolean skipExisting )
        throws AproxWorkflowException
    {
        try
        {
            return storeManager.storeArtifactStore( store, skipExisting );
        }
        catch ( final ProxyDataException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.SERVER_ERROR, "Failed to store: %s. Reason: %s", e, store.getKey(), e.getMessage() );
        }
    }

    public List<? extends ArtifactStore> getAllOfType( final StoreType type )
        throws AproxWorkflowException
    {
        try
        {
            return storeManager.getAllArtifactStores( type );
        }
        catch ( final ProxyDataException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.SERVER_ERROR, "Failed to list: %s. Reason: %s", e, type, e.getMessage() );
        }
    }

    public ArtifactStore get( final StoreKey key )
        throws AproxWorkflowException
    {
        try
        {
            return storeManager.getArtifactStore( key );
        }
        catch ( final ProxyDataException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.SERVER_ERROR, "Failed to retrieve: %s. Reason: %s", e, key, e.getMessage() );
        }
    }

    public void delete( final StoreKey key )
        throws AproxWorkflowException
    {
        try
        {
            storeManager.deleteArtifactStore( key );
        }
        catch ( final ProxyDataException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.SERVER_ERROR, "Failed to delete: %s. Reason: %s", e, key, e.getMessage() );
        }
    }

    public void started()
    {
        logger.info( "\n\n\n\n\n STARTING AProx\n    Version: %s\n    Built-By: %s\n    Commit-ID: %s\n    Built-On: %s\n\n\n\n\n",
                     versioning.getVersion(), versioning.getBuilder(), versioning.getCommitId(), versioning.getTimestamp() );

        logger.info( "Verfiying that AProx DB + basic data is installed..." );
        try
        {
            storeManager.install();

            // make sure the expiration manager is running...
            expirationManager.loadNextExpirations();
        }
        catch ( final ProxyDataException | ExpirationManagerException e )
        {
            throw new RuntimeException( "Failed to boot aprox components: " + e.getMessage(), e );
        }

        logger.info( "...done." );
    }

    public void stopped()
    {
        logger.info( "\n\n\n\n\n SHUTTING DOWN AProx\n    Version: %s\n    Built-By: %s\n    Commit-ID: %s\n    Built-On: %s\n\n\n\n\n",
                     versioning.getVersion(), versioning.getBuilder(), versioning.getCommitId(), versioning.getTimestamp() );
    }

}
