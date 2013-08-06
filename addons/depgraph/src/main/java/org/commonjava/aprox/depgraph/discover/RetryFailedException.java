package org.commonjava.aprox.depgraph.discover;

import java.util.List;

import org.commonjava.maven.cartographer.data.CartoDataException;

public class RetryFailedException
    extends CartoDataException
{

    private static final long serialVersionUID = 1L;

    public RetryFailedException( final String message, final List<Throwable> nested, final Object... params )
    {
        super( message, nested, params );
    }

    public RetryFailedException( final String message, final Object... params )
    {
        super( message, params );
    }

    public RetryFailedException( final String message, final Throwable error, final Object... params )
    {
        super( message, error, params );
    }

}
