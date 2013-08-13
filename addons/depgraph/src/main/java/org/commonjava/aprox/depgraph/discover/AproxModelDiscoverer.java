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
package org.commonjava.aprox.depgraph.discover;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingResult;
import org.apache.maven.model.io.ModelParseException;
import org.apache.maven.model.io.ModelReader;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.depgraph.maven.ArtifactStoreModelResolver;
import org.commonjava.aprox.depgraph.maven.DepgraphModelCache;
import org.commonjava.aprox.depgraph.maven.PropertyExpressionResolver;
import org.commonjava.aprox.depgraph.maven.StoreModelSource;
import org.commonjava.aprox.depgraph.util.AproxDepgraphUtils;
import org.commonjava.aprox.filer.FileManager;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.Repository;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.rest.util.ArtifactPathInfo;
import org.commonjava.aprox.util.LocationUtils;
import org.commonjava.maven.atlas.graph.model.EProjectKey;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.VersionSpec;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.maven.cartographer.discover.DiscoveryResult;
import org.commonjava.maven.cartographer.util.MavenModelProcessor;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.util.logging.Logger;

@ApplicationScoped
public class AproxModelDiscoverer
{

    private static final String FOUND_IN_METADATA = "found-in-repo";

    private static final int MAX_RETRIES = 5;

    private static final Random RAND = new Random();

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
    private CartoDataManager dataManager;

    @Inject
    private DepgraphModelCache modelCache;

    public AproxModelDiscoverer()
    {
    }

    public AproxModelDiscoverer( final StoreDataManager aprox, final ModelReader modelReader,
                                 final ModelBuilder modelBuilder, final FileManager fileManager,
                                 final MavenModelProcessor modelProcessor, final CartoDataManager dataManager,
                                 final DepgraphModelCache modelCache )
    {
        this.aprox = aprox;
        this.modelReader = modelReader;
        this.modelBuilder = modelBuilder;
        this.fileManager = fileManager;
        this.modelProcessor = modelProcessor;
        this.dataManager = dataManager;
        this.modelCache = modelCache;
    }

    public DiscoveryResult discoverRelationships( final Transfer item )
        throws CartoDataException
    {
        final String path = item.getPath();

        if ( !path.endsWith( ".pom" ) )
        {
            return null;
        }

        final StoreKey key = LocationUtils.getKey( item );

        ArtifactStore originatingStore;
        try
        {
            originatingStore = aprox.getArtifactStore( key );
        }
        catch ( final ProxyDataException e )
        {
            throw new CartoDataException( "Failed to retrieve store for: %s. Reason: %s", e, key, e.getMessage() );
        }

        //        logger.info( "Logging: %s with Tensor relationship-graphing system.", event );
        final List<ArtifactStore> stores = getRelevantStores( originatingStore );
        if ( stores == null )
        {
            throw new CartoDataException( "No stores found for: %s.", key );
        }

        final URI source = AproxDepgraphUtils.toDiscoveryURI( originatingStore.getKey() );

        final Model rawModel = loadRawModel( item, source );
        if ( !shouldStore( rawModel, source, path ) )
        {
            return null;
        }

        final Model effectiveModel = loadEffectiveModel( item, source, stores );
        if ( effectiveModel == null )
        {
            // TODO: Will this ever actually be null??
            return null;
        }

        boolean retry = false;
        int count = 0;
        do
        {
            retry = false;

            try
            {
                // TODO: We only need to translate the Model into a series of ProjectRelationship's once...optimize these API calls!
                return storeRelationships( effectiveModel, source, originatingStore, path );
            }
            catch ( final RuntimeException e )
            {
                if ( e.getClass()
                      .getSimpleName()
                      .contains( "Deadlock" ) )
                {
                    final long standoff = ( Math.abs( RAND.nextInt() ) % 8 ) * 4000;
                    logger.warn( "Detected deadlock scenario; retrying relationship storage for: %s in %d ms.",
                                 effectiveModel.getId(), standoff );

                    try
                    {
                        Thread.sleep( standoff );
                    }
                    catch ( final InterruptedException ie )
                    {
                        break;
                    }

                    retry = true;
                }
                else
                {
                    throw e;
                }
            }
            count++;

            if ( count >= MAX_RETRIES )
            {
                logger.error( "Failed to store relationships for %s after %s attempts. Giving up.",
                              effectiveModel.getId(), count );
            }
        }
        while ( retry && count < MAX_RETRIES );

        throw new RetryFailedException(
                                        "Failed to store relationships in %d tries. Database deadlocks prevented storage.",
                                        MAX_RETRIES );
    }

