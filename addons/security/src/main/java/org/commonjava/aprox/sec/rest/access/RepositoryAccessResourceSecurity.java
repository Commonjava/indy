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
import javax.ws.rs.core.Response;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.rest.access.RepositoryAccessResource;
import org.commonjava.badgr.model.Permission;

@Decorator
@RequiresAuthentication
public abstract class RepositoryAccessResourceSecurity
    implements RepositoryAccessResource
{

    @Inject
    @Delegate
    @Default
    private RepositoryAccessResource delegate;

    @Override
    public Response getContent( final String name, final String path )
    {
        SecurityUtils.getSubject()
                     .isPermitted( Permission.name( StoreType.deploy_point.name(), name, Permission.READ ) );

        return delegate.getContent( name, path );
    }

}
