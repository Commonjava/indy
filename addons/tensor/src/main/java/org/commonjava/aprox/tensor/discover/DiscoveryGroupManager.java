package org.commonjava.aprox.tensor.discover;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.commonjava.aprox.change.event.ArtifactStoreUpdateEvent;
import org.commonjava.aprox.change.event.ProxyManagerDeleteEvent;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.tensor.conf.AproxTensorConfig;
import org.commonjava.util.logging.Logger;

@ApplicationScoped
public class DiscoveryGroupManager
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private AproxTensorConfig config;

    @Inject
    private StoreDataManager storeManager;

    @PostConstruct
    public void updateDiscoveryGroup()
    {
        if ( !isAutomaticallyMaintained() )
        {
            return;
        }

        Group group = null;
        try
        {
            group = storeManager.getGroup( config.getDiscoveryGroup() );
        }
        catch ( final ProxyDataException e )
        {
            logger.error( "Lookup for discovery group: %s failed. Error: %s", e, config.getDiscoveryGroup(),
                          e.getMessage() );
            return;
        }

        Group updates;
        try
        {
            updates = createDiscoveryGroup();
        }
        catch ( final ProxyDataException e )
        {
            logger.error( "Lookup for ordered concrete constituency of one or more groups has failed. Error: %s", e,
                          e.getMessage() );
            return;
        }

        boolean changed = false;
        if ( group == null )
        {
            group = updates;
            changed = true;
        }
        else
        {
            final List<StoreKey> gC = group.getConstituents();
            final List<StoreKey> uC = updates.getConstituents();

            if ( !gC.equals( uC ) )
            {
                group.setConstituents( updates.getConstituents() );
                changed = true;
            }
        }

        if ( changed )
        {
            try
            {
                storeManager.storeGroup( group, false );
            }
            catch ( final ProxyDataException e )
            {
                logger.error( "Failed to store discovery group changes for: %s. Error: %s", e,
                              config.getDiscoveryGroup(), e.getMessage() );
            }
        }
    }

    public void onStoreAddOrUpdate( @Observes final ArtifactStoreUpdateEvent evt )
    {
        updateDiscoveryGroup();
    }

    public void onStoreDeletion( @Observes final ProxyManagerDeleteEvent evt )
    {
        updateDiscoveryGroup();
    }

    private boolean isAutomaticallyMaintained()
    {
        final String discoveryGroup = config.getDiscoveryGroup();
        if ( !AproxTensorConfig.DEFAULT_TENSOR_DISCOVERY_GROUP.equals( discoveryGroup ) )
        {
            logger.info( "Tensor discovery group is: %s. This is not the default discovery group (%s), so it is not automatically maintained.",
                         discoveryGroup, AproxTensorConfig.DEFAULT_TENSOR_DISCOVERY_GROUP );
            return false;
        }

        return true;
    }

    private Group createDiscoveryGroup()
        throws ProxyDataException
    {
        final List<Group> allGroups = storeManager.getAllGroups();
        final List<StoreKey> allKeys = new ArrayList<StoreKey>();
        for ( final Group g : allGroups )
        {
            final List<ArtifactStore> stores = storeManager.getOrderedConcreteStoresInGroup( g.getName() );
            if ( stores != null )
            {
                for ( final ArtifactStore store : stores )
                {
                    final StoreKey key = store.getKey();
                    if ( !allKeys.contains( key ) )
                    {
                        allKeys.add( key );
                    }
                }
            }
        }

        return new Group( config.getDiscoveryGroup(), allKeys );
    }
}
