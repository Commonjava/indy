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
package org.commonjava.aprox.tensor.maven;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;

import org.apache.maven.mae.MAEException;
import org.apache.maven.mae.app.AbstractMAEApplication;
import org.apache.maven.mae.internal.container.ComponentSelector;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.resolution.ModelResolver;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

@javax.enterprise.context.ApplicationScoped
public class MavenComponentProvider
{

    private MAEApp app;

    @PostConstruct
    public void startMAE()
        throws MAEException
    {
        app = new MAEApp();
        app.startMAE();
    }

    @Produces
    @Default
    public ModelReader getModelReader()
    {
        return app.getModelReader();
    }

    @Produces
    @Default
    public ModelBuilder getModelBuilder()
    {
        return app.getModelBuilder();
    }

    @Component( role = MAEApp.class )
    private static class MAEApp
        extends AbstractMAEApplication
    {
        @Requirement
        private ModelBuilder modelBuilder;

        public void startMAE()
            throws MAEException
        {
            load();
        }

        ModelReader getModelReader()
        {
            return new DefaultModelReader();
        }

        ModelBuilder getModelBuilder()
        {
            return modelBuilder;
        }

        @Override
        public String getId()
        {
            return getName();
        }

        @Override
        public String getName()
        {
            return "AProx-Tensor-Integration";
        }

        @Override
        public ComponentSelector getComponentSelector()
        {
            return new ComponentSelector().setSelection( ModelResolver.class, "aprox" );
        }
    }

}
