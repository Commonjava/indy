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
package org.commonjava.indy.metrics;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import org.commonjava.indy.metrics.conf.IndyMetricsConfig;
import org.commonjava.indy.metrics.healthcheck.IndyCompoundHealthCheck;
import org.commonjava.indy.metrics.healthcheck.IndyHealthCheck;
import org.commonjava.indy.metrics.reporter.ReporterIntializer;
import org.commonjava.maven.galley.config.TransportMetricConfig;
import org.commonjava.maven.galley.model.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static com.codahale.metrics.MetricRegistry.name;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.commonjava.indy.metrics.IndyMetricsConstants.EXCEPTION;
import static org.commonjava.indy.metrics.IndyMetricsConstants.METER;
import static org.commonjava.indy.metrics.IndyMetricsConstants.SKIP_METRIC;
import static org.commonjava.indy.metrics.IndyMetricsConstants.TIMER;
import static org.commonjava.indy.metrics.jvm.IndyJVMInstrumentation.registerJvmMetric;
import static org.commonjava.indy.model.core.StoreType.remote;
import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;

/**
 * Created by xiabai on 2/27/17.
 */
@ApplicationScoped
public class IndyMetricsManager
{

    private static final Logger logger = LoggerFactory.getLogger( IndyMetricsManager.class );

    @Inject
    private MetricRegistry metricRegistry;

    @Inject
    private HealthCheckRegistry healthCheckRegistry;

    @Inject
    private Instance<IndyHealthCheck> indyHealthChecks;

    @Inject
    private Instance<IndyCompoundHealthCheck> indyCompoundHealthChecks;

    @Inject
    ReporterIntializer reporter;

    @Inject
    private Instance<MetricSetProvider> metricSetProviderInstances;

    @Inject
    private IndyMetricsConfig config;

    private TransportMetricConfig transportMetricConfig;

    @Produces
    public TransportMetricConfig getTransportMetricConfig()
    {
        return transportMetricConfig;
    }

    @PostConstruct
    public void init()
    {
        if ( !config.isMetricsEnabled() )
        {
            logger.info( "Indy metrics subsystem not enabled" );
            return;
        }

        logger.info( "Init metrics subsystem..." );

        registerJvmMetric( config.getNodePrefix(), metricRegistry );

        // Health checks
        indyHealthChecks.forEach( hc -> {
            logger.info( "Registering health check: {}", hc.getName() );
            healthCheckRegistry.register( hc.getName(), hc );
        } );

        indyCompoundHealthChecks.forEach( cc-> {
            Map<String, HealthCheck> healthChecks = cc.getHealthChecks();
            logger.info( "Registering {} health checks from set: {}", healthChecks.size(), cc.getClass().getSimpleName() );
            healthChecks.forEach( (name,check)->{
                logger.info( "Registering health check: {}", name );
                healthCheckRegistry.register( name, check );
            } );
        } );

        metricSetProviderInstances.forEach( ( provider ) -> provider.registerMetricSet( metricRegistry ) );

        if ( config.isMeasureTransport() )
        {
            setUpTransportMetricConfig();
        }
    }

    public void startReporter() throws Exception
    {
        if ( !config.isMetricsEnabled() )
        {
            return;
        }
        logger.info( "Start metrics reporters" );
        reporter.initReporter( metricRegistry );
    }

    private void setUpTransportMetricConfig()
    {
        logger.info( "Adding transport metrics to registry: {}", metricRegistry );
        final String measureRepos = config.getMeasureTransportRepos();
        final List<String> list = new ArrayList<>();
        if ( isNotBlank( measureRepos ) )
        {
            String[] toks = measureRepos.split( "," );
            for ( String s : toks )
            {
                s = s.trim();
                if ( isNotBlank( s ) )
                {
                    if ( s.indexOf( ":" ) < 0 )
                    {
                        s = MAVEN_PKG_KEY + ":" + remote.singularEndpointName() + ":" + s; // use default
                    }
                    list.add( s );
                }
            }
        }
        transportMetricConfig = new TransportMetricConfig()
        {
            @Override
            public boolean isEnabled()
            {
                return true;
            }

            @Override
            public String getNodePrefix()
            {
                return config.getNodePrefix();
            }

            @Override
            public String getMetricUniqueName( Location location )
            {
                String locationName = location.getName();
                for ( String s : list )
                {
                    if ( s.equals( locationName ) )
                    {
                        return normalizeName( s );
                    }

                    if ( s.endsWith( "*" ) ) // handle wildcard
                    {
                        String prefix = s.substring( 0, s.length() - 1 );
                        if ( locationName.startsWith( prefix ) )
                        {
                            return normalizeName( prefix );
                        }
                    }
                }
                return null;
            }
        };
    }

    private String normalizeName( String name )
    {
        return name.replaceAll( ":", "." );
    }

    public Timer getTimer( String name )
    {
        return this.metricRegistry.timer( name );
    }

    public Meter getMeter( String name )
    {
        return metricRegistry.meter( name );
    }

    public <T> T wrapWithStandardMetrics( final Supplier<T> method, final Supplier<String> classifier )
    {
        String name = classifier.get();
        if ( SKIP_METRIC.equals( name ) )
        {
            return method.get();
        }

        String nodePrefix = config.getNodePrefix();

        String metricName = name( nodePrefix, name );

        Timer.Context timer = getTimer( name( metricName, TIMER ) ).time();
        logger.trace( "START: {} ({})", metricName, timer );

        try
        {
            return method.get();
        }
        catch ( Throwable e )
        {
            getMeter(
                   name( name, EXCEPTION ) )
                         .mark();

            getMeter(
                   name( name, EXCEPTION, e.getClass().getSimpleName() ) ).mark();

            throw e;
        }
        finally
        {
            if ( timer != null )
            {
                timer.stop();
            }

            getMeter( name( name, METER ) ).mark();
            logger.trace( "CALLS++ {}", metricName );
        }
    }

}
