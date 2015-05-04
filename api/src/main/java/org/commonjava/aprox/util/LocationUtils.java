/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.aprox.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.HostedRepository;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
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

    //    public static CacheOnlyLocation toCacheLocation( final StoreKey key )
    //    {
    //        if ( key == null )
    //        {
    //            return null;
    //        }
    //
    //        if ( key.getType() == StoreType.group )
    //        {
    //            return new GroupLocation( key.getName() );
    //        }
    //
    //        return new CacheOnlyLocation( key );
    //    }
    //
    //    public static List<? extends KeyedLocation> toCacheLocations( final StoreKey... keys )
    //    {
    //        return toCacheLocations( Arrays.asList( keys ) );
    //    }
    //
    //    public static List<? extends KeyedLocation> toCacheLocations( final Collection<StoreKey> keys )
    //    {
    //        final List<KeyedLocation> result = new ArrayList<KeyedLocation>();
    //        for ( final StoreKey key : keys )
    //        {
    //            final KeyedLocation loc = toCacheLocation( key );
    //            if ( loc != null )
    //            {
    //                result.add( loc );
    //            }
    //        }
    //
    //        return result;
    //    }

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
