package org.commonjava.aprox.core.rest;

import java.text.MessageFormat;

import javax.ws.rs.core.Response;

public class RESTWorkflowException
    extends Exception
{

    private final Response response;

    private Object[] params;

    private transient String formattedMessage;

    public RESTWorkflowException( final Response response )
    {
        this.response = response;
    }

    public RESTWorkflowException( final Response response, final String message, final Object... params )
    {
        super( message );
        this.response = response;
        this.params = params;
    }

    public RESTWorkflowException( final Response response, final String message, final Throwable cause,
                                  final Object... params )
    {
        super( message, cause );
        this.response = response;
        this.params = params;
    }

    private static final long serialVersionUID = 1L;

    public Response getResponse()
    {
        return response;
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
