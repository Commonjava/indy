package org.commonjava.indy.metrics.sigar;

import com.codahale.metrics.MetricRegistry;

/**
 * Created by xiabai on 6/7/17.
 */
public class IndySystemInstrumentation
{
    public static void init( MetricRegistry metricRegistry )
    {
        metricRegistry.register( "system", new OSMetricsSet() );
        metricRegistry.register( "system", new FilesystemMetricsSet() );
        metricRegistry.register( "system", new NetworkMetricsSet() );
    }
}
