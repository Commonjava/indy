/**
 * Copyright (C) 2013 Red Hat, Inc.
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
package org.commonjava.indy.content.index;

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.content.DownloadManager;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.util.LocationUtils;
import org.commonjava.indy.util.PathUtils;
import org.commonjava.maven.galley.model.SpecialPathInfo;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.spi.io.SpecialPathManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.inject.Any;
import javax.inject.Inject;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Decorator for ContentIndexManager which delegate all real artifacts(not metadata) content index to GAV/directory
 * level, which will reduces
 */
@Decorator
public abstract class DirectoryLvContentIndexManagerDecorator
        implements ContentIndexManager
{
    private final Logger logger = LoggerFactory.getLogger( this.getClass() );

    @Delegate
    @Any
    @Inject
    private ContentIndexManager delegate;

    @Inject
    private SpecialPathManager specialPathManager;

    @Inject
    private DownloadManager downloadManager;

    @Inject
    private StoreDataManager storeDataManager;

    @Override
    public boolean removeIndexedStorePath( String path, StoreKey key, Consumer<IndexedStorePath> pathConsumer )
    {
        return delegate.removeIndexedStorePath( getDirPathWithIfMeta( path ), key, pathConsumer );
    }

    @Override
    public void deIndexStorePath( StoreKey key, String path )
    {
        delegate.deIndexStorePath( key, getDirPathWithIfMeta( path ) );
    }

    @Override
    public IndexedStorePath getIndexedStorePath( StoreKey key, String path )
    {
        return delegate.getIndexedStorePath( key, getDirPathWithIfMeta( path ) );
    }

    @Override
    public void indexTransferIn( Transfer transfer, StoreKey... topKeys )
    {

        if ( !transfer.isDirectory() )
        {
            SpecialPathInfo info = specialPathManager.getSpecialPathInfo( transfer.getPath() );
            if ( info == null || !info.isMetadata() )
            {
                try
                {
                    ArtifactStore store = storeDataManager.getArtifactStore( LocationUtils.getKey( transfer ) );
                    Transfer dirPathTxfr =
                            downloadManager.retrieve( store, getDirPathWithIfMeta( transfer.getPath() ) );
                    delegate.indexTransferIn( dirPathTxfr, topKeys );
                }
                catch ( IndyDataException | IndyWorkflowException e )
                {
                    logger.warn(
                            "Indexing transfer in directory level failed. Will indexing original transfer: {}. Error is:{}",
                            transfer, e.getMessage() );
                    delegate.indexTransferIn( transfer, topKeys );
                }
            }
            else
            {
                delegate.indexTransferIn( transfer, topKeys );
            }
        }
        else
        {
            delegate.indexTransferIn( transfer, topKeys );
        }

    }

    @Override
    public void indexPathInStores( String path, StoreKey originKey, StoreKey... topKeys )
    {
        delegate.indexPathInStores( getDirPathWithIfMeta( path ), originKey, topKeys );
    }

    @Override
    public void clearIndexedPathFrom( String path, Set<Group> groups, Consumer<IndexedStorePath> pathConsumer )
    {
        delegate.clearIndexedPathFrom( getDirPathWithIfMeta( path ), groups, pathConsumer );
    }

    private String getDirPathWithIfMeta( String path )
    {
        final SpecialPathInfo info = specialPathManager.getSpecialPathInfo( path );
        if ( info == null || !info.isMetadata() )
        {
            return PathUtils.getCurrentDirPath( path );
        }
        else
        {
            return path;
        }
    }
}
