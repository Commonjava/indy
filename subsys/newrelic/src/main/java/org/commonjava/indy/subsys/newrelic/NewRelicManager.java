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
package org.commonjava.indy.subsys.newrelic;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.SimpleMetricBatchSender;
import com.newrelic.telemetry.SimpleSpanBatchSender;
import com.newrelic.telemetry.TelemetryClient;
import com.newrelic.telemetry.metrics.MetricBatchSender;
import com.newrelic.telemetry.spans.Span;
import com.newrelic.telemetry.spans.SpanBatch;
import com.newrelic.telemetry.spans.SpanBatchSender;
import org.commonjava.cdi.util.weft.ThreadContext;
import org.commonjava.indy.subsys.newrelic.config.NewRelicConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.commonjava.indy.metrics.RequestContextHelper.PREFERRED_ID;
import static org.commonjava.indy.metrics.RequestContextHelper.getContext;

@ApplicationScoped
public class NewRelicManager
{
    private static final String OPEN_SPANS = "newrelic.open-spans";

    private static final String CLOSED_SPANS = "newrelic.closed-spans";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private NewRelicConfiguration configuration;

    private TelemetryClient telemetryClient;

    public NewRelicManager()
    {
    }

    @PostConstruct
    public void init()
    {
        if ( !configuration.isEnabled() )
        {
            logger.info( "New Relic is not enabled" );
            return;
        }
        String key = configuration.getInsertKey();
        if ( isNotBlank( key ) )
        {
            logger.info( "Init New Relic manager" );

            MetricBatchSender metricSender =
                    SimpleMetricBatchSender.builder( key, Duration.of( 10, ChronoUnit.SECONDS ) ).build();

            SpanBatchSender spanSender = SimpleSpanBatchSender.builder( key ).build();
            telemetryClient = new TelemetryClient( metricSender, spanSender );
        }
    }

    @PreDestroy
    public void stop()
    {
        if ( telemetryClient != null )
        {
            telemetryClient.shutdown();
        }
    }

    public Span startRootTracer( String spanName )
    {
        if ( telemetryClient != null )
        {
            Span span = Span.builder( UUID.randomUUID().toString() ).name( spanName ).serviceName( "indy" ).build();

            ThreadContext ctx = ThreadContext.getContext( true );
            ctx.put( OPEN_SPANS, new ArrayDeque<>( Collections.singleton( new SpanEntry( span ) ) ) );

            return span;
        }

        return null;
    }

    public Span startChildSpan( String spanName )
    {
        ThreadContext ctx = ThreadContext.getContext( false );
        if ( telemetryClient != null && ctx != null && ctx.containsKey( OPEN_SPANS ) )
        {
            Deque<SpanEntry> spans = (Deque<SpanEntry>) ctx.get( OPEN_SPANS );
            SpanEntry parent = spans.getLast();

            Span.SpanBuilder builder = Span.builder( UUID.randomUUID().toString() ).name( spanName );
            if ( parent != null )
            {
               builder.serviceName( parent.span.getServiceName() ).parentId( parent.span.getId() );
            }

            Span span = builder.build();

            spans.addLast( new SpanEntry( span ) );
            ctx.put( OPEN_SPANS, spans );

            return span;
        }

        return null;
    }

    public void closeSpans( final Attributes attrs )
    {
        ThreadContext ctx = ThreadContext.getContext( false );
        if ( telemetryClient != null && ctx != null )
        {
            Stream.of( configuration.getFields()).forEach( field->{
                Object value = getContext( field );
                if ( value != null )
                {
                    logger.trace( "NEW RELIC FIELD: {} = {}", field, value );
                    if ( value instanceof Number)
                    {
                        attrs.put( field, (Number) value );
                    }
                    else
                    {
                        attrs.put( field, String.valueOf( value ) );
                    }
                }
            });

            Set<Span> closed = (Set<Span>) ctx.remove( CLOSED_SPANS );
            if ( closed == null )
            {
                closed = new HashSet<>();
            }

            Deque<SpanEntry> open = (Deque<SpanEntry>) ctx.remove( OPEN_SPANS );
            if ( open != null )
            {
                Set<Span> c = closed;
                open.forEach( se -> c.add( Span.builder( se.span.getId() )
                                            .name( se.span.getName() )
                                            .serviceName( se.span.getServiceName() )
                                            .durationMs( System.currentTimeMillis() - se.span.getTimestamp() )
                                            .timestamp( se.span.getTimestamp() )
                                            .parentId( se.span.getParentId() ).build() ) );
            }

            if ( !closed.isEmpty() )
            {
                String traceId = (String) ctx.get( PREFERRED_ID );
                if ( traceId == null )
                {
                    traceId = UUID.randomUUID().toString();
                }

                telemetryClient.sendBatch( new SpanBatch( closed, attrs, traceId ) );
            }
        }
    }

    public void close( final Span span )
    {
        ThreadContext ctx = ThreadContext.getContext( false );
        if ( telemetryClient != null && ctx != null )
        {
            Set<Span> closed = (Set<Span>) ctx.remove( CLOSED_SPANS );
            if ( closed == null )
            {
                closed = new HashSet<>();
                ctx.put( CLOSED_SPANS, closed );
            }

            Deque<SpanEntry> open = (Deque<SpanEntry>) ctx.get( OPEN_SPANS );
            if ( open != null )
            {
                SpanEntry lookup = new SpanEntry( span );
                if ( open.remove( lookup ))
                {
                    closed.add( Span.builder( span.getId() )
                                    .name( span.getName() )
                                    .serviceName( span.getServiceName() )
                                    .durationMs( System.currentTimeMillis() - span.getTimestamp() )
                                    .timestamp( span.getTimestamp() )
                                    .parentId( span.getParentId() )
                                    .build() );
                }
            }
        }
    }

    public Span getActiveSpan()
    {
        ThreadContext ctx = ThreadContext.getContext( false );
        if ( telemetryClient != null && ctx != null )
        {
            LinkedList<SpanEntry> open = (LinkedList<SpanEntry>) ctx.get( OPEN_SPANS );
            if ( open != null && !open.isEmpty() )
            {
                return open.getLast().span;
            }
        }

        return null;
    }

    private static final class SpanEntry
    {
        private Span span;

        SpanEntry(Span span)
        {
            this.span = span;
        }

        @Override
        public boolean equals( final Object o )
        {
            if ( this == o )
            {
                return true;
            }
            if ( !( o instanceof SpanEntry ) )
            {
                return false;
            }
            final SpanEntry spanEntry = (SpanEntry) o;
            return span.getId().equals( spanEntry.span.getId() );
        }

        @Override
        public int hashCode()
        {
            return Objects.hash( span.getId() );
        }
    }

}
