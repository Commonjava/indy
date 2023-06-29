/**
 * Copyright (C) 2011-2023 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.core.model.dto;

import org.commonjava.indy.core.model.StoreEffect;
import org.commonjava.indy.core.model.TrackingKey;
import org.commonjava.indy.model.core.AccessChannel;
import org.commonjava.indy.model.core.StoreKey;

public class ContentTransferDTO
                implements Comparable<ContentTransferDTO>
{
    private StoreKey storeKey;

    private TrackingKey trackingKey;

    private AccessChannel accessChannel;

    private String path;

    private String originUrl;

    private StoreEffect effect;

    public ContentTransferDTO()
    {
    }

    public ContentTransferDTO( final StoreKey storeKey, final TrackingKey trackingKey,
                               final AccessChannel accessChannel, final String path, final String originUrl,
                               final StoreEffect effect )
    {
        this.storeKey = storeKey;
        this.trackingKey = trackingKey;
        this.accessChannel = accessChannel;
        this.path = path.startsWith( "/" ) ? path : "/" + path;
        this.originUrl = originUrl;
        this.effect = effect;
    }

    public String getOriginUrl()
    {
        return originUrl;
    }

    public void setOriginUrl( final String originUrl )
    {
        this.originUrl = originUrl;
    }

    public StoreKey getStoreKey()
    {
        return storeKey;
    }

    public void setStoreKey( final StoreKey storeKey )
    {
        this.storeKey = storeKey;
    }

    public TrackingKey getTrackingKey()
    {
        return trackingKey;
    }

    public void setTrackingKey( TrackingKey trackingKey )
    {
        this.trackingKey = trackingKey;
    }

    public AccessChannel getAccessChannel()
    {
        return accessChannel;
    }

    public void setAccessChannel( final AccessChannel accessChannel )
    {
        this.accessChannel = accessChannel;
    }

    public String getPath()
    {
        return path;
    }

    public void setPath( final String path )
    {
        this.path = path.startsWith( "/" ) ? path : "/" + path;
    }

    public StoreEffect getEffect()
    {
        return this.effect;
    }

    public void setEffect( final StoreEffect effect )
    {
        this.effect = effect;
    }

    @Override
    public int compareTo( final ContentTransferDTO other )
    {
        int comp = storeKey.compareTo( other.getStoreKey() );
        if ( comp == 0 )
        {
            comp = accessChannel.compareTo( other.getAccessChannel() );
        }
        if ( comp == 0 )
        {
            comp = path.compareTo( other.getPath() );
        }

        return comp;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( path == null ) ? 0 : path.hashCode() );
        result = prime * result + ( ( storeKey == null ) ? 0 : storeKey.hashCode() );
        result = prime * result + ( ( accessChannel == null ) ? 0 : accessChannel.hashCode() );
        return result;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final ContentTransferDTO other = (ContentTransferDTO) obj;
        if ( path == null )
        {
            if ( other.path != null )
            {
                return false;
            }
        }
        else if ( !path.equals( other.path ) )
        {
            return false;
        }
        if ( storeKey == null )
        {
            if ( other.storeKey != null )
            {
                return false;
            }
        }
        else if ( !storeKey.equals( other.storeKey ) )
        {
            return false;
        }
        if ( accessChannel == null )
        {
            return other.accessChannel == null;
        }
        // this is complicated by the transition from using MAVEN_REPO to NATIVE for non-proxy access channels.
        else
            return accessChannel.equals( other.accessChannel );
    }

    @Override
    public String toString()
    {
        return String.format(
                        "TrackedContentEntryDTO [\n  storeKey=%s\n  accessChannel=%s\n  path=%s\n  originUrl=%s\n  effect=%s\n]",
                        storeKey, accessChannel, path, originUrl, effect );
    }

}
