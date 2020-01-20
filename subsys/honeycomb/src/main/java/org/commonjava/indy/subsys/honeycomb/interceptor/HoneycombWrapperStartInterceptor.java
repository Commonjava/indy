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
import org.commonjava.indy.measure.annotation.MetricWrapperNamed;
import org.commonjava.indy.measure.annotation.MetricWrapperStart;
import org.commonjava.indy.subsys.honeycomb.HoneycombManager;
import org.commonjava.indy.subsys.honeycomb.config.HoneycombConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.stream.Stream;

import static org.commonjava.indy.metrics.RequestContextHelper.getContext;

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
        if ( !config.isEnabled() )
        {
            return context.proceed();
        }

        String name = HoneycombInterceptorUtils.getMetricNameFromParam( context );
        if ( name == null )
        {
            context.proceed();
        }

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

        return context.proceed();
    }

}
