/*******************************************************************************
 * Copyright (C) 2011  John Casey
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see 
 * <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.core.rest.access;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.commonjava.aprox.core.data.ProxyDataException;
import org.commonjava.aprox.core.data.ProxyDataManager;
import org.commonjava.aprox.core.model.Repository;
import org.commonjava.aprox.core.model.StoreType;
import org.commonjava.auth.couch.model.Permission;
import org.commonjava.util.logging.Logger;

@Path( "/repository" )
@RequestScoped
@RequiresAuthentication
public class RepositoryAccessResource
    extends AbstractSimpleAccessResource<Repository>
{
    private final Logger logger = new Logger( getClass() );

    @Inject
    private ProxyDataManager proxyManager;

    // @Context
    // private UriInfo uriInfo;

    @Override
    protected Repository getArtifactStore( final String name )
    {
        try
        {
            return proxyManager.getRepository( name );
        }
        catch ( ProxyDataException e )
        {
            logger.error( "Failed to retrieve proxy: %s. Reason: %s", e, name, e.getMessage() );
            throw new WebApplicationException(
                                               Response.status( Status.INTERNAL_SERVER_ERROR ).build() );
        }
    }

    @Override
    protected void checkPermission( final String name, final String access )
    {
        SecurityUtils.getSubject().isPermitted( Permission.name( StoreType.repository.name(), name,
                                                                 access ) );
    }
}
