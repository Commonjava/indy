/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * Created by jdcasey on 1/3/17.
 * Based on: http://stackoverflow.com/questions/13857638/global-custom-exception-handler-in-resteasy
 */
@Provider
public class UnhandledIOExceptionHandler
        implements ExceptionMapper<IOException>//, RestProvider
{
    public Response toResponse( IOException exception )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.error( "Unhandled exception: " + exception.getMessage(), exception );

        return Response.status( Response.Status.INTERNAL_SERVER_ERROR )
                       .entity( ExceptionUtils.getFullStackTrace( exception ) )
                       .type( MediaType.TEXT_PLAIN )
                       .build();
    }
}
