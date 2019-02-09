package org.commonjava.indy.metrics.healthcheck.impl;

import org.commonjava.indy.metrics.healthcheck.IndyComponentHC;
import org.commonjava.indy.metrics.healthcheck.IndyCompoundHealthCheck;
import org.commonjava.indy.metrics.healthcheck.IndyHealthCheck;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.atomic.AtomicInteger;

@Named
public class SummaryHealthCheck
        extends IndyHealthCheck
{
    enum SummaryRating
    {
        green, yellow, red;
    }

    private static final String RATING = "rating";

    private static final String UNHEALTHY_COUNT = "unhealthy-count";

    @Inject
    private Instance<IndyComponentHC> looseComponents;

    @Inject
    private Instance<IndyCompoundHealthCheck> looseCompounds;

    @Override
    protected Result check()
            throws Exception
    {
        AtomicInteger count = new AtomicInteger( 0 );
        looseComponents.forEach( check->{
            if ( !check.execute().isHealthy() )
                count.incrementAndGet();
        } );

        looseCompounds.forEach( lc->{
            lc.getHealthChecks().forEach( ( k, check ) -> {
                if ( !check.execute().isHealthy() )
                {
                    count.incrementAndGet();
                }
            } );
        } );

        ResultBuilder rb = Result.builder();
        if ( count.get() > 3 )
        {
            rb.unhealthy().withDetail( RATING, SummaryRating.red );
        }
        else if ( count.get() > 0 )
        {
            rb.unhealthy().withDetail( RATING, SummaryRating.yellow );
        }
        else
        {
            rb.healthy();
        }

        rb.withDetail( UNHEALTHY_COUNT, count.get() );

        return rb.build();
    }
}
