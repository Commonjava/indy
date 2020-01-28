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

import static org.commonjava.indy.metrics.RequestContextHelper.getContext;

@Interceptor
@MetricWrapper
public class HoneycombWrapperEndInterceptor
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private HoneycombConfiguration config;

    @Inject
    private HoneycombManager honeycombManager;

    @AroundInvoke
    public Object operation( InvocationContext context ) throws Exception
    {
        logger.trace( "START: Honeycomb metrics-end wrapper" );
        if ( !config.isEnabled() )
        {
            logger.trace( "SKIP: Honeycomb metrics-end wrapper" );
            return context.proceed();
        }

        if ( !config.isSpanIncluded( context.getMethod() ) )
        {
            logger.trace( "SKIP: Honeycomb metrics-end wrapper (span not configured)" );
            return context.proceed();
        }

        Span span = honeycombManager.getBeeline().getActiveSpan();
        try
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

            return context.proceed();
        }
        finally
        {
            logger.trace( "END: Honeycomb metrics-end wrapper" );
        }
    }

}
