/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
            response = formatResponse( e );
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
            response = formatResponse( e );
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
            response = formatResponse( e );
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
            response = formatResponse( e );
        }
        return response;
    }

    private StoreKey getKey( final String type, final String store )
    {
        final StoreType storeType = StoreType.get( type );
        return new StoreKey( storeType, store );
    }

}
