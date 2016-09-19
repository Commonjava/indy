package org.commonjava.indy.implrepo.data;

import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.action.MigrationAction;
import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.RemoteRepository;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static org.commonjava.indy.implrepo.data.ImpliedReposStoreDataManagerDecorator.IMPLIED_REPO_ORIGIN;
import static org.commonjava.indy.model.core.ArtifactStore.METADATA_ORIGIN;

/**
 * Created by jdcasey on 9/19/16.
 */
@ApplicationScoped
public class ImpliedReposOriginMigrationAction
        implements MigrationAction
{
    @Inject
    private StoreDataManager storeDataManager;

    @Override
    public boolean migrate()
            throws IndyLifecycleException
    {
        List<RemoteRepository> remoteRepositories;
        try
        {
            remoteRepositories = storeDataManager.getAllRemoteRepositories();
        }
        catch ( IndyDataException e )
        {
            throw new IndyLifecycleException( "Cannot retrieve all remote repositories. Reason: %s", e,
                                              e.getMessage() );
        }

        List<RemoteRepository> toStore = new ArrayList<>();
        remoteRepositories.forEach( (repo)->{
            if ( repo.getName().startsWith( "i-" ) )
            {
                repo.setMetadata( METADATA_ORIGIN, IMPLIED_REPO_ORIGIN );
                toStore.add( repo );
            }
        } );

        for ( RemoteRepository repo : toStore )
        {
            try
            {
                storeDataManager.storeArtifactStore( repo, new ChangeSummary( ChangeSummary.SYSTEM_USER,
                                                                              "Adding implied-repository origin metadata" ) );
            }
            catch ( IndyDataException e )
            {
                throw new IndyLifecycleException( "Failed to store %s with implied-repos origin metadata. Reason: %s",
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
        return "Implied-Repositories metadata origin migration";
    }
}
