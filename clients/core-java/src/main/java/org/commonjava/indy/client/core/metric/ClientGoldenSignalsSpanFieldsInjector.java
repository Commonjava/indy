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
package org.commonjava.indy.client.core.metric;

import org.commonjava.o11yphant.metrics.api.Gauge;
import org.commonjava.o11yphant.metrics.api.Meter;
import org.commonjava.o11yphant.metrics.api.Metric;
import org.commonjava.o11yphant.metrics.api.Timer;
import org.commonjava.o11yphant.trace.spi.SpanFieldsInjector;
import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;

import java.util.HashMap;
import java.util.Map;

public class ClientGoldenSignalsSpanFieldsInjector
                implements SpanFieldsInjector
{
    private ClientGoldenSignalsMetricSet goldenSignalsMetricSet;

    public ClientGoldenSignalsSpanFieldsInjector( ClientGoldenSignalsMetricSet goldenSignalsMetricSet )
    {
        this.goldenSignalsMetricSet = goldenSignalsMetricSet;
    }

    @Override
    public void decorateSpanAtClose( SpanAdapter span )
    {
        final Map<String, Metric> metrics = goldenSignalsMetricSet.getMetrics();
        metrics.forEach( ( k, v ) -> {
            Object value = null;
            if ( v instanceof Gauge )
            {
                value = ( (Gauge) v ).getValue();
                span.addField( "golden." + k, value );
            }
            else if ( v instanceof Timer )
            {
                value = ( (Timer) v ).getSnapshot().get95thPercentile();
                span.addField( "golden." + k + ".p95", value );
            }
            else if ( v instanceof Meter )
            {
                value = ( (Meter) v ).getOneMinuteRate();
                span.addField( "golden." + k + ".m1", value );
            }
        } );
    }
}
