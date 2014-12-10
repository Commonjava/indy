package org.commonjava.aprox.core.expire;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.sql.rowset.RowSetWarning;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.action.AproxLifecycleException;
import org.commonjava.aprox.action.BootupAction;
import org.commonjava.aprox.action.ShutdownAction;
import org.commonjava.aprox.core.conf.AproxSchedulerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named( "scheduler-db" )
public class DatabaseLifecycleActions
    implements BootupAction, ShutdownAction
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private static final String APACHEDB_DDL_RESOURCE = "scheduler/quartz-derby.sql";

    private static final String APACHEDB_CREATE_OPTION = "create=true";

    private static final String APACHEDB_DRIVER_SUPER_PACKAGE = "org.apache.derby";

    private static final String APACHEDB_URL_PREFIX = "jdbc:derby:";

    private static final String APACHEDB_SHUTDOWN_URL = "jdbc:derby:;shutdown=true";

    @Inject
    private AproxSchedulerConfig schedulerConfig;

    @Override
    public String getId()
    {
        return "Scheduler database setup";
    }

    @Override
    public int getPriority()
    {
        return 95;
    }

    @Override
    public void init()
        throws AproxLifecycleException
    {
        final CharSequence violations = schedulerConfig.validate();
        if ( violations != null )
        {
            throw new AproxLifecycleException( "Scheduler configuration is not valid:\n%s", violations );
        }

        String ddl = schedulerConfig.getDdlFile();
        final String driverName = schedulerConfig.getDbDriver();
        String url = schedulerConfig.getDbUrl();

        if ( driverName.startsWith( APACHEDB_DRIVER_SUPER_PACKAGE ) )
        {
            final File dbDir = new File( url.substring( APACHEDB_URL_PREFIX.length() ) );
            logger.info( "Looking for existing apacheDB at: {}", dbDir );
            if ( !dbDir.exists() && !url.contains( APACHEDB_CREATE_OPTION ) )
            {
                if ( !url.contains( ";" ) )
                {
                    url += ";";
                }

                url += APACHEDB_CREATE_OPTION;
            }

            if ( ddl == null )
            {
                ddl = APACHEDB_DDL_RESOURCE;
            }
        }

        List<String> lines = null;

        final File ddlFile = new File( ddl );
        if ( ddlFile.exists() )
        {
            try
            {
                lines = FileUtils.readLines( ddlFile );
            }
            catch ( final IOException e )
            {
                throw new AproxLifecycleException( "Failed to read DDL from file: " + ddl, e );
            }
        }

        if ( lines == null )
        {
            final InputStream resource = Thread.currentThread()
                                               .getContextClassLoader()
                                               .getResourceAsStream( ddl );

            if ( resource != null )
            {
                try
                {
                    lines = IOUtils.readLines( resource );
                }
                catch ( final IOException e )
                {
                    throw new AproxLifecycleException( "Failed to read DDL from classpath: " + ddl, e );
                }
            }
        }

        if ( lines != null )
        {
            final List<String> commands = new ArrayList<>();
            final StringBuilder currentCommand = new StringBuilder();
            for ( String line : lines )
            {
                line = line.trim();
                if ( line.length() < 1 || line.startsWith( "--" ) )
                {
                    continue;
                }
                else if ( line.endsWith( ";" ) )
                {
                    line = line.substring( 0, line.length() - 1 );
                    if ( currentCommand.length() > 0 )
                    {
                        currentCommand.append( "\n" )
                                      .append( line );
                        commands.add( currentCommand.toString() );
                        currentCommand.setLength( 0 );
                    }
                    else
                    {
                        commands.add( line );
                    }
                }
                else
                {
                    if ( currentCommand.length() > 0 )
                    {
                        currentCommand.append( "\n" );
                    }
                    currentCommand.append( line );
                }
            }

            Connection connection = null;
            ResultSet tableQuery = null;
            Statement stmt = null;
            try
            {
                Thread.currentThread()
                      .getContextClassLoader()
                      .loadClass( driverName );

                logger.info( "Connecting to DB: {}", url );
                connection = DriverManager.getConnection( url );
                connection.setAutoCommit( true );
            }
            catch ( final ClassNotFoundException e )
            {
                throw new AproxLifecycleException( "Failed to load database driver: " + driverName, e );
            }
            catch ( final SQLWarning | RowSetWarning e )
            {
                logger.debug( e.getMessage(), e );
            }
            catch ( final SQLException e )
            {
                throw new AproxLifecycleException( "Failed to connect to database: " + url, e );
            }

            try
            {
                if ( connection != null )
                {
                    stmt = connection.createStatement();
                    boolean found = false;
                    try
                    {
                        tableQuery = stmt.executeQuery( "select count(*) from qrtz_job_details" );
                        found = true;
                        tableQuery.close();
                    }
                    catch ( final SQLException e )
                    {
                        logger.info( "Failed to query qrtz_job_details for existence. Tables will be created." );
                    }

                    if ( !found )
                    {
                        for ( final String command : commands )
                        {
                            logger.info( "Executing migation SQL:\n\n{}\n\n", command );
                            stmt.execute( command );
                            connection.commit();
                        }
                    }
                    else
                    {
                        logger.info( "Scheduler database tables appear to exist. Skipping." );
                    }
                }
            }
            catch ( final SQLWarning | RowSetWarning e )
            {
                logger.warn( e.getMessage(), e );
            }
            catch ( final SQLException e )
            {
                throw new AproxLifecycleException( "Failed to modify database for scheduler: " + url, e );
            }
            finally
            {
                close( tableQuery, stmt, connection, url );
            }
        }
    }

    private void close( final ResultSet tableQuery, final Statement stmt, final Connection connection, final String url )
    {
        if ( tableQuery != null )
        {
            try
            {
                if ( !tableQuery.isClosed() )
                {
                    tableQuery.close();
                }
            }
            catch ( final SQLException e )
            {
                logger.debug( "Failed to close database table query: " + url, e );
            }
        }

        if ( stmt != null )
        {
            try
            {
                if ( !stmt.isClosed() )
                {
                    stmt.close();
                }
            }
            catch ( final SQLException e )
            {
                logger.debug( "Failed to close database statement instance: " + url, e );
            }
        }

        if ( connection != null )
        {
            try
            {
                connection.close();
            }
            catch ( final SQLException e )
            {
                logger.debug( "Failed to close database connection: " + url, e );
            }
        }
    }

    @Override
    public void stop()
        throws AproxLifecycleException
    {
        final String dbDriver = schedulerConfig.getDbDriver();
        if ( dbDriver.startsWith( APACHEDB_DRIVER_SUPER_PACKAGE ) )
        {
            final String url = APACHEDB_SHUTDOWN_URL;

            Connection connection = null;
            try
            {
                Thread.currentThread()
                      .getContextClassLoader()
                      .loadClass( dbDriver );

                logger.info( "Connecting to DB: {}", url );
                connection = DriverManager.getConnection( url );
            }
            catch ( final ClassNotFoundException e )
            {
                throw new AproxLifecycleException( "Failed to load database driver: " + dbDriver, e );
            }
            catch ( final SQLException e )
            {
                logger.debug( e.getMessage(), e );
            }
            finally
            {
                close( null, null, connection, url );
            }
        }
    }

}
