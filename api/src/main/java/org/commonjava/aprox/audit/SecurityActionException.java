package org.commonjava.aprox.audit;

public class SecurityActionException
    extends RuntimeException
{

    private static final long serialVersionUID = 1L;

    public SecurityActionException( final Throwable error )
    {
        super( error );
    }

}