    private DiscoveryResult storeRelationships( final Model effectiveModel, final URI source,
                                                final ArtifactStore originatingStore, final String path )
    {
        try
        {
            // TODO: Pass on the profiles that were activated when the effective model was built.
            final DiscoveryResult result = modelProcessor.storeModelRelationships( effectiveModel, source );

            if ( originatingStore instanceof Repository )
            {
                final Repository repo = (Repository) originatingStore;

                final Map<String, String> metadata = dataManager.getMetadata( result.getSelectedRef() );
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

                dataManager.addMetadata( result.getSelectedRef(), FOUND_IN_METADATA, foundIn );
            }

            return result;
        }
        catch ( final CartoDataException e )
        {
            final ArtifactPathInfo pathInfo = ArtifactPathInfo.parse( path );

            logger.error( "Failed to store relationships for POM: %s (ID from pathInfo: %s). Reason: %s", e,
                          effectiveModel.getId(), pathInfo == null ? "NONE" : pathInfo.getProjectId(), e.getMessage() );

            if ( pathInfo != null )
            {
                logProjectError( source, pathInfo.getGroupId(), pathInfo.getArtifactId(), pathInfo.getVersion(), e,
                                 path );
            }
            // TODO: Disable for some time period...
        }

        return null;
    }

    // TODO: Somehow, we're ending up in an infinite loop for maven poms...
    private boolean shouldStore( final Model rawModel, final URI source, final String path )
    {
        final ArtifactPathInfo pathInfo = ArtifactPathInfo.parse( path );

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

        //        try
        //        {
        final ProjectVersionRef ref = new ProjectVersionRef( g, a, v );

        final ProjectVersionRef parsed =
            new ProjectVersionRef( pathInfo.getGroupId(), pathInfo.getArtifactId(), pathInfo.getVersion() );

        if ( !parsed.equals( ref ) )
        {
            logProjectError( source,
                             pathInfo.getGroupId(),
                             pathInfo.getArtifactId(),
                             pathInfo.getVersion(),
                             new CartoDataException(
                                                     "Coordinate from POM: '%s' doesn't match coordinate parsed from path: '%s'",
                                                     ref, parsed ), path );

            return false;
        }

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
        catch ( final CartoDataException e )
        {
            logger.error( "Failed to retrieve errors for project: %s. Reason: %s", e, ref, e.getMessage() );
            hasError = true;
        }

        return !hasError && ( !concrete || !contains );
        //        }
        //        catch ( final InvalidVersionSpecificationException e )
        //        {
        //            logger.error( "Failed to parse version for: %s (ID from pathInfo: %s). Error: %s", e, rawModel.getId(),
        //                          pathInfo == null ? "NONE" : pathInfo.getProjectId(), e.getMessage() );
        //
        //            if ( pathInfo != null )
        //            {
        //                logProjectError( source, pathInfo.getGroupId(), pathInfo.getArtifactId(), pathInfo.getVersion(), e,
        //                                 path );
        //            }
        //        }
        //        catch ( final CartoDataException e )
        //        {
        //            logger.error( "Failed to check whether Tensor has captured: %s. Error: %s", e, rawModel.getId(),
        //                          e.getMessage() );
        //            logProjectError( g, a, v, e );
        //        }

        //        return false;
    }

    protected Model loadEffectiveModel( final Transfer item, final URI source, final List<ArtifactStore> stores )
        throws CartoDataException
    {
        final ModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setValidationLevel( ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL );
        request.setModelCache( modelCache );
        request.setModelSource( new StoreModelSource( item, false ) );
        request.setModelResolver( new ArtifactStoreModelResolver( fileManager, stores, false ) );
        request.setSystemProperties( System.getProperties() );

        final String path = item.getPath();

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
            logArtifactError( source, path, e );
            throw new CartoDataException( "Cannot build model instance for POM: %s. Reason: %s", e, path,
                                          e.getMessage() );
        }

        return result.getEffectiveModel();
    }

    private void logProjectError( final URI source, final String g, final String a, final String v, final Throwable e,
                                  final String path )
    {
        try
        {
            dataManager.addError( new EProjectKey( source, new ProjectVersionRef( g, a, v ) ), e );
        }
        catch ( final CartoDataException e1 )
        {
            logger.error( "Failed to log error for POM: %s. Reason: %s", e1, path, e1.getMessage() );
        }
    }

    private void logArtifactError( final URI source, final String path, final Throwable e )
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
                dataManager.addError( new EProjectKey( source, new ProjectVersionRef( info.getGroupId(),
                                                                                      info.getArtifactId(),
                                                                                      info.getVersion() ) ), e );
            }
            catch ( final CartoDataException e1 )
            {
                logger.error( "Failed to log error for POM: %s. Reason: %s", e1, path, e1.getMessage() );
            }
        }
    }

    protected Model loadRawModel( final Transfer item, final URI source )
        throws CartoDataException
    {

        final Map<String, Object> options = new HashMap<String, Object>();
        options.put( ModelReader.IS_STRICT, Boolean.FALSE.toString() );

        final String path = item.getPath();

        InputStream stream = null;
        try
        {
            stream = item.openInputStream( false );

            return modelReader.read( stream, options );
        }
        catch ( final ModelParseException e )
        {
            logArtifactError( source, path, e );
            throw new CartoDataException( "Cannot parse POM: %s. Reason: %s", e, path, e.getMessage() );
        }
        catch ( final IOException e )
        {
            logArtifactError( source, path, e );
            throw new CartoDataException( "Cannot read POM: %s. Reason: %s", e, path, e.getMessage() );
        }
        finally
        {
            closeQuietly( stream );
        }
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

    public DiscoveryResult getDiscoveryResult()
    {
        return null;
    }
}
