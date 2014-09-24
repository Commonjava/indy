package org.commonjava.aprox.model.io;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;

public class AproxSerializationException
    extends JsonProcessingException
{

    public AproxSerializationException( final String msg, final JsonLocation loc, final Throwable rootCause )
    {
        super( msg, loc, rootCause );
    }

    public AproxSerializationException( final String msg, final JsonLocation loc )
    {
        super( msg, loc );
    }

    private static final long serialVersionUID = 1L;

}
