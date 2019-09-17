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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

        Map<String, Timer.Context> timers = initTimers( measure, defaultName );
        List<String> exceptionMeters = initMeters( measure, measure.exceptions(), EXCEPTION, defaultName );
        List<String> meters = initMeters( measure, measure.meters(), METER, defaultName );

        List<String> startMeters = meters.stream().map( name -> name( name, "starts" ) ).collect( Collectors.toList() );

        Set<String> toClean = new HashSet<>();
        toClean.addAll( meters );
        toClean.addAll( startMeters );
        toClean.addAll( timers.keySet() );

        try
        {
            metricsManager.mark( startMeters );

            return context.proceed();
        }
        catch ( Exception e )
        {
            metricsManager.mark( exceptionMeters );

            List<String> eClassMeters = exceptionMeters.stream()
                                                       .map( name -> name( name, e.getClass().getSimpleName() ) )
                                                       .filter( name -> !exceptionMeters.contains( name ) )
                                                       .collect( Collectors.toList() );

            toClean.addAll( eClassMeters );

            metricsManager.mark( eClassMeters );

            throw e;
        }
        finally
        {
            metricsManager.stopTimers( timers );
            metricsManager.mark( meters );
        }
    }

    private List<String> initMeters( final Measure measure, final MetricNamed[] metrics, String classifier,
                                     final String defaultName )
    {
        List<String> meters = new ArrayList<>();

        meters.add( getName( config.getNodePrefix(), DEFAULT, defaultName, classifier ) );
        Stream.of( metrics )
              .map( metric -> getName( config.getNodePrefix(), metric.value(), defaultName, classifier ) )
              .forEach( metric -> meters.add( metric ) );

        logger.trace( "Got meters for {} with classifier: {}: {}", defaultName, classifier, meters );

        return meters;
    }

    private Map<String, Timer.Context> initTimers( final Measure measure, String defaultName )
    {
        Map<String, Timer.Context> timers = new HashMap<>();

        String name = getName( config.getNodePrefix(), DEFAULT, defaultName, TIMER );
        timers.put(name,
                metricsManager.getTimer( name ).time() );

        MetricNamed[] timerMetrics = measure.timers();
        Stream.of( timerMetrics ).forEach( named -> {
            String timerName = getName( config.getNodePrefix(), named.value(), defaultName, TIMER );
            Timer.Context tc = metricsManager.getTimer( timerName ).time();
            logger.trace( "START: {} ({})", timerName, tc );
            timers.put( timerName, tc );
        } );

        return timers;
    }

}
