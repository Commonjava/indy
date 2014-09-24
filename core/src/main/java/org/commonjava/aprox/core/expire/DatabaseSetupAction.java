package org.commonjava.aprox.core.expire;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.action.start.AproxInitException;
import org.commonjava.aprox.action.start.MigrationAction;
import org.commonjava.aprox.core.conf.AproxSchedulerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named( "scheduler-db" )
public class DatabaseSetupAction
    implements MigrationAction
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private static final String DEFAULT_DDL_RESOURCE = "scheduler/quartz-derby.sql";

    private static final String QUARTZ_DATASOURCE_PREFIX = "org.quartz.dataSource.";

    private static final String DS_DRIVER = "driver";

    private static final String DS_URL = "url";

    private static final String DDL_PROP = "ddl";

    @Inject
    private AproxSchedulerConfig schedulerConfig;

    @Override
    public String getId()
    {
        return "Scheduler database setup";
    }

    @Override
    public boolean migrate()
        throws AproxInitException
    {
        String ddl = null;
        final Map<String, String> connectInfo = new HashMap<>();
        for ( final Map.Entry<String, String> entry : schedulerConfig.getConfiguration()
                                                                     .entrySet() )
        {
            final String key = entry.getKey();
            if ( DDL_PROP.equalsIgnoreCase( key ) )
            {
                ddl = entry.getValue();
            }
            else if ( key.startsWith( QUARTZ_DATASOURCE_PREFIX ) )
            {
                final String[] parts = key.split( "\\." );
                connectInfo.put( parts[parts.length - 1].toLowerCase(), entry.getValue() );
            }
        }

        List<String> lines = null;
        if ( ddl != null )
        {
            try
            {
                lines = FileUtils.readLines( new File( ddl ) );
            }
            catch ( final IOException e )
            {
                throw new AproxInitException( "Failed to read DDL from: " + ddl, e );
            }
        }

        if ( lines == null )
        {
            final InputStream resource = Thread.currentThread()
                                               .getContextClassLoader()
                                               .getResourceAsStream( DEFAULT_DDL_RESOURCE );

            if ( resource != null )
            {
                try
                {
                    lines = IOUtils.readLines( resource );
                }
                catch ( final IOException e )
                {
                    throw new AproxInitException( "Failed to read DDL from: " + DEFAULT_DDL_RESOURCE, e );
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

            final String driverName = connectInfo.get( DS_DRIVER );
            final String url = connectInfo.get( DS_URL );
            Connection connection = null;
            ResultSet tableQuery = null;
            Statement stmt = null;
            try
            {
                Thread.currentThread()
                      .getContextClassLoader()
                      .loadClass( driverName );

                connection = DriverManager.getConnection( url );
                connection.setAutoCommit( true );

                final DatabaseMetaData dmd = connection.getMetaData();
                tableQuery = dmd.getTables( connection.getCatalog(), null, "qrtz.+", null );
                if ( !tableQuery.next() )
                {
                    tableQuery.close();

                    stmt = connection.createStatement();
                    for ( final String command : commands )
                    {
                        logger.info( "Executing migation SQL:\n\n{}\n\n", command );
                        stmt.execute( command );
                    }
                }

                return true;
            }
            catch ( final ClassNotFoundException e )
            {
                throw new AproxInitException( "Failed to load database driver: " + driverName, e );
            }
            catch ( final SQLException e )
            {
                throw new AproxInitException( "Failed to connect to database: " + url, e );
            }
            finally
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
        }

        return false;
    }

}
