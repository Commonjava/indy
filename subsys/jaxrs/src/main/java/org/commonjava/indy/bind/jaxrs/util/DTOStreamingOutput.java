/**
 * Copyright (C) 2011-2023 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.bind.jaxrs.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.output.CountingOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;

public class DTOStreamingOutput
        implements StreamingOutput
{
    private final ObjectMapper mapper;

    private final Object dto;

    public DTOStreamingOutput( final ObjectMapper mapper, final Object dto )
    {
        this.mapper = mapper;
        this.dto = dto;
    }

    @Override
    public String toString()
    {
        try
        {
            return mapper.writeValueAsString( dto );
        }
        catch ( JsonProcessingException e )
        {
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.error( "Could not render toString() for DTO: " + dto, e );
            return String.valueOf( dto );
        }
    }

    @Override
    public void write( final OutputStream outputStream )
            throws IOException, WebApplicationException
    {
        CountingOutputStream cout = new CountingOutputStream( outputStream );
        try
        {
            mapper.writeValue( cout, dto );
        }
        finally
        {
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.trace( "Wrote: {} bytes", cout.getByteCount() );
        }
    }
}
