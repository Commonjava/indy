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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.commonjava.aprox.boot.AproxBootException;
import org.commonjava.aprox.boot.BootStatus;
import org.commonjava.aprox.client.core.Aprox;
import org.commonjava.aprox.client.core.AproxClientModule;
import org.commonjava.aprox.model.core.io.AproxObjectMapper;
import org.commonjava.aprox.test.fixture.core.CoreServerFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.rules.Timeout;

import com.fasterxml.jackson.databind.Module;

public abstract class AbstractAproxFunctionalTest
{
    private static final int NAME_LEN = 8;

    private static final String NAME_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_";

    protected static final String TEST_TIMEOUT_SYSPROP = "testTimeout";

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
        Thread.currentThread()
              .setName( getClass().getSimpleName() + "." + name.getMethodName() );

        fixture = newServerFixture();
        fixture.start();

        if ( !fixture.isStarted() )
        {
            final BootStatus status = fixture.getBootStatus();
            throw new IllegalStateException( "server fixture failed to boot.", status.getError() );
        }

        client =
            new Aprox( fixture.getUrl(), new AproxObjectMapper( getAdditionalMapperModules() ),
                       getAdditionalClientModules() ).connect();
    }

    protected final long getTestTimeoutSeconds()
    {
        return getTestTimeoutMultiplier() * Long.parseLong( System.getProperty( TEST_TIMEOUT_SYSPROP, "120" ) );
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

    protected CoreServerFixture newServerFixture()
        throws AproxBootException, IOException
    {
        return new CoreServerFixture();
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
