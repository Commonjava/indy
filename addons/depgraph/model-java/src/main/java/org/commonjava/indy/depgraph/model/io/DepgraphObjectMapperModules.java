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
package org.commonjava.indy.depgraph.model.io;

import com.fasterxml.jackson.databind.Module;
import org.commonjava.indy.model.core.io.ModuleSet;
import org.commonjava.indy.model.core.io.SimpleModuleSet;
import org.commonjava.maven.atlas.graph.jackson.ProjectRelationshipSerializerModule;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.NeoSpecificProjectRelationshipSerializerModule;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.NeoSpecificProjectVersionRefSerializerModule;
import org.commonjava.maven.atlas.ident.jackson.ProjectVersionRefSerializerModule;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Named;
import java.util.Arrays;

/**
 * Created by jdcasey on 8/20/15.
 */
@ApplicationScoped
public class DepgraphObjectMapperModules
{

    private final NeoSpecificProjectVersionRefSerializerModule neoRefModule;

    private final ProjectVersionRefSerializerModule refModule;

    private ProjectRelationshipSerializerModule relModule;

    private NeoSpecificProjectRelationshipSerializerModule neoRelModule;

    public DepgraphObjectMapperModules()
    {
        refModule = ProjectVersionRefSerializerModule.INSTANCE;
        relModule = ProjectRelationshipSerializerModule.INSTANCE;
        neoRelModule = NeoSpecificProjectRelationshipSerializerModule.INSTANCE;
        neoRefModule = NeoSpecificProjectVersionRefSerializerModule.INSTANCE;
    }

    public Iterable<Module> getSerializerModules()
    {
        return Arrays.asList( refModule, relModule, neoRelModule, neoRefModule );
    }

    @Produces
    @Named( "depgraph-module-set" )
    public ModuleSet getModuleSet()
    {
        return new SimpleModuleSet( refModule, relModule, neoRelModule, neoRefModule );
    }

    @Produces
    @Default
    public ProjectRelationshipSerializerModule getRelationshipModule()
    {
        return relModule;
    }

    @Produces
    @Default
    public NeoSpecificProjectRelationshipSerializerModule getNeoRelationshipModule()
    {
        return neoRelModule;
    }

    @Produces
    @Default
    public NeoSpecificProjectVersionRefSerializerModule getNeoRefModule()
    {
        return neoRefModule;
    }

}
