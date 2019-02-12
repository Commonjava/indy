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
package org.commonjava.indy.metrics.jaxrs.interceptor;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import org.commonjava.indy.measure.annotation.Measure;
import org.commonjava.indy.measure.annotation.MetricNamed;
import org.commonjava.indy.metrics.IndyMetricsManager;
import org.commonjava.indy.metrics.conf.IndyMetricsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.codahale.metrics.MetricRegistry.name;
import static org.commonjava.indy.metrics.IndyMetricsConstants.DEFAULT;
import static org.commonjava.indy.metrics.IndyMetricsConstants.EXCEPTION;
import static org.commonjava.indy.metrics.IndyMetricsConstants.METER;
import static org.commonjava.indy.metrics.IndyMetricsConstants.TIMER;
import static org.commonjava.indy.metrics.IndyMetricsConstants.getDefaultName;
import static org.commonjava.indy.metrics.IndyMetricsConstants.getName;

@Interceptor
@Measure
public class MetricsInterceptor
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private IndyMetricsManager metricsManager;

    @Inject
    private IndyMetricsConfig config;

    @AroundInvoke
    public Object operation( InvocationContext context ) throws Exception
    {
        if ( !config.isMetricsEnabled() )
        {
            return context.proceed();
        }

        Method method = context.getMethod();
        Measure measure = method.getAnnotation( Measure.class );
        if ( measure == null )
        {
            measure = method.getDeclaringClass().getAnnotation( Measure.class );
        }

        String defaultName = getDefaultName( context.getMethod().getDeclaringClass(), context.getMethod().getName() );
        logger.trace( "Gathering metrics for: {} using context: {}", defaultName, context.getContextData() );

        boolean inject = measure.timers().length < 1 && measure.exceptions().length < 1 && measure.meters().length < 1;

        List<Timer.Context> timers = initTimers( measure, defaultName, inject );
        List<String> exceptionMeters = initMeters( measure, measure.exceptions(), EXCEPTION, defaultName, inject );
        List<String> meters = initMeters( measure, measure.meters(), METER, defaultName, inject );

        try
        {
            meters.forEach( ( name ) -> {
                Meter meter = metricsManager.getMeter( name + ".starts" );
                logger.trace( "CALLS++ {}", name );
                meter.mark();

                logger.trace("Meter count for: {} is: {}", name, new Object(){
                    public String toString(){
                        return String.valueOf( metricsManager.getMeter( name ).getCount() );
                    }
                });
            } );

            return context.proceed();
        }
        catch ( Exception e )
        {
            exceptionMeters.forEach( ( name ) -> {
                metricsManager.getMeter( name ).mark();
                logger.trace("Meter count for: {} is: {}", name, new Object(){
                    public String toString(){
                        return String.valueOf( metricsManager.getMeter( name ).getCount() );
                    }
                });
                metricsManager.getMeter( name( name, e.getClass().getSimpleName() ) ).mark();
                logger.trace("Meter count for: {} is: {}", name, new Object(){
                    public String toString(){
                        return String.valueOf( metricsManager.getMeter( name ).getCount() );
                    }
                });
            } );

            throw e;
        }
        finally
        {
            if ( timers != null )
            {
                timers.forEach( timer->{
                    logger.trace( "STOP: {}", timer );
                    timer.stop();
                } );
            }

            meters.forEach( ( name ) -> {
                Meter meter = metricsManager.getMeter( name );
                logger.trace( "CALLS++ {}", name );
                meter.mark();

                logger.trace("Meter count for: {} is: {}", name, new Object(){
                    public String toString(){
                        return String.valueOf( metricsManager.getMeter( name ).getCount() );
                    }
                });
            } );
        }
    }

    private List<String> initMeters( final Measure measure, final MetricNamed[] metrics, String classifier,
                                     final String defaultName, final boolean inject )
    {
        List<String> meters;
        if ( inject && ( metrics == null || metrics.length == 0 ) )
        {
            meters = Collections.singletonList( getName( config.getNodePrefix(), DEFAULT, defaultName, classifier ) );
        }
        else
        {
            meters = Stream.of( metrics )
                           .map( metric -> getName( config.getNodePrefix(), metric.value(), defaultName, classifier ) )
                           .collect( Collectors.toList() );
        }

        logger.trace( "Got meters for {} with classifier: {}: {}", defaultName, classifier, meters );

        return meters;
    }

    private List<Timer.Context> initTimers( final Measure measure, String defaultName, final boolean inject )
    {
        List<Timer.Context> timers;

        MetricNamed[] timerMetrics = measure.timers();
        if ( inject && timerMetrics.length == 0 )
        {
            timers = Collections.singletonList(
                    metricsManager.getTimer( getName( config.getNodePrefix(), DEFAULT, defaultName, TIMER ) ).time() );
        }
        else
        {
            timers = Stream.of( timerMetrics ).map( named -> {
                String name = getName( config.getNodePrefix(), named.value(), defaultName, TIMER );
                Timer.Context tc = metricsManager.getTimer( name ).time();
                logger.trace( "START: {} ({})", name, tc );
                return tc;
            } ).collect( Collectors.toList() );
        }

        return timers;
    }

}
