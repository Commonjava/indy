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
