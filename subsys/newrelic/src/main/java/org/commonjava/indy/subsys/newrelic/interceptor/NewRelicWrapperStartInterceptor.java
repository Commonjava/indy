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
import org.commonjava.indy.measure.annotation.MetricWrapperStart;
import org.commonjava.indy.subsys.newrelic.NewRelicManager;
import org.commonjava.indy.subsys.newrelic.config.NewRelicConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@Interceptor
@MetricWrapperStart
public class NewRelicWrapperStartInterceptor
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private NewRelicConfiguration config;

    @Inject
    private NewRelicManager manager;

    @AroundInvoke
    public Object operation( InvocationContext context ) throws Exception
    {
        logger.trace( "START: New Relic metrics-start wrapper" );
        if ( !config.isEnabled() )
        {
            logger.trace( "SKIP: New Relic metrics-start wrapper" );
            return context.proceed();
        }

        String name = NewRelicInterceptorUtils.getMetricNameFromParam( context );
        if ( name == null || !config.isSpanIncluded( context.getMethod() ) )
        {
            logger.trace( "SKIP: New Relic metrics-start wrapper (no span name or span not configured)" );
            context.proceed();
        }

        try
        {
            Span span = manager.startChildSpan( name );
            logger.trace( "startChildSpan, span: {}, defaultName: {}", span, name );
        }
        catch ( Exception e )
        {
            logger.error( "Error in New Relic subsystem! " + e.getMessage(), e );
        }
        finally
        {
            logger.trace( "END: New Relic metrics-start wrapper" );
        }

        return context.proceed();
    }

}
