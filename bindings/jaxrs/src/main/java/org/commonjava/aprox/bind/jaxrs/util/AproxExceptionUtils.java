/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
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
