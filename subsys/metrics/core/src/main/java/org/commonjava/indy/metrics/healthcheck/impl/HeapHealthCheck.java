package org.commonjava.indy.metrics.healthcheck.impl;

import org.commonjava.indy.metrics.healthcheck.IndyComponentHC;
import org.commonjava.indy.metrics.healthcheck.IndyHealthCheck;

import javax.inject.Named;

@Named
public class HeapHealthCheck
        extends IndyComponentHC
{
    private static final double GB = Math.pow(1024, 3);

    private static final String FREE_GB = "free-gb";

    private static final String USED_GB = "used-gb";

    private static final String TOTAL_GB = "total-gb";

    private static final String MAX_GB = "max-gb";

    private static final String CURRENT_LOAD = "current-load-pct";

    private static final float HEALTHY_LOAD_MAX = 0.9f;

    @Override
    protected Result check()
            throws Exception
    {
        Runtime runtime = Runtime.getRuntime();
        ResultBuilder builder = Result.builder();

        double free = runtime.freeMemory();
        double total = runtime.totalMemory();
        double used = total-free;

        double max = runtime.maxMemory();
        double load = 100 * (used / max);

        if ( load > HEALTHY_LOAD_MAX )
        {
            builder.unhealthy();
        }
        else
        {
            builder.healthy();
        }

        builder.withDetail( FREE_GB, free / GB )
               .withDetail( USED_GB, used / GB )
               .withDetail( MAX_GB, max / GB )
               .withDetail( TOTAL_GB, total / GB ).withDetail( CURRENT_LOAD, load );

        return builder.build();
    }
}
