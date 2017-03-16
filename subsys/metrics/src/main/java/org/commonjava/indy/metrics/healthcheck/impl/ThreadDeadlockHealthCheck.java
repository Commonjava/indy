package org.commonjava.indy.metrics.healthcheck.impl;

import com.codahale.metrics.jvm.ThreadDeadlockDetector;
import org.commonjava.indy.metrics.healthcheck.IndyHealthCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Created by xiabai on 3/10/17.
 * implements IndyMetricsHealthCheck
 */
public class ThreadDeadlockHealthCheck
                extends IndyHealthCheck
{
    private static final Logger logger = LoggerFactory.getLogger( ThreadDeadlockHealthCheck.class );

    @Override
    public String getName()
    {
        return "ThreadDeadlock";
    }

    private final ThreadDeadlockDetector detector;

    public ThreadDeadlockHealthCheck()
    {
        this( new ThreadDeadlockDetector() );
    }

    public ThreadDeadlockHealthCheck( ThreadDeadlockDetector detector )
    {
        this.detector = detector;
    }

    @Override
    public Result check() throws Exception
    {
        final Set<String> threads = detector.getDeadlockedThreads();
        if ( threads.isEmpty() )
        {
            return Result.healthy();
        }
        return Result.unhealthy( threads.toString() );
    }
}
