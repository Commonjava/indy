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
package org.commonjava.aprox.core.content;

import java.io.IOException;
import java.util.Set;

import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.content.AbstractContentGenerator;
import org.commonjava.aprox.content.DownloadManager;
import org.commonjava.aprox.core.content.group.GroupMergeHelper;
import org.commonjava.aprox.data.AproxDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMergedContentGenerator
    extends AbstractContentGenerator
{
    protected final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    protected DownloadManager fileManager;

    @Inject
    protected StoreDataManager storeManager;

    @Inject
    protected GroupMergeHelper helper;

    protected AbstractMergedContentGenerator()
    {
    }

    protected AbstractMergedContentGenerator( final DownloadManager fileManager, final StoreDataManager storeManager,
                                              final GroupMergeHelper helper )
    {
        this.fileManager = fileManager;
        this.storeManager = storeManager;
        this.helper = helper;
    }

    @Override
    public final void handleContentDeletion( final ArtifactStore store, final String path )
        throws AproxWorkflowException
    {
        if ( path.endsWith( getMergedMetadataName() ) )
        {
            clearAllMerged( store, path );
        }
    }

    @Override
    public final void handleContentStorage( final ArtifactStore store, final String path, final Transfer result )
        throws AproxWorkflowException
    {
        if ( path.endsWith( getMergedMetadataName() ) )
        {
            clearAllMerged( store, path );
        }
    }

    protected abstract String getMergedMetadataName();

    protected void clearAllMerged( final ArtifactStore store, final String path )
        throws AproxWorkflowException
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
        catch ( final AproxDataException e )
        {
            throw new AproxWorkflowException(

            "Failed to lookup groups whose membership contains: {} (to trigger re-generation on demand: {}. Error: {}",
                                              e, store.getKey(), path, e.getMessage() );
        }
    }

    protected void clearMergedFile( final Group group, final String path )
        throws AproxWorkflowException
    {
        // delete so it'll be recomputed.
        final Transfer target = fileManager.getStorageReference( group, path );
        try
        {
            logger.debug( "Deleting merged file: {}", target );
            target.delete();
            helper.deleteChecksumsAndMergeInfo( group, path );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException(

            "Failed to delete generated file (to allow re-generation on demand: {}. Error: {}", e,
                                              target.getFullPath(), e.getMessage() );
        }
    }

}
