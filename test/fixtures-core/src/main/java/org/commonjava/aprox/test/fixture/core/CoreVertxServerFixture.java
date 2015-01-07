package org.commonjava.aprox.test.fixture.core;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Properties;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.commonjava.aprox.bind.vertx.boot.BootOptions;
import org.commonjava.aprox.bind.vertx.boot.BootStatus;
import org.commonjava.aprox.bind.vertx.boot.Booter;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoreVertxServerFixture
{

    private static final int MAX_PORTGEN_TRIES = 16;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final BootOptions options;

    private Booter booter;

    private BootStatus status;

    public CoreVertxServerFixture( final TemporaryFolder folder )
    {
        this( newBootOptions( null, folder.newFolder( "aprox-home." )
                                          .getAbsolutePath() ) );
    }

    public CoreVertxServerFixture( final BootOptions options )
    {
        this.options = options;
    }

    public CoreVertxServerFixture()
    {
        this( newTemporaryFolder() );
    }

    public CoreVertxServerFixture( final File aproxHome )
    {
        this( null, aproxHome );
    }

    public CoreVertxServerFixture( final File bootDefaults, final File aproxHome )
    {
        this( newBootOptions( bootDefaults, aproxHome.getAbsolutePath() ) );
    }

    public String getUrl()
    {
        return String.format( "http://127.0.0.1:%d/api/", options.getPort() );
    }

    public BootStatus getBootStatus()
    {
        return status;
    }

    public boolean isStarted()
    {
        return status.isSuccess();
    }

    public void start()
        throws Exception
    {
        if ( options.isHelp() )
        {
            throw new IllegalArgumentException( "Cannot start server when help option is enabled." );
        }

        booter = new Booter( options );
        logger.info( "\n\n\n\nAProx STARTING UP\n\n\n" );
        status = booter.start();

        if ( status == null )
        {
            throw new IllegalStateException( "Failed to start server!" );
        }
        else if ( status.isFailed() )
        {
            throw new IllegalStateException( "Failed to start server!", status.getError() );
        }
    }

    public void stop()
    {
        if ( status.isSuccess() )
        {
            logger.info( "\n\n\n\nAProx SHUTTING DOWN\n\n\n" );
            booter.stop();
        }
    }

    private static TemporaryFolder newTemporaryFolder()
    {
        final TemporaryFolder folder = new TemporaryFolder();
        try
        {
            folder.create();
        }
        catch ( final IOException e )
        {
            throw new IllegalStateException( "Failed to init TemporaryFolder: " + e.getMessage(), e );
        }

        return folder;
    }

    private static BootOptions newBootOptions( final File bootDefaults, final String aproxHome )
    {
        final Properties properties = System.getProperties();
        properties.setProperty( "aprox.home", aproxHome );
        System.setProperties( properties );

        try
        {
            final BootOptions options = new BootOptions( bootDefaults, aproxHome );
            options.setPort( generatePort() );

            return options;
        }
        catch ( IOException | InterpolationException e )
        {
            throw new IllegalStateException( "Cannot start core AProx server with the given configuration: "
                + e.getMessage(), e );
        }
    }

    private static int generatePort()
    {
        final Random rand = new Random();
        int tries = 0;
        while ( tries < MAX_PORTGEN_TRIES )
        {
            final int port = Math.abs( rand.nextInt() ) % 32000;
            ServerSocket ss = null;
            try
            {
                ss = new ServerSocket( port );
                return port;
            }
            catch ( final IOException | IllegalArgumentException e )
            {
            }
            finally
            {
                IOUtils.closeQuietly( ss );
            }

            tries++;
        }

        throw new IllegalStateException( "Cannot find an open port after " + MAX_PORTGEN_TRIES + " tries. Giving up." );
    }

}
