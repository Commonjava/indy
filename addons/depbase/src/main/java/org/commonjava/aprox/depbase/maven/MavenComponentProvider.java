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
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.apache.maven.model.building.ModelBuilder;
import org.commonjava.util.logging.Logger;

@Singleton
public class MavenComponentProvider
{
    private final Logger logger = new Logger( getClass() );

    private MAELoader mae;

    @Produces
    @Default
    public ModelBuilder getModelBuilder()
    {
        return mae.getModelBuilder();
    }

    public MavenComponentProvider()
    {
        logger.info( "Constructor for MavenComponentProvider..." );
    }

    @PostConstruct
    public void loadMAE()
    {
        logger.info( "Loading MAE components..." );
        mae = new MAELoader();

        logger.info( "ModelBuilder is: %s", mae.getModelBuilder() );
    }

}
