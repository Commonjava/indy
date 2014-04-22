/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.sec.rest.access;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.rest.access.GroupAccessResource;
import org.commonjava.badgr.model.Permission;

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
        SecurityUtils.getSubject()
                     .isPermitted( Permission.name( StoreType.group.name(), name, Permission.READ ) );

        return delegate.getProxyContent( name, path );
    }

    @Override
    public Response createContent( final String name, final String path, final HttpServletRequest request )
    {
        SecurityUtils.getSubject()
                     .isPermitted( Permission.name( StoreType.group.name(), name, Permission.READ ) );

        return delegate.createContent( name, path, request );
    }

}
