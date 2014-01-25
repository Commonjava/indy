package org.commonjava.aprox.dotmaven.vertx;

import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatResponse;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import net.sf.webdav.exceptions.WebdavException;

import org.commonjava.aprox.bind.vertx.boot.AProxRouter;
import org.commonjava.aprox.dotmaven.webctl.DotMavenService;
import org.commonjava.util.logging.Logger;
import org.commonjava.vertx.vabr.Method;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.anno.Routes;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.commonjava.web.vertx.impl.VertXWebdavRequest;
import org.commonjava.web.vertx.impl.VertXWebdavResponse;
import org.vertx.java.core.http.HttpServerRequest;

@Handles( "dotMavenDAV" )
@ApplicationScoped
public class DotMavenHandler
    implements RequestHandler
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private DotMavenService service;

    @Routes( { @Route( method = Method.ANY, path = "/mavdav/:?path" ) } )
    public void handle( final HttpServerRequest request )
    {
        try
        {
            service.service( new VertXWebdavRequest( request, AProxRouter.PREFIX, "/mavdav", null ), new VertXWebdavResponse( request.response() ) );
        }
        catch ( WebdavException | IOException e )
        {
            logger.error( "Failed to service mavdav request: %s", e, e.getMessage() );
            formatResponse( e, request.response() );
        }
    }

}
