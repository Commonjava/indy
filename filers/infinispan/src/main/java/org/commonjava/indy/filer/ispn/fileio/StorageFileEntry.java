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

import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.io.ByteBufferImpl;
import org.infinispan.marshall.core.MarshalledEntry;
import org.infinispan.metadata.InternalMetadata;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Serializable entry corresponding to an artifact file in storage within a GridFilesystem (on the data cache, not the
 * metadata cache).
 * Created by jdcasey on 3/11/16.
 */
public class StorageFileEntry
    implements MarshalledEntry<String, byte[]>, Externalizable
{
    private transient StorageFileMetadata metadataInstance;

    private byte[] metadata;

    private byte[] data;

    private String path;

    public StorageFileEntry( MarshalledEntry<? extends String, ? extends byte[]> entry )
    {
        this.data = entry.getValue();
        this.path = entry.getKey();
        this.metadataInstance = new StorageFileMetadata( entry.getMetadata() );
    }

    public StorageFileEntry()
    {
    }

    @Override
    public ByteBuffer getKeyBytes()
    {
        byte[] bytes = path.getBytes( StandardCharsets.UTF_8 );
        return new ByteBufferImpl( bytes, 0, bytes.length );
    }

    @Override
    public ByteBuffer getValueBytes()
    {
        return new ByteBufferImpl( data, 0, data.length );
    }

    @Override
    public ByteBuffer getMetadataBytes()
    {
        convertMetadataInstanceToBytes();
        return new ByteBufferImpl( metadata, 0, metadata.length );
    }

    @Override
    public String getKey()
    {
        return path;
    }

    @Override
    public byte[] getValue()
    {
        return data;
    }

    @Override
    public InternalMetadata getMetadata()
    {
        convertMetadataBytesToInstance();
        return metadataInstance;
    }

    private synchronized void convertMetadataInstanceToBytes()
    {
        if ( metadata == null && metadataInstance != null )
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try(ObjectOutputStream oout = new ObjectOutputStream( baos ))
            {
                oout.writeObject( metadataInstance );
            }
            catch ( IOException e )
            {
                throw new RuntimeException( "Cannot serialize metadata instance", e );
            }

            metadata = baos.toByteArray();
        }
    }

    private synchronized void convertMetadataBytesToInstance()
    {
        if ( metadataInstance == null && metadata != null )
        {
            try (ObjectInputStream oin = new ObjectInputStream( new ByteArrayInputStream( metadata ) ))
            {
                metadataInstance = (StorageFileMetadata) oin.readObject();
            }
            catch ( ClassNotFoundException | IOException e )
            {
                throw new RuntimeException( "Cannot deserialize metadata instance", e );
            }
        }
    }

    public static long peekExpiryTime( ObjectInput in )
            throws IOException
    {
        return in.readLong();
    }

    @Override
    public synchronized void writeExternal( ObjectOutput out )
            throws IOException
    {
        convertMetadataInstanceToBytes();
        convertMetadataBytesToInstance();

        out.writeLong( metadataInstance.expiryTime() );
        metadataInstance = null;

        out.writeObject( path );
        out.writeObject( metadata );
        out.writeObject( data );
    }

    @Override
    public void readExternal( ObjectInput in )
            throws IOException, ClassNotFoundException
    {
        load( in, true, true );
    }

    public void load( ObjectInput in, boolean readMetadata, boolean readData )
            throws IOException, ClassNotFoundException
    {
        in.readLong(); // throw away the optimized expiry peek field
        path = (String) in.readObject();
        if ( readMetadata )
        {
            metadata = (byte[]) in.readObject();
        }

        if ( readData )
        {
            data = (byte[]) in.readObject();
        }
    }
}
