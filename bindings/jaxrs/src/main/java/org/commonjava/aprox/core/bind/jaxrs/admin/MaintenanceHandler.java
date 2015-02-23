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
package org.commonjava.aprox.core.bind.jaxrs.admin;

import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatResponse;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.jaxrs.AproxResources;
import org.commonjava.aprox.core.ctl.ContentController;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path( "/api/admin/maint" )
public class MaintenanceHandler
    implements AproxResources
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ContentController contentController;

    @Path( "/rescan/{type: (hosted|group|remote)}/{name}" )
    @GET
    public Response rescan( final @PathParam( "type" ) String type, @PathParam( "name" ) final String name )
    {
        final StoreKey key = getKey( type, name );

        Response response;
        try
        {
            contentController.rescan( key );
            response = Response.ok()
                               .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( String.format( "Failed to rescan: %s. Reason: %s", key, e.getMessage() ), e );
            response = formatResponse( e, true );
        }
        return response;
    }

    @Path( "/rescan/all" )
    @GET
    public Response rescanAll()
    {
        Response response;
        try
        {
            contentController.rescanAll();
            response = Response.ok()
                               .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( String.format( "Failed to rescan: ALL. Reason: %s", e.getMessage() ), e );
            response = formatResponse( e, true );
        }
        return response;
    }

    @Path( "/delete/all{path: (/.+)?}" )
    @GET
    public Response deleteAllViaGet( final @PathParam( "path" ) String path )
    {
        Response response;
        try
        {
            contentController.deleteAll( path );
            response = Response.ok()
                               .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( String.format( "Failed to delete: %s in: ALL. Reason: %s", e.getMessage() ), e );
            response = formatResponse( e, true );
        }
        return response;
    }

    @Path( "/content/all{path: (/.+)?}" )
    @DELETE
    public Response deleteAll( final @PathParam( "path" ) String path )
    {
        Response response;
        try
        {
            contentController.deleteAll( path );
            response = Response.ok()
                               .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( String.format( "Failed to delete: %s in: ALL. Reason: %s", e.getMessage() ), e );
            response = formatResponse( e, true );
        }
        return response;
    }

    private StoreKey getKey( final String type, final String store )
    {
        final StoreType storeType = StoreType.get( type );
        return new StoreKey( storeType, store );
    }

}
