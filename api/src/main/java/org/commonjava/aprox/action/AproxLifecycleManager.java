package org.commonjava.aprox.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.commonjava.aprox.stats.AProxVersioning;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the startup sequence (managing {@link BootupAction}, {@link MigrationAction}, and {@link StartupAction} instances in order), and the 
 * shutdown sequence (managing {@link ShutdownAction} instances in order.
 * 
 * @author jdcasey
 */
@ApplicationScoped
public class AproxLifecycleManager
{
    private static final Comparator<AproxLifecycleAction> PRIORITY_COMPARATOR = new Comparator<AproxLifecycleAction>()
    {
        @Override
        public int compare( final AproxLifecycleAction first, final AproxLifecycleAction second )
        {
            final int comp = first.getPriority() - second.getPriority();
            if ( comp < 0 )
            {
                return 1;
            }
            else if ( comp > 0 )
            {
                return -1;
            }

            return 0;
        }
    };

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private AProxVersioning versioning;

    @Inject
    private Instance<BootupAction> bootupActionInstances;

    @Inject
    private Instance<MigrationAction> migrationActionInstances;

    @Inject
    private Instance<StartupAction> startupActionInstances;

    @Inject
    private Instance<ShutdownAction> shutdownActionInstances;

    private List<BootupAction> bootupActions;

    private List<MigrationAction> migrationActions;

    private List<StartupAction> startupActions;

    private List<ShutdownAction> shutdownActions;

    protected AproxLifecycleManager()
    {
    }

    public AproxLifecycleManager( final AProxVersioning versioning, final Iterable<BootupAction> bootupActionInstances,
                                  final Iterable<MigrationAction> migrationActionInstances,
                                  final Iterable<StartupAction> startupActionInstances,
                                  final Iterable<ShutdownAction> shutdownActionInstances )
    {
        initialize( bootupActionInstances, migrationActionInstances, startupActionInstances, shutdownActionInstances );
    }

    @PostConstruct
    public void init()
    {
        initialize( bootupActionInstances, migrationActionInstances, startupActionInstances, shutdownActionInstances );
    }

    private void initialize( final Iterable<BootupAction> bootupActionInstances,
                             final Iterable<MigrationAction> migrationActionInstances,
                             final Iterable<StartupAction> startupActionInstances,
                             final Iterable<ShutdownAction> shutdownActionInstances )
    {
        bootupActions = new ArrayList<>();
        for ( final BootupAction action : bootupActionInstances )
        {
            bootupActions.add( action );
        }
        Collections.sort( bootupActions, PRIORITY_COMPARATOR );

        migrationActions = new ArrayList<>();
        for ( final MigrationAction action : migrationActionInstances )
        {
            migrationActions.add( action );
        }
        Collections.sort( migrationActions, PRIORITY_COMPARATOR );

        startupActions = new ArrayList<>();
        for ( final StartupAction action : startupActionInstances )
        {
            startupActions.add( action );
        }
        Collections.sort( startupActions, PRIORITY_COMPARATOR );

        shutdownActions = new ArrayList<>();
        for ( final ShutdownAction action : shutdownActionInstances )
        {
            shutdownActions.add( action );
        }
        Collections.sort( shutdownActions, PRIORITY_COMPARATOR );
    }

    /**
     * Start sequence is:
     * <ul>
     *   <li>Start all {@link BootupAction} instances, with highest priority executing first.</li>
     *   <li>Run all {@link MigrationAction} instances, with highest priority executing first.</li>
     *   <li>Run all {@link StartupAction} instances, with highest priority executing first.</li>
     * </ul>
     * @throws AproxLifecycleException
     */
    public void start()
        throws AproxLifecycleException
    {
        logger.info( "\n\n\n\n\n STARTING AProx\n    Version: {}\n    Built-By: {}\n    Commit-ID: {}\n    Built-On: {}\n\n\n\n\n",
                     versioning.getVersion(), versioning.getBuilder(), versioning.getCommitId(),
                     versioning.getTimestamp() );

        runBootupActions();
        runMigrationActions();
        runStartupActions();

        logger.info( "...done. AProx is ready to run." );
    }

    /**
     * Run all {@link ShutdownAction} instances, with highest priority executing first.
     * @throws AproxLifecycleException
     */
    public void stop()
        throws AproxLifecycleException
    {
        logger.info( "\n\n\n\n\n SHUTTING DOWN AProx\n    Version: {}\n    Built-By: {}\n    Commit-ID: {}\n    Built-On: {}\n\n\n\n\n",
                     versioning.getVersion(), versioning.getBuilder(), versioning.getCommitId(),
                     versioning.getTimestamp() );

        runShutdownActions();

        logger.info( "...done. AProx is ready to shut down." );
    }

    private void runBootupActions()
        throws AproxLifecycleException
    {
        if ( bootupActions != null )
        {
            logger.info( "Running bootup actions..." );
            for ( final BootupAction action : bootupActions )
            {
                logger.info( "Running bootup action: '{}'", action.getId() );
                action.init();
            }
        }
    }

    private void runMigrationActions()
        throws AproxLifecycleException
    {
        boolean changed = false;
        if ( migrationActions != null )
        {
            logger.info( "Running migration actions..." );
            for ( final MigrationAction action : migrationActions )
            {
                logger.info( "Running migration action: '{}'", action.getId() );
                changed = action.migrate() || changed;
            }
        }
    }

    private void runStartupActions()
        throws AproxLifecycleException
    {
        if ( startupActions != null )
        {
            logger.info( "Running startup actions..." );
            for ( final StartupAction action : startupActions )
            {
                logger.info( "Running startup action: '{}'", action.getId() );
                action.start();
            }
        }
    }

    private void runShutdownActions()
        throws AproxLifecycleException
    {
        if ( shutdownActions != null )
        {
            logger.info( "Running shutdown actions..." );
            for ( final ShutdownAction action : shutdownActions )
            {
                logger.info( "Running shutdown action: '{}'", action.getId() );
                action.stop();
            }
        }
    }

    /**
     * Create a Runnable that can be used in {@link Runtime#addShutdownHook(Thread)}.
     */
    public Runnable createShutdownRunnable()
    {
        return new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    stop();
                }
                catch ( final AproxLifecycleException e )
                {
                    throw new RuntimeException( "\n\nFailed to stop AProx: " + e.getMessage(), e );
                }
            }
        };
    }

}
