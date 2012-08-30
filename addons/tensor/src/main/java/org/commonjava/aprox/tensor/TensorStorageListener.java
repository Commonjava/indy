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
package org.commonjava.aprox.tensor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingResult;
import org.apache.maven.model.io.ModelParseException;
import org.apache.maven.model.io.ModelReader;
import org.commonjava.aprox.core.change.event.FileStorageEvent;
import org.commonjava.aprox.core.change.event.FileStorageEvent.Type;
import org.commonjava.aprox.core.data.ProxyDataException;
import org.commonjava.aprox.core.data.StoreDataManager;
import org.commonjava.aprox.core.model.ArtifactStore;
import org.commonjava.aprox.core.model.Group;
import org.commonjava.aprox.core.rest.util.FileManager;
import org.commonjava.aprox.tensor.maven.ArtifactStoreModelResolver;
import org.commonjava.aprox.tensor.maven.ModelVersions;
import org.commonjava.tensor.data.TensorDataException;
import org.commonjava.tensor.util.MavenModelProcessor;
import org.commonjava.util.logging.Logger;

@Singleton
public class TensorStorageListener
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private StoreDataManager aprox;

    @Inject
    private ModelReader modelReader;

    @Inject
    private ModelBuilder modelBuilder;

    @Inject
    private FileManager fileManager;

    @Inject
    private MavenModelProcessor modelProcessor;

    public void handleFileEvent( @Observes final FileStorageEvent event )
    {
        if ( Type.GENERATE == event.getType() )
        {
            return;
        }

        if ( !event.getPath()
                   .endsWith( ".pom" ) )
        {
            return;
        }

        logger.info( "Processing direct project relationships for: %s", event );
        final ArtifactStore originatingStore = event.getStore();
        final List<ArtifactStore> stores = getRelevantStores( originatingStore );
        if ( stores == null )
        {
            return;
        }

        final Model rawModel = loadRawModel( event );
        if ( rawModel == null )
        {
            return;
        }

        final Model effectiveModel = loadEffectiveModel( event, stores );
        if ( effectiveModel == null )
        {
            return;
        }

        final ModelVersions versions = new ModelVersions( effectiveModel );
        versions.update( rawModel );

        try
        {
            modelProcessor.storeModelRelationships( rawModel );
        }
        catch ( final TensorDataException e )
        {
            logger.error( "Failed to store relationships for POM: %s. Reason: %s", e, rawModel.getId(), e.getMessage() );
        }
    }

    protected Model loadEffectiveModel( final FileStorageEvent event, final List<ArtifactStore> stores )
    {
        final ModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setValidationLevel( ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL );
        request.setPomFile( new File( event.getStorageLocation() ) );
        request.setModelResolver( new ArtifactStoreModelResolver( fileManager, stores ) );

        ModelBuildingResult result = null;
        try
        {
            result = modelBuilder.build( request );
        }
        catch ( final ModelBuildingException e )
        {
            logger.error( "Cannot build model instance for POM: %s. Reason: %s", e, event.getPath(), e.getMessage() );
        }

        if ( result == null )
        {
            return null;
        }

        return result.getEffectiveModel();
    }

    protected Model loadRawModel( final FileStorageEvent event )
    {

        final Map<String, Object> options = new HashMap<String, Object>();
        options.put( ModelReader.IS_STRICT, Boolean.FALSE.toString() );

        try
        {
            return modelReader.read( new File( event.getStorageLocation() ), options );
        }
        catch ( final ModelParseException e )
        {
            logger.error( "Cannot parse POM: %s. Reason: %s", e, event.getPath(), e.getMessage() );
        }
        catch ( final IOException e )
        {
            logger.error( "Cannot read POM: %s. Reason: %s", e, event.getPath(), e.getMessage() );
        }

        return null;
    }

    protected List<ArtifactStore> getRelevantStores( final ArtifactStore originatingStore )
    {
        List<ArtifactStore> stores = new ArrayList<ArtifactStore>();
        stores.add( originatingStore );

        try
        {
            final Set<? extends Group> groups = aprox.getGroupsContaining( originatingStore.getKey() );
            for ( final Group group : groups )
            {
                if ( group == null )
                {
                    continue;
                }

                final List<? extends ArtifactStore> orderedStores =
                    aprox.getOrderedConcreteStoresInGroup( group.getName() );

                if ( orderedStores != null )
                {
                    for ( final ArtifactStore as : orderedStores )
                    {
                        if ( as == null || stores.contains( as ) )
                        {
                            continue;
                        }

                        stores.add( as );
                    }
                }
            }
        }
        catch ( final ProxyDataException e )
        {
            logger.error( "Cannot lookup full store list for groups containing artifact store: %s. Reason: %s", e,
                          originatingStore.getKey(), e.getMessage() );
            stores = null;
        }

        return stores;
    }
}
