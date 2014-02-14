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
package org.commonjava.aprox.bind.vertx.admin;

import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatResponse;
import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.setStatus;

import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.vertx.util.PathParam;
import org.commonjava.aprox.core.rest.ContentController;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.util.ApplicationStatus;
import org.commonjava.util.logging.Logger;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.anno.Routes;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.commonjava.vertx.vabr.types.Method;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;

@Handles( prefix = "/admin/maint" )
public class DefaultMaintenanceResource
    implements RequestHandler
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private ContentController contentController;

    @Routes( { @Route( path = "/rescan/:type/:name", method = Method.GET ) } )
    public void rescan( final Buffer buffer, final HttpServerRequest request )
    {
        final String type = request.params()
                                   .get( PathParam.type.key() );
        final String name = request.params()
                                   .get( PathParam.name.key() );

        final StoreKey key = getKey( type, name );

        try
        {
            contentController.rescan( key );
            setStatus( ApplicationStatus.OK, request );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( "Failed to rescan: %s. Reason: %s", e, key, e.getMessage() );
            formatResponse( e, request );
        }
    }

    @Routes( { @Route( path = "/rescan/all", method = Method.GET ) } )
    public void rescanAll( final Buffer buffer, final HttpServerRequest request )
    {
        try
        {
            contentController.rescanAll();
            setStatus( ApplicationStatus.OK, request );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( "Failed to rescan: ALL. Reason: %s", e, e.getMessage() );
            formatResponse( e, request );
        }
    }

    /*@formatter:off*/
    @Routes( { 
        @Route( path = "/delete/all:?path=(/.+)", method = Method.GET ), 
        @Route( routeKey="alt", path = "/content/all:?path=(/.+)", method = Method.DELETE ) 
    } )
    /*@formatter:on*/
    public void deleteAll( final Buffer buffer, final HttpServerRequest request )
    {
        final String path = request.params()
                                   .get( PathParam.path.key() );

        try
        {
            contentController.deleteAll( path );
            setStatus( ApplicationStatus.OK, request );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( "Failed to delete: %s in: ALL. Reason: %s", e, e.getMessage() );
            formatResponse( e, request );
        }
    }

    private StoreKey getKey( final String type, final String store )
    {
        final StoreType storeType = StoreType.get( type );
        return new StoreKey( storeType, store );
    }

}
