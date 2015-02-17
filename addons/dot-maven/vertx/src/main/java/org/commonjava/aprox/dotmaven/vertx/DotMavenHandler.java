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
package org.commonjava.aprox.dotmaven.vertx;

import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatResponse;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import net.sf.webdav.exceptions.WebdavException;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.bind.vertx.MasterRouter;
import org.commonjava.aprox.bind.vertx.util.PathParam;
import org.commonjava.aprox.dotmaven.inject.DotMavenApp;
import org.commonjava.aprox.dotmaven.webctl.DotMavenService;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.anno.Routes;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.commonjava.vertx.vabr.types.BindingType;
import org.commonjava.vertx.vabr.types.Method;
import org.commonjava.web.vertx.impl.VertXWebdavRequest;
import org.commonjava.web.vertx.impl.VertXWebdavResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.http.HttpServerRequest;

@Handles( key = "dotMavenDAV" )
@DotMavenApp
@ApplicationScoped
public class DotMavenHandler
    implements RequestHandler
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private DotMavenService service;

    @Inject
    private MasterRouter masterRouter;

    // NOTE: /mavdav/ prefix is in the DotMavenRouter.
    @Routes( { @Route( method = Method.ANY, path = ":?path=(.*)", binding = BindingType.raw ) } )
    public void handle( final HttpServerRequest request )
    {
        request.response()
               .setStatusCode( 200 )
               .setStatusMessage( "OK" );

        request.pause();
        String path = request.params()
                             .get( PathParam.path.key() );

        if ( path != null && ( path.length() < 1 || path.equals( "/" ) ) )
        {
            path = null;
        }

        VertXWebdavRequest req = null;
        final VertXWebdavResponse response = new VertXWebdavResponse( request.response() );

        try
        {
            String contextPath = masterRouter.getPrefix();
            if ( contextPath == null )
            {
                contextPath = "";
            }

            req = new VertXWebdavRequest( request, contextPath, "/mavdav", path, null );

            service.service( req, response );

        }
        catch ( WebdavException | IOException e )
        {
            logger.error( String.format( "Failed to service mavdav request: %s", e.getMessage() ), e );
            formatResponse( e, request );
        }
        finally
        {
            IOUtils.closeQuietly( req );
            IOUtils.closeQuietly( response );

            try
            {
                request.response()
                       .end();
            }
            catch ( final IllegalStateException e )
            {
            }
        }
    }
}
