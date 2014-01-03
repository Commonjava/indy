/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
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
