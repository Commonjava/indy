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
package org.commonjava.aprox.test.fixture.core;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Properties;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.commonjava.aprox.boot.AproxBootException;
import org.commonjava.aprox.boot.BootFinder;
import org.commonjava.aprox.boot.BootInterface;
import org.commonjava.aprox.boot.BootOptions;
import org.commonjava.aprox.boot.BootStatus;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoreServerFixture
    implements Closeable
{

    private static final int MAX_PORTGEN_TRIES = 16;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final BootOptions options;

    private final BootInterface booter;

    private BootStatus status;

    private TemporaryFolder temp;

    public CoreServerFixture( final TemporaryFolder folder )
        throws AproxBootException, IOException
    {
        this( BootFinder.find(), newBootOptions( null, folder.newFolder( "aprox-home" )
                                          .getAbsolutePath() ) );
    }

    public CoreServerFixture( final BootInterface booter, final BootOptions options )
    {
        this.booter = booter;
        this.options = editBootOptions( options, null );
    }

    public CoreServerFixture()
        throws AproxBootException, IOException
    {
        this.booter = BootFinder.find();
        this.temp = newTemporaryFolder();
        this.options = newBootOptions( null, temp.newFolder( "aprox-home" )
                                                 .getAbsolutePath() );
    }

    public CoreServerFixture( final BootInterface booter, final File bootDefaults, final File aproxHome )
    {
        this( booter, newBootOptions( bootDefaults, aproxHome.getAbsolutePath() ) );
    }

    public CoreServerFixture( final BootInterface booter, final File aproxHome,
                                   final BootOptions bootOptions )
    {
        this( booter, editBootOptions( bootOptions, aproxHome.getAbsolutePath() ) );
    }

    public String getUrl()
    {
        return String.format( "http://127.0.0.1:%d/api/", options.getPort() );
    }

    public BootStatus getBootStatus()
    {
        return status;
    }

    public BootOptions getBootOptions()
    {
        return options;
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

        logger.info( "\n\n\n\nAProx STARTING UP\n\n\n" );
        status = booter.start( options );

        if ( status == null )
        {
            throw new IllegalStateException( "Failed to start server!" );
        }
        else if ( status.isFailed() )
        {
            throw new IllegalStateException( "Failed to start server!", status.getError() );
        }
    }

    @Override
    public void close()
    {
        stop();
    }

    public void stop()
    {
        if ( status != null && status.isSuccess() )
        {
            logger.info( "\n\n\n\nAProx SHUTTING DOWN\n\n\n" );
            booter.stop();
        }

        if ( temp != null )
        {
            temp.delete();
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

    private static BootOptions editBootOptions( final BootOptions options, final String aproxHome )
    {
        if ( aproxHome != null )
        {
            final Properties properties = System.getProperties();
            properties.setProperty( "aprox.home", aproxHome );
            System.setProperties( properties );
        }

        options.setPort( generatePort() );

        return options;
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
