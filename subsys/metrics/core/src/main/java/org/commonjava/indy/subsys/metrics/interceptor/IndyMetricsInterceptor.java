/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.subsys.metrics.interceptor;

import com.codahale.metrics.Timer;
import org.commonjava.o11yphant.annotation.Measure;
import org.commonjava.indy.subsys.metrics.IndyMetricsManager;
import org.commonjava.indy.subsys.metrics.conf.IndyMetricsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.codahale.metrics.MetricRegistry.name;
import static org.commonjava.indy.IndyContentConstants.NANOS_PER_MILLISECOND;
import static org.commonjava.o11yphant.metrics.MetricsConstants.DEFAULT;
import static org.commonjava.o11yphant.metrics.MetricsConstants.EXCEPTION;
import static org.commonjava.o11yphant.metrics.MetricsConstants.METER;
import static org.commonjava.o11yphant.metrics.MetricsConstants.TIMER;
import static org.commonjava.o11yphant.metrics.MetricsConstants.getDefaultName;
import static org.commonjava.o11yphant.metrics.MetricsConstants.getName;

@Interceptor
@Measure
public class IndyMetricsInterceptor
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private IndyMetricsManager metricsManager;

    @Inject
    private IndyMetricsConfig config;

    @AroundInvoke
    public Object operation( InvocationContext context ) throws Exception
    {
        if ( !config.isMetricsEnabled() || !metricsManager.checkMetered() )
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

        Map<String, Timer.Context> timers = initTimers( measure, defaultName );
        List<String> exceptionMeters = initMeters( measure, EXCEPTION, defaultName );
        List<String> meters = initMeters( measure, METER, defaultName );

        List<String> startMeters = meters.stream().map( name -> name( name, "starts" ) ).collect( Collectors.toList() );

        long start = System.nanoTime();

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

            metricsManager.mark( eClassMeters );

            throw e;
        }
        finally
        {
            metricsManager.stopTimers( timers );
            metricsManager.mark( meters );

            double elapsed = (System.nanoTime() - start) / NANOS_PER_MILLISECOND;

            metricsManager.accumulate( defaultName, elapsed );
        }
    }

    private List<String> initMeters( final Measure measure, String classifier,
                                     final String defaultName )
    {
        List<String> meters = new ArrayList<>();

        meters.add( getName( config.getNodePrefix(), DEFAULT, defaultName, classifier ) );

        logger.trace( "Got meter for {} with classifier: {}: {}", defaultName, classifier, meters );

        return meters;
    }

    private Map<String, Timer.Context> initTimers( final Measure measure, String defaultName )
    {
        Map<String, Timer.Context> timers = new HashMap<>();

        String name = getName( config.getNodePrefix(), DEFAULT, defaultName, TIMER );
        timers.put(name,
                metricsManager.startTimer( name ) );

        return timers;
    }

}
