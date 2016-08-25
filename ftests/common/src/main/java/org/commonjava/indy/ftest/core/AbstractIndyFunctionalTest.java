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
package org.commonjava.indy.ftest.core;

import com.fasterxml.jackson.databind.Module;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.commonjava.indy.boot.BootStatus;
import org.commonjava.indy.boot.IndyBootException;
import org.commonjava.indy.client.core.Indy;
import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.junit.rules.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.junit.Assert.fail;

public abstract class AbstractIndyFunctionalTest
{
    private static final int NAME_LEN = 8;

    private static final String NAME_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_";

    public static final long DEFAULT_TEST_TIMEOUT = 120;

    public static final String TIMEOUT_ENV_FACTOR_SYSPROP = "testEnvTimeoutMultiplier";

    protected Indy client;

    protected CoreServerFixture fixture;

    protected final Logger logger = LoggerFactory.getLogger( getClass() );

    @Rule
    public TestName name = new TestName();

    @Rule
    public Timeout timeout = Timeout.builder()
                                    .withLookingForStuckThread( true )
                                    .withTimeout( getTestTimeoutSeconds(), TimeUnit.SECONDS )
                                    .build();

    protected File etcDir;

    protected File dataDir;

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

        client = new Indy( fixture.getUrl(), new IndyObjectMapper( getAdditionalMapperModules() ),
                            getAdditionalClientModules() ).connect();
    }

    protected float getTestEnvironmentTimeoutMultiplier()
    {
        return Float.parseFloat( System.getProperty( TIMEOUT_ENV_FACTOR_SYSPROP, "1" ) );
    }

    protected void waitForEventPropagation()
    {
        long ms = 1000 * getTestTimeoutMultiplier();

        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.info( "Waiting {}ms for Indy server events to clear.", ms );
        // give events time to propagate
        try
        {
            Thread.sleep( ms );
        }
        catch ( InterruptedException e )
        {
            e.printStackTrace();
            fail( "Thread interrupted while waiting for server events to propagate.");
        }
    }

    protected final long getTestTimeoutSeconds()
    {
        return (long) ( getTestTimeoutMultiplier() * getTestEnvironmentTimeoutMultiplier() * DEFAULT_TEST_TIMEOUT );
    }

    protected int getTestTimeoutMultiplier()
    {
        return 1;
    }

    @After
    public void stop()
    {
        waitForEventPropagation();
        waitForEventPropagation();
        closeQuietly( fixture );
        closeQuietly( client );
    }

    protected final CoreServerFixture newServerFixture()
            throws IndyBootException, IOException
    {
        final CoreServerFixture fixture = new CoreServerFixture();

        etcDir = new File( fixture.getBootOptions().getIndyHome(), "etc/indy" );
        dataDir = new File( fixture.getBootOptions().getIndyHome(), "var/lib/indy/data" );

        initBaseTestConfig( fixture );
        initTestConfig( fixture );
        initTestData( fixture );

        return fixture;
    }

    protected void initTestConfig( CoreServerFixture fixture )
            throws IOException
    {
    }

    protected void initTestData( CoreServerFixture fixture )
            throws IOException
    {
    }

    protected void initBaseTestConfig( CoreServerFixture fixture )
            throws IOException
    {
        if ( isSchedulerEnabled() )
        {
            writeConfigFile( "conf.d/scheduler.conf", readTestResource( "default-test-scheduler.conf" ) );
        }
        else
        {
            writeConfigFile( "conf.d/scheduler.conf", "[scheduler]\nenabled=false" );
        }
    }

    protected boolean isSchedulerEnabled()
    {
        return true;
    }

    protected String readTestResource( String resource )
            throws IOException
    {
        return IOUtils.toString( Thread.currentThread().getContextClassLoader().getResourceAsStream( resource ) );
    }

    protected void writeConfigFile( String confPath, String contents )
            throws IOException
    {
        File confFile = new File( etcDir, confPath );
        logger.info( "Writing configuration to: {}\n\n{}\n\n", confFile, contents );

        confFile.getParentFile().mkdirs();

        FileUtils.write( confFile, contents );
    }

    protected void writeDataFile( String path, String contents )
            throws IOException
    {
        File confFile = new File( dataDir, path );

        logger.info( "Writing data file to: {}\n\n{}\n\n", confFile, contents );
        confFile.getParentFile().mkdirs();

        FileUtils.write( confFile, contents );
    }

    protected Collection<Module> getAdditionalMapperModules()
    {
        return Collections.emptySet();
    }

    protected Collection<IndyClientModule> getAdditionalClientModules()
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

    protected TemporaryFolder getTemp()
    {
        return fixture.getTempFolder();
    }

    protected boolean isEmpty( String val )
    {
        return val == null || val.length() < 1;
    }

}
