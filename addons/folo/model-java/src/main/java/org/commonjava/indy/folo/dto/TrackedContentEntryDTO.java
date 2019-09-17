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
package org.commonjava.indy.folo.dto;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import org.commonjava.indy.model.core.AccessChannel;
import org.commonjava.indy.model.core.StoreKey;

public class TrackedContentEntryDTO
    implements Comparable<TrackedContentEntryDTO>
{

    @ApiModelProperty( value = "The Indy key for the repository/group this where content was stored.",
                       allowableValues = "remote:<name>, hosted:<name>, group:<name>" )
    private StoreKey storeKey;

    @ApiModelProperty( value = "Type of content access, whether \"normal\" content API or generic HTTP proxy.",
                       allowableValues = "GENERIC_PROXY, MAVEN" )
    private AccessChannel accessChannel;

    private String path;

    @ApiModelProperty( value = "If resolved from a remote repository, this is the origin URL, otherwise empty/null." )
    private String originUrl;

    @ApiModelProperty( value = "URL to this path on the local Indy instance." )
    private String localUrl;

    private String md5;

    private String sha256;

    private String sha1;

    private Long size;

    public TrackedContentEntryDTO()
    {
    }

    public TrackedContentEntryDTO( final StoreKey storeKey, final AccessChannel accessChannel, final String path )
    {
        this.storeKey = storeKey;
        setAccessChannel( accessChannel );
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

    public void setSha1( final String sha1 )
    {
        this.sha1 = sha1;
    }

    public String getSha1()
    {
        return sha1;
    }
    public StoreKey getStoreKey()
    {
        return storeKey;
    }

    public void setStoreKey( final StoreKey storeKey )
    {
        this.storeKey = storeKey;
    }

    public AccessChannel getAccessChannel()
    {
        return accessChannel;
    }

    public void setAccessChannel( final AccessChannel accessChannel )
    {
        this.accessChannel = accessChannel == AccessChannel.MAVEN_REPO ? AccessChannel.NATIVE : accessChannel;
    }

    public String getPath()
    {
        return path;
    }

    public void setPath( final String path )
    {
        this.path = path.startsWith( "/" ) ? path : "/" + path;
    }

    public Long getSize()
    {
        return size;
    }

    public void setSize( final Long size )
    {
        this.size = size;
    }

    @Override
    public int compareTo( final TrackedContentEntryDTO other )
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
        if ( accessChannel == null )
        {
            if ( other.accessChannel != null )
            {
                return false;
            }
        }
        // this is complicated by the transition from using MAVEN_REPO to NATIVE for non-proxy access channels.
        else if ( !accessChannel.equals( other.accessChannel ) && !( accessChannel == AccessChannel.NATIVE
                && other.accessChannel == AccessChannel.MAVEN_REPO ) && !( accessChannel == AccessChannel.MAVEN_REPO
                && other.accessChannel == AccessChannel.NATIVE ) )
        {
            return false;
        }

        return true;
    }

    @Override
    public String toString()
    {
        return String.format( "TrackedContentEntryDTO [\n  storeKey=%s\n  accessChannel=%s\n  path=%s\n  originUrl=%s\n  localUrl=%s\n  size=%d\n md5=%s\n  sha256=%s\n]",
                              storeKey, accessChannel, path, originUrl, localUrl, size, md5, sha256 );
    }

}
