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
package org.commonjava.indy.subsys.honeycomb.interceptor;

import io.honeycomb.beeline.tracing.Span;
import org.commonjava.cdi.util.weft.ThreadContext;
import org.commonjava.indy.measure.annotation.MetricWrapperStart;
import org.commonjava.indy.subsys.honeycomb.HoneycombManager;
import org.commonjava.indy.subsys.honeycomb.config.HoneycombConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import static org.commonjava.indy.metrics.IndyMetricsConstants.SKIP_METRIC;
import static org.commonjava.indy.metrics.RequestContextHelper.getContext;
import static org.commonjava.indy.subsys.honeycomb.interceptor.HoneycombInterceptorUtils.SAMPLE_OVERRIDE;

@Interceptor
@MetricWrapperStart
public class HoneycombWrapperStartInterceptor
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private HoneycombConfiguration config;

    @Inject
    private HoneycombManager honeycombManager;

    @AroundInvoke
    public Object operation( InvocationContext context ) throws Exception
    {
        logger.trace( "START: Honeycomb metrics-start wrapper" );
        if ( !config.isEnabled() )
        {
            logger.trace( "SKIP: Honeycomb metrics-start wrapper" );
            return context.proceed();
        }

        String name = HoneycombInterceptorUtils.getMetricNameFromParam( context );
        if ( name == null || SKIP_METRIC.equals( name ) || config.getSampleRate( context.getMethod() ) < 1 )
        {
            logger.trace( "SKIP: Honeycomb metrics-start wrapper (no span name or span not configured)" );
            return context.proceed();
        }

//        ThreadContext.getContext( true ).put( SAMPLE_OVERRIDE, Boolean.TRUE );
        try
        {
            Span span = honeycombManager.startChildSpan( name );
            if ( span != null )
            {
                span.markStart();
            }

            logger.trace( "startChildSpan, span: {}, defaultName: {}", span, name );
        }
        catch ( Exception e )
        {
            logger.error( "Error in honeycomb subsystem! " + e.getMessage(), e );
        }
        finally
        {
            logger.trace( "END: Honeycomb metrics-start wrapper" );
        }

        return context.proceed();
    }

}
