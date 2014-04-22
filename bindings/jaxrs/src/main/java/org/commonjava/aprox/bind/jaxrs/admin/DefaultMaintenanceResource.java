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
package org.commonjava.aprox.bind.jaxrs.admin;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.jaxrs.util.AproxExceptionUtils;
import org.commonjava.aprox.core.rest.ContentController;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
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
            logger.error( String.format( "Failed to rescan: %s. Reason: %s", key, e.getMessage() ), e );
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
            logger.error( String.format( "Failed to rescan: ALL. Reason: %s", e.getMessage() ), e );
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
            logger.error( String.format( "Failed to delete: %s in: ALL. Reason: %s", e.getMessage() ), e );
            return AproxExceptionUtils.formatResponse( e );
        }
    }

    private StoreKey getKey( final String type, final String store )
    {
        final StoreType storeType = StoreType.get( type );
        return new StoreKey( storeType, store );
    }

}
