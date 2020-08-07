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

import io.honeycomb.libhoney.EventPostProcessor;
import io.honeycomb.libhoney.eventdata.EventData;
import org.commonjava.o11yphant.metrics.api.Meter;
import org.commonjava.o11yphant.metrics.DefaultMetricsManager;
import org.commonjava.indy.subsys.metrics.conf.IndyMetricsConfig;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import static org.commonjava.o11yphant.metrics.MetricsConstants.METER;
import static org.commonjava.o11yphant.metrics.util.NameUtils.getDefaultName;
import static org.commonjava.o11yphant.metrics.util.NameUtils.getName;

@ApplicationScoped
public class IndyEventPostProcessor implements EventPostProcessor
{
    @Inject
    private DefaultMetricsManager metricsManager;

    @Inject
    private IndyMetricsConfig metricsConfig;

    private final static String TRANSFER_HONEYCOMB_EVENT = "indy.transferred.honeycomb.event";

    @Override
    public void process( EventData<?> eventData )
    {
        if ( metricsConfig != null && metricsManager != null )
        {
            String name = getName( metricsConfig.getNodePrefix(), TRANSFER_HONEYCOMB_EVENT,
                                   getDefaultName( IndyEventPostProcessor.class, "process" ), METER );
            Meter meter = metricsManager.getMeter( name );
            meter.mark();
        }
    }
}
