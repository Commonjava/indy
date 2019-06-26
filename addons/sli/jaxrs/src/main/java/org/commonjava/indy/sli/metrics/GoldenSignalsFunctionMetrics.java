package org.commonjava.indy.sli.metrics;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.Timer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class GoldenSignalsFunctionMetrics
{
    private String name;

    private Timer latency = new Timer();

    private Meter errors = new Meter();

    private Meter throughput = new Meter();

    GoldenSignalsFunctionMetrics( String name )
    {
        this.name = name;
    }

    Map<String, Metric> getMetrics()
    {
        Map<String, Metric> metrics = new HashMap<>();
        metrics.put( name + ".latency", latency );
        metrics.put( name + ".errors", errors );
        metrics.put( name + ".throughput", throughput );

        return metrics;
    }

    public GoldenSignalsFunctionMetrics latency( long duration )
    {
        latency.update( duration, NANOSECONDS );
        return this;
    }

    public GoldenSignalsFunctionMetrics error()
    {
        errors.mark();
        return this;
    }

    public GoldenSignalsFunctionMetrics call()
    {
        throughput.mark();
        return this;
    }
}
