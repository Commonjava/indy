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

import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.common.version.InvalidVersionSpecificationException;
import org.apache.maven.graph.common.version.VersionSpec;
import org.apache.maven.graph.effective.EProjectRelationships;
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
import org.commonjava.aprox.model.Repository;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.rest.util.ArtifactPathInfo;
import org.commonjava.aprox.tensor.maven.ArtifactStoreModelResolver;
import org.commonjava.aprox.tensor.maven.PropertyExpressionResolver;
import org.commonjava.aprox.tensor.maven.StoreModelSource;
import org.commonjava.aprox.tensor.maven.TensorModelCache;
import org.commonjava.tensor.data.TensorDataException;
import org.commonjava.tensor.data.TensorDataManager;
import org.commonjava.tensor.util.MavenModelProcessor;
import org.commonjava.util.logging.Logger;

public class TensorStorageListenerRunnable
    implements Runnable
{

    private static final String FOUND_IN_METADATA = "found-in-repo";

    private final Logger logger = new Logger( getClass() );

    private final StoreDataManager aprox;

    private final ModelReader modelReader;

    private final ModelBuilder modelBuilder;

    private final FileManager fileManager;

    private final MavenModelProcessor modelProcessor;

    private final TensorDataManager dataManager;

    private final TensorModelCache tensorModelCache;

    private final FileAccessEvent event;

    public TensorStorageListenerRunnable( final StoreDataManager aprox, final ModelReader modelReader,
                                          final ModelBuilder modelBuilder, final FileManager fileManager,
                                          final MavenModelProcessor modelProcessor,
                                          final TensorDataManager dataManager, final TensorModelCache tensorModelCache,
                                          final FileAccessEvent event )
    {
        this.aprox = aprox;
        this.modelReader = modelReader;
        this.modelBuilder = modelBuilder;
        this.fileManager = fileManager;
        this.modelProcessor = modelProcessor;
        this.dataManager = dataManager;
        this.tensorModelCache = tensorModelCache;
        this.event = event;
    }

    @Override
    public void run()
    {
        //        logger.info( "[RUN] TensorStorageListenerRunnable for: %s", event );

        final String path = event.getStorageItem()
                                 .getPath();
        if ( !path.endsWith( ".pom" ) )
        {
            return;
        }

        final StoreKey key = event.getStorageItem()
                                  .getStoreKey();

        ArtifactStore originatingStore;
        try
        {
            originatingStore = aprox.getArtifactStore( key );
        }
        catch ( final ProxyDataException e )
        {
            logger.error( "Failed to retrieve store for: %s. Reason: %s", e, key, e.getMessage() );
            return;
        }

        //        logger.info( "Logging: %s with Tensor relationship-graphing system.", event );
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

        if ( !shouldStore( rawModel, path ) )
        {
            return;
        }

        final Model effectiveModel = loadEffectiveModel( event, stores );
        if ( effectiveModel == null )
        {
            return;
        }

        try
        {
            // TODO: Pass on the profiles that were activated when the effective model was built.
            final EProjectRelationships rels = modelProcessor.storeModelRelationships( effectiveModel );

            if ( originatingStore instanceof Repository )
            {
                final Repository repo = (Repository) originatingStore;

                final Map<String, String> metadata = dataManager.getMetadata( rels.getKey() );
                String foundIn = null;
                if ( metadata != null )
                {
                    foundIn = metadata.get( FOUND_IN_METADATA );
                }

                if ( foundIn == null )
                {
                    foundIn = "";
                }

                if ( foundIn.length() > 0 )
                {
                    foundIn += ",";
                }

                foundIn += repo.getName() + "@" + repo.getUrl();

                dataManager.addMetadata( rels.getKey(), FOUND_IN_METADATA, foundIn );
            }
        }
        catch ( final TensorDataException e )
        {
            final ArtifactPathInfo pathInfo = ArtifactPathInfo.parse( path );

            logger.error( "Failed to store relationships for POM: %s (ID from pathInfo: %s). Reason: %s", e,
                          effectiveModel.getId(), pathInfo == null ? "NONE" : pathInfo.getProjectId(), e.getMessage() );

            if ( pathInfo != null )
            {
                logProjectError( pathInfo.getGroupId(), pathInfo.getArtifactId(), pathInfo.getVersion(), e, path );
            }
            // TODO: Disable for some time period...
        }
    }

    // TODO: Somehow, we're ending up in an infinite loop for maven poms...
    private boolean shouldStore( final Model rawModel, final String path )
    {
        final Parent parent = rawModel.getParent();

        String g = rawModel.getGroupId();
        final String a = rawModel.getArtifactId();

        final PropertyExpressionResolver expressionResolver = new PropertyExpressionResolver( rawModel.getProperties() );

        // NOTE: This is BAD, but the fact is some POMs have an expression for their version.
        String v = expressionResolver.resolve( rawModel.getVersion() );

        if ( parent != null )
        {
            if ( g == null )
            {
                g = parent.getGroupId();
            }

            if ( v == null )
            {
                v = expressionResolver.resolve( parent.getVersion() );
            }
        }

        try
        {
            final ProjectVersionRef ref = new ProjectVersionRef( g, a, v );
            final VersionSpec versionSpec = ref.getVersionSpec();

            // If this is a snapshot version, store it again in order to update it.
            // NOTE: We need a way to flush out the old relationships reliably when updating!
            final boolean concrete = versionSpec.isConcrete();

            final boolean contains = dataManager.contains( ref );

            boolean hasError = false;
            try
            {
                hasError = dataManager.hasErrors( ref );
            }
            catch ( final TensorDataException e )
            {
                logArtifactError( path, e );
            }

            return !hasError && ( !concrete || !contains );
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            final ArtifactPathInfo pathInfo = ArtifactPathInfo.parse( path );
            logger.error( "Failed to parse version for: %s (ID from pathInfo: %s). Error: %s", e, rawModel.getId(),
                          pathInfo == null ? "NONE" : pathInfo.getProjectId(), e.getMessage() );

            if ( pathInfo != null )
            {
                logProjectError( pathInfo.getGroupId(), pathInfo.getArtifactId(), pathInfo.getVersion(), e, path );
            }
        }
        //        catch ( final TensorDataException e )
        //        {
        //            logger.error( "Failed to check whether Tensor has captured: %s. Error: %s", e, rawModel.getId(),
        //                          e.getMessage() );
        //            logProjectError( g, a, v, e );
        //        }

        return false;
    }

    protected Model loadEffectiveModel( final FileEvent event, final List<ArtifactStore> stores )
    {
        final ModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setValidationLevel( ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL );
        request.setModelCache( tensorModelCache );
        request.setModelSource( new StoreModelSource( event.getStorageItem(), false ) );
        request.setModelResolver( new ArtifactStoreModelResolver( fileManager, stores, false ) );
        request.setSystemProperties( System.getProperties() );

        final String path = event.getStorageItem()
                                 .getPath();

        ModelBuildingResult result = null;
        try
        {
            result = modelBuilder.build( request );

            if ( result.getProblems() != null )
            {
                // TODO
            }
        }
        catch ( final ModelBuildingException e )
        {
            logger.error( "Cannot build model instance for POM: %s. Reason: %s", e, path, e.getMessage() );
            logArtifactError( path, e );
        }

        if ( result == null )
        {
            return null;
        }

        // TODO: Pass back the profiles that were activated when the effective model was built, for inclusion in the graph facts.
        return result.getEffectiveModel();
    }

    private void logProjectError( final String g, final String a, final String v, final Throwable e, final String path )
    {
        try
        {
            dataManager.addError( new ProjectVersionRef( g, a, v ), e );
        }
        catch ( final TensorDataException e1 )
        {
            logger.error( "Failed to log error for POM: %s. Reason: %s", e1, path, e1.getMessage() );
        }
    }

    private void logArtifactError( final String path, final Throwable e )
    {
        final ArtifactPathInfo info = ArtifactPathInfo.parse( path );
        if ( info == null )
        {
            logger.error( "Path does not appear to be an artifact: %s", path );
        }
        else
        {
            try
            {
                dataManager.addError( new ProjectVersionRef( info.getGroupId(), info.getArtifactId(), info.getVersion() ),
                                      e );
            }
            catch ( final TensorDataException e1 )
            {
                logger.error( "Failed to log error for POM: %s. Reason: %s", e1, path, e1.getMessage() );
            }
        }
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
                          .openInputStream( false );

            return modelReader.read( stream, options );
        }
        catch ( final ModelParseException e )
        {
            logger.error( "Cannot parse POM: %s. Reason: %s", e, path, e.getMessage() );
            logArtifactError( path, e );
        }
        catch ( final IOException e )
        {
            logger.error( "Cannot read POM: %s. Reason: %s", e, path, e.getMessage() );
            logArtifactError( path, e );
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
