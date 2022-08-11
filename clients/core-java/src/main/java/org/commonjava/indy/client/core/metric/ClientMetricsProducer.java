/**
 * Copyright (C) 2020 Red Hat, Inc.
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

import org.commonjava.cdi.util.weft.config.DefaultWeftConfig;
import org.commonjava.cdi.util.weft.config.WeftConfig;
import org.commonjava.o11yphant.metrics.TrafficClassifier;
import org.commonjava.o11yphant.metrics.conf.DefaultMetricsConfig;
import org.commonjava.o11yphant.metrics.conf.MetricsConfig;
import org.commonjava.o11yphant.metrics.sli.GoldenSignalsMetricSet;
import org.commonjava.o11yphant.metrics.system.StoragePathProvider;

import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Produces;

/**
 * This producer is used to provide the missing CDI deps for indy client metrics sets. Now all
 * produces provided alternative ones, because it will break indy metrics providers. In future the
 * indy-client libs will be extracted to a single lib, so then we will set these providers as default.
 */
public class ClientMetricsProducer
{
    @Produces
    @Alternative
    public TrafficClassifier getClientTrafficClassifier()
    {
        return new ClientTrafficClassifier();
    }

    @Produces
    @Alternative
    public GoldenSignalsMetricSet getClientMetricSet()
    {
        return new ClientGoldenSignalsMetricSet();
    }

    @Produces
    @Alternative
    public MetricsConfig getMetricsConfig()
    {
        return new DefaultMetricsConfig();
    }

    @Produces
    @Alternative
    public WeftConfig getWeftConfig()
    {
        return new DefaultWeftConfig();
    }

    @Produces
    @Alternative
    public StoragePathProvider getStoragePathProvider()
    {
        return () -> null;
    }
}
