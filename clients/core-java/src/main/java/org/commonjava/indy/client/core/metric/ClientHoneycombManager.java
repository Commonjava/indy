/**
 * Copyright (C) 2020 Red Hat, Inc. (nos-devel@redhat.com)
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
package org.commonjava.indy.client.core.metric;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import io.honeycomb.beeline.tracing.Span;
import io.honeycomb.libhoney.EventPostProcessor;
import org.commonjava.cdi.util.weft.ThreadContext;
import org.commonjava.o11yphant.honeycomb.CustomTraceIdProvider;
import org.commonjava.o11yphant.honeycomb.HoneycombManager;
import org.commonjava.o11yphant.honeycomb.RootSpanFields;
import org.commonjava.o11yphant.honeycomb.SpanContext;
import org.commonjava.o11yphant.honeycomb.config.HoneycombConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.commonjava.o11yphant.metrics.MetricsConstants.AVERAGE_TIME_MS;
import static org.commonjava.o11yphant.metrics.MetricsConstants.CUMULATIVE_COUNT;
import static org.commonjava.o11yphant.metrics.MetricsConstants.CUMULATIVE_TIMINGS;
import static org.commonjava.o11yphant.metrics.MetricsConstants.MAX_TIME_MS;
import static org.commonjava.o11yphant.metrics.RequestContextConstants.REQUEST_PHASE_START;
import static org.commonjava.o11yphant.metrics.RequestContextHelper.getContext;
import static org.commonjava.o11yphant.metrics.util.NameUtils.name;

public class ClientHoneycombManager
                extends HoneycombManager
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public ClientHoneycombManager( HoneycombConfiguration honeycombConfiguration,
                                   ClientTraceSampler clientTraceSampler )
    {
        super( honeycombConfiguration, clientTraceSampler );
    }

    public void addFields( Span span )
    {
        if ( beeline != null )
        {
            ThreadContext ctx = ThreadContext.getContext( false );
            if ( ctx != null )
            {
                configuration.getFieldSet().forEach( field -> {
                    Object value = getContext( field );
                    if ( value != null )
                    {
                        span.addField( field, value );
                    }
                } );

                Map<String, Double> cumulativeTimings = (Map<String, Double>) ctx.get( CUMULATIVE_TIMINGS );
                if ( cumulativeTimings != null )
                {
                    cumulativeTimings.forEach( ( k, v ) -> span.addField( CUMULATIVE_TIMINGS + "." + k, v ) );
                }

                Map<String, Integer> cumulativeCounts = (Map<String, Integer>) ctx.get( CUMULATIVE_COUNT );
                if ( cumulativeCounts != null )
                {
                    cumulativeCounts.forEach( ( k, v ) -> span.addField( CUMULATIVE_COUNT + "." + k, v ) );
                }
            }
            addRootSpanFields( span );
        }
    }
}
