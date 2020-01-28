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
import org.commonjava.indy.measure.annotation.MetricWrapper;
import org.commonjava.indy.subsys.honeycomb.HoneycombManager;
import org.commonjava.indy.subsys.honeycomb.config.HoneycombConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.util.stream.Stream;

import static org.commonjava.indy.metrics.IndyMetricsConstants.getDefaultName;
import static org.commonjava.indy.metrics.RequestContextHelper.getContext;

@Interceptor
@MetricWrapper
public class HoneycombWrapperInterceptor
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private HoneycombConfiguration config;

    @Inject
    private HoneycombManager honeycombManager;

    @AroundInvoke
    public Object operation( InvocationContext context ) throws Exception
    {
        logger.info( "START: Honeycomb lambda wrapper" );
        if ( !config.isEnabled() )
        {
            logger.info( "SKIP Honeycomb lambda wrapper" );
            return context.proceed();
        }

        String name = HoneycombInterceptorUtils.getMetricNameFromParam( context );
        if ( name == null || !config.isSpanIncluded( context.getMethod() ) )
        {
            logger.info( "SKIP Honeycomb lambda wrapper (no span name or span not configured)" );
            context.proceed();
        }

        Span span = null;
        try
        {
            span = honeycombManager.startChildSpan( name );
            if ( span != null )
            {
                span.markStart();
            }

            logger.trace( "startChildSpan, span: {}, defaultName: {}", span, name );
            return context.proceed();
        }
        finally
        {
            if ( span != null )
            {
                Span theSpan = span;
                Stream.of( config.getFields()).forEach( field->{
                    Object value = getContext( field );
                    if ( value != null )
                    {
                        theSpan.addField( field, value );
                    }
                });

                logger.trace( "closeSpan, {}", span );
                span.close();
            }

            logger.info( "END: Honeycomb lambda wrapper" );
        }
    }

}
