package org.commonjava.aprox.bind.vertx.util;

import org.commonjava.vertx.vabr.util.RouteHeader;
import org.vertx.java.core.http.HttpServerRequest;

public final class VertxRequestUtils
{

    private VertxRequestUtils()
    {
    }

    public static String getVersionlessAccept( final HttpServerRequest request, final String defaultAccept )
    {
        String accept = request.headers()
                               .get( RouteHeader.base_accept.header() );
        if ( accept == null )
        {
            accept = request.headers()
                            .get( RouteHeader.accept.header() );
        }

        if ( accept == null )
        {
            accept = defaultAccept;
        }

        return accept;
    }

}
