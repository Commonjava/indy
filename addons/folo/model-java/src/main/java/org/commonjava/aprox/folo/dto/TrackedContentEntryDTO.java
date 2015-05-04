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
package org.commonjava.aprox.folo.dto;

import org.commonjava.aprox.model.core.StoreKey;

public class TrackedContentEntryDTO
    implements Comparable<TrackedContentEntryDTO>
{

    private StoreKey storeKey;

    private String path;

    private String originUrl;

    private String localUrl;

    private String md5;

    private String sha256;

    public TrackedContentEntryDTO()
    {
    }

    public TrackedContentEntryDTO( final StoreKey storeKey, final String path )
    {
        this.storeKey = storeKey;
        this.path = path.startsWith( "/" ) ? path : "/" + path;
    }

    public String getOriginUrl()
    {
        return originUrl;
    }

    public void setOriginUrl( final String originUrl )
    {
        this.originUrl = originUrl;
    }

    public String getLocalUrl()
    {
        return localUrl;
    }

    public void setLocalUrl( final String localUrl )
    {
        this.localUrl = localUrl;
    }

    public String getMd5()
    {
        return md5;
    }

    public void setMd5( final String md5 )
    {
        this.md5 = md5;
    }

    public String getSha256()
    {
        return sha256;
    }

    public void setSha256( final String sha256 )
    {
        this.sha256 = sha256;
    }

    public StoreKey getStoreKey()
    {
        return storeKey;
    }

    public void setStoreKey( final StoreKey storeKey )
    {
        this.storeKey = storeKey;
    }

    public String getPath()
    {
        return path;
    }

    public void setPath( final String path )
    {
        this.path = path.startsWith( "/" ) ? path : "/" + path;
    }

    @Override
    public int compareTo( final TrackedContentEntryDTO other )
    {
        int comp = storeKey.compareTo( other.getStoreKey() );
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
        final TrackedContentEntryDTO other = (TrackedContentEntryDTO) obj;
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
        return true;
    }

    @Override
    public String toString()
    {
        return String.format( "TrackedContentEntryDTO [\n  storeKey=%s\n  path=%s\n  originUrl=%s\n  localUrl=%s\n  md5=%s\n  sha256=%s\n]",
                              storeKey, path, originUrl, localUrl, md5, sha256 );
    }

}
