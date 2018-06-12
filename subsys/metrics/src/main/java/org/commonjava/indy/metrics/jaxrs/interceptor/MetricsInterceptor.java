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
import org.apache.commons.lang3.ClassUtils;
import org.commonjava.indy.metrics.IndyMetricsManager;
import org.commonjava.indy.measure.annotation.MetricNamed;
import org.commonjava.indy.measure.annotation.Measure;
import org.commonjava.indy.metrics.conf.IndyMetricsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.codahale.metrics.MetricRegistry.name;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.commonjava.indy.metrics.IndyMetricsConstants.EXCEPTION;
import static org.commonjava.indy.metrics.IndyMetricsConstants.METER;
import static org.commonjava.indy.metrics.IndyMetricsConstants.TIMER;
import static org.commonjava.indy.measure.annotation.MetricNamed.DEFAULT;

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

        logger.trace( "Gathering metrics for: {}", context.getContextData() );
        String nodePrefix = config.getNodePrefix();

        String defaultName = getDefaultName( context );

        List<Timer.Context> timers = Stream.of( measure.timers() ).map( named ->
                                        {
                                            String name = getName( nodePrefix, named, defaultName );
                                            Timer.Context tc = metricsManager.getTimer( name ).time();
                                            logger.trace( "START: {} ({})", name, tc );
                                            return tc;
                                        } )
                                        .collect( Collectors.toList() );

        try
        {
            return context.proceed();
        }
        catch ( Exception e )
        {
            Stream.of( measure.exceptions() ).forEach( ( named ) ->
                                        {
                                            String name = getName( nodePrefix, named, defaultName, EXCEPTION );
                                            Meter meter = metricsManager.getMeter( name );
                                            logger.trace( "ERRORS++ {}", name );
                                            meter.mark();
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
            Stream.of( measure.meters() ).forEach( ( named ) ->
                                        {
                                            String name = getName( nodePrefix, named, defaultName );
                                            Meter meter = metricsManager.getMeter( name );
                                            logger.trace( "CALLS++ {}", name );
                                            meter.mark();
                                        } );
        }
    }

    /**
     * Get default metric name. Use abbreviated package name, e.g., foo.bar.ClassA.methodB -> f.b.ClassA.methodB
     */
    private String getDefaultName( InvocationContext context )
    {
        // minimum len 1 shortens the package name and keeps class name
        String cls = ClassUtils.getAbbreviatedName( context.getMethod().getDeclaringClass().getName(), 1 );
        String method = context.getMethod().getName();
        return name( cls, method );
    }

    /**
     * Get the metric fullname.
     * @param named user specified name
     * @param defaultName 'class name + method name', not null.
     */
    private String getName( String nodePrefix, MetricNamed named, String defaultName )
    {
        String name = named.value();
        if ( isBlank( name ) || name.equals( DEFAULT ) )
        {
            name = defaultName;
        }
        return name( nodePrefix, name );
    }

    private String getName( String nodePrefix, MetricNamed named, String defaultName, String suffix )
    {
        return name( getName( nodePrefix, named, defaultName ), suffix );
    }

}
