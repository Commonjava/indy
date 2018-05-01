/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.filer.def.perf;

import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.galley.CacheOnlyLocation;
import org.commonjava.indy.model.galley.RepositoryLocation;
import org.commonjava.maven.galley.cache.routes.RouteSelector;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Location;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.junit.Assert.*;

public class RouteSelectorPerfTest
{
    final RouteSelector nonCompiledOne = ( resource ) -> {
        if ( resource != null )
        {
            String pattern = "^indy:maven:hosted:.*$";
            final Location loc = resource.getLocation();
            if ( loc != null )
            {
                final String uri = loc.getUri();
                return uri != null && uri.matches( pattern );
            }
        }
        return false;
    };

    final RouteSelector compiledOne = new RouteSelector()
    {
        final Pattern localPattern = Pattern.compile( "^indy:maven:hosted:.*$" );

        @Override
        public boolean isDisposable( ConcreteResource resource )
        {
            if ( resource != null )
            {
                final Location loc = resource.getLocation();
                if ( loc != null )
                {
                    final String uri = loc.getUri();
                    return uri != null && localPattern.matcher( uri ).matches();
                }
            }
            return false;
        }
    };

    private ConcreteResource hostedResource;

    private ConcreteResource remoteResource;

    @Before
    public void setUp()
    {
        hostedResource = new ConcreteResource( new CacheOnlyLocation( new HostedRepository( MAVEN_PKG_KEY, "hosted" ) ),
                                               String.format( "/path/to/my/hosted/%s", "index.html" ) );
        remoteResource =
                new ConcreteResource( new RepositoryLocation( new RemoteRepository( MAVEN_PKG_KEY, "remote", "http://foo.bar/" ) ),
                                      String.format( "/path/to/my/remote/%s", "index.html" ) );
    }

    @Test
    public void testInSingleThread()
    {
        final int loopTime = 2000000;
        long current = System.currentTimeMillis();
        for ( int i = 0; i < loopTime; i++ )
        {
            if ( i % 2 == 0 )
            {
                assertTrue( nonCompiledOne.isDisposable( hostedResource ) );
            }
            else
            {
                assertFalse( nonCompiledOne.isDisposable( remoteResource ) );
            }
        }
        long end = System.currentTimeMillis();
        System.out.println(
                String.format( "non compiled regex selector matches %s in single thread costs %s ms", loopTime,
                               ( end - current ) + "" ) );

        current = System.currentTimeMillis();
        for ( int i = 0; i < loopTime; i++ )
        {
            if ( i % 2 == 0 )
            {
                assertTrue( compiledOne.isDisposable( hostedResource ) );
            }
            else
            {
                assertFalse( compiledOne.isDisposable( remoteResource ) );
            }
        }
        end = System.currentTimeMillis();
        System.out.println( String.format( "compiled regex selector matches %s in single thread costs %s ms", loopTime,
                                           ( end - current ) + "" ) );
    }

    @Test
    public void testInMultiThreads()
            throws Exception
    {
        final int tasks = 20;
        final int loopTime = 100000;
        final Executor executor = Executors.newFixedThreadPool( tasks );
        final CountDownLatch latch = new CountDownLatch( tasks );
        long current = System.currentTimeMillis();
        for ( int i = 0; i < tasks; i++ )
        {
            executor.execute( () -> {
                for ( int j = 0; j < loopTime; j++ )
                {
                    if ( j % 2 == 0 )
                    {
                        assertTrue( nonCompiledOne.isDisposable( hostedResource ) );
                    }
                    else
                    {
                        assertFalse( nonCompiledOne.isDisposable( remoteResource ) );
                    }
                }
                latch.countDown();
            } );
        }
        latch.await();
        long end = System.currentTimeMillis();
        System.out.println(
                String.format( "non compiled regex selector matches %s in %s threads with each of %s matches costs %s",
                               tasks * loopTime, tasks, loopTime, ( end - current ) + "" ) );

        final CountDownLatch latch2 = new CountDownLatch( tasks );
        current = System.currentTimeMillis();
        for ( int i = 0; i < tasks; i++ )
        {
            executor.execute( () -> {
                for ( int j = 0; j < loopTime; j++ )
                {
                    if ( j % 2 == 0 )
                    {
                        assertTrue( compiledOne.isDisposable( hostedResource ) );
                    }
                    else
                    {
                        assertFalse( compiledOne.isDisposable( remoteResource ) );
                    }
                }
                latch2.countDown();
            } );
        }
        latch2.await();
        end = System.currentTimeMillis();
        System.out.println(
                String.format( "compiled regex selector matches %s in %s threads with each of %s matches costs %s",
                               tasks * loopTime, tasks, loopTime, ( end - current ) + "" ) );
    }
}
