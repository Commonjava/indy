package org.commonjava.aprox.depgraph.json;

import org.commonjava.maven.cartographer.CartoException;

public class DepgraphSerializationException
    extends CartoException
{

    private static final long serialVersionUID = 1L;

    public DepgraphSerializationException( final String message, final Object... params )
    {
        super( message, params );
    }

    public DepgraphSerializationException( final String message, final Throwable error, final Object... params )
    {
        super( message, error, params );
    }

}
