package org.commonjava.aprox.bind.vertx.util;

import org.commonjava.aprox.util.ApplicationContent;
import org.commonjava.vertx.vabr.util.RouteHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.http.HttpServerRequest;

public final class VertxRequestUtils
{
    private static final Logger logger = LoggerFactory.getLogger( VertxRequestUtils.class );

    private VertxRequestUtils()
    {
    }

    public static String getStandardAccept( final HttpServerRequest request, final String defaultAccept )
    {
        String accept = request.headers()
                               .get( RouteHeader.base_accept.header() );

        logger.info( "Got base-accept ({}): {}", RouteHeader.base_accept.header(), accept );
        if ( accept == null )
        {
            accept = request.headers()
                            .get( RouteHeader.accept.header() );

            logger.info( "Got raw-accept ({}): {}", RouteHeader.accept.header(), accept );
        }

        if ( accept != null )
        {
            accept = ApplicationContent.getStandardAccept( accept );
        }

        if ( accept == null )
        {
            logger.info( "Using default accept: {}", defaultAccept );
            accept = defaultAccept;
        }

        return accept;
    }

}
