/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.commonjava.aprox.depgraph.maven;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.resolution.ModelResolver;
import org.commonjava.aprox.subsys.maven.MavenComponentDefinition;
import org.commonjava.aprox.subsys.maven.MavenComponentDefinitions;
import org.commonjava.aprox.subsys.maven.MavenComponentException;
import org.commonjava.aprox.subsys.maven.MavenComponentManager;

@javax.enterprise.context.ApplicationScoped
public class MavenComponentProvider
    implements MavenComponentDefinitions
{

    /*@formatter:off*/
    private static final Set<MavenComponentDefinition<?, ?>> DEFINITIONS =
        Collections.unmodifiableSet( 
            Collections.<MavenComponentDefinition<?, ?>> singleton( 
                new MavenComponentDefinition<ModelResolver, ArtifactStoreModelResolver>( ModelResolver.class, ArtifactStoreModelResolver.class, "aprox" )
            )
        );
    /*@formatter:on*/

    @Inject
    private MavenComponentManager componentManager;

    @Produces
    @Default
    public ModelReader getModelReader()
    {
        return new DefaultModelReader();
    }

    @Produces
    @Default
    public ModelBuilder getModelBuilder()
        throws MavenComponentException
    {
        return componentManager.getComponent( ModelBuilder.class );
    }

    @Override
    public Iterator<MavenComponentDefinition<?, ?>> iterator()
    {
        return DEFINITIONS.iterator();
    }

    @Override
    public Set<MavenComponentDefinition<?, ?>> getComponents()
    {
        return DEFINITIONS;
    }

}
