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
package org.commonjava.indy.rest.apigen;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.ServletInfo;
import org.commonjava.indy.bind.jaxrs.IndyDeploymentProvider;
import org.commonjava.indy.bind.jaxrs.util.CdiInjectorFactoryImpl;
import org.commonjava.indy.bind.jaxrs.util.RequestScopeListener;
import org.commonjava.indy.conf.UIConfiguration;
import org.commonjava.indy.stats.IndyVersioning;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Application;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Deployment that enabled Swagger ONLY for the duration of running the SwaggerExportTest, in this module. This
 * will download and save the generated YAML / JSON files, for eventual inclusion in the normal Indy UI.
 */
@ApplicationScoped
public class SwaggerGeneratorDeployment
        extends IndyDeploymentProvider
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private IndyVersioning versioning;

    @Inject
    private UIConfiguration uiConfiguration;

    public SwaggerGeneratorDeployment()
    {
    }

    public DeploymentInfo getDeploymentInfo( final String contextRoot, Application application )
    {
        // we have to disable the UI for this to work.
        uiConfiguration.setEnabled( false );

        final ResteasyDeployment deployment = new ResteasyDeployment();

        deployment.setApplication( application );
        deployment.setInjectorFactoryClass( CdiInjectorFactoryImpl.class.getName() );

        final ServletInfo resteasyServlet = Servlets.servlet( "Swagger Generator Servlet", HttpServlet30Dispatcher.class )
                                                    .setAsyncSupported( true )
                                                    .setLoadOnStartup( 2 )
                                                    .addMapping( "/swagger.json" )
                                                    .addMapping( "/swagger.yaml" );

        final BeanConfig beanConfig = new BeanConfig();
        beanConfig.setResourcePackage( "org.commonjava.indy" );
        beanConfig.setBasePath( "/" );
        beanConfig.setLicense( "ASLv2" );
        beanConfig.setLicenseUrl( "http://www.apache.org/licenses/LICENSE-2.0" );
        beanConfig.setScan( true );
        beanConfig.setVersion( versioning.getApiVersion() );

        final DeploymentInfo di = new DeploymentInfo().addListener( Servlets.listener( RequestScopeListener.class ) )
                                                      //                                .addInitParameter( "resteasy.scan", Boolean.toString( true ) )
                                                      .setContextPath( contextRoot )
                                                      .addServletContextAttribute( ResteasyDeployment.class.getName(),
                                                                                   deployment )
                                                      .addServlet( resteasyServlet )
                                                      .setDeploymentName( "Swagger Generator Deployment" )
                                                      .setClassLoader( ClassLoader.getSystemClassLoader() );

        logger.info( "\n\n\n\nReturning DeploymentInfo for Swagger generator\n\n\n\n" );

        return di;
    }

    public Set<Class<?>> getAdditionalClasses()
    {
        final Set<Class<?>> classes = new LinkedHashSet<>();
        classes.addAll( Arrays.asList( ApiListingResource.class, SwaggerSerializers.class ) );
        return classes;
    }
}
