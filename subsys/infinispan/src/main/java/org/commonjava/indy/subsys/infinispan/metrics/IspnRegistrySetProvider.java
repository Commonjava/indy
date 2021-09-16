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
package org.commonjava.indy.subsys.infinispan.metrics;

import org.commonjava.o11yphant.metrics.api.MetricSet;
import org.commonjava.o11yphant.metrics.MetricSetProvider;
import org.commonjava.indy.subsys.metrics.conf.IndyMetricsConfig;
import org.commonjava.indy.subsys.infinispan.CacheProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.commonjava.indy.subsys.infinispan.metrics.IspnCheckRegistrySet.INDY_METRIC_ISPN;
import static org.commonjava.o11yphant.metrics.util.NameUtils.name;

@ApplicationScoped
public class IspnRegistrySetProvider
                implements MetricSetProvider
{
    @Inject
    private IndyMetricsConfig metricsConfig;

    @Inject
    private CacheProducer cacheProducer;

    @Inject
    private Instance<IspnCacheRegistry> cacheRegistrySet;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private IspnCheckRegistrySet metricSet;

    @Override
    public synchronized MetricSet getMetricSet()
    {
        if ( metricSet == null )
        {
            logger.info( "Adding ISPN checks" );
            String gauges = metricsConfig.getIspnGauges();
            List<String> list = null;
            if ( gauges != null )
            {
                list = Arrays.asList( gauges.trim().split( "\\s*,\\s*" ) );
            }

            for ( IspnCacheRegistry cacheRegistry : cacheRegistrySet )
            {
                Set<String> caches = cacheRegistry.getCacheNames();
                if ( caches != null )
                {
                    caches.forEach( ( n ) -> cacheProducer.getCacheManager().getCache( n ) );
                }
            }

            this.metricSet = new IspnCheckRegistrySet( cacheProducer.getCacheManager(), list );
        }

        return metricSet;
    }

    @Override
    public String getName()
    {
        return name( metricsConfig.getNodePrefix(), INDY_METRIC_ISPN );
    }

    @Override
    public boolean isEnabled()
    {
        return metricsConfig.isIspnMetricsEnabled();
    }

    @Override
    public void reset()
    {
        metricSet.reset();
    }
}
