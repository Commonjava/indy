package org.commonjava.indy.httprox.data;

import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.action.MigrationAction;
import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.httprox.handler.ProxyAcceptHandler;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.HostedRepository;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jdcasey on 9/19/16.
 */
public class HttProxOriginMigrationAction
        implements MigrationAction
{
    @Inject
    private StoreDataManager storeDataManager;

    @Override
    public boolean migrate()
            throws IndyLifecycleException
    {
        List<HostedRepository> hostedRepositories;
        try
        {
            hostedRepositories = storeDataManager.getAllHostedRepositories();
        }
        catch ( IndyDataException e )
        {
            throw new IndyLifecycleException( "Cannot retrieve all remote repositories. Reason: %s", e,
                                              e.getMessage() );
        }

        List<HostedRepository> toStore = new ArrayList<>();
        hostedRepositories.forEach( (repo)->{
            if ( repo.getDescription() != null && repo.getDescription().contains( "HTTProx proxy" ) )
            {
                repo.setMetadata( ArtifactStore.METADATA_ORIGIN, ProxyAcceptHandler.HTTPROX_ORIGIN );
                toStore.add( repo );
            }
        } );

        for ( HostedRepository repo : toStore )
        {
            try
            {
                storeDataManager.storeArtifactStore( repo, new ChangeSummary( ChangeSummary.SYSTEM_USER,
                                                                              "Adding HttProx origin metadata" ) );
            }
            catch ( IndyDataException e )
            {
                throw new IndyLifecycleException( "Failed to store %s with HttProx origin metadata. Reason: %s",
                                                  e, repo == null ? "NULL REPO" : repo.getKey(), e.getMessage() );
            }
        }

        return !toStore.isEmpty();
    }

    @Override
    public int getMigrationPriority()
    {
        return 10;
    }

    @Override
    public String getId()
    {
        return "HttProx origin metadata migration";
    }
}
