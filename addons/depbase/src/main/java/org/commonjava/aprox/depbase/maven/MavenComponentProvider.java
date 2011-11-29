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
package org.commonjava.aprox.depbase.maven;

import javax.annotation.PostConstruct;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.mae.MAEException;
import org.apache.maven.mae.app.AbstractMAEApplication;
import org.apache.maven.mae.boot.embed.MAEEmbedderBuilder;
import org.apache.maven.mae.internal.container.ComponentSelector;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.resolution.ModelResolver;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

import com.google.inject.Provides;

@Singleton
@Component( role = MavenComponentProvider.class )
public class MavenComponentProvider
    extends AbstractMAEApplication
{

    @Requirement
    private ModelBuilder modelBuilder;

    @Provides
    @Named( "MAE" )
    public ModelBuilder getModelBuilder()
    {
        return modelBuilder;
    }

    @PostConstruct
    public void loadMAE()
    {
        try
        {
            super.load();
        }
        catch ( final MAEException e )
        {
            throw new RuntimeException( "Failed to initialize MAE container. Cannot load Maven components. Reason: "
                + e.getMessage(), e );
        }
    }

    @Override
    public String getId()
    {
        return "depbase-aprox";
    }

    @Override
    public String getName()
    {
        return "DepBase-AProx-Bridge";
    }

    @Override
    protected void configureBuilder( final MAEEmbedderBuilder builder )
        throws MAEException
    {
        builder.withClassScanningEnabled( false );
        super.configureBuilder( builder );
    }

    @Override
    public ComponentSelector getComponentSelector()
    {
        return new ComponentSelector().setSelection( ModelResolver.class, "aprox" );
    }

}
