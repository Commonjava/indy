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
package org.commonjava.indy.pkg.maven.content;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import static org.commonjava.indy.pkg.maven.util.MetadataZipUtils.unzip;
import static org.commonjava.indy.pkg.maven.util.MetadataZipUtils.zip;

/**
 * Wrapper MetadataInfo around byte arrays which contain gzipped, serialized Metadata / MetadataInfo content
 * so we can minimize the heap usage.
 */
public class MetadataInfoWrapper
                extends MetadataInfo
                implements Externalizable
{
    private byte[] metadataBytes;

    private byte[] metadataMergeInfoBytes;

    public MetadataInfoWrapper()
    {
    }

    @Override
    public void setMetadata( Metadata metadata )
    {
        try
        {
            this.metadataBytes = zip( metadata );
        }
        catch ( IOException e )
        {
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.error( "Failed to zip", e );
        }
    }

    @Override
    public void setMetadataMergeInfo( String metadataMergeInfo )
    {
        this.metadataMergeInfoBytes = metadataMergeInfo.getBytes();
    }

    @Override
    public Metadata getMetadata()
    {
        try
        {
            return unzip( metadataBytes );
        }
        catch ( Exception e )
        {
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.error( "Failed to unzip", e );
        }
        return null;
    }

    @Override
    public String getMetadataMergeInfo()
    {
        try
        {
            return unzip( metadataMergeInfoBytes );
        }
        catch ( Exception e )
        {
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.error( "Failed to unzip", e );
        }
        return null;
    }

    @Override
    public void writeExternal( ObjectOutput objectOutput ) throws IOException
    {
        objectOutput.writeInt( metadataBytes.length );
        objectOutput.write( metadataBytes );
        objectOutput.writeInt( metadataMergeInfoBytes.length );
        objectOutput.write( metadataMergeInfoBytes );
    }

    @Override
    public void readExternal( ObjectInput objectInput ) throws IOException, ClassNotFoundException
    {
        int len = objectInput.readInt();
        metadataBytes = new byte[len];
        objectInput.read( metadataBytes );
        len = objectInput.readInt();
        metadataMergeInfoBytes = new byte[len];
        objectInput.read( metadataMergeInfoBytes );
    }

    public MetadataInfoWrapper wrap( MetadataInfo metadataInfo )
    {
        this.setMetadata( metadataInfo.getMetadata() );
        this.setMetadataMergeInfo( metadataInfo.getMetadataMergeInfo() );
        return this;
    }
}
