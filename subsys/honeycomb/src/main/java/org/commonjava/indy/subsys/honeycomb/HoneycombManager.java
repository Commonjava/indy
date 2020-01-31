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
package org.commonjava.indy.subsys.honeycomb;

import io.honeycomb.beeline.tracing.Beeline;
import io.honeycomb.beeline.tracing.Span;
import io.honeycomb.beeline.tracing.propagation.PropagationContext;
import io.honeycomb.libhoney.HoneyClient;
import org.commonjava.cdi.util.weft.ThreadContext;
import org.commonjava.indy.metrics.RequestContextHelper;
import org.commonjava.indy.subsys.honeycomb.config.HoneycombConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import java.util.Map;

import static org.commonjava.indy.metrics.RequestContextHelper.CUMULATIVE_COUNTS;
import static org.commonjava.indy.metrics.RequestContextHelper.CUMULATIVE_TIMINGS;
import static org.commonjava.indy.metrics.RequestContextHelper.REQUEST_PARENT_SPAN;
import static org.commonjava.indy.metrics.RequestContextHelper.TRACE_ID;
import static org.commonjava.indy.metrics.RequestContextHelper.getContext;

@ApplicationScoped
public class HoneycombManager
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private HoneyClient client;

    @Inject
    private HoneycombContextualizer honeycombContextualizer;

    @Inject
    private HoneycombConfiguration configuration;

    @Inject
    private IndyTraceSampler traceSampler;

    public HoneycombManager()
    {
    }

    public HoneyClient getClient()
    {
        return honeycombContextualizer.getHoneyClient();
    }

    public Beeline getBeeline()
    {
        Beeline bl = honeycombContextualizer.getBeeline();

        logger.info( "Returning Beeline: {}", bl );

        return bl;
    }

    public Span startRootTracer( String spanName )
    {
        Beeline beeline = getBeeline();
        if ( beeline != null )
        {
            SpanContext context = ParentSpanContextualizer.getCurrentSpanContext();
            if ( context == null )
            {
                String traceId = RequestContextHelper.getContext( TRACE_ID );
                String parentId = RequestContextHelper.getContext( REQUEST_PARENT_SPAN );


                context = new SpanContext( traceId, parentId );
            }

            PropagationContext parentContext = new PropagationContext( context.getTraceId(), context.getParentSpanId(), null, null );
            Span rootSpan = beeline.getSpanBuilderFactory()
                                   .createBuilder()
                                   .setParentContext( parentContext )
                                   .setSpanName( spanName )
                                   .setServiceName( "indy" )
                                   .build();

            rootSpan = beeline.getTracer().startTrace( rootSpan );

            ParentSpanContextualizer.setCurrentSpanContext( new SpanContext( rootSpan ) );

            logger.debug( "Started root span with trace ID: {}", rootSpan.getTraceId() );
            return rootSpan;
        }

        return null;
    }

    public Span startChildSpan( String spanName )
    {
        Beeline beeline = getBeeline();
        if ( beeline != null )
        {
            Span span = beeline.startChildSpan( spanName );
            if ( span.isNoop() )
            {
                return startRootTracer( spanName );
            }

            logger.debug( "Child span: {} (is no-op? {}, parent: {})", span.getSpanName(),
                          span == null || span.isNoop(), span.getParentSpanId() );

            return span;
        }

        return null;
    }

    public void addFields( Span span )
    {
        ThreadContext ctx = ThreadContext.getContext( false );
        if ( ctx != null )
        {
            configuration.getFieldSet().forEach( field->{
                Object value = getContext( field );
                if ( value != null )
                {
                    span.addField( field, value );
                }
            });

            Map<String, Double> cumulativeTimings = (Map<String, Double>) ctx.get( CUMULATIVE_TIMINGS );
            if ( cumulativeTimings != null )
            {
                cumulativeTimings.forEach(
                        ( k, v ) -> span.addField( CUMULATIVE_TIMINGS + "." + k, v ) );
            }

            Map<String, Integer> cumulativeCounts = (Map<String, Integer>) ctx.get( CUMULATIVE_COUNTS );
            if ( cumulativeCounts != null )
            {
                cumulativeCounts.forEach(
                        ( k, v ) -> span.addField( CUMULATIVE_COUNTS + "." + k, v ) );
            }
        }

    }
}
