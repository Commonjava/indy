package org.commonjava.indy.filer.def;

import org.commonjava.indy.metrics.IndyMetricsManager;
import org.commonjava.maven.galley.spi.metrics.TimingProvider;

public class IndyTimingProvider
        implements TimingProvider
{
    private final String name;

    private final IndyMetricsManager metricsManager;

    public IndyTimingProvider( final String name, final IndyMetricsManager metricsManager )
    {
        this.name = name;
        this.metricsManager = metricsManager;
    }

    @Override
    public void start( final String name )
    {
        metricsManager.startTimer( name );
    }

    @Override
    public long stop()
    {
        return metricsManager.stopTimer( name );
    }
}
