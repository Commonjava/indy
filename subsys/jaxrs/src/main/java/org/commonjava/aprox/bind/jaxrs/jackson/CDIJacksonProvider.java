/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.aprox.bind.jaxrs.jackson;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.commonjava.aprox.bind.jaxrs.RestProvider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.commonjava.aprox.model.core.io.AproxObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Consumes( { "application/json", "application/*+json", "text/json" } )
@Produces( { "application/json", "application/*+json", "text/json" } )
public class CDIJacksonProvider
    extends JacksonJsonProvider
    implements RestProvider
{

    @Inject
    private AproxObjectMapper mapper;

    @Override
    public ObjectMapper locateMapper( final Class<?> type, final MediaType mediaType )
    {
        AproxObjectMapper aom = mapper;
        if ( aom == null )
        {
            final CDI<Object> cdi = CDI.current();
            aom = cdi.select( AproxObjectMapper.class )
                      .get();
        }

        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.info("Returning AproxObjectMapper with registered modules: {}", mapper.getRegisteredModuleNames() );

        return aom;
    }
}
