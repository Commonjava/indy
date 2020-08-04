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
package org.commonjava.indy.sli.metrics;

import org.commonjava.o11yphant.metrics.api.healthcheck.HealthCheck;
import org.commonjava.o11yphant.metrics.api.Meter;
import org.commonjava.o11yphant.metrics.api.Metric;
import org.commonjava.o11yphant.metrics.api.Timer;
import org.commonjava.o11yphant.metrics.healthcheck.impl.HealthCheckResult;
import org.commonjava.o11yphant.metrics.impl.O11Meter;
import org.commonjava.o11yphant.metrics.impl.O11Timer;

import java.util.HashMap;
import java.util.Map;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class GoldenSignalsFunctionMetrics
{
    private final String name;

    private final Meter load;

    private final Timer latency;

    private final Meter errors;

    private final Meter throughput;

    GoldenSignalsFunctionMetrics( String name )
    {
        this.name = name;
        this.load = new O11Meter();
        this.errors = new O11Meter(  );
        this.throughput = new O11Meter(  );
        this.latency = new O11Timer(  );
    }

    Map<String, Metric> getMetrics()
    {
        Map<String, Metric> metrics = new HashMap<>();
        metrics.put( name + ".latency", latency );
        metrics.put( name + ".errors", errors );
        metrics.put( name + ".throughput", throughput );
        metrics.put( name + ".load", load );

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

    public HealthCheck getHealthCheck()
    {
        return new GSFunctionHealthCheck();
    }

    public GoldenSignalsFunctionMetrics started()
    {
        load.mark();
        return this;
    }

    final class GSFunctionHealthCheck
                    implements HealthCheck
    {
        @Override
        public Result check()
                throws Exception
        {
            // FIXME: We need need to incorporate the SLO targets to determine whether health / unhealthy.
            return new HealthCheckResult(true)
                                    .withDetail( "latency", latency.getSnapshot().get99thPercentile() )
                                    .withDetail( "errors", errors.getOneMinuteRate() )
                                    .withDetail( "throughput", throughput.getOneMinuteRate() )
                                    .withDetail( "load", load.getOneMinuteRate() );
        }
    }
}
