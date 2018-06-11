package org.commonjava.indy.bind.jaxrs.metrics;

import com.codahale.metrics.servlets.HealthCheckServlet;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.ServletInfo;
import org.commonjava.indy.bind.jaxrs.IndyDeploymentProvider;
import org.commonjava.indy.metrics.conf.IndyMetricsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.core.Application;

public class IndyHealthCheckDeploymentProvider
                extends IndyDeploymentProvider
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private IndyMetricsConfig config;

    @Override
    public DeploymentInfo getDeploymentInfo( String contextRoot, Application application )
    {
        if ( !config.isMetricsEnabled() )
        {
            return null;
        }

        final ServletInfo servlet =
                        Servlets.servlet( "healthcheck", HealthCheckServlet.class ).addMapping( "/healthcheck" );

        final DeploymentInfo di = new DeploymentInfo().addListener(
                        Servlets.listener( IndyHealthCheckServletContextListener.class ) )
                                                      .setContextPath( contextRoot )
                                                      .addServlet( servlet )
                                                      .setDeploymentName( "HealthCheck Deployment" )
                                                      .setClassLoader( ClassLoader.getSystemClassLoader() );

        logger.info( "Returning deployment info for health check" );
        return di;
    }
}
