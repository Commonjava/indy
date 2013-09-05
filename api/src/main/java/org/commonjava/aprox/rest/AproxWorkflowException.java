package org.commonjava.aprox.rest;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

public class AproxWorkflowException
    extends Exception
{

    private final Object[] params;

    private transient String formattedMessage;

    private Status status;

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

    public AproxWorkflowException( final Status status, final String message, final Object... params )
    {
        super( message );
        this.params = params;
        this.status = status;
    }

    private Object formatEntity()
    {
        final StringWriter sw = new StringWriter();
        sw.append( getMessage() );

        final Throwable cause = getCause();
        if ( cause != null )
        {
            sw.append( "\n\n" );
            cause.printStackTrace( new PrintWriter( sw ) );
        }

        return sw;
    }

    private static final long serialVersionUID = 1L;

    public Response getResponse()
    {
        return getResponse( true );
    }

    public Response getResponse( final boolean includeExplanation )
    {
        ResponseBuilder rb;
        if ( status != null )
        {
            rb = Response.status( status );
        }
        else
        {
            rb = Response.serverError();
        }

        if ( includeExplanation )
        {
            rb.entity( formatEntity() );
        }

        return rb.build();
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
