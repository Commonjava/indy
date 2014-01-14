package org.commonjava.aprox.bind.vertx.util;

import java.nio.file.Paths;

import javax.enterprise.context.RequestScoped;

import org.commonjava.aprox.core.util.UriFormatter;

@RequestScoped
public class VertXUriFormatter
    implements UriFormatter
{

    @Override
    public String formatAbsolutePathTo( final String base, final String... parts )
    {
        return Paths.get( "/api/1.0", Paths.get( base, parts )
                                           .toString() )
                    .toString();
    }

}
