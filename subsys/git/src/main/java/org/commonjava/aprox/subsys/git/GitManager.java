package org.commonjava.aprox.subsys.git;

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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.commonjava.aprox.audit.ChangeSummary;
import org.commonjava.maven.atlas.ident.util.JoinString;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.IndexDiff.StageState;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revplot.PlotCommit;
import org.eclipse.jgit.revplot.PlotCommitList;
import org.eclipse.jgit.revplot.PlotLane;
import org.eclipse.jgit.revplot.PlotWalk;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
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

    public GitManager( final GitConfig config )
        throws GitSubsystemException
    {
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

        String email = repo.getConfig()
                           .getString( "user", null, "email" );

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

        // TODO: Get the email info from somewhere...
        this.email = email;

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

            commit.setMessage( summary.getSummary() )
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

    public GitManager deleteAndCommit( final ChangeSummary summary, final File... deleted )
        throws GitSubsystemException
    {
        return deleteAndCommit( summary, Arrays.asList( deleted ) );
    }

    public GitManager deleteAndCommit( final ChangeSummary summary, final Collection<File> deleted )
        throws GitSubsystemException
    {
        try
        {
            RmCommand rm = git.rm();
            CommitCommand commit = git.commit();

            for ( final File file : deleted )
            {
                final String filepath = relativize( file );

                rm = rm.addFilepattern( filepath );
                commit = commit.setOnly( filepath );
            }

            logger.info( "Deleting:\n  " + join( deleted, "\n  " ) + "\n\nSummary: " + summary );

            rm.call();

            commit.setMessage( summary.getSummary() )
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
            final ObjectId oid = repo.resolve( "HEAD" );

            final PlotWalk pw = new PlotWalk( repo );
            final RevCommit rc = pw.parseCommit( oid );
            pw.markStart( rc );

            final String filepath = relativize( f );

            pw.setTreeFilter( AndTreeFilter.create( PathFilter.create( filepath ), TreeFilter.ANY_DIFF ) );

            final PlotCommitList<PlotLane> cl = new PlotCommitList<>();
            cl.source( pw );
            if ( cl.size() < start )
            {
                return Collections.emptyList();
            }

            int to;
            if ( length < 0 )
            {
                to = cl.size();
            }
            else
            {
                to = start + length - 1;
            }

            if ( cl.size() < to )
            {
                to = cl.size() - 1;
            }

            final List<PlotCommit<PlotLane>> commits = cl.subList( start, to );
            final List<ChangeSummary> changelogs = new ArrayList<ChangeSummary>();
            for ( final PlotCommit<PlotLane> commit : commits )
            {
                changelogs.add( toChangeSummary( commit ) );
            }

            return changelogs;
        }
        catch ( RevisionSyntaxException | IOException e )
        {
            throw new GitSubsystemException( "Failed to resolve HEAD commit for: %s. Reason: %s", e, f, e.getMessage() );
        }
    }

    private ChangeSummary toChangeSummary( final PlotCommit<PlotLane> commit )
    {
        return new ChangeSummary( commit.getAuthorIdent()
                                        .getName(), commit.getFullMessage() );
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
