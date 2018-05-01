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
import org.commonjava.indy.metrics.conf.annotation.IndyMetricsNamed;
import org.commonjava.indy.measure.annotation.IndyMetrics;
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

/**
 * Created by xiabai on 2/27/17.
 */
@Interceptor
@IndyMetrics
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

        IndyMetrics metrics = context.getMethod().getAnnotation( IndyMetrics.class );
        if ( metrics == null )
        {
            return context.proceed();
        }

        logger.debug( "Gathering metrics for: {} (metrics annotation: {})", context.getContextData(), metrics );

        Measure measures = metrics.measure();
        List<Timer.Context> timers = Stream.of( measures.timers() )
                                           .map( named -> {
                                               Timer.Context tc = util.getTimer( named ).time();
                                               logger.debug( "START: {} ({})", named, tc );
                                               return tc;
                                           } )
                                           .collect( Collectors.toList() );

        try
        {
            return context.proceed();
        }
        catch ( Exception e )
        {
            Measure me = metrics.exceptions();
            Stream.of( me.meters() ).forEach( ( named ) ->
                                              {
                                                  Meter requests = util.getMeter( named );
                                                  logger.debug( "ERRORS++ {}", named);
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
            Stream.of( measures.meters() ).forEach( ( named ) ->
                                                    {
                                                        Meter requests = util.getMeter( named );
                                                        logger.debug( "CALLS++ {}", named);
                                                        requests.mark();
                                                    } );
        }
    }
}
