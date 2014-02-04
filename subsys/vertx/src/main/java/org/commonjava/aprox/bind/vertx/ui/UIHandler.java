package org.commonjava.aprox.bind.vertx.ui;

import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.setStatus;
import static org.commonjava.aprox.rest.util.ApplicationStatus.BAD_REQUEST;
import static org.commonjava.aprox.rest.util.ApplicationStatus.NOT_FOUND;
import static org.commonjava.aprox.rest.util.ApplicationStatus.OK;
import static org.commonjava.aprox.rest.util.RequestUtils.formatDateHeader;
import static org.commonjava.vertx.vabr.Method.ANY;
import static org.commonjava.vertx.vabr.Method.GET;

import java.io.File;

import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;
import javax.inject.Inject;

import org.commonjava.aprox.bind.vertx.conf.UIConfiguration;
import org.commonjava.aprox.bind.vertx.util.PathParam;
import org.commonjava.aprox.rest.util.ApplicationHeader;
import org.commonjava.util.logging.Logger;
import org.commonjava.vertx.vabr.Method;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.vertx.java.core.http.HttpServerRequest;

@Handles( key = "UIHandler" )
@UIApp
public class UIHandler
    implements RequestHandler
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private UIConfiguration config;

    private final FileTypeMap typeMap = MimetypesFileTypeMap.getDefaultFileTypeMap();

    @Route( path = ":?path=(.+)", method = ANY )
    public void handleUIRequest( final HttpServerRequest request )
    {
        final Method method = Method.valueOf( request.method() );
        switch ( method )
        {
            case GET:
            case HEAD:
            {
                final File uiDir = config.getUIDir();
                logger.info( "UI basedir: '%s'", uiDir );

                String path = request.params()
                                     .get( PathParam.path.key() );

                if ( path == null )
                {
                    logger.info( "null path. Using /index.html" );
                    path = "index.html";
                }
                else if ( path.endsWith( "/" ) )
                {
                    path += "index.html";
                    logger.info( "directory path. Using %s", path );
                }

                if ( path.startsWith( "/" ) )
                {
                    logger.info( "Trimming leading '/' from path" );
                    path = path.substring( 1 );
                }

                final File resource = new File( uiDir, path );
                logger.info( "Checking for existence of: '%s'", resource );
                if ( resource.exists() )
                {
                    if ( method == GET )
                    {
                        logger.info( "sending file" );
                        request.response()
                               .putHeader( ApplicationHeader.last_modified.key(), formatDateHeader( resource.lastModified() ) )
                               .sendFile( resource.getAbsolutePath() );
                    }
                    else
                    {
                        logger.info( "sending OK" );
                        // TODO: set headers for content info...
                        setStatus( OK, request );
                        request.response()
                               .setChunked( true )
                               .putHeader( ApplicationHeader.content_type.key(), typeMap.getContentType( resource ) )
                               .putHeader( ApplicationHeader.content_length.key(), Long.toString( resource.length() ) )
                               .putHeader( ApplicationHeader.last_modified.key(), formatDateHeader( resource.lastModified() ) )
                               .end();
                    }
                }
                else
                {
                    logger.info( "sending 404" );
                    setStatus( NOT_FOUND, request );
                }
                break;
            }
            default:
            {
                logger.error( "cannot handle request for method: %s", method );
                setStatus( BAD_REQUEST, request );
            }
        }
    }
}
