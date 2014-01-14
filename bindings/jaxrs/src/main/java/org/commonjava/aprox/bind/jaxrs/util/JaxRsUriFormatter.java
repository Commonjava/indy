package org.commonjava.aprox.bind.jaxrs.util;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.UriBuilder;

import org.commonjava.aprox.bind.jaxrs.RESTApplication;
import org.commonjava.aprox.core.util.UriFormatter;

public class JaxRsUriFormatter
    implements UriFormatter
{

    private static final ApplicationPath APP_PATH = RESTApplication.class.getAnnotation( ApplicationPath.class );

    private final UriBuilder builder;

    public JaxRsUriFormatter( final UriBuilder builder )
    {
        this.builder = builder;
    }

    @Override
    public String formatAbsolutePathTo( final String base, final String... parts )
    {
        UriBuilder b = builder.replacePath( "" )
                              .path( APP_PATH.value() );

        b = b.path( base );
        for ( final String part : parts )
        {
            b = b.path( part );
        }

        return b.build()
                .toString();
    }
}
