package org.commonjava.aprox.util;

import java.util.ArrayList;
import java.util.List;

import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.DeployPoint;
import org.commonjava.aprox.model.Repository;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.model.galley.CacheOnlyLocation;
import org.commonjava.aprox.model.galley.GroupLocation;
import org.commonjava.aprox.model.galley.KeyedLocation;
import org.commonjava.aprox.model.galley.RepositoryLocation;
import org.commonjava.maven.galley.auth.AttributePasswordManager;
import org.commonjava.maven.galley.auth.PasswordIdentifier;
import org.commonjava.maven.galley.event.FileEvent;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.Transfer;

public final class LocationUtils
{
    private LocationUtils()
    {
    }

    public static KeyedLocation toLocation( final ArtifactStore store )
    {
        final StoreType type = store.getKey()
                                    .getType();
        switch ( type )
        {
            case group:
            {
                return new GroupLocation( store.getName() );
            }
            case deploy_point:
            {
                return new CacheOnlyLocation( (DeployPoint) store );
            }
            case repository:
            default:
            {
                final Repository repository = (Repository) store;
                final RepositoryLocation location = new RepositoryLocation( repository );
                AttributePasswordManager.bind( location, PasswordIdentifier.KEY_PASSWORD, repository.getKeyPassword() );
                AttributePasswordManager.bind( location, PasswordIdentifier.PROXY_PASSWORD,
                                               repository.getProxyPassword() );
                AttributePasswordManager.bind( location, PasswordIdentifier.USER_PASSWORD, repository.getPassword() );

                return location;
            }
        }
    }

    public static CacheOnlyLocation toLocation( final StoreKey key )
    {
        if ( key.getType() == StoreType.group )
        {
            return new GroupLocation( key.getName() );
        }

        return new CacheOnlyLocation( key );
    }

    public static List<? extends KeyedLocation> toLocations( final List<? extends ArtifactStore> stores )
    {
        final List<KeyedLocation> locations = new ArrayList<>();
        for ( final ArtifactStore store : stores )
        {
            if ( store instanceof Repository )
            {
                locations.add( toLocation( store ) );
            }
            else if ( store instanceof DeployPoint )
            {
                locations.add( toLocation( store ) );
            }
            else
            {
                locations.add( toLocation( store.getKey() ) );
            }
        }

        return locations;
    }

    public static StoreKey getKey( final FileEvent event )
    {
        return getKey( event.getTransfer() );
    }

    public static StoreKey getKey( final Transfer transfer )
    {
        final Location loc = transfer.getLocation();

        if ( loc instanceof KeyedLocation )
        {
            return ( (KeyedLocation) loc ).getKey();
        }

        return null;
    }

}
