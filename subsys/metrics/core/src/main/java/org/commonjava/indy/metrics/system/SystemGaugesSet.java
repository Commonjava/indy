/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.metrics.system;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A set of gauges for system gauges, including stats on cpu, process, physical mem, swap mem.
 */
public class SystemGaugesSet
        implements MetricSet
{
    private final Logger logger = LoggerFactory.getLogger( this.getClass() );

    private final OperatingSystemMXBean operatingSystemMXBean;

    public SystemGaugesSet()
    {
        this( ManagementFactory.getOperatingSystemMXBean() );
    }

    public SystemGaugesSet( OperatingSystemMXBean mxBean )
    {
        this.operatingSystemMXBean = mxBean;
    }

    public Map<String, Metric> getMetrics()
    {
        if ( !( operatingSystemMXBean instanceof com.sun.management.OperatingSystemMXBean ) )
        {
            return Collections.emptyMap();
        }

        final com.sun.management.OperatingSystemMXBean osMxBean =
                (com.sun.management.OperatingSystemMXBean) operatingSystemMXBean;

        final Map<String, Metric> gauges = new HashMap<>();

        try
        {
            gauges.put( "process.cpu.load", (Gauge<Double>) osMxBean::getProcessCpuLoad );
            gauges.put( "system.cpu.load", (Gauge<Double>) osMxBean::getSystemCpuLoad );
            gauges.put( "system.load.avg", (Gauge<Double>) osMxBean::getSystemLoadAverage );
            gauges.put( "process.cpu.time.ms", (Gauge<Long>) () -> osMxBean.getProcessCpuTime() * 1000 );

            gauges.put( "mem.total.swap", (Gauge<Long>) osMxBean::getTotalSwapSpaceSize );
            gauges.put( "mem.total.physical", (Gauge<Long>) osMxBean::getTotalPhysicalMemorySize );
            gauges.put( "mem.free.physical", (Gauge<Long>) osMxBean::getFreePhysicalMemorySize );
            gauges.put( "mem.free.swap", (Gauge<Long>) osMxBean::getFreeSwapSpaceSize );
        }
        catch ( Throwable e )
        {
            logger.warn( "Can not get system level metrics. Reason: {}", e.getMessage() );
        }

        return Collections.unmodifiableMap( gauges );

    }

}
