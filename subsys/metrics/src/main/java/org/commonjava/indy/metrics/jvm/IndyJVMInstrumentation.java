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
