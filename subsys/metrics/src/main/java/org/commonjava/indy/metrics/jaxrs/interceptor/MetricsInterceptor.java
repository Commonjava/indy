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
import org.commonjava.indy.IndyMetricsManager;
import org.commonjava.indy.measure.annotation.MetricNamed;
import org.commonjava.indy.metrics.conf.annotation.IndyMetricsNamed;
import org.commonjava.indy.measure.annotation.Measure;
import org.commonjava.indy.metrics.conf.IndyMetricsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.codahale.metrics.MetricRegistry.name;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.commonjava.indy.IndyMetricsNames.EXCEPTION;
import static org.commonjava.indy.IndyMetricsNames.METER;
import static org.commonjava.indy.IndyMetricsNames.TIMER;

/**
 * Created by xiabai on 2/27/17.
 */
@Interceptor
@Measure
public class MetricsInterceptor
{

    private static final Logger logger = LoggerFactory.getLogger( MetricsInterceptor.class );

    @Inject
    IndyMetricsManager util;

    @Inject
    @IndyMetricsNamed
    IndyMetricsConfig config;

    @AroundInvoke
    public Object operation( InvocationContext context ) throws Exception
    {
        if ( !config.isMetricsEnabled() )
            return context.proceed();

        Measure measure = context.getMethod().getAnnotation( Measure.class );
        if ( measure == null )
        {
            return context.proceed();
        }

        logger.trace( "Gathering metrics for: {}", context.getContextData() );

        String cls = context.getMethod().getDeclaringClass().getName();
        String method = context.getMethod().getName();
        String defaultName = name( cls, method );

        List<Timer.Context> timers = Stream.of( measure.timers() )
                                           .map( named -> {
                                               String name = getName( named, defaultName, TIMER );
                                               Timer.Context tc = util.getTimer( name ).time();
                                               logger.debug( "START: {} ({})", name, tc );
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
                                                  String name = getName( named, defaultName, EXCEPTION );
                                                  Meter requests = util.getMeter( name );
                                                  logger.debug( "ERRORS++ {}", name );
                                                  requests.mark();
                                              } );

            throw e;
        }
        finally
        {
            if ( timers != null )
            {
                timers.forEach( timer->{
                    logger.debug( "STOP: {}", timer );
                    timer.stop();
                } );

            }
            Stream.of( measure.meters() ).forEach( ( named ) ->
                                                    {
                                                        String name = getName( named, defaultName, METER );
                                                        Meter requests = util.getMeter( name );
                                                        logger.debug( "CALLS++ {}", name );
                                                        requests.mark();
                                                    } );
        }
    }

    private String getName( MetricNamed named, String defaultName, String suffix )
    {
        String name = named.value();
        if ( isNotBlank( name ) )
        {
            return name;
        }
        return name( defaultName, suffix );
    }

}
