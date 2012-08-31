/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.commonjava.aprox.sec.rest.admin;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.rest.admin.GroupAdminResource;
import org.commonjava.couch.rbac.Permission;

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
