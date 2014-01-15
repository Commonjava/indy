/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import org.commonjava.maven.galley.auth.PasswordEntry;
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
        if ( store == null )
        {
            return null;
        }

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
                AttributePasswordManager.bind( location, PasswordEntry.KEY_PASSWORD, repository.getKeyPassword() );
                AttributePasswordManager.bind( location, PasswordEntry.PROXY_PASSWORD, repository.getProxyPassword() );
                AttributePasswordManager.bind( location, PasswordEntry.USER_PASSWORD, repository.getPassword() );

                return location;
            }
        }
    }

    public static CacheOnlyLocation toCacheLocation( final StoreKey key )
    {
        if ( key == null )
        {
            return null;
        }

        if ( key.getType() == StoreType.group )
        {
            return new GroupLocation( key.getName() );
        }

        return new CacheOnlyLocation( key );
    }

    public static List<? extends KeyedLocation> toCacheLocations( final StoreKey... keys )
    {
        return toCacheLocations( Arrays.asList( keys ) );
    }

    public static List<? extends KeyedLocation> toCacheLocations( final Collection<StoreKey> keys )
    {
        final List<KeyedLocation> result = new ArrayList<KeyedLocation>();
        for ( final StoreKey key : keys )
        {
            final KeyedLocation loc = toCacheLocation( key );
            if ( loc != null )
            {
                result.add( loc );
            }
        }

        return result;
    }

    public static List<? extends KeyedLocation> toLocations( final ArtifactStore... stores )
    {
        return toLocations( Arrays.asList( stores ) );
    }

    public static List<? extends KeyedLocation> toLocations( final List<? extends ArtifactStore> stores )
    {
        final List<KeyedLocation> locations = new ArrayList<KeyedLocation>();
        for ( final ArtifactStore store : stores )
        {
            final KeyedLocation loc = toLocation( store );
            if ( loc != null )
            {
                locations.add( loc );
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
        if ( transfer == null )
        {
            return null;
        }

        final Location loc = transfer.getLocation();

        if ( loc instanceof KeyedLocation )
        {
            return ( (KeyedLocation) loc ).getKey();
        }

        return null;
    }

}
