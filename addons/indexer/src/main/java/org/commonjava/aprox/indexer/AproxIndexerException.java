package org.commonjava.aprox.indexer;

import java.text.MessageFormat;
import java.util.IllegalFormatException;

public class AproxIndexerException
    extends Exception
{
    private static final long serialVersionUID = 1L;

    private final Object[] params;

    public AproxIndexerException( final String message, final Throwable error, final Object... params )
    {
        super( message, error );
        this.params = params;
    }

    public AproxIndexerException( final String message, final Object... params )
    {
        super( message );
        this.params = params;
    }

    @Override
    public String getLocalizedMessage()
    {
        return getMessage();
    }

    @Override
    public String getMessage()
    {
        String message = super.getMessage();

        if ( params != null )
        {
            try
            {
                message = String.format( message, params );
            }
            catch ( final IllegalFormatException ife )
            {
                try
                {
                    message = MessageFormat.format( message, params );
                }
                catch ( final IllegalArgumentException iae )
                {
                }
            }
        }

        return message;
    }

}
