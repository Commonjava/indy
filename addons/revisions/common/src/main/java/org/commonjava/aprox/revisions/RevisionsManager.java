package org.commonjava.aprox.revisions;

import static org.apache.commons.lang.StringUtils.join;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.commonjava.aprox.audit.ChangeSummary;
import org.commonjava.aprox.revisions.conf.RevisionsConfig;
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

    private static final String[] DATA_DIR_GITIGNORES = { "depgraph", "shelflife" };

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private GitManager dataFileGit;

    @Inject
    private RevisionsConfig revisionsConfig;

    @Inject
    private DataFileManager dataFileManager;

    protected RevisionsManager()
    {
    }

    public RevisionsManager( final RevisionsConfig revisionsConfig, final DataFileManager dataFileManager )
        throws GitSubsystemException, IOException
    {
        this.revisionsConfig = revisionsConfig;
        this.dataFileManager = dataFileManager;
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

        final GitConfig dataConf = new GitConfig( dataDir, revisionsConfig.getDataUpstreamUrl() );
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
    }

    public void pushDateUpdates()
        throws GitSubsystemException
    {
        dataFileGit.pushUpdates();
    }

    public List<ChangeSummary> getDataChangeLog( final File f, final int start, final int length )
        throws GitSubsystemException
    {
        return dataFileGit.getChangelog( f, start, length );
    }

}
