package org.commonjava.aprox.setback.rest;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.setback.data.SetBackDataException;
import org.commonjava.aprox.setback.data.SetBackSettingsManager;
import org.commonjava.aprox.subsys.flatfile.conf.FlatFile;

@ApplicationScoped
public class SetBackController
{
    @Inject
    private SetBackSettingsManager manager;

    protected SetBackController()
    {
    }

    public SetBackController( final SetBackSettingsManager manager )
    {
        this.manager = manager;
    }

    public FlatFile getSetBackSettings( final StoreKey key )
        throws AproxWorkflowException
    {
        return manager.getSetBackSettings( key );
    }

    public boolean deleteSetBackSettings( final StoreKey key )
        throws AproxWorkflowException
    {
        try
        {
            if ( manager.deleteStoreSettings( key ) )
            {
                manager.generateStoreSettings( key );
                return true;
            }
        }
        catch ( final SetBackDataException e )
        {
            throw new AproxWorkflowException( "Failed to delete SetBack settings.xml for: %s. Reason: %s", e, key,
                                              e.getMessage() );
        }

        return false;
    }

}
