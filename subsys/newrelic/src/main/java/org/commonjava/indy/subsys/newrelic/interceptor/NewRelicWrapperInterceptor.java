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
import org.commonjava.indy.measure.annotation.MetricWrapper;
import org.commonjava.indy.subsys.newrelic.NewRelicManager;
import org.commonjava.indy.subsys.newrelic.config.NewRelicConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.util.stream.Stream;

import static org.commonjava.indy.metrics.RequestContextHelper.getContext;

@Interceptor
@MetricWrapper
public class NewRelicWrapperInterceptor
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
        logger.info( "START: New Relic lambda wrapper for: {}", sig );
        if ( !config.isEnabled() )
        {
            logger.info( "SKIP New Relic lambda wrapper for: {}", sig );
            return context.proceed();
        }

        String name = NewRelicInterceptorUtils.getMetricNameFromParam( context );
        if ( name == null || !config.isSpanIncluded( context.getMethod() ) )
        {
            logger.info( "SKIP New Relic lambda wrapper (no span name: '{}' or span not configured for: '{}')", name, sig );
            context.proceed();
        }

        Span span = null;
        try
        {
            span = manager.startChildSpan( name );

            logger.trace( "startChildSpan, span: {}, defaultName: {} for: {}", span, name, sig );
            return context.proceed();
        }
        finally
        {
            if ( span != null )
            {
                logger.trace( "closeSpan, {} for: {}", span, sig );
                manager.close( span );
            }

            logger.info( "END: New Relic lambda wrapper for: {}", sig );
        }
    }

}
