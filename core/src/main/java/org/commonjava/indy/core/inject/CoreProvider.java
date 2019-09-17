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
package org.commonjava.indy.core.inject;

import com.fasterxml.jackson.databind.Module;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.model.core.io.ModuleSet;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

@ApplicationScoped
public class CoreProvider
{

    @Inject
    private Instance<Module> objectMapperModules;

    @Inject
    private Instance<ModuleSet> objectMapperModuleSets;

    private IndyObjectMapper objectMapper;

    public CoreProvider()
    {
    }

    @PostConstruct
    public void init()
    {
        this.objectMapper = new IndyObjectMapper( objectMapperModules, objectMapperModuleSets );
    }

    @Produces
    @Default
//    @Production
    public IndyObjectMapper getIndyObjectMapper()
    {
//        Logger logger = LoggerFactory.getLogger( getClass() );
//        logger.info( "Core mapper is: {}", objectMapper );
        return objectMapper;
    }

}
