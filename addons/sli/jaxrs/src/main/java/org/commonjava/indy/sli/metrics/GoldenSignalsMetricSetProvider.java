package org.commonjava.indy.sli.metrics;

import com.codahale.metrics.MetricRegistry;
import org.commonjava.indy.metrics.MetricSetProvider;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class GoldenSignalsMetricSetProvider
        implements MetricSetProvider
{
    @Inject
    private GoldenSignalsMetricSet metricSet;

    @Override
    public void registerMetricSet( final MetricRegistry registry )
    {
        registry.register( "sli.golden", metricSet );
    }
}
