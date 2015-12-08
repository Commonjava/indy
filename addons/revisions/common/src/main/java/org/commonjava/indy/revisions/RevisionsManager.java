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
package org.commonjava.indy.revisions;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.join;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.change.event.IndyLifecycleEvent;
import org.commonjava.indy.flat.data.DataFileStoreDataManager;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.revisions.conf.RevisionsConfig;
import org.commonjava.indy.subsys.datafile.DataFile;
import org.commonjava.indy.subsys.datafile.DataFileManager;
import org.commonjava.indy.subsys.datafile.change.DataFileEvent;
import org.commonjava.indy.subsys.datafile.change.DataFileEventType;
import org.commonjava.indy.subsys.git.GitConfig;
import org.commonjava.indy.subsys.git.GitManager;
import org.commonjava.indy.subsys.git.GitSubsystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class RevisionsManager
{

    private static final String[] DATA_DIR_GITIGNORES = { "depgraph", "scheduler" };

    public static final String CATCHUP_CHANGELOG = "Committing files modified outside of the Indy UI.";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private GitManager dataFileGit;

    private boolean started;

    @Inject
    private RevisionsConfig revisionsConfig;

    @Inject
    private DataFileManager dataFileManager;

    @Inject
    private DataFileStoreDataManager storeManager;

    protected RevisionsManager()
    {
    }

    public RevisionsManager( final RevisionsConfig revisionsConfig, final DataFileManager dataFileManager,
                             final DataFileStoreDataManager storeManager )
        throws GitSubsystemException, IOException
    {
        this.revisionsConfig = revisionsConfig;
        this.dataFileManager = dataFileManager;
        this.storeManager = storeManager;
        setup();
    }

    @PostConstruct
    public void setup()
    {
        try
        {
            final File dataDir = dataFileManager.getDetachedDataBasedir();
            final File gitignore = new File( dataDir, ".gitignore" );

            dataDir.mkdirs();
            FileUtils.write( gitignore, join( DATA_DIR_GITIGNORES, "\n" ) );

            final GitConfig dataConf =
                new GitConfig( dataDir, revisionsConfig.getDataUpstreamUrl(), true ).setRemoteBranchName( revisionsConfig.getBranchName() )
                                                                                    .setUserEmail( revisionsConfig.getUserEmail() );

            dataFileGit = new GitManager( dataConf );
        }
        catch ( GitSubsystemException | IOException e )
        {
            throw new IllegalStateException( "Failed to start revisions manager: " + e.getMessage(), e );
        }
        finally
        {
        }
    }

    public void onLifecycleEvent( @Observes final IndyLifecycleEvent event )
    {
        if ( IndyLifecycleEvent.Type.started == event.getType() )
        {
            started = true;

            try
            {
                logger.info( "Indy started; committing externally changed files." );

                final ChangeSummary summary = new ChangeSummary( ChangeSummary.SYSTEM_USER, CATCHUP_CHANGELOG );
                dataFileGit.commitModifiedFiles( summary );

                if ( revisionsConfig.isPushEnabled() )
                {
                    dataFileGit.pushUpdates();
                }
            }
            catch ( final GitSubsystemException e )
            {
                logger.error( "Failed to commit pre-existing uncommitted changes in revisions manager: "
                                  + e.getMessage(), e );
            }
        }
    }

    public void onDataFileEvent( @Observes final DataFileEvent event )
    {
        if ( !started )
        {
            logger.debug( "Indy system is not marked as started. Skipping data file events in revisions manager." );
            return;
        }

        try
        {
            if ( event.getType() == DataFileEventType.accessed )
            {
                return;
            }

            if ( event.getType() == DataFileEventType.deleted )
            {
                dataFileGit.deleteAndCommit( event.getSummary(), event.getFile() );
            }
            else
            {
                dataFileGit.addAndCommitFiles( event.getSummary(), event.getFile() );
            }

            if ( revisionsConfig.isPushEnabled() )
            {
                dataFileGit.pushUpdates();
            }
        }
        catch ( final GitSubsystemException e )
        {
            logger.error( String.format( "Failed to commit changes: %s. Reason: %s", event, e.getMessage() ), e );
        }
    }

    public void pullDataUpdates()
        throws GitSubsystemException
    {
        dataFileGit.pullUpdates( revisionsConfig.getConflictStrategy() );

        // FIXME: fire events to signal data owners to reload...
        // FIXME: Return some sort of status
    }

    public void pushDataUpdates()
        throws GitSubsystemException
    {
        dataFileGit.pushUpdates();
        // FIXME: Return some sort of status
    }

    public List<ChangeSummary> getDataChangeLog( final StoreKey key, final int start, final int count )
        throws GitSubsystemException
    {
        final DataFile dataFile = storeManager.getDataFile( key );
        return dataFileGit.getChangelog( dataFile.getDetachedFile(), start, count );
    }

    public List<ChangeSummary> getDataChangeLog( String path, final int start, final int length )
        throws GitSubsystemException
    {
        final File basedir = dataFileManager.getDetachedDataBasedir();
        if ( new File( path ).isAbsolute() )
        {
            if ( !path.startsWith( basedir.getPath() ) )
            {
                throw new GitSubsystemException( "Cannot reference path outside of data basedir." );
            }

            path = Paths.get( basedir.toURI() )
                        .relativize( Paths.get( path ) )
                        .toString();
        }

        final File file;
        if ( isEmpty( path ) || path.equals( "/" ) )
        {
            file = basedir;
        }
        else
        {
            file = dataFileManager.getDataFile( path )
                                  .getDetachedFile();
        }

        return dataFileGit.getChangelog( file, start, length );
    }

    public List<ChangeSummary> getDataChangeLog( final File f, final int start, final int count )
        throws GitSubsystemException
    {
        return dataFileGit.getChangelog( f, start, count );
    }

}
