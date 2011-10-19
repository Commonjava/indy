package org.commonjava.aprox.sec.rest.admin;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.commonjava.aprox.core.model.StoreType;
import org.commonjava.aprox.core.rest.admin.GroupAdminResource;
import org.commonjava.auth.couch.model.Permission;

@Decorator
@RequiresAuthentication
public abstract class GroupAdminResourceSecurity
    implements GroupAdminResource
{

    @Delegate
    @Inject
    @Default
    private GroupAdminResource delegate;

    @Override
    public Response create()
    {
        SecurityUtils.getSubject().isPermitted( Permission.name( StoreType.group.name(),
                                                                 Permission.ADMIN ) );

        return delegate.create();
    }

    @Override
    public Response store( final String name )
    {
        SecurityUtils.getSubject().isPermitted( Permission.name( StoreType.group.name(),
                                                                 Permission.ADMIN ) );

        return delegate.store( name );
    }

    @Override
    public Response getAll()
    {
        SecurityUtils.getSubject().isPermitted( Permission.name( StoreType.group.name(),
                                                                 Permission.ADMIN ) );

        return delegate.getAll();
    }

    @Override
    public Response get( final String name )
    {
        SecurityUtils.getSubject().isPermitted( Permission.name( StoreType.group.name(),
                                                                 Permission.ADMIN ) );

        return delegate.get( name );
    }

    @Override
    public Response delete( final String name )
    {
        SecurityUtils.getSubject().isPermitted( Permission.name( StoreType.group.name(),
                                                                 Permission.ADMIN ) );

        return delegate.delete( name );
    }

}
