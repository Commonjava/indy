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
package org.commonjava.indy.core.content;

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.content.ContentGenerator;
import org.commonjava.indy.content.DownloadManager;
import org.commonjava.indy.content.StoreResource;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.io.checksum.TransferMetadata;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ContentMetadataGenerator
                implements ContentGenerator
{
    public static final String FORCE_CHECKSUM_AND_WRITE = "force-checksum-and-write";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private static final Set<String> HANDLED_FILENAMES = Collections.unmodifiableSet( new HashSet<String>()
    {
        {
            add( ".md5" );
            add( ".sha1" );
            add( ".sha256" );
        }
    } );

    @Inject
    private DefaultContentDigester contentDigester;

    @Inject
    private DownloadManager downloads;

    @Override
    public Transfer generateFileContent( final ArtifactStore store, final String path,
                                         final EventMetadata eventMetadata ) throws IndyWorkflowException
    {
        if ( !canProcess( path ) || ( store.getType() != StoreType.hosted ) ) // only generate for hosted checksum
        {
            return null;
        }

        String contentPath = path.substring( 0, path.lastIndexOf( "." ) );
        eventMetadata.set( FORCE_CHECKSUM_AND_WRITE, Boolean.TRUE );
        TransferMetadata ret = contentDigester.digest( store.getKey(), contentPath, eventMetadata );

        if ( ret == null || ret.getDigests() == null || ret.getDigests().isEmpty() )
        {
            logger.debug( "Content metadata generated failed, path: {}, meta: {}", path, ret );
            return null;
        }

        Transfer transfer = downloads.getStorageReference( store, path );
        if ( transfer != null && transfer.exists() )
        {
            logger.debug( "Content metadata generated, path: {}", path );
            return transfer;
        }

        return null;
    }

    @Override
    public List<StoreResource> generateDirectoryContent( final ArtifactStore store, final String path,
                                                         final List<StoreResource> existing,
                                                         final EventMetadata eventMetadata )
                    throws IndyWorkflowException
    {
        return null;
    }

    @Override
    public Transfer generateGroupFileContent( final Group group, final List<ArtifactStore> members, final String path,
                                              final EventMetadata eventMetadata ) throws IndyWorkflowException
    {
        return null;
    }

    @Override
    public List<StoreResource> generateGroupDirectoryContent( final Group group, final List<ArtifactStore> members,
                                                              final String path, final EventMetadata eventMetadata )
                    throws IndyWorkflowException
    {
        return null;
    }

    @Override
    @Deprecated
    public void handleContentStorage( final ArtifactStore store, final String path, final Transfer result,
                                      final EventMetadata eventMetadata ) throws IndyWorkflowException
    {
    }

    @Override
    public void handleContentDeletion( final ArtifactStore store, final String path, final EventMetadata eventMetadata )
                    throws IndyWorkflowException
    {
        HANDLED_FILENAMES.forEach( ext -> {
            final Transfer meta = downloads.getStorageReference( store, path + ext );
            if ( meta.exists() )
            {
                try
                {
                    meta.delete( false );
                }
                catch ( final IOException e )
                {
                    logger.debug( "Failed to delete content metadata: " + meta, e );
                }
            }
        } );
    }

    @Override
    public boolean canProcess( final String path )
    {
        for ( final String filename : HANDLED_FILENAMES )
        {
            if ( path.endsWith( filename ) )
            {
                return true;
            }
        }
        return false;
    }

}
