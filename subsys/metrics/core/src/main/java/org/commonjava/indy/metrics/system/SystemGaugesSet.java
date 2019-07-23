/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A set of gauges for system gauges, including stats on cpu, process, physical mem, swap mem.
 */
@ApplicationScoped
@Named
public class SystemGaugesSet
        implements MetricSet
{
    private final Logger logger = LoggerFactory.getLogger( this.getClass() );

    @Inject
    private StoragePathProvider storagePathProvider;

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
            gauges.put( "process.cpu.time.ms", (Gauge<Long>) () -> osMxBean.getProcessCpuTime() / 1000 );

            gauges.put( "mem.total.swap", (Gauge<Long>) osMxBean::getTotalSwapSpaceSize );
            gauges.put( "mem.total.physical", (Gauge<Long>) osMxBean::getTotalPhysicalMemorySize );
            gauges.put( "mem.free.physical", (Gauge<Long>) osMxBean::getFreePhysicalMemorySize );
            gauges.put( "mem.free.swap", (Gauge<Long>) osMxBean::getFreeSwapSpaceSize );
        }
        catch ( Throwable e )
        {
            logger.warn( "Cannot get system level metrics. Reason: {}", e.getMessage() );
        }

        try
        {
            final File storePath = getIndyStorageDir();
            if ( storePath.exists() && storePath.isDirectory() )
            {
                gauges.put( "store.indy.total", (Gauge<Long>) storePath::getTotalSpace );
                gauges.put( "store.indy.usable", (Gauge<Long>) storePath::getUsableSpace );
            }
            else
            {
                logger.warn( "Cannot trace indy storage usage because storage path {} not defined.",
                             storePath.getCanonicalPath() );
            }
        }
        catch ( Throwable e )
        {
            logger.warn( "Cannot trace indy storage usage. Reason: {}", e.getMessage() );
        }

        return Collections.unmodifiableMap( gauges );

    }

    private File getIndyStorageDir()
    {
        if ( storagePathProvider != null && storagePathProvider.getStoragePath() != null )
        {
            return storagePathProvider.getStoragePath();
        }

        // if indy config for storage path not defined, we use docker defined one.
        final String DEFAULT_STORAGE_DIR = "/var/lib/indy/storage";
        return new File( DEFAULT_STORAGE_DIR );
    }

}
