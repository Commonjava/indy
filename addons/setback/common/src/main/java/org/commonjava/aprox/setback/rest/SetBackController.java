package org.commonjava.aprox.setback.rest;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.data.AproxDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.setback.data.SetBackDataException;
import org.commonjava.aprox.setback.data.SetBackSettingsManager;
import org.commonjava.aprox.subsys.datafile.DataFile;

@ApplicationScoped
public class SetBackController
{
    @Inject
    private SetBackSettingsManager manager;

    @Inject
    private StoreDataManager storeManager;

    protected SetBackController()
    {
    }

    public SetBackController( final SetBackSettingsManager manager, final StoreDataManager storeManager )
    {
        this.manager = manager;
        this.storeManager = storeManager;
    }

    public DataFile getSetBackSettings( final StoreKey key )
        throws AproxWorkflowException
    {
        return manager.getSetBackSettings( key );
    }

    public boolean deleteSetBackSettings( final StoreKey key )
        throws AproxWorkflowException
    {
        try
        {
            final ArtifactStore store = storeManager.getArtifactStore( key );
            if ( store != null )
            {
                if ( manager.deleteStoreSettings( store ) )
                {
                    manager.generateStoreSettings( store );
                    return true;
                }
            }
        }
        catch ( final SetBackDataException e )
        {
            throw new AproxWorkflowException( "Failed to delete SetBack settings.xml for: %s. Reason: %s", e, key,
                                              e.getMessage() );
        }
        catch ( final AproxDataException e )
        {
            throw new AproxWorkflowException( "Failed to retrieve ArtifactStore for: %s. Reason: %s", e, key,
                                              e.getMessage() );
        }

        return false;
    }

}
