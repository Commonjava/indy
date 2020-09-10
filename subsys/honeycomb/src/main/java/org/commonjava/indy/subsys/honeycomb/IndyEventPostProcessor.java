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

import com.codahale.metrics.Meter;
import io.honeycomb.libhoney.EventPostProcessor;
import io.honeycomb.libhoney.eventdata.EventData;
import org.commonjava.indy.metrics.IndyMetricsManager;
import org.commonjava.indy.metrics.conf.IndyMetricsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import static org.commonjava.indy.metrics.IndyMetricsConstants.METER;
import static org.commonjava.indy.metrics.IndyMetricsConstants.getDefaultName;
import static org.commonjava.indy.metrics.IndyMetricsConstants.getName;

@ApplicationScoped
public class IndyEventPostProcessor implements EventPostProcessor
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private IndyMetricsManager metricsManager;

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
