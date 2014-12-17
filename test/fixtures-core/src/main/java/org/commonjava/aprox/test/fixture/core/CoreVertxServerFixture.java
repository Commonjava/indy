package org.commonjava.aprox.test.fixture.core;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Properties;
import java.util.Random;

import org.codehaus.plexus.interpolation.InterpolationException;
import org.commonjava.aprox.bind.vertx.boot.BootOptions;
import org.commonjava.aprox.bind.vertx.boot.BootStatus;
import org.commonjava.aprox.bind.vertx.boot.Booter;
import org.junit.rules.ExternalResource;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoreVertxServerFixture
    extends ExternalResource
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

    public CoreVertxServerFixture( final File aproxHome )
    {
        this( null, aproxHome );
    }

    public CoreVertxServerFixture( final File bootDefaults, final File aproxHome )
    {
        this( newBootOptions( bootDefaults, aproxHome.getAbsolutePath() ) );
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

    public String getUrl()
    {
        return String.format( "http://127.0.0.1:%d/api/", options.getPort() );
    }

    private static int generatePort()
    {
        final Random rand = new Random();
        int tries = 0;
        while ( tries < MAX_PORTGEN_TRIES )
        {
            final int port = Math.abs( rand.nextInt() ) % 32000;
            try
            {
                new ServerSocket( port );
                return port;
            }
            catch ( final IOException | IllegalArgumentException e )
            {
            }

            tries++;
        }

        throw new IllegalStateException( "Cannot find an open port after " + MAX_PORTGEN_TRIES + " tries. Giving up." );
    }

    public BootStatus getBootStatus()
    {
        return status;
    }

    public boolean isBooted()
    {
        return status.isSuccess();
    }

    @Override
    public void before()
        throws Throwable
    {
        if ( options.isHelp() )
        {
            throw new IllegalArgumentException( "Cannot start server when help option is enabled." );
        }

        booter = new Booter( options );
        logger.info( "\n\n\n\nAProx STARTING UP\n\n\n" );
        status = booter.start();
    }

    @Override
    public void after()
    {
        if ( status.isSuccess() )
        {
            logger.info( "\n\n\n\nAProx SHUTTING DOWN\n\n\n" );
            booter.stop();
        }
    }

}
