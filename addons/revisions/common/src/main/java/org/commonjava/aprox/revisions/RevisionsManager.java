package org.commonjava.aprox.revisions;

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
import org.commonjava.aprox.audit.ChangeSummary;
import org.commonjava.aprox.flat.data.DataFileStoreDataManager;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.revisions.conf.RevisionsConfig;
import org.commonjava.aprox.subsys.datafile.DataFile;
import org.commonjava.aprox.subsys.datafile.DataFileManager;
import org.commonjava.aprox.subsys.datafile.change.DataFileEvent;
import org.commonjava.aprox.subsys.datafile.change.DataFileEventType;
import org.commonjava.aprox.subsys.git.GitConfig;
import org.commonjava.aprox.subsys.git.GitManager;
import org.commonjava.aprox.subsys.git.GitSubsystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class RevisionsManager
{

    private static final String[] DATA_DIR_GITIGNORES = { "depgraph", "scheduler" };

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private GitManager dataFileGit;

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
        throws GitSubsystemException, IOException
    {
        final File dataDir = dataFileManager.getDetachedDataBasedir();
        final File gitignore = new File( dataDir, ".gitignore" );

        dataDir.mkdirs();
        FileUtils.write( gitignore, join( DATA_DIR_GITIGNORES, "\n" ) );

        final GitConfig dataConf = new GitConfig( dataDir, revisionsConfig.getDataUpstreamUrl(), true );
        dataFileGit = new GitManager( dataConf );

        final ChangeSummary summary =
            new ChangeSummary( ChangeSummary.SYSTEM_USER, "Committing files modified outside of the AProx UI." );
        dataFileGit.commitModifiedFiles( summary );

        if ( revisionsConfig.isPushEnabled() )
        {
            dataFileGit.pushUpdates();
        }
    }

    public void onDataFileEvent( @Observes final DataFileEvent event )
    {
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
