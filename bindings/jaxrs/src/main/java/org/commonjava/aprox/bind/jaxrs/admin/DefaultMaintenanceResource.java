/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.bind.jaxrs.admin;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.commonjava.aprox.bind.jaxrs.util.AproxExceptionUtils;
import org.commonjava.aprox.core.rest.ContentController;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path( "/admin/maint" )
@ApplicationScoped
public class DefaultMaintenanceResource
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ContentController contentController;

    @GET
    @Path( "/rescan/{type}/{name}" )
    public Response rescan( @PathParam( "type" ) final String type, @PathParam( "name" ) final String name )
    {
        final StoreKey key = getKey( type, name );
        Response response = Response.status( Status.NOT_FOUND )
                                    .build();

        try
        {
            contentController.rescan( key );
            return Response.ok()
                           .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( "Failed to rescan: {}. Reason: {}", e, key, e.getMessage() );
            response = AproxExceptionUtils.formatResponse( e );
        }

        return response;
    }

    @GET
    @Path( "/rescan/all" )
    public Response rescanAll()
    {
        try
        {
            contentController.rescanAll();
            return Response.ok()
                           .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( "Failed to rescan: ALL. Reason: {}", e, e.getMessage() );
            return AproxExceptionUtils.formatResponse( e );
        }
    }

    @GET
    @Path( "/delete/all{path: (/.+)?}" )
    public Response deleteAll( @PathParam( "path" ) final String path )
    {
        try
        {
            contentController.deleteAll( path );
            return Response.ok()
                           .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( "Failed to delete: {} in: ALL. Reason: {}", e, e.getMessage() );
            return AproxExceptionUtils.formatResponse( e );
        }
    }

    private StoreKey getKey( final String type, final String store )
    {
        final StoreType storeType = StoreType.get( type );
        return new StoreKey( storeType, store );
    }

}
