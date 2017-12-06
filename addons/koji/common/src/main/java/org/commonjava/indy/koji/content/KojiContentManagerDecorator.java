/**
 * Copyright (C) 2011-2017 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.koji.content;

import com.redhat.red.build.koji.KojiClient;
import com.redhat.red.build.koji.KojiClientException;
import com.redhat.red.build.koji.model.xmlrpc.KojiArchiveInfo;
import com.redhat.red.build.koji.model.xmlrpc.KojiArchiveQuery;
import com.redhat.red.build.koji.model.xmlrpc.KojiBuildInfo;
import com.redhat.red.build.koji.model.xmlrpc.KojiBuildState;
import com.redhat.red.build.koji.model.xmlrpc.KojiSessionInfo;
import com.redhat.red.build.koji.model.xmlrpc.KojiTagInfo;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.ObjectBasedValueSource;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.commonjava.indy.IndyMetricsNames;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.content.ContentManager;
import org.commonjava.indy.content.index.ContentIndexManager;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.koji.conf.IndyKojiConfig;
import org.commonjava.indy.koji.metrics.IndyMetricsKojiNames;
import org.commonjava.indy.measure.annotation.IndyMetrics;
import org.commonjava.indy.measure.annotation.Measure;
import org.commonjava.indy.measure.annotation.MetricNamed;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.subsys.template.IndyGroovyException;
import org.commonjava.indy.subsys.template.ScriptEngine;
import org.commonjava.indy.util.LocationUtils;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.util.ArtifactPathInfo;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.commonjava.maven.galley.spi.nfc.NotFoundCache;
import org.commonjava.maven.galley.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.inject.Inject;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.commonjava.indy.pkg.maven.content.group.MavenMetadataMerger.METADATA_NAME;

/**
 * {@link ContentManager} decorator that watches the retrieve() methods. If the result is going to be a null {@link Transfer}
 * this decorator will attempt the following:
 * <ol>
 *     <li>Parse the path into a GAV</li>
 *     <li>Lookup matching builds in Koji</li>
 *     <li>Sort the builds by creation timestamp, with earliest Date first.</li>
 *     <li>Iterate through the builds until we find a build we can proxy:
 *       <ol>
 *           <li>List the tags that contain the build</li>
 *           <li>Check that the tag matches a {@link IndyKojiConfig} tag-whitelist pattern</li>
 *           <li>If the tag is accepted, format the storage URL to the build and create a {@link RemoteRepository} to proxy it.</li>
 *       </ol>
 *     </li>
 *     <li>If we have a proxied build repository from above, lookup the target group whose membership should be modified, based on the entrypoint group's name (using {@link IndyKojiConfig} target-groups</li>
 *     <li>Modify the target group's membership. Store the remote repository and the target group.</li>
 *     <li>Attempt to retrieve the requested path from the new proxy repository.</li>
 *     <li>Return the results.</li>
 * </ol>
 *
 * <b>NOTE:</b> Currently we're not attempting to wrap retrieveAll() or retrieveFirst() methods, since these are only
 * used by the PromotionValidationTools class, which exposes methods to promotion validation scripts. This is not the
 * place to be adding new Koji build proxies...
 *
 * Created by jdcasey on 5/20/16.
 */
