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
package org.commonjava.indy.subsys.newrelic.interceptor;

import com.newrelic.telemetry.spans.Span;
import org.commonjava.indy.measure.annotation.Measure;
import org.commonjava.indy.subsys.newrelic.NewRelicManager;
import org.commonjava.indy.subsys.newrelic.config.NewRelicConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.lang.reflect.Method;
import java.util.stream.Stream;

import static org.commonjava.indy.metrics.IndyMetricsConstants.getDefaultName;
import static org.commonjava.indy.metrics.RequestContextHelper.getContext;

@Interceptor
@Measure
public class NewRelicMeasureInterceptor
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private NewRelicConfiguration config;

    @Inject
    private NewRelicManager manager;

    @AroundInvoke
    public Object operation( InvocationContext context ) throws Exception
    {
        String sig = context.getMethod().getDeclaringClass().getSimpleName() + "." + context.getMethod().getName();
        logger.info( "START: New Relic methodwrapper for: {}", sig );
        if ( !config.isEnabled() )
        {
            logger.trace( "SKIP: New Relic method wrapper disabled for: {}", sig );
            return context.proceed();
        }

        Method method = context.getMethod();
        Measure measure = method.getAnnotation( Measure.class );
        if ( measure == null )
        {
            measure = method.getDeclaringClass().getAnnotation( Measure.class );
        }

        if ( measure == null || !config.isSpanIncluded( method ) )
        {
            logger.trace( "SKIP: New Relic method wrapper (no @Measure annotation or span is not configured for: {})", sig );
            return context.proceed();
        }

        Class<?> cls = context.getMethod().getDeclaringClass();

        String defaultName = getDefaultName( cls, context.getMethod().getName() );

        Span span = null;
        try
        {
            span = manager.startChildSpan( defaultName );
            logger.trace( "New Relic startChildSpan, span: {}, defaultName: {} for: {}", span, defaultName, sig );
            return context.proceed();
        }
        finally
        {
            if ( span != null )
            {
                Span theSpan = span;

                logger.trace( "closeSpan, {} for: {}", span, sig );
                manager.close( span );
            }

            logger.trace( "END: Honeycomb method wrapper for: {}", sig );
        }
    }

}
