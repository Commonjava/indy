/**
 * Copyright (C) 2013 Red Hat, Inc.
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
package org.commonjava.indy.metrics.context;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.commonjava.indy.metrics.context.reporter.ContextConsoleJsonReporter;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ContextMetricSimpleTest
{
    private final MetricRegistry metrics = new MetricRegistry();

    private final Executor threadPools = Executors.newFixedThreadPool( 5 );

    @Test
    public void test()
            throws Exception
    {
        final int threads = 5;

        final CountDownLatch latch = new CountDownLatch( 5 );

        for ( int i = 0; i < threads; i++ )
        {
            final int seconds = i;
            threadPools.execute( () -> {
                try
                {
                    final long threadId = Thread.currentThread().getId();

                    ContextTimer timer = getOrRegister(
                            String.format( "context_timer for thread %s", Thread.currentThread().getName() ),
                            new ContextTimer(), ContextTimer.class );

                    ContextMeter requests = getOrRegister(
                            String.format( "context_requests for thread %s", Thread.currentThread().getName() ),
                            new ContextMeter(), ContextMeter.class );


                    requests.addContext( "request-id", String.format( "pnc-%s", threadId ) );
                    requests.addContext( "internal-id", String.format( "indy-internal-%s", threadId ) );
                    requests.addContext( "external-id", String.format( "indy-external-%s", threadId ) );
                    timer.addContext( "user", String.format( "user-%s", seconds ) );
                    requests.mark();


                    timer.addContext( "request-id", String.format( "pnc-%s", threadId ) );
                    timer.addContext( "internal-id", String.format( "indy-internal-%s", threadId ) );
                    timer.addContext( "external-id", String.format( "indy-external-%s", threadId ) );
                    timer.addContext( "user", String.format( "user-%s", seconds ) );

                    Timer.Context context = timer.time();
                    waitSeconds( seconds );
                    context.stop();

                    timer = getOrRegister(
                            String.format( "context_timer for thread %s", Thread.currentThread().getName() ),
                            new ContextTimer(), ContextTimer.class );

                    timer.addContext( "request-id", String.format( "pnc-%s-again", threadId ) );
                    timer.addContext( "internal-id", String.format( "indy-internal-%s-again", threadId ) );
                    timer.addContext( "external-id", String.format( "indy-external-%s-again", threadId ) );
                    timer.addContext( "user", String.format( "user-%s-again", seconds ) );

                    context = timer.time();
                    waitSeconds( seconds );
                    context.stop();



                    waitSeconds( 5 );
                }
                finally
                {
                    latch.countDown();
                }
            } );
        }
        startReport();
        latch.await();

    }

    void startReport()
    {
        ContextConsoleJsonReporter reporter = ContextConsoleJsonReporter.forRegistry( metrics )
                                                                        .convertRatesTo( TimeUnit.SECONDS )
                                                                        .convertDurationsTo( TimeUnit.MILLISECONDS )
                                                                        .build();
        reporter.start( 1, TimeUnit.SECONDS );
    }

    void waitSeconds( int secs )
    {
        try
        {
            Thread.sleep( secs * 1000 );
        }
        catch ( InterruptedException e )
        {
        }
    }

    private <T extends Metric> T getOrRegister( String name, T metric, Class<T> metricClass )
    {
        Metric m = metrics.getMetrics().get( name );
        if ( m != null && metricClass.isAssignableFrom( m.getClass() ) )
        {
            return (T) m;
        }
        if ( m == null )
        {
            metrics.register( name, metric );
            return metric;
        }

        throw new IllegalArgumentException(
                "A metric named " + name + " already exists, but it is not a type of " + metricClass.getName() );
    }
}
