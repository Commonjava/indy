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
package org.commonjava.indy.bind.jaxrs.jackson;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.commonjava.indy.bind.jaxrs.RestProvider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Consumes( { "application/json", "application/*+json", "text/json" } )
@Produces( { "application/json", "application/*+json", "text/json" } )
public class CDIJacksonProvider
    extends JacksonJsonProvider
    implements RestProvider
{

    @Inject
    private IndyObjectMapper mapper;

    @Override
    public ObjectMapper locateMapper( final Class<?> type, final MediaType mediaType )
    {
        IndyObjectMapper aom = mapper;
        if ( aom == null )
        {
            final CDI<Object> cdi = CDI.current();
            aom = cdi.select( IndyObjectMapper.class )
                      .get();
        }

        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.info("Returning IndyObjectMapper: {} with registered modules: {}", aom, aom.getRegisteredModuleNames() );

        return aom;
    }
}
