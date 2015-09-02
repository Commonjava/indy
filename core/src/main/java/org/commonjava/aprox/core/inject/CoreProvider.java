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
package org.commonjava.aprox.core.inject;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.commonjava.aprox.inject.Production;
import org.commonjava.aprox.model.core.io.AproxObjectMapper;

import com.fasterxml.jackson.databind.Module;
import org.commonjava.aprox.model.core.io.ModuleSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class CoreProvider
{

    @Inject
    private Instance<Module> objectMapperModules;

    @Inject
    private Instance<ModuleSet> objectMapperModuleSets;

    private AproxObjectMapper objectMapper;

    public CoreProvider()
    {
    }

    @PostConstruct
    public void init()
    {
        this.objectMapper = new AproxObjectMapper( objectMapperModules, objectMapperModuleSets );
    }

    @Produces
    @Default
    @Production
    public AproxObjectMapper getAproxObjectMapper()
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.info( "Core mapper is: {}", objectMapper );
        return objectMapper;
    }

}
