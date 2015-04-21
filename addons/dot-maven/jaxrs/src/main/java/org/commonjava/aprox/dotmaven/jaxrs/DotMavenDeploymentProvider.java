package org.commonjava.aprox.dotmaven.jaxrs;

import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.util.ImmediateInstanceFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.Servlet;

import org.commonjava.aprox.bind.jaxrs.AproxDeploymentProvider;

@ApplicationScoped
@Named
public class DotMavenDeploymentProvider
    extends AproxDeploymentProvider
{

    @Inject
    DotMavenServlet servlet;

    @Override
    public DeploymentInfo getDeploymentInfo()
    {
        final ServletInfo servletInfo = Servlets.servlet( "DotMaven", DotMavenServlet.class )
                                                .setAsyncSupported( true )
                                                .setLoadOnStartup( 3 )
                                                .addMapping( "/mavdav*" )
                                                .addMapping( "/mavdav/*" );

        servletInfo.setInstanceFactory( new ImmediateInstanceFactory<Servlet>( servlet ) );

        return new DeploymentInfo().addServlet( servletInfo );
    }
}
