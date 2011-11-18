package org.commonjava.aprox.sec.rest.access;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.commonjava.aprox.core.model.StoreType;
import org.commonjava.aprox.core.rest.access.DeployPointAccessResource;
import org.commonjava.auth.couch.model.Permission;

@Decorator
@RequiresAuthentication
public abstract class DeployPointAccessResourceSecurity
    implements DeployPointAccessResource
{

    @Inject
    @Delegate
    @Default
    private DeployPointAccessResource delegate;

    @Override
    public Response getContent( final String name, final String path )
    {
        SecurityUtils.getSubject().isPermitted( Permission.name( StoreType.deploy_point.name(),
                                                                 name, Permission.READ ) );

        return delegate.getContent( name, path );
    }

    @Override
    public Response createContent( final String name, final String path,
                                   final HttpServletRequest request )
    {
        SecurityUtils.getSubject().isPermitted( Permission.name( StoreType.deploy_point.name(),
                                                                 name, Permission.READ ) );

        return delegate.createContent( name, path, request );
    }

}
