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
package org.commonjava.indy.promote.metrics;

import com.codahale.metrics.Gauge;
import org.commonjava.indy.metrics.IndyMetricsManager;
import org.commonjava.indy.promote.model.PathsPromoteResult;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
public class PathGauges
{
    @Inject
    private IndyMetricsManager metricsManager;

    private AtomicInteger total = new AtomicInteger();

    private AtomicInteger completed = new AtomicInteger();

    private AtomicInteger skipped = new AtomicInteger();

    public PathGauges()
    {
    }

    @PostConstruct
    public void init()
    {
        registerPathPromotionGauges( metricsManager );
    }

    private void registerPathPromotionGauges( IndyMetricsManager metricsManager )
    {
        Map<String, Gauge<Integer>> gauges = new HashMap<>();
        gauges.put( "total", () -> getTotal() );
        gauges.put( "completed", () -> getCompleted() );
        gauges.put( "skipped", () -> getSkipped() );
        metricsManager.addGauges( this.getClass(), "last", gauges );
    }

    public int getTotal()
    {
        return total.get();
    }

    public int getCompleted()
    {
        return completed.get();
    }

    public int getSkipped()
    {
        return skipped.get();
    }

    public void setTotal( int total )
    {
        this.total.set( total );
    }

    public void setSkipped( int size )
    {
        skipped.set( size );
    }

    public void setCompleted( int size )
    {
        completed.set( size );
    }

    public void update( int total, PathsPromoteResult result )
    {
        setTotal( total );
        setCompleted( result.getCompletedPaths().size() );
        setSkipped( result.getSkippedPaths().size() );
    }
}
