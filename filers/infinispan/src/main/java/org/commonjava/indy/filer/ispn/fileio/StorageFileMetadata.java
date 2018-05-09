/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.filer.ispn.fileio;

import org.infinispan.container.versioning.EntryVersion;
import org.infinispan.container.versioning.NumericVersion;
import org.infinispan.metadata.InternalMetadata;
import org.infinispan.metadata.Metadata;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.concurrent.TimeUnit;

/**
 * Metadata for files in a storage-style GridFilesystem cache.
 * Created by jdcasey on 3/11/16.
 */
public class StorageFileMetadata
    implements InternalMetadata, Externalizable
{
    private long created;

    private long expiryTime;

    private long lastUsed;

    private long lifespan;

    private long maxIdle;

    private EntryVersion version;

    public StorageFileMetadata( InternalMetadata metadata )
    {
        created = metadata.created();
        expiryTime = metadata.expiryTime();
        lastUsed = metadata.lastUsed();
        lifespan = metadata.lifespan();
        maxIdle = metadata.maxIdle();
        version = metadata.version();
    }

    public StorageFileMetadata( long created, long expiryTime, long lastUsed, long lifespan, long maxIdle,
                                EntryVersion version )
    {
        this.created = created;
        this.expiryTime = expiryTime;
        this.lastUsed = lastUsed;
        this.lifespan = lifespan;
        this.maxIdle = maxIdle;
        this.version = version;
    }

    @Override
    public long created()
    {
        return created;
    }

    @Override
    public long lastUsed()
    {
        return lastUsed;
    }

    @Override
    public boolean isExpired( long now )
    {
        return now > expiryTime;
    }

    @Override
    public long expiryTime()
    {
        return expiryTime;
    }

    @Override
    public long lifespan()
    {
        return lifespan;
    }

    @Override
    public long maxIdle()
    {
        return maxIdle;
    }

    @Override
    public EntryVersion version()
    {
        return version;
    }

    @Override
    public Builder builder()
    {
        return new StorageFileMetadataBuilder();
    }

    @Override
    public void writeExternal( ObjectOutput out )
            throws IOException
    {
        out.writeLong( expiryTime );
        out.writeLong( created );
        out.writeLong( lastUsed );
        out.writeLong( lifespan );
        out.writeLong( maxIdle );
        out.writeObject( version );
    }

    @Override
    public void readExternal( ObjectInput in )
            throws IOException, ClassNotFoundException
    {
        expiryTime = in.readLong();
        created = in.readLong();
        lastUsed = in.readLong();
        lifespan = in.readLong();
        maxIdle = in.readLong();
        version = (EntryVersion) in.readObject();
    }

    public static final class StorageFileMetadataBuilder implements Builder
    {
        private long created = System.currentTimeMillis();

        private long expiryTime = -1;

        private long lastUsed = -1;

        private long lifespan = -1;

        private long maxIdle = -1;

        private EntryVersion version;

        @Override
        public Builder lifespan( long time, TimeUnit unit )
        {
            lifespan = TimeUnit.MILLISECONDS.convert( time, unit );
            return this;
        }

        @Override
        public Builder lifespan( long time )
        {
            lifespan = time;
            return this;
        }

        @Override
        public Builder maxIdle( long time, TimeUnit unit )
        {
            maxIdle = TimeUnit.MILLISECONDS.convert( time, unit );
            return this;
        }

        @Override
        public Builder maxIdle( long time )
        {
            maxIdle = time;
            return this;
        }

        @Override
        public Builder version( EntryVersion version )
        {
            this.version = version;
            return this;
        }

        @Override
        public Metadata build()
        {
            if ( version == null )
            {
                version = new NumericVersion( 0L );
            }

            return new StorageFileMetadata( created, expiryTime, lastUsed, lifespan, maxIdle, version );
        }

        @Override
        public Builder merge( Metadata metadata )
        {
            return this;
        }
    }
}
