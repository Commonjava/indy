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
package org.commonjava.aprox.bind.jaxrs.util;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.util.ApplicationStatus;

public final class AproxExceptionUtils
{

    private AproxExceptionUtils()
    {
    }

    public static Response formatResponse( final Throwable error )
    {
        return formatResponse( null, error, true );
    }

    public static Response formatResponse( final ApplicationStatus status, final Throwable error )
    {
        return formatResponse( status, error, true );
    }

    public static Response formatResponse( final Throwable error, final boolean includeExplanation )
    {
        return formatResponse( null, error, includeExplanation );
    }

    public static Response formatResponse( final ApplicationStatus status, final Throwable error, final boolean includeExplanation )
    {
        ResponseBuilder rb;
        if ( status != null )
        {
            rb = Response.status( status.code() );
        }
        else if ( ( error instanceof AproxWorkflowException ) && ( (AproxWorkflowException) error ).getStatus() > 0 )
        {
            rb = Response.status( ( (AproxWorkflowException) error ).getStatus() );
        }
        else
        {
            rb = Response.serverError();
        }

        if ( includeExplanation )
        {
            rb.entity( formatEntity( error ) );
        }

        return rb.build();
    }

    public static CharSequence formatEntity( final Throwable error )
    {
        final StringWriter sw = new StringWriter();
        sw.append( error.getMessage() );

        final Throwable cause = error.getCause();
        if ( cause != null )
        {
            sw.append( "\n\n" );
            cause.printStackTrace( new PrintWriter( sw ) );
        }

        return sw.toString();
    }

}
