package org.commonjava.aprox.rest;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;

import org.commonjava.aprox.rest.util.ApplicationStatus;

public class AproxWorkflowException
    extends Exception
{
    private final Object[] params;

    private transient String formattedMessage;

    private int status;

    public AproxWorkflowException( final String message, final Object... params )
    {
        super( message );
        this.params = params;
    }

    public AproxWorkflowException( final String message, final Throwable cause, final Object... params )
    {
        super( message, cause );
        this.params = params;
    }

    public AproxWorkflowException( final ApplicationStatus status, final String message, final Object... params )
    {
        super( message );
        this.params = params;
        this.status = status.code();
    }

    public AproxWorkflowException( final int status, final String message, final Object... params )
    {
        super( message );
        this.params = params;
        this.status = status;
    }

    public CharSequence formatEntity()
    {
        final StringWriter sw = new StringWriter();
        sw.append( getMessage() );

        final Throwable cause = getCause();
        if ( cause != null )
        {
            sw.append( "\n\n" );
            cause.printStackTrace( new PrintWriter( sw ) );
        }

        return sw.toString();
    }

    private static final long serialVersionUID = 1L;

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

    public int getStatus()
    {
        return status < 1 ? ApplicationStatus.BAD_REQUEST.code() : status;
    }
}
