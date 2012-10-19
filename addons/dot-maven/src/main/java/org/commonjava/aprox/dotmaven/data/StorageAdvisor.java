package org.commonjava.aprox.dotmaven.data;

import static org.commonjava.aprox.model.StoreType.deploy_point;

import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.dotmaven.DotMavenException;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.DeployPoint;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.util.logging.Logger;

@RequestScoped
public class StorageAdvisor
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private StoreDataManager dataManager;

    @SuppressWarnings( "incomplete-switch" )
    public StorageAdvice getStorageAdvice( final ArtifactStore store )
        throws DotMavenException
    {
        boolean deployable = false;
        boolean releases = true;
        boolean snapshots = false;

        final StoreType type = store.getKey()
                                    .getType();
        all: switch ( type )
        {
            case group:
            {
                List<ArtifactStore> constituents;
                try
                {
                    constituents = dataManager.getOrderedConcreteStoresInGroup( store.getName() );
                }
                catch ( final ProxyDataException e )
                {
                    throw new DotMavenException(
                                                 "Failed to retrieve constituent ArtifactStores for group: %s. Reason: %s",
                                                 e, store.getName(), e.getMessage() );
                }

                for ( final ArtifactStore as : constituents )
                {
                    if ( as.getKey()
                           .getType() == deploy_point )
                    {
                        deployable = true;

                        final DeployPoint dp = (DeployPoint) as;

                        // TODO: If we have two deploy points with different settings, only the first will be used here!
                        snapshots = dp.isAllowSnapshots();
                        releases = dp.isAllowReleases();

                        logger.info( "\n\n\n\nDeploy point: %s allows releases? %s Releases boolean set to: %s\n\n\n\n",
                                     dp.getName(), dp.isAllowReleases(), releases );

                        break all;
                    }
                }
                break;
            }
            case deploy_point:
            {
                deployable = true;

                final DeployPoint dp = (DeployPoint) store;
                snapshots = dp.isAllowSnapshots();
                releases = dp.isAllowReleases();

                logger.info( "Deploy point: %s allows releases? %s", dp.getName(), dp.isAllowReleases() );
                break;
            }
        }

        return new StorageAdvice( store, deployable, releases, snapshots );
    }

}
