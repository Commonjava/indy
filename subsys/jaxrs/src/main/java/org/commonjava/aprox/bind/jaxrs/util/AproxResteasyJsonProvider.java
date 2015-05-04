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
package org.commonjava.aprox.bind.jaxrs.util;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Produces;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import org.commonjava.aprox.model.core.io.AproxObjectMapper;
import org.commonjava.aprox.util.ApplicationContent;

import com.fasterxml.jackson.databind.ObjectMapper;

@Provider
@Produces( ApplicationContent.application_json )
@ApplicationScoped
public class AproxResteasyJsonProvider
    implements ContextResolver<ObjectMapper>
{
    @Inject
    private AproxObjectMapper mapper;

    @Override
    public ObjectMapper getContext( final Class<?> type )
    {
        return mapper;
    }

}
