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
import org.slf4j.MDC;

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.codahale.metrics.MetricRegistry.name;
import static org.commonjava.indy.metrics.IndyMetricsConstants.DEFAULT;
import static org.commonjava.indy.metrics.IndyMetricsConstants.EXCEPTION;
import static org.commonjava.indy.metrics.IndyMetricsConstants.METER;
import static org.commonjava.indy.metrics.IndyMetricsConstants.TIMER;
import static org.commonjava.indy.metrics.IndyMetricsConstants.getDefaultName;
import static org.commonjava.indy.metrics.IndyMetricsConstants.getName;
import static org.commonjava.indy.metrics.MetricsConstants.FINAL_METRICS;
import static org.commonjava.indy.metrics.MetricsConstants.METRICS_PHASE;
import static org.commonjava.indy.metrics.MetricsConstants.PRELIMINARY_METRICS;

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

        if ( measure == null )
        {
            return context.proceed();
        }

        String defaultName = getDefaultName( context.getMethod().getDeclaringClass(), context.getMethod().getName() );
        logger.trace( "Gathering metrics for: {} using context: {}", defaultName, context.getContextData() );

        boolean inject = measure.timers().length < 1 && measure.exceptions().length < 1 && measure.meters().length < 1;

        Map<String, Timer.Context> timers = initTimers( measure, defaultName, inject );
        List<String> exceptionMeters = initMeters( measure, measure.exceptions(), EXCEPTION, defaultName, inject );
        List<String> meters = initMeters( measure, measure.meters(), METER, defaultName, inject );

        try
        {
            meters.forEach( ( name ) -> {
                Meter meter = metricsManager.getMeter( name + ".starts" );
                logger.trace( "CALLS++ {}", name );

                MDC.put( name + ".starts", "1" );
                meter.mark();

                logger.trace("Meter count for: {} is: {}", name, new Object(){
                    public String toString(){
                        return String.valueOf( metricsManager.getMeter( name ).getCount() );
                    }
                });
            } );

            MDC.put( METRICS_PHASE, PRELIMINARY_METRICS );
            logger.info( "Preliminary metrics" );

            return context.proceed();
        }
        catch ( Exception e )
        {
            new ArrayList<>( exceptionMeters).forEach( ( name ) -> {
                MDC.put( name, "1" );
                metricsManager.getMeter( name ).mark();

                logger.trace("Meter count for: {} is: {}", name, new Object(){
                    public String toString(){
                        return String.valueOf( metricsManager.getMeter( name ).getCount() );
                    }
                });

                String eMeterName = name( name, e.getClass().getSimpleName() );
                if ( !exceptionMeters.contains( eMeterName ) )
                {
                    exceptionMeters.add( eMeterName );

                    metricsManager.getMeter( eMeterName ).mark();
                    MDC.put( name, "1" );

                    logger.trace("Meter count for: {} is: {}", name, new Object(){
                        public String toString(){
                            return String.valueOf( metricsManager.getMeter( name ).getCount() );
                        }
                    });
                }
            } );

            throw e;
        }
        finally
        {
            if ( timers != null )
            {
                timers.forEach( (name, timer)->{
                    long duration = timer.stop();
                    MDC.put( name, Long.toString( duration ) );
                    logger.trace( "STOP: {}", timer );
                } );
            }

            meters.forEach( ( name ) -> {
                Meter meter = metricsManager.getMeter( name );
                logger.trace( "CALLS++ {}", name );

                MDC.put( name, "1" );
                meter.mark();

                logger.trace("Meter count for: {} is: {}", name, new Object(){
                    public String toString(){
                        return String.valueOf( metricsManager.getMeter( name ).getCount() );
                    }
                });
            } );

            MDC.put( METRICS_PHASE, FINAL_METRICS );
            logger.info( "Final metrics" );

            meters.forEach( name -> {
                MDC.remove( name + ".starts" );
                MDC.remove( name );
            } );
            exceptionMeters.forEach( name -> MDC.remove( name ) );
            timers.forEach( ( name, timer ) -> MDC.remove( name ) );
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

    private Map<String, Timer.Context> initTimers( final Measure measure, String defaultName, final boolean inject )
    {
        Map<String, Timer.Context> timers;

        MetricNamed[] timerMetrics = measure.timers();
        if ( inject && timerMetrics.length == 0 )
        {
            String name = getName( config.getNodePrefix(), DEFAULT, defaultName, TIMER );
            timers = Collections.singletonMap(name,
                    metricsManager.getTimer( name ).time() );
        }
        else
        {
            timers = new HashMap<>();
            Stream.of( timerMetrics ).forEach( named -> {
                String name = getName( config.getNodePrefix(), named.value(), defaultName, TIMER );
                Timer.Context tc = metricsManager.getTimer( name ).time();
                logger.trace( "START: {} ({})", name, tc );
                timers.put( name, tc );
            } );
        }

        return timers;
    }

}
