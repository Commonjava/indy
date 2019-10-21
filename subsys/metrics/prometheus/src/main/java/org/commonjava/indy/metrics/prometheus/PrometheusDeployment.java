package org.commonjava.indy.metrics.prometheus;

import com.codahale.metrics.MetricRegistry;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;
import io.prometheus.client.exporter.MetricsServlet;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.ServletInfo;
import org.commonjava.indy.bind.jaxrs.IndyDeploymentProvider;
import org.commonjava.indy.bind.jaxrs.metrics.IndyHealthCheckServletContextListener;
import org.commonjava.indy.metrics.conf.IndyMetricsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Application;

@ApplicationScoped
public class PrometheusDeployment
        extends IndyDeploymentProvider
{
    private static final String PROMETHEUS_REPORTER = "prometheus";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private IndyMetricsConfig config;

    @Inject
    private MetricRegistry metricRegistry;

    @Override
    public DeploymentInfo getDeploymentInfo( String contextRoot, Application application )
    {
        if ( !config.isMetricsEnabled() || !config.getReporter().contains( PROMETHEUS_REPORTER ) )
        {
            return null;
        }

        CollectorRegistry.defaultRegistry.register( new DropwizardExports( metricRegistry ) );

        final ServletInfo servlet =
                Servlets.servlet( "prometheus-metrics", MetricsServlet.class ).addMapping( "/metrics" );

        final DeploymentInfo di = new DeploymentInfo().addListener(
                Servlets.listener( IndyHealthCheckServletContextListener.class ) )
                                                      .setContextPath( contextRoot )
                                                      .addServlet( servlet )
                                                      .setDeploymentName( "Prometheus Metrics Deployment" )
                                                      .setClassLoader( ClassLoader.getSystemClassLoader() );

        logger.info( "Returning deployment info for Prometheus metrics servlet" );
        return di;
    }
}
