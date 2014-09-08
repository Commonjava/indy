package org.commonjava.aprox.audit;

public class SecuritySystemException
    extends Exception
{

    private static final long serialVersionUID = 1L;

    public SecuritySystemException( final SecurityException error )
    {
        super( error );
    }

    public SecurityException getSecurityException()
    {
        return (SecurityException) getCause();
    }

}
