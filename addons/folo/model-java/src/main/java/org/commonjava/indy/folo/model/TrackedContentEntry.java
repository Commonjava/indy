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
package org.commonjava.indy.folo.model;

import org.commonjava.indy.model.core.AccessChannel;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import static org.commonjava.indy.model.core.AccessChannel.GENERIC_PROXY;
import static org.commonjava.indy.pkg.PackageTypeConstants.PKG_TYPE_GENERIC_HTTP;
import static org.commonjava.indy.pkg.PackageTypeConstants.PKG_TYPE_MAVEN;

public class TrackedContentEntry
        implements Comparable<TrackedContentEntry>,Externalizable
{
    private static final int VERSION = 2;

    private TrackingKey trackingKey;

    private StoreKey storeKey;

    private AccessChannel accessChannel;

    private String path;

    private String originUrl;

    private StoreEffect effect;

    private String md5;

    private String sha256;

    private String sha1;

    private Long size;

    private long index = System.currentTimeMillis();

    public TrackedContentEntry()
    {
    }

    public TrackedContentEntry( final TrackingKey trackingKey, final StoreKey storeKey,
                                final AccessChannel accessChannel, final String originUrl, final String path,
                                final StoreEffect effect, final Long size,
                                final String md5, final String sha1, final String sha256 )
    {
        this.trackingKey = trackingKey;
        this.storeKey = storeKey;
        this.accessChannel = accessChannel;
        this.path = path;
        this.originUrl = originUrl;
        this.effect = effect;
        this.md5=md5;
        this.sha1=sha1;
        this.sha256=sha256;
        this.size = size;
    }

    public String getOriginUrl()
    {
        return originUrl;
    }

    public String getMd5()
    {
        return md5;
    }

    public String getSha256()
    {
        return sha256;
    }

    public String getSha1()
    {
        return sha1;
    }

    public StoreKey getStoreKey()
    {
        return storeKey;
    }

    public AccessChannel getAccessChannel()
    {
        return accessChannel;
    }

    public String getPath()
    {
        return path;
    }

    public TrackingKey getTrackingKey()
    {
        return trackingKey;
    }

    public StoreEffect getEffect()
    {
        return effect;
    }

    public Long getSize()
    {
        return size;
    }

    public long getIndex()
    {
        return index;
    }

    public void setStoreKey( StoreKey storeKey )
    {
        this.storeKey = storeKey;
    }

    public void setOriginUrl( String originUrl )
    {
        this.originUrl = originUrl;
    }

    @Override
    public int compareTo( final TrackedContentEntry other )
    {
        int comp = trackingKey.getId().compareTo( other.getTrackingKey().getId() );
        if ( comp == 0 )
        {
            comp = storeKey.compareTo( other.getStoreKey() );
        }
        if ( comp == 0 )
        {
            comp = accessChannel.compareTo( other.getAccessChannel() );
        }
        if ( comp == 0 )
        {
            comp = path.compareTo( other.getPath() );
        }
        if ( comp == 0 )
        {
            comp = effect.compareTo( other.getEffect() );
        }

        return comp;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( trackingKey == null ) ? 0 : trackingKey.hashCode() );
        result = prime * result + ( ( path == null ) ? 0 : path.hashCode() );
        result = prime * result + ( ( storeKey == null ) ? 0 : storeKey.hashCode() );
        result = prime * result + ( ( accessChannel == null ) ? 0 : accessChannel.hashCode() );
        result = prime * result + ( ( effect == null ) ? 0 : effect.hashCode() );
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
        final TrackedContentEntry other = (TrackedContentEntry) obj;
        if ( trackingKey == null )
        {
            if ( other.trackingKey != null )
            {
                return false;
            }
        }
        else if ( !trackingKey.equals( other.trackingKey ) )
        {
            return false;
        }
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
        else if ( !accessChannel.equals( other.accessChannel ) )
        {
            return false;
        }
        if ( effect == null )
        {
            if ( other.effect != null )
            {
                return false;
            }
        }
        else if ( !effect.equals( other.effect ) )
        {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return String.format(
                "TrackedContentEntry [\n  trackingKey=%s\n  storeKey=%s\n  accessChannel=%s\n  path=%s\n  originUrl=%s\n  effect=%s\n  md5=%s\n  sha1=%s\n  sha256=%s\nObject hashcode=%s\n]",
                trackingKey, storeKey, accessChannel, path, originUrl, effect, md5, sha1, sha256, super.hashCode() );
    }

    @Override
    public void writeExternal( final ObjectOutput out )
            throws IOException
    {
        out.writeObject( Integer.toString( VERSION ) );
        out.writeObject( trackingKey );
        out.writeObject( storeKey.getPackageType() );
        out.writeObject( storeKey.getName() );
        out.writeObject( storeKey.getType().name() );
        out.writeObject( accessChannel.name() );
        out.writeObject( path == null ? "" : path );
        out.writeObject( originUrl == null ? "" : originUrl );
        out.writeObject( effect == null ? "" : effect.name() );
        out.writeObject( md5 == null ? "" : md5 );
        out.writeObject( sha1 == null ? "" : sha1 );
        out.writeObject( sha256 == null ? "" : sha256 );
        out.writeObject( size );
        out.writeLong( index );
    }

    @Override
    public void readExternal( final ObjectInput in )
            throws IOException, ClassNotFoundException
    {
        // This is a little awkward. The original version didn't have a version constant, so it wasn't possible
        // to just read it from the data stream and use it to guide the deserialization process. Instead,
        // we have to read the first object and determine whether it's the object version or the tracking key,
        // which was the first field in the serialized data stream back in the first version of this class.
        Object whatIsThis = in.readObject();

        int version;
        String packageType;

        if ( whatIsThis == null )
        {
            // we see NumberFormatException: For input string: "null" in parseInt. It might be because we forget
            // to persist the trackingKey for some reason. In this case, we just ignore it and continues
            version = 1;
            trackingKey = null;
            packageType = PKG_TYPE_MAVEN;
        }
        else if ( whatIsThis instanceof TrackingKey )
        {
            version = 1;
            trackingKey = (TrackingKey) whatIsThis;
            packageType = PKG_TYPE_MAVEN;
        }
        else
        {
            version = Integer.parseInt( String.valueOf( whatIsThis ) );
            trackingKey = (TrackingKey) in.readObject();
            packageType = (String) in.readObject();
        }

        // TODO: We should make future versioning / deserialization decisions based on the version we read / infer above
        if ( version > VERSION )
        {
            throw new IOException(
                    "This class is of an older version: " + VERSION + " vs. the version read from the data stream: "
                            + version + ". Cannot deserialize." );
        }

        final String storeKeyName = (String) in.readObject();
        final StoreType storeType = StoreType.get( (String) in.readObject() );

        final String accessChannelStr = (String) in.readObject();
        accessChannel = "".equals( accessChannelStr ) ? null : AccessChannel.valueOf( accessChannelStr );

        if ( version == 1 && accessChannel == GENERIC_PROXY )
        {
            packageType = PKG_TYPE_GENERIC_HTTP;
        }

        storeKey = new StoreKey( packageType, storeType, storeKeyName );

        final String pathStr = (String) in.readObject();
        path = "".equals( pathStr ) ? null : pathStr;

        final String originUrlStr = (String) in.readObject();
        originUrl = "".equals( originUrlStr ) ? null : originUrlStr;

        final String effectStr = (String) in.readObject();
        effect = "".equals( effectStr ) ? null : StoreEffect.valueOf( effectStr );

        final String md5Str = (String) in.readObject();
        md5 = "".equals( md5Str ) ? null : md5Str;

        final String sha1Str = (String) in.readObject();
        sha1 = "".equals( sha1Str ) ? null : sha1Str;

        final String sha256Str = (String) in.readObject();
        sha256 = "".equals( sha256Str ) ? null : sha256Str;

        size = (Long) in.readObject();

        index = in.readLong();

    }

}
