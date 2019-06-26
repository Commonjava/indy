package org.commonjava.indy.sli.jaxrs;

import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.util.ImmediateInstanceFactory;
import org.commonjava.indy.bind.jaxrs.IndyDeploymentProvider;
import org.commonjava.indy.bind.jaxrs.ResourceManagementFilter;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.DispatcherType;
import javax.ws.rs.core.Application;

@ApplicationScoped
public class SLIDeploymentProvider
        extends IndyDeploymentProvider
{
    @Inject
    private GoldenSignalsFilter goldenSignalsFilter;

    @Override
    public DeploymentInfo getDeploymentInfo( final String contextRoot, final Application application )
    {
        DeploymentInfo di = new DeploymentInfo();

        FilterInfo filterInfo = Servlets.filter( "SLI Reporting", GoldenSignalsFilter.class,
                                                    new ImmediateInstanceFactory<GoldenSignalsFilter>(
                                                            this.goldenSignalsFilter ) );

        di.addFilter( filterInfo )
          .addFilterUrlMapping( filterInfo.getName(), "/api/*", DispatcherType.REQUEST );

        return di;
    }
}
