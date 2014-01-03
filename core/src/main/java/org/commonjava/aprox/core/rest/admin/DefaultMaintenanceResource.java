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
package org.commonjava.aprox.core.rest.admin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.filer.FileManager;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.aprox.rest.admin.MaintenanceResource;
import org.commonjava.util.logging.Logger;

@Path( "/admin/maint" )
@RequestScoped
public class DefaultMaintenanceResource
    implements MaintenanceResource
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private FileManager fileManager;

    @Inject
    private StoreDataManager aprox;

    @Override
    @GET
    @Path( "/rescan/{type}/{name}" )
    public Response rescan( @PathParam( "type" ) final String type, @PathParam( "name" ) final String name )
    {
        final StoreKey key = getKey( type, name );

        Response response = Response.status( Status.NOT_FOUND )
                                    .build();

        ArtifactStore artifactStore;
        try
        {
            artifactStore = aprox.getArtifactStore( key );

            if ( artifactStore != null )
            {
                fileManager.rescan( artifactStore );
                response = Response.ok()
                                   .build();
            }
        }
        catch ( final ProxyDataException e )
        {
            logger.error( "Failed to rescan: %s. Reason: %s", e, key, e.getMessage() );
            response = Response.serverError()
                               .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( "Failed to rescan: %s. Reason: %s", e, key, e.getMessage() );
            response = e.getResponse();
        }

        return response;
    }

    @Override
    @GET
    @Path( "/rescan/all" )
    public Response rescanAll()
    {
        Response response = Response.ok()
                                    .build();

        try
        {
            final List<ArtifactStore> stores = getAllNonGroupStores();
            fileManager.rescanAll( stores );
        }
        catch ( final ProxyDataException e )
        {
            logger.error( "Failed to rescan: ALL. Reason: %s", e, e.getMessage() );
            response = Response.serverError()
                               .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( "Failed to rescan: ALL. Reason: %s", e, e.getMessage() );
            response = e.getResponse();
        }

        return response;
    }

    @Override
    @GET
    @Path( "/delete/{type}/{name}{path: (/.+)?}" )
    public Response delete( @PathParam( "type" ) final String type, @PathParam( "name" ) final String name,
                            @PathParam( "path" ) final String path )
    {
        final StoreKey key = getKey( type, name );
        Response response = Response.ok()
                                    .build();

        try
        {
            final ArtifactStore store = aprox.getArtifactStore( key );

            fileManager.delete( store, path );
        }
        catch ( final ProxyDataException e )
        {
            logger.error( "Failed to delete: %s in: %s. Reason: %s", e, key, e.getMessage() );
            response = Response.serverError()
                               .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( "Failed to delete: %s in: %s. Reason: %s", e, key, e.getMessage() );
            response = e.getResponse();
        }

        return response;
    }

    @Override
    @GET
    @Path( "/delete/all{path: (/.+)?}" )
    public Response deleteAll( @PathParam( "path" ) final String path )
    {
        Response response = Response.ok()
                                    .build();

        try
        {
            final List<ArtifactStore> stores = getAllNonGroupStores();
            fileManager.deleteAll( stores, path );
        }
        catch ( final ProxyDataException e )
        {
            logger.error( "Failed to delete: %s in: ALL. Reason: %s", e, e.getMessage() );
            response = Response.serverError()
                               .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( "Failed to delete: %s in: ALL. Reason: %s", e, e.getMessage() );
            response = e.getResponse();
        }

        return response;
    }

    private StoreKey getKey( final String type, final String store )
    {
        final StoreType storeType = StoreType.get( type );
        return new StoreKey( storeType, store );
    }

    private List<ArtifactStore> getAllNonGroupStores()
        throws ProxyDataException
    {
        final List<ArtifactStore> stores = new ArrayList<ArtifactStore>( aprox.getAllArtifactStores() );

        for ( final Iterator<ArtifactStore> it = stores.iterator(); it.hasNext(); )
        {
            final ArtifactStore store = it.next();
            if ( store.getKey()
                      .getType() == StoreType.group )
            {
                it.remove();
            }
        }

        return stores;
    }

}
