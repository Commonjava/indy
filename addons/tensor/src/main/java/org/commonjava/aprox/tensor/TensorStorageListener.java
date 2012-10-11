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

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.common.version.InvalidVersionSpecificationException;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingResult;
import org.apache.maven.model.io.ModelParseException;
import org.apache.maven.model.io.ModelReader;
import org.commonjava.aprox.change.event.FileAccessEvent;
import org.commonjava.aprox.change.event.FileEvent;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.filer.FileManager;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.tensor.maven.ArtifactStoreModelResolver;
import org.commonjava.aprox.tensor.maven.ModelVersions;
import org.commonjava.aprox.tensor.maven.StoreModelSource;
import org.commonjava.tensor.data.TensorDataException;
import org.commonjava.tensor.data.TensorDataManager;
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

    @Inject
    private TensorDataManager dataManager;

    public void handleFileAccessEvent( @Observes final FileAccessEvent event )
    {
        final String path = event.getStorageItem()
                                 .getPath();
        if ( !path.endsWith( ".pom" ) )
        {
            return;
        }

        final ArtifactStore originatingStore = getStore( event.getStorageItem()
                                                              .getStoreKey() );

        logger.info( "Logging: %s with Tensor relationship-graphing system.", event );
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

        if ( !shouldStore( rawModel ) )
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
            // TODO: Pass on the profiles that were activated when the effective model was built.
            modelProcessor.storeModelRelationships( effectiveModel );
        }
        catch ( final TensorDataException e )
        {
            logger.error( "Failed to store relationships for POM: %s. Reason: %s", e, effectiveModel.getId(),
                          e.getMessage() );
        }
    }

    private ArtifactStore getStore( final StoreKey key )
    {
        try
        {
            switch ( key.getType() )
            {
                case deploy_point:
                {
                    return aprox.getDeployPoint( key.getName() );
                }
                case group:
                {
                    return aprox.getGroup( key.getName() );
                }
                default:
                {
                    return aprox.getRepository( key.getName() );
                }
            }
        }
        catch ( final ProxyDataException e )
        {
            logger.error( "Failed to retrieve store for: %s. Reason: %s", e, key, e.getMessage() );
        }

        return null;
    }

    private boolean shouldStore( final Model rawModel )
    {
        final Parent parent = rawModel.getParent();

        String g = rawModel.getGroupId();
        final String a = rawModel.getArtifactId();
        String v = rawModel.getVersion();

        if ( parent != null )
        {
            if ( g == null )
            {
                g = parent.getGroupId();
            }

            if ( v == null )
            {
                v = parent.getVersion();
            }
        }

        try
        {
            final ProjectVersionRef ref = new ProjectVersionRef( g, a, v );

            // If this is a snapshot version, store it again in order to update it.
            // NOTE: We need a way to flush out the old relationships reliably when updating!
            final boolean concrete = ref.getVersionSpec()
                                        .isConcrete();

            final boolean contains = dataManager.contains( ref );

            return !concrete || !contains;
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            logger.error( "Failed to parse version for: %s. Error: %s", e, rawModel.getId(), e.getMessage() );
        }
        catch ( final TensorDataException e )
        {
            logger.error( "Failed to check whether Tensor has captured: %s. Error: %s", e, rawModel.getId(),
                          e.getMessage() );
        }

        return false;
    }

    protected Model loadEffectiveModel( final FileEvent event, final List<ArtifactStore> stores )
    {
        final ModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setValidationLevel( ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL );
        request.setModelSource( new StoreModelSource( event.getStorageItem() ) );
        request.setModelResolver( new ArtifactStoreModelResolver( fileManager, stores ) );
        request.setSystemProperties( System.getProperties() );

        final String path = event.getStorageItem()
                                 .getPath();

        ModelBuildingResult result = null;
        try
        {
            result = modelBuilder.build( request );
        }
        catch ( final ModelBuildingException e )
        {
            logger.error( "Cannot build model instance for POM: %s. Reason: %s", e, path, e.getMessage() );
        }

        if ( result == null )
        {
            return null;
        }

        // TODO: Pass back the profiles that were activated when the effective model was built, for inclusion in the graph facts.
        return result.getEffectiveModel();
    }

    protected Model loadRawModel( final FileEvent event )
    {

        final Map<String, Object> options = new HashMap<String, Object>();
        options.put( ModelReader.IS_STRICT, Boolean.FALSE.toString() );

        final String path = event.getStorageItem()
                                 .getPath();

        InputStream stream = null;
        try
        {
            stream = event.getStorageItem()
                          .openInputStream();
            return modelReader.read( stream, options );
        }
        catch ( final ModelParseException e )
        {
            logger.error( "Cannot parse POM: %s. Reason: %s", e, path, e.getMessage() );
        }
        catch ( final IOException e )
        {
            logger.error( "Cannot read POM: %s. Reason: %s", e, path, e.getMessage() );
        }
        finally
        {
            closeQuietly( stream );
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
