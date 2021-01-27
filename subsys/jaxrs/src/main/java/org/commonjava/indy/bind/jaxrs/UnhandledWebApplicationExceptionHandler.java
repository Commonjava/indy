/**
 * Copyright (C) 2011-2021 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.bind.jaxrs;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This WebApplicationException handler will check for a HTTP response status
 * code (SC) in the exception and ensure the client will receive the corresponding SC code.
 *
 */
public class UnhandledWebApplicationExceptionHandler
        implements ExceptionMapper<WebApplicationException>
{
    @Override
    public Response toResponse( WebApplicationException exception )
    {
        Response response = null;
        if ( exception.getResponse() != null && exception.getResponse().getStatusInfo() != null )
        {
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.error( "Unhandled exception: " + exception.getMessage(), exception );

            response = Response.status( exception.getResponse().getStatusInfo() )
                .entity( ExceptionUtils.getStackTrace( exception ) )
                .type( MediaType.TEXT_PLAIN )
                .build();
        }
        else
        {
            response = new UnhandledThrowableHandler().toResponse( exception );
        }
        return response;
    }
}
