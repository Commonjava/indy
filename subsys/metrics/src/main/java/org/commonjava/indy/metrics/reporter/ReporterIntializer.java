package org.commonjava.indy.metrics.reporter;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import org.commonjava.indy.metrics.conf.IndyMetricsConfig;
import org.commonjava.indy.metrics.filter.HealthCheckMetricFilter;
import org.commonjava.indy.metrics.filter.JVMMetricFilter;
import org.commonjava.indy.metrics.filter.SimpleMetricFilter;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Created by xiabai on 3/3/17.
 */
@ApplicationScoped
public class ReporterIntializer
{

    @Inject
    IndyMetricsConfig config;

    public void initReporter( MetricRegistry metrics ) throws Exception
    {

        String reporter = config.getReporter();
        if ( IndyMetricsConfig.INDY_METRICS_REPORTER_GRPHITEREPORTER.equals( reporter ) )
        {
            initGraphiteReporterForSimpleMetric( metrics, config );
            initGraphiteReporterForJVMMetric( metrics, config );
            initGraphiteReporterForHealthCheckMetric( metrics, config );
        }

    }

    private void initConsoleReporter( MetricRegistry metrics, IndyMetricsConfig config )
    {
        ConsoleReporter.forRegistry( metrics )
                       .build()
                       .start( IndyMetricsConfig.INDY_METRICS_REPORTER_GRPHITEREPORTER_DEFAULT_PERIOD,
                               TimeUnit.SECONDS );
    }

    private void initGraphiteReporterForSimpleMetric( MetricRegistry metrics, IndyMetricsConfig config )
    {
        final Graphite graphite =
                        new Graphite( new InetSocketAddress( config.getGrphiterHostName(), config.getGrphiterPort() ) );
        final GraphiteReporter reporter = GraphiteReporter.forRegistry( metrics )
                                                          .prefixedWith( config.getGrphiterPrefix() )
                                                          .convertRatesTo( TimeUnit.SECONDS )
                                                          .convertDurationsTo( TimeUnit.MILLISECONDS )
                                                          .filter( new SimpleMetricFilter() )
                                                          .build( graphite );
        reporter.start( config.getGrphiterSimplePriod(), TimeUnit.SECONDS );
    }

    private void initGraphiteReporterForJVMMetric( MetricRegistry metrics, IndyMetricsConfig config )
    {
        final Graphite graphite =
                        new Graphite( new InetSocketAddress( config.getGrphiterHostName(), config.getGrphiterPort() ) );
        final GraphiteReporter reporter = GraphiteReporter.forRegistry( metrics )
                                                          .prefixedWith( config.getGrphiterPrefix() )
                                                          .convertRatesTo( TimeUnit.SECONDS )
                                                          .convertDurationsTo( TimeUnit.MILLISECONDS )
                                                          .filter( new JVMMetricFilter() )
                                                          .build( graphite );
        reporter.start( config.getGrphiterJVMPriod(), TimeUnit.SECONDS );
    }

    private void initGraphiteReporterForHealthCheckMetric( MetricRegistry metrics, IndyMetricsConfig config )
    {
        final Graphite graphite =
                        new Graphite( new InetSocketAddress( config.getGrphiterHostName(), config.getGrphiterPort() ) );
        final GraphiteReporter reporter = GraphiteReporter.forRegistry( metrics )
                                                          .prefixedWith( config.getGrphiterPrefix() )
                                                          .convertRatesTo( TimeUnit.SECONDS )
                                                          .convertDurationsTo( TimeUnit.MILLISECONDS )
                                                          .filter( new HealthCheckMetricFilter() )
                                                          .build( graphite );
        reporter.start( config.getGrphiterHealthcheckPeriod(), TimeUnit.SECONDS );
    }
}
