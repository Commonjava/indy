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
package org.commonjava.indy.core.content;

import java.io.IOException;
import java.util.Set;

import javax.inject.Inject;

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.content.AbstractContentGenerator;
import org.commonjava.indy.content.DirectContentAccess;
import org.commonjava.indy.content.DownloadManager;
import org.commonjava.indy.core.content.group.GroupMergeHelper;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMergedContentGenerator
    extends AbstractContentGenerator
{
    protected final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    protected DirectContentAccess fileManager;

    @Inject
    protected StoreDataManager storeManager;

    @Inject
    protected GroupMergeHelper helper;

    protected AbstractMergedContentGenerator()
    {
    }

    protected AbstractMergedContentGenerator( final DirectContentAccess fileManager, final StoreDataManager storeManager,
                                              final GroupMergeHelper helper )
    {
        this.fileManager = fileManager;
        this.storeManager = storeManager;
        this.helper = helper;
    }

    @Override
    public final void handleContentDeletion( final ArtifactStore store, final String path,
                                             final EventMetadata eventMetadata )
        throws IndyWorkflowException
    {
        if ( path.endsWith( getMergedMetadataName() ) )
        {
            clearAllMerged( store, path );
        }
    }

    @Override
    public final void handleContentStorage( final ArtifactStore store, final String path, final Transfer result,
                                            final EventMetadata eventMetadata )
        throws IndyWorkflowException
    {
        if ( path.endsWith( getMergedMetadataName() ) )
        {
            clearAllMerged( store, path );
        }
    }

    protected abstract String getMergedMetadataName();

    protected void clearAllMerged( final ArtifactStore store, final String path )
        throws IndyWorkflowException
    {
        if ( StoreType.group == store.getKey()
                                     .getType() )
        {
            final Group group = (Group) store;
            clearMergedFile( group, path );
        }

        try
        {
            final Set<Group> groups = storeManager.getGroupsContaining( store.getKey() );
            if ( groups != null )
            {
                for ( final Group group : groups )
                {
                    clearMergedFile( group, path );
                }
            }
        }
        catch ( final IndyDataException e )
        {
            throw new IndyWorkflowException(

            "Failed to lookup groups whose membership contains: {} (to trigger re-generation on demand: {}. Error: {}",
                                              e, store.getKey(), path, e.getMessage() );
        }
    }

    protected void clearMergedFile( final Group group, final String path )
        throws IndyWorkflowException
    {
        // delete so it'll be recomputed.
        final Transfer target = fileManager.getTransfer( group, path );
        try
        {
            logger.debug( "Deleting merged file: {}", target );
            target.delete( true );
            helper.deleteChecksumsAndMergeInfo( group, path );
        }
        catch ( final IOException e )
        {
            throw new IndyWorkflowException(

            "Failed to delete generated file (to allow re-generation on demand: {}. Error: {}", e,
                                              target.getFullPath(), e.getMessage() );
        }
    }

}