@Decorator
public abstract class KojiContentManagerDecorator
        implements ContentManager
{
    private Logger logger = LoggerFactory.getLogger( getClass() );

    private static final String CREATION_TRIGGER_GAV = "creation-trigger-GAV";

    private static final String NVR = "koji-NVR";

    public static final String KOJI_ORIGIN = "koji";

    public static final String KOJI_ORIGIN_BINARY = "koji-binary";

    private static final String KOJI_REPO_CREATOR_SCRIPT = "koji-repo-creator.groovy";

    @Delegate
    @Inject
    private ContentManager delegate;

    @Inject
    private StoreDataManager storeDataManager;

    @Inject
    private KojiClient kojiClient;

    @Inject
    private IndyKojiConfig config;

    @Inject
    private ScriptEngine scriptEngine;

    @Inject
    private NotFoundCache nfc;

    @Inject
    private KojiBuildAuthority buildAuthority;

    @Inject
    private ContentIndexManager indexManager;

    public KojiRepositoryCreator createRepoCreator()
    {
        KojiRepositoryCreator creator = null;
        try
        {
            creator = scriptEngine.parseStandardScriptInstance( ScriptEngine.StandardScriptType.store_creators,
                                                                KOJI_REPO_CREATOR_SCRIPT, KojiRepositoryCreator.class );
        }
        catch ( IndyGroovyException e )
        {
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.error( String.format( "Cannot create KojiRepositoryCreator instance: %s. Disabling Koji support.",
                                         e.getMessage() ), e );
            config.setEnabled( false );
        }
        return creator;
    }

    @Override
    @IndyMetrics( measure = @Measure( timers = @MetricNamed( name = IndyMetricsKojiNames.METHOD_CONTENTMANAGER_EXISTS
                    + IndyMetricsNames.TIMER ), meters = @MetricNamed( name =
                    IndyMetricsKojiNames.METHOD_CONTENTMANAGER_EXISTS + IndyMetricsNames.METER ) ) )
    public boolean exists( ArtifactStore store, String path )
            throws IndyWorkflowException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.debug( "KOJI: Delegating initial existence check for: {}/{}", store.getKey(), path );
        boolean result = delegate.exists( store, path );
        if ( !result && StoreType.group == store.getKey().getType() )
        {
            Group group = (Group) store;

            logger.info( "KOJI: Checking whether Koji contains a build matching: {}", path );
            RemoteRepository kojiProxy = findKojiBuildAnd( store, path, new EventMetadata(), null, this::createRemoteRepository );
            if ( kojiProxy != null )
            {
                adjustTargetGroup( kojiProxy, group );
                result = delegate.exists( kojiProxy, path );
            }

            if ( result )
            {
                nfc.clearMissing( new ConcreteResource( LocationUtils.toLocation( store ), path ) );
            }
        }

        return result;
    }

    @Override
    public Transfer retrieve( final ArtifactStore store, final String path )
            throws IndyWorkflowException
    {
        return retrieve( store, path, new EventMetadata() );
    }

    @Override
    @IndyMetrics( measure = @Measure( timers = @MetricNamed( name = IndyMetricsKojiNames.METHOD_CONTENTMANAGER_RETRIEVE
                    + IndyMetricsNames.TIMER ), meters = @MetricNamed( name =
                    IndyMetricsKojiNames.METHOD_CONTENTMANAGER_RETRIEVE + IndyMetricsNames.METER ) ) )
    public Transfer retrieve( final ArtifactStore store, final String path, final EventMetadata eventMetadata )
            throws IndyWorkflowException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.debug( "KOJI: Delegating initial retrieval attempt for: {}/{}", store.getKey(), path );
        Transfer result = delegate.retrieve( store, path, eventMetadata );
        if ( result == null && StoreType.group == store.getKey().getType() )
        {
            logger.info( "KOJI: Checking for Koji build matching: {}", path );
            Group group = (Group) store;

            RemoteRepository kojiProxy = findKojiBuildAnd( store, path, eventMetadata, null, this::createRemoteRepository );
            if ( kojiProxy != null )
            {
                adjustTargetGroup( kojiProxy, group );
                result = delegate.retrieve( kojiProxy, path, eventMetadata );
            }

            if ( result != null )
            {
                nfc.clearMissing( new ConcreteResource( LocationUtils.toLocation( store ), path ) );
            }
        }

        // Finally, pass the Transfer back.
        return result;
    }

    @Override
    public Transfer getTransfer( StoreKey storeKey, String path, TransferOperation op )
            throws IndyWorkflowException
    {
        ArtifactStore store;
        try
        {
            store = storeDataManager.getArtifactStore( storeKey );
        }
        catch ( IndyDataException e )
        {
            throw new IndyWorkflowException( "Cannot retrieve artifact store definition for: %s. Reason: %s", e,
                                             storeKey, e.getMessage() );
        }

        if ( store == null )
        {
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.warn( "No such store: {} (while retrieving transfer for path: {} (op: {})", storeKey, path, op );
            return null;
        }

        return getTransfer( store, path, op );
    }

    @Override
    public Transfer getTransfer( final ArtifactStore store, final String path, final TransferOperation operation )
            throws IndyWorkflowException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.debug( "KOJI: Delegating initial getTransfer() attempt for: {}/{}", store.getKey(), path );
        Transfer result = delegate.getTransfer( store, path, operation );
        if ( result == null && TransferOperation.DOWNLOAD == operation && StoreType.group == store.getKey().getType() )
        {
            logger.info( "KOJI: Checking for Koji build matching: {}", path );
            Group group = (Group) store;

            RemoteRepository kojiProxy = findKojiBuildAnd( store, path, new EventMetadata(), null, this::createRemoteRepository );
            if ( kojiProxy != null )
            {
                adjustTargetGroup( kojiProxy, group );

                EventMetadata eventMetadata =
                        new EventMetadata().set( ContentManager.ENTRY_POINT_STORE, store.getKey() );
                result = delegate.retrieve( kojiProxy, path, eventMetadata );
            }

            if ( result != null && result.exists() )
            {
                nfc.clearMissing( new ConcreteResource( LocationUtils.toLocation( store ), path ) );
            }
        }

        // Finally, pass the Transfer back.
        return result;
    }

    @IndyMetrics( measure = @Measure( timers = @MetricNamed( name =
                    IndyMetricsKojiNames.METHOD_CONTENTMANAGER_EXISTS
                                    + IndyMetricsNames.TIMER ), meters = @MetricNamed( name =
                    IndyMetricsKojiNames.METHOD_CONTENTMANAGER_FINDKOJIBUILDAND
                                    + IndyMetricsNames.METER ) ), exceptions = @Measure( meters = @MetricNamed( name =
                    IndyMetricsKojiNames.METHOD_CONTENTMANAGER_FINDKOJIBUILDAND + IndyMetricsNames.EXCEPTION ) ) )
    private <T> T findKojiBuildAnd( ArtifactStore store, String path, EventMetadata eventMetadata, T defValue, KojiBuildAction<T> action )
            throws IndyWorkflowException
    {
        if ( !config.getEnabled() )
        {
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.debug( "Koji content-manager decorator is disabled." );
            logger.debug("When koji addon is disenabled , path:{},config instance is {}",path,config.toString());
            return defValue;
        }

        Group group = (Group) store;

        if ( !config.isEnabledFor( group.getName() ) )
        {
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.debug( "Koji content-manager decorator not enabled for: {}.", store.getKey() );
            logger.debug("When the group is disenabled , path:{},config instance is {}",path,config.toString());
            return defValue;
        }

        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.debug("When the koji is enabled , path:{},config instance is {}",path,config.toString());
        // TODO: This won't work for maven-metadata.xml files! We need to hit a POM or jar or something first.
        ArtifactPathInfo pathInfo = ArtifactPathInfo.parse( path );
        if ( pathInfo != null )
        {
            ArtifactRef artifactRef = pathInfo.getArtifact();
            logger.info( "Searching for Koji build: {}", artifactRef );

            return proxyKojiBuild( store.getKey(), artifactRef, path, eventMetadata, defValue, action );
        }
        else
        {
            logger.info( "Path is not a maven artifact reference: {}", path );
        }

        return defValue;
    }

    private <T> T proxyKojiBuild( final StoreKey inStore, final ArtifactRef artifactRef, final String path, EventMetadata eventMetadata, T defValue, KojiBuildAction<T> consumer )
            throws IndyWorkflowException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        try
        {
            return kojiClient.withKojiSession( ( session ) -> {
                List<KojiBuildInfo> builds = kojiClient.listBuildsContaining( artifactRef, session );

                Collections.sort( builds, ( build1, build2 ) -> build2.getCreationTime()
                                                                      .compareTo( build1.getCreationTime() ) );

                logger.debug( "Got {} builds from koji. Looking for best match.", builds.size() );

                for ( KojiBuildInfo build : builds )
                {
                    if ( build.getBuildState() != KojiBuildState.COMPLETE )
                    {
                        logger.debug( "Build: {} is not completed. The state is {}. Skipping.",
                                      build.getNvr(), build.getBuildState() );
                        continue;
                    }

                    boolean buildAllowed = false;
                    if ( isBinaryBuild(build) )
                    {
                        // This is not a real build, it's a binary import.
                        if ( config.isProxyBinaryBuilds() )
                        {
                            logger.info("Trying binary build: {} with id: {}", build.getNvr(), build.getId());
                            buildAllowed = true;
                        }
                        else
                        {
                            logger.debug("Skipping binary build: {} with id: {}", build.getNvr(), build.getId());
                        }
                    }
                    else
                    {
                        logger.info("Trying build: {} with id: {}", build.getNvr(), build.getId());
                        if ( !config.isTagPatternsEnabled() )
                        {
                            buildAllowed = true;
                        }
                        else
                        {
                            List<KojiTagInfo> tags = kojiClient.listTags(build.getId(), session);
                            logger.debug("Build is in {} tags...", tags.size());

                            for (KojiTagInfo tag : tags) {
                                // If the tags match patterns configured in whitelist, construct a new remote repo.
                                if (config.isTagAllowed(tag.getName())) {
                                    logger.info("Koji tag is on whitelist: {}", tag.getName());
                                    buildAllowed = true;
                                    break;
                                } else {
                                    logger.debug("Tag: {} is not in the whitelist.", tag.getName());
                                }
                            }
                        }
                    }

                    if (buildAllowed) {
                        // If the authoritative store is not configured, one or both systems is missing MD5 information,
                        // or the artifact matches the one in the authoritative store, go ahead.
                        if (buildAuthority.isAuthorized(path, eventMetadata, artifactRef, build, session)) {
                            return consumer.execute(inStore, artifactRef, build, session);
                        }
                    } else {
                        logger.debug("No whitelisted tags found for: {}", build.getNvr());
                    }
                }

                logger.trace( "No builds were found that matched the restrictions." );

                return defValue;
            } );
        }
        catch ( KojiClientException e )
        {
            throw new IndyWorkflowException( "Cannot retrieve builds for: %s. Error: %s", e, artifactRef,
                                             e.getMessage() );
        }
    }

    private boolean isBinaryBuild( KojiBuildInfo build )
    {
        return build.getTaskId() == null;
    }

    private RemoteRepository createRemoteRepository( StoreKey inStore, ArtifactRef artifactRef, final KojiBuildInfo build,
                                                     final KojiSessionInfo session )
            throws KojiClientException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        try
        {
            KojiArchiveQuery archiveQuery = new KojiArchiveQuery().withBuildId( build.getId() ).withType( "maven" );
            List<KojiArchiveInfo> archives = kojiClient.listArchives( archiveQuery, session );

            boolean isBinaryBuild = isBinaryBuild( build );
            String name = getRepositoryName( build, isBinaryBuild );
            StoreKey remoteKey = new StoreKey( inStore.getPackageType(), StoreType.remote, name );

            RemoteRepository remote = ( RemoteRepository ) storeDataManager.getArtifactStore( remoteKey );
            if ( remote == null )
            {
                // Using a RemoteRepository allows us to use the higher-level APIs in Indy, as opposed to TransferManager
                final KojiRepositoryCreator creator = createRepoCreator();

                if ( creator == null )
                {
                    throw new KojiClientException( "Cannot proceed without a valid KojiRepositoryCreator instance." );
                }

                remote = creator.createRemoteRepository( inStore.getPackageType(), name, formatStorageUrl( build ),
                                                         config.getDownloadTimeoutSeconds() );

                remote.setServerCertPem( config.getServerPemContent() );

                if ( isBinaryBuild )
                {
                    remote.setMetadata( ArtifactStore.METADATA_ORIGIN, KOJI_ORIGIN_BINARY );
                }
                else
                {
                    remote.setMetadata( ArtifactStore.METADATA_ORIGIN, KOJI_ORIGIN );
                }

                // set pathMaskPatterns using build output paths
                Set<String> patterns = new HashSet<>();
                patterns.addAll( archives.stream()
                                         .map( a -> a.getGroupId().replace( '.', '/' ) + "/" + a.getArtifactId()
                                                 + "/" + a.getVersion() + "/" + a.getFilename() )
                                         .collect( Collectors.toSet() ) );

                // pre-index the koji build artifacts and set authoritative index of the remote to let the
                // koji remote repo directly go through the content index
                patterns.forEach( path->indexManager.indexPathInStores( path, remoteKey ) );
                remote.setAuthoritativeIndex( true );
                remote.setPathMaskPatterns( patterns );

                remote.setMetadata( CREATION_TRIGGER_GAV, artifactRef.toString() );
                remote.setMetadata( NVR, build.getNvr() );

                final ChangeSummary changeSummary = new ChangeSummary( ChangeSummary.SYSTEM_USER,
                                                                       "Creating remote repository for Koji build: " + build
                                                                               .getNvr() );

                storeDataManager.storeArtifactStore( remote, changeSummary, false, true, new EventMetadata() );

                logger.debug( "Koji {}, add pathMaskPatterns: {}", name, patterns );
            }
            else
            {
                if ( remote.isDisabled() )
                {
                    logger.info( "Remote repository {} already exists, but is currently disabled. Returning null.",
                                 remoteKey );
                    remote = null;
                }
                else
                {
                    // this is a strange situation - delegate did not find the requested resource while Koji claims it
                    // is contained in this repo and it already exists and is enabled. Possibly corrupted list of path
                    // mask patterns set on the repo or content index
                    logger.warn( "Remote repository {} already exists. Using it as is.", remoteKey );
                }
            }

            return remote;
        }
        catch ( MalformedURLException e )
        {
            throw new KojiClientException( "Koji add-on seems misconifigured. Could not generate URL to repo for "
                                                   + "build: %s\nBase URL: %s\nError: %s", e, build.getNvr(),
                                           config.getStorageRootUrl(), e.getMessage() );
        }
        catch ( IOException e )
        {
            throw new KojiClientException(
                    "Failed to read server SSL PEM information from Koji configuration for new hosted repo: %s", e,
                    e.getMessage() );
        }
        catch ( IndyDataException e )
        {
            throw new KojiClientException( "Failed to store temporary remote repo: %s", e, e.getMessage() );
        }
    }

    private Set<String> getPatterns( ArtifactRef artifactRef, List<KojiArchiveInfo> archives )
    {
        Set<String> patterns = new HashSet<>();
        for ( KojiArchiveInfo a : archives )
        {
            if ( !isVerSignedAllowedWithVersion( artifactRef.getVersionStringRaw() ) )
            {
                continue;
            }
            String pattern = getPatternString( artifactRef, a );
            if ( pattern != null )
            {
                patterns.add( pattern );
            }
        }
        if ( !patterns.isEmpty() )
        {
            String meta = getMetaString( artifactRef ); // Add metadata.xml to path mask patterns
            patterns.add( meta );
        }
        return patterns;
    }

    private String getPatternString( ArtifactRef artifact, KojiArchiveInfo a )
    {
        String gId = artifact.getGroupId();
        String artiId = artifact.getArtifactId();
        String ver = artifact.getVersionStringRaw();

        if ( gId == null || artiId == null || ver == null )
        {
            logger.trace( "Pattern ignored, gId: {}, artiId: {}, ver: {}", gId, artiId, ver );
            return null;
        }
        String pattern = gId.replace( '.', '/' ) + "/" + artiId + "/" + ver + "/" + a.getFilename();
        logger.trace( "Pattern: {}", pattern );

        return pattern;
    }

    private String getMetaString( ArtifactRef artifact )
    {
        String gId = artifact.getGroupId();
        String artiId = artifact.getArtifactId();

        if ( gId == null || artiId == null )
        {
            logger.trace( "Meta ignored, gId: {}, artiId: {}", gId, artiId );
            return null;
        }
        String meta = gId.replace( '.', '/' ) + "/" + artiId + "/" + METADATA_NAME;
        logger.trace( "Meta: {}", meta );
        return meta;
    }

    private String getRepositoryName( final KojiBuildInfo build, final boolean isBinaryBuild )
    {
        StringSearchInterpolator interpolator = new StringSearchInterpolator();
        interpolator.addValueSource( new ObjectBasedValueSource( build ) );

        try
        {
            return interpolator.interpolate( isBinaryBuild ?
                    config.getBinayNamingFormat() : config.getNamingFormat() );
        }
        catch ( InterpolationException e )
        {
            throw new RuntimeException( "Cannot resolve expressions in Koji configuration.", e );
        }
    }

    private Group adjustTargetGroup( final RemoteRepository buildRepo, final Group group )
            throws IndyWorkflowException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );

        // try to lookup the group -> targetGroup mapping in config, using the
        // entry-point group as the lookup key. If that returns null, the targetGroup is
        // the entry-point group.
        Group targetGroup = group;

        boolean isBinaryBuild = KOJI_ORIGIN_BINARY.equals( buildRepo.getMetadata( ArtifactStore.METADATA_ORIGIN) );

        String targetName = isBinaryBuild ? config.getTargetBinaryGroup( group.getName() )
                : config.getTargetGroup( group.getName() );

        if ( targetName != null )
        {
            try
            {
                targetGroup = storeDataManager.query().packageType( group.getPackageType() ).getGroup( targetName );
            }
            catch ( IndyDataException e )
            {
                throw new IndyWorkflowException(
                        "Cannot lookup koji-addition target group: %s (source group: %s). Reason: %s", e, targetName,
                        group.getName(), e.getMessage() );
            }
        }

        logger.info( "Adding Koji build proxy: {} to group: {}", buildRepo.getKey(), targetGroup.getKey() );

        // Append the new remote repo as a member of the targetGroup.
        targetGroup.addConstituent( buildRepo );
        try
        {
            final ChangeSummary changeSummary = new ChangeSummary( ChangeSummary.SYSTEM_USER,
                                                                   "Adding remote repository for Koji build: "
                                                                           + buildRepo.getMetadata( NVR ) );

            storeDataManager.storeArtifactStore( targetGroup, changeSummary, false, true, new EventMetadata() );
        }
        catch ( IndyDataException e )
        {
            throw new IndyWorkflowException( "Cannot store target-group: %s changes for: %s. Error: %s", e,
                                             targetGroup.getName(), buildRepo.getMetadata( NVR ), e.getMessage() );
        }

        logger.info( "Retrieving GAV: {} from: {}", buildRepo.getMetadata( CREATION_TRIGGER_GAV ), buildRepo );

        return targetGroup;

        // TODO: how to index it for the group...?
    }

    private String formatStorageUrl( final KojiBuildInfo buildInfo )
            throws MalformedURLException
    {
        String url =
                UrlUtils.buildUrl( config.getStorageRootUrl(), "packages", buildInfo.getName(), buildInfo.getVersion(),
                                   buildInfo.getRelease(), "maven" );

        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.info( "Using Koji URL: {}", url );

        return url;
    }

    private interface KojiBuildAction<T>
    {
        T execute( StoreKey inStore, ArtifactRef artifactRef, KojiBuildInfo build, KojiSessionInfo session )
                throws KojiClientException;
    }

}
