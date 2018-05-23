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
import org.commonjava.indy.model.core.PackageTypes;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor;
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

import static org.apache.commons.lang.StringUtils.isNotBlank;
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
    MetricRegistry metricRegistry;

    @Inject
    @Any
    Instance<IndyHealthCheck> indyMetricsHealthChecks;

    @Inject
    ReporterIntializer reporter;

    @Inject
    @IndyMetricsNamed
    IndyMetricsConfig config;

    private TransportMetricConfig transportMetricConfig;

    @Produces
    public TransportMetricConfig getTransportMetricConfig()
    {
        return transportMetricConfig;
    }

    @PostConstruct
    public void initMetric()
    {
        logger.warn( "Starting metrics subsystem..." );

        if ( !config.isMetricsEnabled() )
        {
            return;
        }

        IndyJVMInstrumentation.init( metricRegistry );
        IndyHealthCheckRegistrySet healthCheckRegistrySet = new IndyHealthCheckRegistrySet();

        indyMetricsHealthChecks.forEach( indyHealthCheck ->
                                         {
                                             logger.info( "Registering health check: {}", indyHealthCheck.getName() );
                                             healthCheckRegistrySet.register( indyHealthCheck.getName(),
                                                                              (HealthCheck) indyHealthCheck );
                                         } );
        try
        {
            logger.info( "Adding health checks to registry: {}", metricRegistry );
            metricRegistry.register( healthCheckRegistrySet.getName(), healthCheckRegistrySet );
            reporter.initReporter( metricRegistry );
        }
        catch ( Exception e )
        {
            logger.error( e.getMessage() );
            throw new RuntimeException( e );
        }

        // Set up TransportMetricConfig
        if ( config.isMeasureTransport() )
        {
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
    }

    private String normalizeName( String name )
    {
        return name.replaceAll( ":", "." );
    }

    public Timer getTimer( String name )
    {
        logger.trace( "call in IndyMetricsManager.getTimer" );
        return this.metricRegistry.timer( name );
    }

    public Meter getMeter( String name )
    {
        logger.trace( "call in IndyMetricsManager.getMeter" );
        return metricRegistry.meter( name );
    }

}
