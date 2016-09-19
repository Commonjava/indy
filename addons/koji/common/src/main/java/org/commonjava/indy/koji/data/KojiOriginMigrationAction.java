package org.commonjava.indy.koji.data;

import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.action.MigrationAction;
import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static org.commonjava.indy.koji.content.KojiContentManagerDecorator.KOJI_ORIGIN;
import static org.commonjava.indy.model.core.ArtifactStore.METADATA_ORIGIN;

/**
 * Created by jdcasey on 9/19/16.
 */
public class KojiOriginMigrationAction
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
            if ( repo.getDescription() != null && repo.getDescription().contains( "Koji build" ) )
            {
                repo.setMetadata( METADATA_ORIGIN, KOJI_ORIGIN );
                toStore.add( repo );
            }
        } );

        for ( HostedRepository repo : toStore )
        {
            try
            {
                storeDataManager.storeArtifactStore( repo, new ChangeSummary( ChangeSummary.SYSTEM_USER,
                                                                              "Adding Koji origin metadata" ) );
            }
            catch ( IndyDataException e )
            {
                throw new IndyLifecycleException( "Failed to store %s with Koji origin metadata. Reason: %s",
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
        return "Koji origin metadata migration";
    }
}
