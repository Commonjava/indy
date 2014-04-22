/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.HostedRepository;
import org.commonjava.aprox.model.RemoteRepository;
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
            case hosted:
            {
                return new CacheOnlyLocation( (HostedRepository) store );
            }
            case remote:
            default:
            {
                final RemoteRepository repository = (RemoteRepository) store;
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
