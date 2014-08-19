package org.commonjava.aprox.bind.vertx;

import org.commonjava.aprox.bind.vertx.util.PathParam;
import org.commonjava.aprox.util.ApplicationHeader;
import org.commonjava.aprox.util.ApplicationStatus;
import org.commonjava.maven.galley.util.PathUtils;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.commonjava.vertx.vabr.types.BindingType;
import org.commonjava.vertx.vabr.types.BuiltInParam;
import org.commonjava.vertx.vabr.types.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.http.HttpServerRequest;

@Handles( "/1.0" )
public class DeprecatedApi10Redirector
    implements RequestHandler
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Route( path = ":path=(/.*)", method = Method.ANY, binding = BindingType.raw )
    public void handle( final HttpServerRequest request )
    {
        final String to = request.params()
                                 .get( PathParam.path.key() );

        final String uri = PathUtils.normalize( request.params()
                                                       .get( BuiltInParam._routeContextUrl.key() ), to );

        logger.warn( "[DEPRECATION] Redirecting '{}'\n  to: '{}'", request.absoluteURI(), uri );

        request.resume()
               .response()
               .setStatusCode( ApplicationStatus.MOVED_PERMANENTLY.code() )
               .setStatusMessage( ApplicationStatus.MOVED_PERMANENTLY.message() )
               .putHeader( ApplicationHeader.deprecated.key(), uri )
               .putHeader( "URI", uri )
               .putHeader( "Location", uri )
               .end();
    }

}
