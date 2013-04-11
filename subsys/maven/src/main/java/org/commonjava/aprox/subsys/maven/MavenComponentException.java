package org.commonjava.aprox.subsys.maven;

import java.text.MessageFormat;

public class MavenComponentException
    extends Exception
{
    private static final long serialVersionUID = 1L;

    private final Object[] params;

    private String formattedMessage;

    public MavenComponentException( final String message, final Throwable cause, final Object... params )
    {
        super( message, cause );
        this.params = params;
    }

    public MavenComponentException( final String message, final Object... params )
    {
        super( message );
        this.params = params;
    }

    @Override
    public synchronized String getMessage()
    {
        if ( formattedMessage == null )
        {
            final String format = super.getMessage();
            if ( params == null || params.length < 1 )
            {
                formattedMessage = format;
            }
            else
            {
                final String original = formattedMessage;
                try
                {
                    formattedMessage = String.format( format, params );
                }
                catch ( final Error e )
                {
                }
                catch ( final RuntimeException e )
                {
                }
                catch ( final Exception e )
                {
                }

                if ( formattedMessage == null || original == formattedMessage )
                {
                    try
                    {
                        formattedMessage = MessageFormat.format( format, params );
                    }
                    catch ( final Error e )
                    {
                        formattedMessage = format;
                        throw e;
                    }
                    catch ( final RuntimeException e )
                    {
                        formattedMessage = format;
                        throw e;
                    }
                    catch ( final Exception e )
                    {
                        formattedMessage = format;
                    }
                }
            }
        }

        return formattedMessage;
    }
}
