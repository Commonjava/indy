package org.commonjava.aprox.subsys.git;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.join;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.commonjava.aprox.audit.ChangeSummary;
import org.commonjava.maven.atlas.ident.util.JoinString;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.IndexDiff.StageState;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revplot.PlotCommit;
import org.eclipse.jgit.revplot.PlotCommitList;
import org.eclipse.jgit.revplot.PlotLane;
import org.eclipse.jgit.revplot.PlotWalk;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitManager
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final Git git;

    private final String email;

    private final Repository repo;

    private final File rootDir;

    private final GitConfig config;

    public GitManager( final GitConfig config )
        throws GitSubsystemException
    {
        this.config = config;
        rootDir = config.getContentDir();
        final String cloneUrl = config.getCloneFrom();

        boolean checkUpdate = false;
        if ( cloneUrl != null )
        {
            logger.info( "Cloning: {} into: {}", cloneUrl, rootDir );
            if ( rootDir.isDirectory() )
            {
                checkUpdate = true;
            }
            else
            {
                final boolean mkdirs = rootDir.mkdirs();
                logger.info( "git dir {} (mkdir result: {}; is directory? {}) contains:\n  {}", rootDir, mkdirs,
                             rootDir.isDirectory(), join( rootDir.listFiles(), "\n  " ) );
                try
                {
                    Git.cloneRepository()
                       .setURI( cloneUrl )
                       .setDirectory( rootDir )
                       .setRemote( "origin" )
                       .call();
                }
                catch ( final GitAPIException e )
                {
                    throw new GitSubsystemException( "Failed to clone remote URL: {} into: {}. Reason: {}", e,
                                                     cloneUrl, rootDir, e.getMessage() );
                }
            }
        }

        final File dotGitDir = new File( rootDir, ".git" );

        logger.info( "Setting up git manager for: {}", dotGitDir );
        try
        {
            repo = new FileRepositoryBuilder().readEnvironment()
                                              .setGitDir( dotGitDir )
                                              .build();
        }
        catch ( final IOException e )
        {
            throw new GitSubsystemException( "Failed to create Repository instance for: {}. Reason: {}", e, dotGitDir,
                                             e.getMessage() );
        }

        String[] preExistingFromCreate = null;
        if ( !dotGitDir.isDirectory() )
        {
            preExistingFromCreate = rootDir.list();

            try
            {
                repo.create();
            }
            catch ( final IOException e )
            {
                throw new GitSubsystemException( "Failed to create git repo: {}. Reason: {}", e, rootDir,
                                                 e.getMessage() );
            }
        }

        String originUrl = repo.getConfig()
                               .getString( "remote", "origin", "url" );
        if ( originUrl == null )
        {
            originUrl = cloneUrl;
            logger.info( "Setting origin URL: {}", originUrl );

            repo.getConfig()
                .setString( "remote", "origin", "url", originUrl );

            repo.getConfig()
                .setString( "remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*" );
        }

        String email = repo.getConfig()
                           .getString( "user", null, "email" );

        if ( email == null )
        {
            email = config.getUserEmail();
        }

        if ( email == null )
        {
            try
            {
                email = "aprox@" + InetAddress.getLocalHost()
                                              .getCanonicalHostName();

            }
            catch ( final UnknownHostException e )
            {
                throw new GitSubsystemException( "Failed to resolve 'localhost'. Reason: {}", e, e.getMessage() );
            }
        }

        if ( repo.getConfig()
                 .getString( "user", null, "email" ) == null )
        {
            repo.getConfig()
                .setString( "user", null, "email", email );
        }

        this.email = email;

        git = new Git( repo );

        if ( preExistingFromCreate != null && preExistingFromCreate.length > 0 )
        {
            addAndCommitPaths( new ChangeSummary( ChangeSummary.SYSTEM_USER, "Committing pre-existing files." ),
                               preExistingFromCreate );
        }

        if ( checkUpdate )
        {
            pullUpdates();
        }
    }

    public GitManager addAndCommitFiles( final ChangeSummary summary, final File... files )
        throws GitSubsystemException
    {
        return addAndCommitFiles( summary, Arrays.asList( files ) );
    }

    public GitManager addAndCommitFiles( final ChangeSummary summary, final Collection<File> files )
        throws GitSubsystemException
    {
        final Set<String> paths = new HashSet<>();
        for ( final File f : files )
        {
            final String path = relativize( f );

            if ( path != null && path.length() > 0 )
            {
                paths.add( path );
            }

        }

        return addAndCommitPaths( summary, paths );
    }

    private String relativize( final File f )
    {
        return Paths.get( rootDir.toURI() )
                    .relativize( Paths.get( f.toURI() ) )
                    .toString();
    }

    public GitManager addAndCommitPaths( final ChangeSummary summary, final String... paths )
        throws GitSubsystemException
    {
        return addAndCommitPaths( summary, Arrays.asList( paths ) );
    }

    public GitManager addAndCommitPaths( final ChangeSummary summary, final Collection<String> paths )
        throws GitSubsystemException
    {
        if ( !verifyChangesExist( paths ) )
        {
            logger.info( "No actual changes in:\n  {}\n\nSkipping commit.", join( paths, "\n  " ) );
            return this;
        }

        try
        {
            final AddCommand add = git.add();
            final CommitCommand commit = git.commit();
            for ( final String filepath : paths )
            {
                add.addFilepattern( filepath );
            }

            logger.info( "Adding:\n  " + join( paths, "\n  " ) + "\n\nSummary: " + summary );

            add.call();

            commit.setMessage( buildMessage( summary, paths ) )
                  .setAuthor( summary.getUser(), email )
                  .call();
        }
        catch ( final NoFilepatternException e )
        {
            throw new GitSubsystemException( "Cannot add to git: " + e.getMessage(), e );
        }
        catch ( final GitAPIException e )
        {
            throw new GitSubsystemException( "Cannot add to git: " + e.getMessage(), e );
        }

        return this;
    }

    private boolean verifyChangesExist( final Collection<String> paths )
        throws GitSubsystemException
    {
        try
        {
            final DiffFormatter formatter = new DiffFormatter( System.out );
            formatter.setRepository( repo );

            // resolve the HEAD object
            final ObjectId oid = repo.resolve( Constants.HEAD );
            if ( oid == null )
            {
                // if there's no head, then these must be real changes...
                return true;
            }

            // reset a new tree object to the HEAD
            final RevWalk walk = new RevWalk( repo );
            final RevCommit commit = walk.parseCommit( oid );
            final RevTree treeWalk = walk.parseTree( commit );

            // construct filters for the paths we're trying to add/commit
            final List<TreeFilter> filters = new ArrayList<>();
            for ( final String path : paths )
            {
                filters.add( PathFilter.create( path ) );
            }

            // we're interested in trees with an actual diff. This should improve walk performance.
            filters.add( TreeFilter.ANY_DIFF );

            // set the path filters from above
            walk.setTreeFilter( AndTreeFilter.create( filters ) );

            // setup the tree for doing the comparison vs. uncommitted files
            final CanonicalTreeParser tree = new CanonicalTreeParser();
            final ObjectReader oldReader = repo.newObjectReader();
            try
            {
                tree.reset( oldReader, treeWalk.getId() );
            }
            finally
            {
                oldReader.release();
            }
            walk.dispose();

            // this iterator will actually scan the uncommitted files for diff'ing
            final FileTreeIterator files = new FileTreeIterator( repo );

            // do the scan.
            final List<DiffEntry> entries = formatter.scan( tree, files );

            // we're not interested in WHAT the differences are, only that there are differences.
            return entries != null && !entries.isEmpty();
        }
        catch ( final IOException e )
        {
            throw new GitSubsystemException( "Failed to scan for actual changes among: %s. Reason: %s", e, paths,
                                             e.getMessage() );
        }
    }

    private String buildMessage( final ChangeSummary summary, final Collection<String> paths )
    {
        final StringBuilder message = new StringBuilder().append( summary.getSummary() );
        if ( config.isCommitFileManifestsEnabled() )
        {
            message.append( "\n\nFiles changed:\n" )
                   .append( join( paths, "\n" ) );

        }

        return message.toString();
    }

    public GitManager deleteAndCommit( final ChangeSummary summary, final File... deleted )
        throws GitSubsystemException
    {
        return deleteAndCommit( summary, Arrays.asList( deleted ) );
    }

    public GitManager deleteAndCommit( final ChangeSummary summary, final Collection<File> files )
        throws GitSubsystemException
    {
        final Set<String> paths = new HashSet<>();
        for ( final File f : files )
        {
            final String path = relativize( f );

            if ( path != null && path.length() > 0 )
            {
                paths.add( path );
            }

        }

        return deleteAndCommitPaths( summary, paths );
    }

    public GitManager deleteAndCommitPaths( final ChangeSummary summary, final String... paths )
        throws GitSubsystemException
    {
        return deleteAndCommitPaths( summary, Arrays.asList( paths ) );
    }

    public GitManager deleteAndCommitPaths( final ChangeSummary summary, final Collection<String> paths )
        throws GitSubsystemException
    {
        try
        {
            RmCommand rm = git.rm();
            CommitCommand commit = git.commit();

            for ( final String path : paths )
            {
                rm = rm.addFilepattern( path );
                commit = commit.setOnly( path );
            }

            logger.info( "Deleting:\n  " + join( paths, "\n  " ) + "\n\nSummary: " + summary );

            rm.call();

            commit.setMessage( buildMessage( summary, paths ) )
                  .setAuthor( summary.getUser(), email )
                  .call();
        }
        catch ( final NoFilepatternException e )
        {
            throw new GitSubsystemException( "Cannot remove from git: " + e.getMessage(), e );
        }
        catch ( final GitAPIException e )
        {
            throw new GitSubsystemException( "Cannot remove from git: " + e.getMessage(), e );
        }

        return this;
    }

    public ChangeSummary getHeadCommit( final File f )
        throws GitSubsystemException
    {
        try
        {
            final ObjectId oid = repo.resolve( "HEAD" );

            final PlotWalk pw = new PlotWalk( repo );
            final RevCommit rc = pw.parseCommit( oid );
            pw.markStart( rc );

            final String filepath = relativize( f );

            pw.setTreeFilter( AndTreeFilter.create( PathFilter.create( filepath ), TreeFilter.ANY_DIFF ) );

            final PlotCommitList<PlotLane> cl = new PlotCommitList<>();
            cl.source( pw );
            cl.fillTo( 1 );

            final PlotCommit<PlotLane> commit = cl.get( 0 );

            return toChangeSummary( commit );
        }
        catch ( RevisionSyntaxException | IOException e )
        {
            throw new GitSubsystemException( "Failed to resolve HEAD commit for: %s. Reason: %s", e, f, e.getMessage() );
        }
    }

    public List<ChangeSummary> getChangelog( final File f, final int start, final int length )
        throws GitSubsystemException
    {
        if ( length == 0 )
        {
            return Collections.emptyList();
        }

        try
        {
            final ObjectId oid = repo.resolve( Constants.HEAD );

            final PlotWalk pw = new PlotWalk( repo );
            final RevCommit rc = pw.parseCommit( oid );
            toChangeSummary( rc );
            pw.markStart( rc );

            final String filepath = relativize( f );
            logger.info( "Getting changelog for: {} (start: {}, length: {})", filepath, start, length );

            if ( !isEmpty( filepath ) && !filepath.equals( "/" ) )
            {
                pw.setTreeFilter( AndTreeFilter.create( PathFilter.create( filepath ), TreeFilter.ANY_DIFF ) );
            }
            else
            {
                pw.setTreeFilter( TreeFilter.ANY_DIFF );
            }

            final List<ChangeSummary> changelogs = new ArrayList<ChangeSummary>();
            int count = 0;
            final int stop = length > 0 ? length + 1 : 0;
            RevCommit commit = null;
            while ( ( commit = pw.next() ) != null && ( stop < 1 || count < stop ) )
            {
                if ( count < start )
                {
                    count++;
                    continue;
                }

                //                printFiles( commit );
                changelogs.add( toChangeSummary( commit ) );
                count++;
            }

            if ( length < -1 )
            {
                final int remove = ( -1 * length ) - 1;
                for ( int i = 0; i < remove; i++ )
                {
                    changelogs.remove( changelogs.size() - 1 );
                }
            }

            return changelogs;
        }
        catch ( RevisionSyntaxException | IOException e )
        {
            throw new GitSubsystemException( "Failed to resolve HEAD commit for: %s. Reason: %s", e, f, e.getMessage() );
        }
    }

    //    private void printFiles( final RevCommit commit )
    //        throws IOException
    //    {
    //        final RevWalk tree = new RevWalk( repo );
    //        final RevCommit parent = commit.getParentCount() > 0 ? tree.parseCommit( commit.getParent( 0 )
    //                                                                                       .getId() ) : null;
    //
    //        final DiffFormatter df = new DiffFormatter( DisabledOutputStream.INSTANCE );
    //        df.setRepository( repo );
    //        df.setDiffComparator( RawTextComparator.DEFAULT );
    //        df.setDetectRenames( true );
    //
    //        final List<DiffEntry> diffs;
    //        if ( parent == null )
    //        {
    //            diffs =
    //                df.scan( new EmptyTreeIterator(),
    //                         new CanonicalTreeParser( null, tree.getObjectReader(), commit.getTree() ) );
    //        }
    //        else
    //        {
    //            diffs = df.scan( parent.getTree(), commit.getTree() );
    //        }
    //
    //        for ( final DiffEntry diff : diffs )
    //        {
    //            logger.info( "({} {} {}", diff.getChangeType()
    //                                          .name(), diff.getNewMode()
    //                                                       .getBits(), diff.getNewPath() );
    //        }
    //    }

    private ChangeSummary toChangeSummary( final RevCommit commit )
    {
        final PersonIdent who = commit.getAuthorIdent();
        final Date when = new Date( TimeUnit.MILLISECONDS.convert( commit.getCommitTime(), TimeUnit.SECONDS ) );
        return new ChangeSummary( who.getName(), commit.getFullMessage(), when, commit.getId()
                                                                                      .name() );
    }

    public GitManager pullUpdates()
        throws GitSubsystemException
    {
        return pullUpdates( ConflictStrategy.merge );
    }

    public GitManager pullUpdates( final ConflictStrategy strategy )
        throws GitSubsystemException
    {
        try
        {
            git.pull()
               .setStrategy( strategy.mergeStrategy() )
               .setRemoteBranchName( config.getRemoteBranchName() )
               .setRebase( true )
               .call();
        }
        catch ( final GitAPIException e )
        {
            throw new GitSubsystemException( "Cannot pull content updates via git: " + e.getMessage(), e );
        }

        return this;
    }

    public GitManager pushUpdates()
        throws GitSubsystemException
    {
        try
        {
            git.push()
               .call();
        }
        catch ( final GitAPIException e )
        {
            throw new GitSubsystemException( "Cannot push content updates via git: " + e.getMessage(), e );
        }

        return this;
    }

    public String getOriginUrl()
    {
        return git.getRepository()
                  .getConfig()
                  .getString( "remote", "origin", "url" );
    }

    public GitManager commitModifiedFiles( final ChangeSummary changeSummary )
        throws GitSubsystemException
    {
        Status status;
        try
        {
            status = git.status()
                        .call();
        }
        catch ( NoWorkTreeException | GitAPIException e )
        {
            throw new GitSubsystemException( "Failed to retrieve status of: %s. Reason: %s", e, rootDir, e.getMessage() );
        }

        final Map<String, StageState> css = status.getConflictingStageState();
        if ( !css.isEmpty() )
        {
            throw new GitSubsystemException( "%s contains conflicts. Cannot auto-commit.\n  %s", rootDir,
                                             new JoinString( "\n  ", css.entrySet() ) );
        }

        final Set<String> toAdd = new HashSet<>();
        final Set<String> modified = status.getModified();
        if ( modified != null && !modified.isEmpty() )
        {
            toAdd.addAll( modified );
        }

        final Set<String> untracked = status.getUntracked();
        if ( untracked != null && !untracked.isEmpty() )
        {
            toAdd.addAll( untracked );
        }

        final Set<String> untrackedFolders = status.getUntrackedFolders();
        if ( untrackedFolders != null && !untrackedFolders.isEmpty() )
        {
            toAdd.addAll( untrackedFolders );

            //            for ( String folderPath : untrackedFolders )
            //            {
            //                File dir = new File( rootDir, folderPath );
            //                Files.walkFileTree( null, null )
            //            }
        }

        if ( !toAdd.isEmpty() )
        {
            addAndCommitPaths( changeSummary, toAdd );
        }

        return this;
    }
}
