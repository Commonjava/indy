/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.test.fixture.core;

import static org.commonjava.propulsor.boot.PortFinder.findOpenPort;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.codehaus.plexus.interpolation.InterpolationException;
import org.commonjava.propulsor.boot.BootException;
import org.commonjava.propulsor.boot.BootFinder;
import org.commonjava.propulsor.boot.BootInterface;
import org.commonjava.propulsor.boot.BootOptions;
import org.commonjava.propulsor.boot.BootStatus;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoreServerFixture
    implements Closeable
{

    private static final int MAX_PORTGEN_TRIES = 16;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final BootOptions options;

    private BootInterface booter;

    private BootStatus status;

    private TemporaryFolder temp;

    public CoreServerFixture( final TemporaryFolder temp )
        throws BootException, IOException
    {
        this( BootFinder.find(), newBootOptions( null, temp.newFolder( "indy-home" ).getAbsolutePath() ) );
        this.temp = temp;
    }

    public CoreServerFixture( final BootInterface booter, final BootOptions options )
    {
        this.booter = booter;
        this.options = editBootOptions( options, null );
        this.temp = newTemporaryFolder();
    }

    public CoreServerFixture()
        throws BootException, IOException
    {
        this.booter = BootFinder.find();
        this.temp = newTemporaryFolder();
        this.options = newBootOptions( null, temp.newFolder( "indy-home" )
                                                 .getAbsolutePath() );
    }

    public CoreServerFixture( final BootInterface booter, final File bootDefaults, final File indyHome )
    {
        this( booter, newBootOptions( bootDefaults, indyHome.getAbsolutePath() ) );
    }

    public CoreServerFixture( final BootInterface booter, final File indyHome,
                                   final BootOptions bootOptions )
    {
        this( booter, editBootOptions( bootOptions, indyHome.getAbsolutePath() ) );
    }

    public TemporaryFolder getTempFolder()
    {
        return temp;
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

        logger.info( "\n\n\n\nIndy STARTING UP\n\n\n" );
        booter.start( options );
        status = new BootStatus();
        status.markSuccess();
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
            logger.info( "\n\n\n\nIndy SHUTTING DOWN\n\n\n" );
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

    private static BootOptions newBootOptions( final File bootDefaults, final String indyHome )
    {
        final Properties properties = System.getProperties();
        properties.setProperty( "indy.home", indyHome );
        System.setProperties( properties );

        try
        {
            final BootOptions options = new BootOptions( "indy", indyHome, bootDefaults );
            options.setPort( findOpenPort( MAX_PORTGEN_TRIES ) );

            return options;
        }
        catch ( IOException | InterpolationException e )
        {
            throw new IllegalStateException( "Cannot start core Indy server with the given configuration: "
                + e.getMessage(), e );
        }
    }

    private static BootOptions editBootOptions( final BootOptions options, final String indyHome )
    {
        if ( indyHome != null )
        {
            final Properties properties = System.getProperties();
            properties.setProperty( "indy.home", indyHome );
            System.setProperties( properties );
        }

        options.setPort( findOpenPort( MAX_PORTGEN_TRIES ) );

        return options;
    }

}
