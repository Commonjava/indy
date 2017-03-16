package org.commonjava.indy.metrics.jvm;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jvm.*;

import java.lang.management.ManagementFactory;

/**
 * Created by xiabai on 3/10/17.
 */
public class IndyJVMInstrumentation
{
    private static final String INDY_METRIC_JVM_MEMORY = "jvm.memory";

    private static final String INDY_METRIC_JVM_GARBAGE = "jvm.garbage";

    private static final String INDY_METRIC_JVM_THREADS = "jvm.threads";

    private static final String INDY_METRIC_JVM_FILES = "jvm.files";

    private static final String INDY_METRIC_JVM_BUFFERS = "jvm.buffers";

    private static final String INDY_METRIC_JVM_CLASSLOADING = "jvm.classloading";

    public static void init( MetricRegistry metricRegistry )
    {
        metricRegistry.register( INDY_METRIC_JVM_MEMORY, new MemoryUsageGaugeSet() );
        metricRegistry.register( INDY_METRIC_JVM_GARBAGE, new GarbageCollectorMetricSet() );
        metricRegistry.register( INDY_METRIC_JVM_THREADS, new ThreadStatesGaugeSet() );
        metricRegistry.register( INDY_METRIC_JVM_FILES, new FileDescriptorRatioGauge() );
        metricRegistry.register( INDY_METRIC_JVM_CLASSLOADING, new ClassLoadingGaugeSet() );
        metricRegistry.register( INDY_METRIC_JVM_BUFFERS,
                                 new BufferPoolMetricSet( ManagementFactory.getPlatformMBeanServer() ) );
    }
}
