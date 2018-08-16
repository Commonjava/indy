package org.commonjava.indy.metrics;

import com.codahale.metrics.MetricRegistry;

public interface MetricSetProvider
{
    void registerMetricSet( MetricRegistry registry );
}
