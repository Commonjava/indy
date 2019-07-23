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

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.content.ContentGenerator;
import org.commonjava.indy.content.DownloadManager;
import org.commonjava.indy.content.StoreResource;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.transport.htcli.model.HttpExchangeMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpMetadataCleanupGenerator
    implements ContentGenerator
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private DownloadManager downloads;

    @Override
    public Transfer generateFileContent( final ArtifactStore store, final String path, final EventMetadata eventMetadata )
        throws IndyWorkflowException
    {
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
                                              final EventMetadata eventMetadata )
        throws IndyWorkflowException
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

    //This method is out of date which will block aritfacts uploading http-metadata generation, so remove the code
    @Override
    @Deprecated
    public void handleContentStorage( final ArtifactStore store, final String path, final Transfer result,
                                      final EventMetadata eventMetadata )
        throws IndyWorkflowException
    {
//        final Transfer meta = result.getSiblingMeta( HttpExchangeMetadata.FILE_EXTENSION );
//        if ( meta.exists() )
//        {
//            try
//            {
//                meta.delete( false );
//            }
//            catch ( final IOException e )
//            {
//                logger.debug( "Failed to delete HTTP exchange metadata: " + meta, e );
//            }
//        }
    }

    @Override
    public void handleContentDeletion( final ArtifactStore store, final String path, final EventMetadata eventMetadata )
        throws IndyWorkflowException
    {
        final Transfer meta = downloads.getStorageReference( store, path + HttpExchangeMetadata.FILE_EXTENSION );
        if ( meta.exists() )
        {
            try
            {
                meta.delete( false );
            }
            catch ( final IOException e )
            {
                logger.debug( "Failed to delete HTTP exchange metadata: " + meta, e );
            }
        }
    }

    @Override
    public boolean canProcess( final String path )
    {
        return false;
    }

}
