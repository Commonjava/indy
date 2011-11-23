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
import org.commonjava.aprox.core.rest.access.GroupAccessResource;
import org.commonjava.auth.couch.model.Permission;

@Decorator
@RequiresAuthentication
public abstract class GroupAccessResourceSecurity
    implements GroupAccessResource
{

    @Delegate
    @Inject
    @Default
    private GroupAccessResource delegate;

    @Override
    public Response getProxyContent( final String name, final String path )
    {
        SecurityUtils.getSubject().isPermitted( Permission.name( StoreType.group.name(), name,
                                                                 Permission.READ ) );

        return delegate.getProxyContent( name, path );
    }

    @Override
    public Response createContent( final String name, final String path,
                                   final HttpServletRequest request )
    {
        SecurityUtils.getSubject().isPermitted( Permission.name( StoreType.group.name(), name,
                                                                 Permission.READ ) );

        return delegate.createContent( name, path, request );
    }

}
