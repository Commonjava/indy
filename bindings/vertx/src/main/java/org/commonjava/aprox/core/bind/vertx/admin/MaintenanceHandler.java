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
package org.commonjava.aprox.core.bind.vertx.admin;

import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatResponse;

import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.vertx.util.PathParam;
import org.commonjava.aprox.core.ctl.ContentController;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.anno.Routes;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.commonjava.vertx.vabr.types.Method;
import org.commonjava.vertx.vabr.util.Respond;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;

@Handles( prefix = "/admin/maint" )
public class MaintenanceHandler
    implements RequestHandler
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

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
            Respond.to( request )
                   .ok()
                   .send();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( String.format( "Failed to rescan: %s. Reason: %s", key, e.getMessage() ), e );
            formatResponse( e, request );
        }
    }

    @Routes( { @Route( path = "/rescan/all", method = Method.GET ) } )
    public void rescanAll( final Buffer buffer, final HttpServerRequest request )
    {
        try
        {
            contentController.rescanAll();
            Respond.to( request )
                   .ok()
                   .send();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( String.format( "Failed to rescan: ALL. Reason: %s", e.getMessage() ), e );
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
            Respond.to( request )
                   .ok()
                   .send();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( String.format( "Failed to delete: %s in: ALL. Reason: %s", e.getMessage() ), e );
            formatResponse( e, request );
        }
    }

    private StoreKey getKey( final String type, final String store )
    {
        final StoreType storeType = StoreType.get( type );
        return new StoreKey( storeType, store );
    }

}
