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
package org.commonjava.aprox.ftest.core;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Properties;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.commonjava.aprox.boot.AproxBootException;
import org.commonjava.aprox.boot.BootStatus;
import org.commonjava.aprox.client.core.Aprox;
import org.commonjava.aprox.client.core.AproxClientModule;
import org.commonjava.aprox.core.conf.AproxSchedulerConfig;
import org.commonjava.aprox.model.core.io.AproxObjectMapper;
import org.commonjava.aprox.test.fixture.core.CoreServerFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.rules.Timeout;

import com.fasterxml.jackson.databind.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractAproxFunctionalTest
{
    private static final int NAME_LEN = 8;

    private static final String NAME_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_";

    protected static final String TEST_TIMEOUT_SYSPROP = "testTimeout";

    protected static final String DEFAULT_TEST_TIMEOUT = "120";

    protected Aprox client;

    protected CoreServerFixture fixture;

    @Rule
    public TestName name = new TestName();

    @Rule
    public Timeout timeout = Timeout.builder()
                                    .withLookingForStuckThread( true )
                                    .withTimeout( getTestTimeoutSeconds(), TimeUnit.SECONDS )
                                    .build();

    @SuppressWarnings( "resource" )
    @Before
    public void start()
            throws Throwable
    {
        final long start = System.currentTimeMillis();
        TimerTask task = new TimerTask()
        {
            @Override
            public void run()
            {
                long time = System.currentTimeMillis();
                System.out.printf( "\n\n\nDate: %s\nElapsed: %s\n\n\n", new Date( time ),
                                   Duration.between( Instant.ofEpochMilli( start ), Instant.ofEpochMilli( time ) ) );
            }
        };

        new Timer().scheduleAtFixedRate( task, 0, 5000 );

        Thread.currentThread().setName( getClass().getSimpleName() + "." + name.getMethodName() );

        fixture = newServerFixture();
        fixture.start();

        if ( !fixture.isStarted() )
        {
            final BootStatus status = fixture.getBootStatus();
            throw new IllegalStateException( "server fixture failed to boot.", status.getError() );
        }

        client = new Aprox( fixture.getUrl(), new AproxObjectMapper( getAdditionalMapperModules() ),
                            getAdditionalClientModules() ).connect();
    }

    protected final long getTestTimeoutSeconds()
    {
        String timeout = System.getProperty( TEST_TIMEOUT_SYSPROP );
        if ( StringUtils.isEmpty( timeout ) )
        {
            timeout = DEFAULT_TEST_TIMEOUT;
        }

        return getTestTimeoutMultiplier() * Long.parseLong( timeout );
    }

    protected int getTestTimeoutMultiplier()
    {
        return 1;
    }

    @After
    public void stop()
    {
        closeQuietly( fixture );
        closeQuietly( client );
    }

    protected final CoreServerFixture newServerFixture()
            throws AproxBootException, IOException
    {
        final CoreServerFixture fixture = new CoreServerFixture();

        File etcDir = new File( fixture.getBootOptions().getAproxHome(), "etc/aprox" );

        initBaseTestConfig( fixture, etcDir );
        initTestConfig( fixture, etcDir );

        return fixture;
    }

    protected void initTestConfig( CoreServerFixture fixture, File etcDir )
            throws IOException
    {
    }

    protected void initBaseTestConfig( CoreServerFixture fixture, File etcDir )
            throws IOException
    {
        final File confFile = new File( etcDir, "conf.d/scheduler.conf" );

        confFile.getParentFile().mkdirs();
        //
        //        File sql = fixture.getTempFolder().newFile( "quartz-h2.sql" );
        //
        //        InputStream sqlStream =
        //                Thread.currentThread().getContextClassLoader().getResourceAsStream( "scheduler/quartz-h2.sql" );
        //
        //        try (FileOutputStream out = new FileOutputStream( sql ))
        //        {
        //            IOUtils.copy( sqlStream, out );
        //        }
        //
        //        InputStream defaultConfig = new AproxSchedulerConfig().getDefaultConfig();
        //        String config = IOUtils.toString( defaultConfig );
        //
        //        config = config.replaceAll( "org.quartz.dataSource.ds.URL = .*",
        //                                    "org.quartz.dataSource.ds.URL = jdbc:h2:mem:ds;INIT=runscript from '" + sql.getAbsolutePath() + "'" );
        //
        //        Logger logger = LoggerFactory.getLogger( getClass() );
        //        logger.debug( "scheduler.conf contents:\n\n{}\n\n", config );
        //
        //        FileUtils.write( confFile, config );

        FileUtils.write( confFile, "[scheduler]\nenabled=false" );
    }

    protected Collection<Module> getAdditionalMapperModules()
    {
        return Collections.emptySet();
    }

    protected Collection<AproxClientModule> getAdditionalClientModules()
    {
        return Collections.emptySet();
    }

    protected String newName()
    {
        final Random rand = new Random();
        final StringBuilder sb = new StringBuilder();
        for ( int i = 0; i < NAME_LEN; i++ )
        {
            sb.append( NAME_CHARS.charAt( ( Math.abs( rand.nextInt() ) % ( NAME_CHARS.length() - 1 ) ) ) );
        }

        return sb.toString();
    }

    protected String newUrl()
    {
        return String.format( "http://%s.com/", newName() );
    }

}
