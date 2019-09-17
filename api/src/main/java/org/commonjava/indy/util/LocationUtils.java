/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.galley.CacheOnlyLocation;
import org.commonjava.indy.model.galley.GroupLocation;
import org.commonjava.indy.model.galley.KeyedLocation;
import org.commonjava.indy.model.galley.RepositoryLocation;
import org.commonjava.maven.galley.auth.AttributePasswordManager;
import org.commonjava.maven.galley.auth.PasswordEntry;
import org.commonjava.maven.galley.event.FileEvent;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.Transfer;

public final class LocationUtils
{
    public static final String PATH_STYLE = "pathStyle";

    public static final String KEYED_LOCATION_METADATA = "keyedLocation";

    private LocationUtils()
    {
    }

    public static KeyedLocation toLocation( final ArtifactStore store )
    {
        if ( store == null )
        {
            return null;
        }

        KeyedLocation location = (KeyedLocation) store.getTransientMetadata( KEYED_LOCATION_METADATA );
        if ( location != null )
        {
            return location;
        }

        final StoreType type = store.getKey()
                                    .getType();

        switch ( type )
        {
            case group:
            {
                location = new GroupLocation( store.getPackageType(), store.getName() );
                break;
            }
            case hosted:
            {
                location = new CacheOnlyLocation( (HostedRepository) store );
                break;
            }
            case remote:
            default:
            {
                final RemoteRepository repository = (RemoteRepository) store;
                location = new RepositoryLocation( repository );
                AttributePasswordManager.bind( location, PasswordEntry.KEY_PASSWORD, repository.getKeyPassword() );
                AttributePasswordManager.bind( location, PasswordEntry.PROXY_PASSWORD, repository.getProxyPassword() );
                AttributePasswordManager.bind( location, PasswordEntry.USER_PASSWORD, repository.getPassword() );
            }
        }

        if ( location != null )
        {
            location.setAttribute( PATH_STYLE, store.getPathStyle() );

            Map<String, String> metadata = store.getMetadata();
            if ( metadata != null )
            {
                Location loc = location;
                metadata.forEach( ( k, v ) -> {
                    if ( !loc.getAttributes().containsKey( k ) )
                    {
                        loc.setAttribute( k, v );
                    }
                } );
            }
        }

        store.setTransientMetadata( KEYED_LOCATION_METADATA, location );

        return location;
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
