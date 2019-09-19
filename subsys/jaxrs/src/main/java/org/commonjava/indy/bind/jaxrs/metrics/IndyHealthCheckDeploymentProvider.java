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
package org.commonjava.indy.bind.jaxrs.metrics;

import com.codahale.metrics.servlets.HealthCheckServlet;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.ServletInfo;
import org.commonjava.indy.bind.jaxrs.IndyDeploymentProvider;
import org.commonjava.indy.metrics.IndyMetricsManager;
import org.commonjava.indy.metrics.conf.IndyMetricsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Application;

@ApplicationScoped
public class IndyHealthCheckDeploymentProvider
                extends IndyDeploymentProvider
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private IndyMetricsConfig config;

    @Inject
    private IndyMetricsManager metricsManager;

    @Override
    public DeploymentInfo getDeploymentInfo( String contextRoot, Application application )
    {
        if ( !config.isMetricsEnabled() )
        {
            return null;
        }

        final ServletInfo servlet =
                        Servlets.servlet( "healthcheck", HealthCheckServlet.class ).addMapping( "/healthcheck" ).addMapping( "/healthchecks" );

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
