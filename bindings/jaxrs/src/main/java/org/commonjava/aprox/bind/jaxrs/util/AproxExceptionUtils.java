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

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.commonjava.aprox.rest.AproxWorkflowException;

public final class AproxExceptionUtils
{

    private AproxExceptionUtils()
    {
    }

    public static Response formatResponse( final AproxWorkflowException error )
    {
        return formatResponse( error, true );
    }

    public static Response formatResponse( final AproxWorkflowException error, final boolean includeExplanation )
    {
        ResponseBuilder rb;
        if ( error.getStatus() > 0 )
        {
            rb = Response.status( error.getStatus() );
        }
        else
        {
            rb = Response.serverError();
        }

        if ( includeExplanation )
        {
            rb.entity( error.formatEntity() );
        }

        return rb.build();
    }

}
