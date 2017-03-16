package org.commonjava.indy;

import com.codahale.metrics.*;
import com.codahale.metrics.health.HealthCheck;
import org.commonjava.indy.measure.annotation.IndyMetrics;
import org.commonjava.indy.measure.annotation.Measure;
import org.commonjava.indy.measure.annotation.MetricNamed;
import org.commonjava.indy.metrics.conf.IndyMetricsConfig;
import org.commonjava.indy.metrics.healthcheck.IndyHealthCheckRegistrySet;
import org.commonjava.indy.metrics.healthcheck.IndyHealthCheck;
import org.commonjava.indy.metrics.jvm.IndyJVMInstrumentation;
import org.commonjava.indy.metrics.reporter.ReporterIntializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;

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
    IndyMetricsConfig config;

    @PostConstruct
    public void initMetric()
    {

        if ( !config.isEnabled() )
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

    public Timer getTimer( IndyMetrics metrics, Measure measures, MetricNamed named )
    {
        logger.info( "call in IndyMetricsManager.getTimer" );
        Class<?> c = getClass( metrics, measures, named );
        return this.metricRegistry.timer( name( c, named.name() ) );
    }

    public Meter getMeter( IndyMetrics metrics, Measure measures, MetricNamed named )
    {
        logger.info( "call in IndyMetricsManager.getMeter" );
        Class<?> c = getClass( metrics, measures, named );
        return metricRegistry.meter( name( c, named.name() ) );
    }

    private Class<?> getClass( IndyMetrics metrics, Measure measures, MetricNamed named )
    {
        Class<?> c = named.c();
        if ( Void.class.equals( c ) )
        {
            c = measures.c();
        }

        if ( Void.class.equals( c ) )
        {
            c = metrics.c();
        }

        return c;
    }

}
