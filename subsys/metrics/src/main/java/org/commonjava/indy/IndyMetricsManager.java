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
package org.commonjava.indy;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.health.HealthCheck;
import org.commonjava.indy.measure.annotation.MetricNamed;
import org.commonjava.indy.metrics.conf.IndyMetricsConfig;
import org.commonjava.indy.metrics.conf.annotation.IndyMetricsNamed;
import org.commonjava.indy.metrics.healthcheck.IndyHealthCheck;
import org.commonjava.indy.metrics.healthcheck.IndyHealthCheckRegistrySet;
import org.commonjava.indy.metrics.jvm.IndyJVMInstrumentation;
import org.commonjava.indy.metrics.reporter.ReporterIntializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

/**
 * Created by xiabai on 2/27/17.
 */
@ApplicationScoped
public class IndyMetricsManager
{

    private static final Logger logger = LoggerFactory.getLogger( IndyMetricsManager.class );

    @Inject
    MetricRegistry metricRegistry;

    @Inject
    @Any
    Instance<IndyHealthCheck> indyMetricsHealthChecks;

    @Inject
    ReporterIntializer reporter;

    @Inject
    @IndyMetricsNamed
    IndyMetricsConfig config;

    @PostConstruct
    public void initMetric()
    {

        if ( !config.isMetricsEnabled() )
            return;
        IndyJVMInstrumentation.init( metricRegistry );
        IndyHealthCheckRegistrySet healthCheckRegistrySet = new IndyHealthCheckRegistrySet();

        indyMetricsHealthChecks.forEach( indyHealthCheck ->
                                         {
                                             healthCheckRegistrySet.register( indyHealthCheck.getName(),
                                                                              (HealthCheck) indyHealthCheck );
                                         } );
        try
        {
            metricRegistry.register( healthCheckRegistrySet.getName(), healthCheckRegistrySet );
            reporter.initReporter( metricRegistry );
        }
        catch ( Exception e )
        {
            logger.error( e.getMessage() );
            throw new RuntimeException( e );
        }
    }

    public Timer getTimer( MetricNamed named )
    {
        logger.trace( "call in IndyMetricsManager.getTimer" );
        return this.metricRegistry.timer( named.name() );
    }

    public Meter getMeter( MetricNamed named )
    {
        logger.trace( "call in IndyMetricsManager.getMeter" );
        return metricRegistry.meter( named.name() );
    }

}
