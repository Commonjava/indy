package org.commonjava.aprox.bind.vertx.util;

import org.commonjava.vertx.vabr.util.RouteHeader;
import org.vertx.java.core.http.HttpServerRequest;

public final class VertxRequestUtils
{

    //    private static final Map<String, String> CANONICAL_ACCEPTS = new HashMap<String, String>()
    //    {
    //        {
    //            put( "application/aprox+json", ApplicationContent.application_json );
    //            put( "application/aprox+html", ApplicationContent.text_html );
    //            put( "application/aprox+plain", ApplicationContent.text_plain );
    //            put( "application/aprox+zip", ApplicationContent.application_zip );
    //            put( "application/aprox+xml", ApplicationContent.application_json );
    //        }
    //
    //    };
    //
    private VertxRequestUtils()
    {
    }

    public static String getCanonicalAccept( final HttpServerRequest request, final String defaultAccept )
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

        //        accept = CANONICAL_ACCEPTS.get( accept.toLowerCase() );

        return accept;
    }
}
