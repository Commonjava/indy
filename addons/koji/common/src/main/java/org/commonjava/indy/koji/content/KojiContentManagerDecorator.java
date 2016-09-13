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
package org.commonjava.indy.koji.content;

import com.redhat.red.build.koji.KojiClient;
import com.redhat.red.build.koji.KojiClientException;
import com.redhat.red.build.koji.model.util.ValueHolder;
import com.redhat.red.build.koji.model.xmlrpc.KojiArchiveInfo;
import com.redhat.red.build.koji.model.xmlrpc.KojiBuildArchiveCollection;
import com.redhat.red.build.koji.model.xmlrpc.KojiBuildInfo;
import com.redhat.red.build.koji.model.xmlrpc.KojiSessionInfo;
import com.redhat.red.build.koji.model.xmlrpc.KojiTagInfo;
import org.apache.commons.io.IOUtils;
import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.cdi.util.weft.WeftManaged;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.content.ContentManager;
import org.commonjava.indy.content.DirectContentAccess;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.koji.conf.IndyKojiConfig;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.subsys.template.IndyGroovyException;
import org.commonjava.indy.subsys.template.ScriptEngine;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.util.ArtifactPathInfo;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.commonjava.maven.galley.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

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
//@ApplicationScoped
public abstract class KojiContentManagerDecorator
        implements ContentManager
{
    public static final String CREATION_TRIGGER_GAV = "creation-trigger-GAV";

    public static final String NVR = "koji-NVR";

    public static final String KOJI_ORIGIN = "koji";

    private static final String KOJI_REPO_CREATOR_SCRIPT = "koji-repo-creator.groovy";

    @Delegate
    @Inject
    private ContentManager delegate;

    @Inject
    private DirectContentAccess directContentAccess;

    @Inject
    private StoreDataManager storeDataManager;

    @Inject
    private KojiClient kojiClient;

    @Inject
    private IndyKojiConfig config;

    @Inject
    @WeftManaged
    @ExecutorConfig( named = "koji-downloads", threads = 4, daemon = false )
    private ExecutorService executorService;

    @Inject
    private ScriptEngine scriptEngine;

    private ExecutorCompletionService<Transfer> executor;

    private KojiRepositoryCreator creator;

    @PostConstruct
    public void init()
    {
        executor = new ExecutorCompletionService<Transfer>( executorService );
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
    }

    @Override
    public Transfer retrieve( final ArtifactStore store, final String path )
            throws IndyWorkflowException
    {
        return retrieve( store, path, new EventMetadata() );
    }

    @Override
    public Transfer retrieve( final ArtifactStore store, final String path, final EventMetadata eventMetadata )
            throws IndyWorkflowException
    {
        Transfer result = delegate.retrieve( store, path, eventMetadata );
        if ( result == null && StoreType.group == store.getKey().getType() )
        {
            if ( !config.getEnabled() )
            {
                Logger logger = LoggerFactory.getLogger( getClass() );
                logger.info( "Koji content-manager decorator is disabled." );
                return result;
            }

            Group group = (Group) store;

            if ( !config.isEnabledFor( group.getName() ) )
            {
                Logger logger = LoggerFactory.getLogger( getClass() );
                logger.info( "Koji content-manager decorator not enabled for: {}.", store.getKey() );
                return result;
            }

            Logger logger = LoggerFactory.getLogger( getClass() );

            // TODO: This won't work for maven-metadata.xml files! We need to hit a POM or jar or something first.
            ArtifactPathInfo pathInfo = ArtifactPathInfo.parse( path );
            if ( pathInfo != null )
            {
                ArtifactRef artifactRef = pathInfo.getArtifact();
                logger.info( "Searching for Koji build: {}", artifactRef );

                RepoAndTransfer proxyResult = proxyKojiBuild( artifactRef, path, eventMetadata );
                if ( proxyResult != null )
                {
                    result = adjustTargetGroupAndRetrieve( proxyResult, group, path, eventMetadata );
                }
            }
            else
            {
                logger.info( "Path is not a maven artifact reference: {}", path );
            }
        }

        // Finally, pass the Transfer back.
        return result;
    }

    private RepoAndTransfer proxyKojiBuild( final ArtifactRef artifactRef, final String originatingPath,
                                            final EventMetadata eventMetadata )
            throws IndyWorkflowException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        try
        {
            return kojiClient.withKojiSession( ( session ) -> {
                List<KojiBuildInfo> builds = kojiClient.listBuildsContaining( artifactRef, session );

                Collections.sort( builds, ( build1, build2 ) -> build1.getCreationTime()
                                                                      .compareTo( build2.getCreationTime() ) );

                logger.debug( "Got {} builds from koji. Looking for best match.", builds.size() );

                for ( KojiBuildInfo build : builds )
                {
                    if ( build.getTaskId() == null )
                    {
                        logger.debug( "Build: {} is not a real build. It looks like a binary import. Skipping.",
                                      build.getNvr() );
                        // This is not a real build, it's a binary import.
                        continue;
                    }

                    logger.info( "Trying build: {} with id: {}", build.getNvr(), build.getId() );
                    List<KojiTagInfo> tags = kojiClient.listTags( build.getId(), session );
                    logger.debug( "Build is in {} tags...", tags.size() );
                    for ( KojiTagInfo tag : tags )
                    {
                        // If the tags match patterns configured in whitelist, construct a new remote repo.
                        if ( config.isTagAllowed( tag.getName() ) )
                        {
                            logger.info( "Koji tag is on whitelist: {}", tag.getName() );
                            return transferBuild( build, originatingPath, artifactRef, eventMetadata, session );
                        }
                        else
                        {
                            logger.debug( "Tag: {} is not in the whitelist.", tag.getName() );
                        }
                    }

                    logger.debug( "No whitelisted tags found for: {}", build.getNvr() );
                }

                logger.debug( "No builds were found that matched the restrictions." );

                return null;
            } );
        }
        catch ( KojiClientException e )
        {
            throw new IndyWorkflowException( "Cannot retrieve builds for: %s. Error: %s", e, artifactRef,
                                             e.getMessage() );
        }
    }

    private RepoAndTransfer transferBuild( final KojiBuildInfo build, final String originatingPath, final ArtifactRef artifactRef,
                                           final EventMetadata eventMetadata, final KojiSessionInfo session )
            throws KojiClientException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        try
        {
            KojiBuildArchiveCollection archiveCollection = kojiClient.listArchivesForBuild( build.getId(), session );

            String name = "koji-" + build.getNvr();

            // Using a RemoteRepository allows us to use the higher-level APIs in Indy, as opposed to TransferManager
            RemoteRepository remote = creator.createRemoteRepository( name, formatStorageUrl( build ),
                                                                      config.getDownloadTimeoutSeconds() );

            remote.setServerCertPem( config.getServerPemContent() );
            remote.setMetadata( ArtifactStore.METADATA_ORIGIN, KOJI_ORIGIN );
            storeDataManager.storeArtifactStore( remote, new ChangeSummary( ChangeSummary.SYSTEM_USER,
                                                                            "Creating TEMPORARY remote repository for Koji build: "
                                                                                    + build.getNvr() ) );

            HostedRepository hosted = creator.createHostedRepository( name, artifactRef, build.getNvr(), eventMetadata );

            hosted.setMetadata( ArtifactStore.METADATA_ORIGIN, KOJI_ORIGIN );
            storeDataManager.storeArtifactStore( hosted, new ChangeSummary( ChangeSummary.SYSTEM_USER,
                                                                            "Creating hosted repository for Koji build: "
                                                                                    + build.getNvr() ) );

            if ( archiveCollection != null )
            {
                RepoAndTransfer result = new RepoAndTransfer();
                result.repo = hosted;

                AtomicInteger counter = new AtomicInteger( 0 );
                ValueHolder<Throwable> requestedTransferError = new ValueHolder<>();
                archiveCollection.stream()
                                 .filter( ( archive ) -> artifactRef.equals( archive.asArtifact() ) )
                                 .findFirst()
                                 .ifPresent( ( archive ) -> result.transfer =
                                         transferBuildArtifact( archive, remote, hosted, build, eventMetadata, counter,
                                                                true,
                                                                ( path, error, e ) -> requestedTransferError.setValue(
                                                                        e ) ) );

                Throwable error = requestedTransferError.getValue();
                if ( error != null )
                {
                    throw new KojiClientException( "Failed to transfer requested path: %s from build: %s. Reason: %s",
                                                   error, originatingPath, build.getNvr(), error.getMessage() );
                }

                executorService.execute( () -> {
                    try
                    {
                        archiveCollection.stream()
                                         .filter( ( archive ) -> !artifactRef.equals( archive.asArtifact() ) )
                                         .forEach( ( archive ) -> transferBuildArtifact( archive, remote, hosted, build,
                                                                                         eventMetadata, counter, false,
                                                                                         ( path, message, e ) -> logger.error(
                                                                                                 message.toString(),
                                                                                                 e ) ) );

                        for ( int i = 0; i < counter.get(); i++ )
                        {
                            Future<Transfer> future = executor.take();
                            future.get();
                        }
                    }
                    catch ( InterruptedException e )
                    {
                        logger.warn(
                                "WARNING! Local copy of build may be INCOMPLETE: {}\n  The thread was interrupted while awaiting transfers from Koji build.",
                                build.getNvr() );
                    }
                    catch ( ExecutionException e )
                    {
                        logger.error( "Failed to retrieve Transfer from Koji build-transfer future.", e );
                    }
                    finally
                    {
                        try
                        {
                            storeDataManager.deleteArtifactStore( remote.getKey(),
                                                                  new ChangeSummary( ChangeSummary.SYSTEM_USER,
                                                                                     "Removing temporary remote repository for: "
                                                                                             + build.getNvr() ) );
                        }
                        catch ( IndyDataException e )
                        {
                            logger.error( String.format(
                                    "Failed to remove temporary transfer remote repository for build: %s. Reason: %s",
                                    build.getName(), e.getMessage() ), e );
                        }
                    }

                    logger.info( "Transfer of Koji build: {} is finished.", build.getNvr() );
                } );

                return result;
            }
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

        return null;
    }

    private Transfer transferBuildArtifact( final KojiArchiveInfo archive, final RemoteRepository remote, final HostedRepository hosted,
                                            final KojiBuildInfo build, final EventMetadata eventMetadata, final AtomicInteger counter,
                                            final boolean wait, final ErrorHandler errorHandler )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        String path = String.format( "%s/%s/%s/%s", archive.getGroupId().replace( '.', '/' ), archive.getArtifactId(),
                                     archive.getVersion(), archive.getFilename() );

        // download manager get path from remote
        Transfer transfer = null;
        try
        {
            remote.setDisabled( false );
            logger.info( "Retrieving {} from Koji build: {}", path, build.getNvr() );
            transfer = directContentAccess.retrieveRaw( remote, path, eventMetadata );
        }
        catch ( IndyWorkflowException e )
        {
            errorHandler.error( path, new StringFormatter( "Failed to retrieve: %s from Koji build: %s.", e, path,
                                                           build.getNvr(), e.getMessage() ), e );
        }

        // copy download to hosted
        if ( transfer != null )
        {
            try
            {
                Transfer target = delegate.getTransfer( hosted, path, TransferOperation.DOWNLOAD );
                Transfer source = transfer;

                counter.incrementAndGet();

                // We thread this off so we can make maximum use of partyline to read while the transfer is still in
                // progress, and so we can execute multiple downloads in parallel.
                //
                // If we're not waiting on this transfer, then don't worry about synchronizing to watch for the start of
                // the transfer.
                executor.submit( () -> {
                    try (InputStream in = source.openInputStream( true, eventMetadata );
                         OutputStream out = target.openOutputStream( TransferOperation.UPLOAD, true, eventMetadata ))
                    {
                        if ( wait )
                        {
                            synchronized ( target )
                            {
                                target.notifyAll();
                            }
                        }

                        IOUtils.copy( in, out );
                    }
                    return target;
                } );

                if ( wait )
                {
                    synchronized ( target )
                    {
                        logger.debug( "Waiting for {} download to start for Koji build: {}", path, build.getNvr() );
                        target.wait();
                    }
                }

                return target;
            }
            catch ( IndyWorkflowException e )
            {
                errorHandler.error( path,
                                    new StringFormatter( "Failed to store: %s from Koji build: %s in: %s.", e, path,
                                                         build.getNvr(), hosted.getKey(), e.getMessage() ), e );
            }
            catch ( InterruptedException e )
            {
                logger.info( "Interrupted while waiting for transfer: {} of Koji build: {} to start", path,
                             build.getNvr() );
            }
        }

        return transfer;
    }

    private Transfer adjustTargetGroupAndRetrieve( final RepoAndTransfer proxyResult, final Group group, final String path,
                                                   final EventMetadata eventMetadata )
            throws IndyWorkflowException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );

        // Then, try to lookup the group -> targetGroup mapping in config, using the
        // entry-point group as the lookup key. If that returns null, the targetGroup is
        // the entry-point group.
        Group targetGroup = group;

        String targetName = config.getTargetGroup( group.getName() );
        if ( targetName != null )
        {
            try
            {
                targetGroup = storeDataManager.getGroup( targetName );
            }
            catch ( IndyDataException e )
            {
                throw new IndyWorkflowException(
                        "Cannot lookup koji-addition target group: %s (source group: %s). Reason: %s", e, targetName,
                        group.getName(), e.getMessage() );
            }
        }

        HostedRepository buildRepo = proxyResult.repo;
        logger.info( "Adding Koji build proxy: {} to group: {}", buildRepo.getKey(), targetGroup.getKey() );

        // Append the new remote repo as a member of the targetGroup.
        targetGroup.addConstituent( buildRepo );
        try
        {
            storeDataManager.storeArtifactStore( targetGroup, new ChangeSummary( ChangeSummary.SYSTEM_USER,
                                                                                 "Adding hosted repository for Koji build: "
                                                                                         + buildRepo.getMetadata(
                                                                                         NVR ) ) );
        }
        catch ( IndyDataException e )
        {
            throw new IndyWorkflowException( "Cannot store target-group: %s changes for: %s. Error: %s", e,
                                             targetGroup.getName(), buildRepo.getMetadata( NVR ), e.getMessage() );
        }

        logger.info( "Retrieving GAV: {} from: {}", buildRepo.getMetadata( CREATION_TRIGGER_GAV ), buildRepo );

        return proxyResult.transfer;

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

    interface ErrorHandler
    {
        void error( String path, Object error, Throwable e );
    }

    private static final class StringFormatter
    {
        private String format;

        private Object[] params;

        public StringFormatter( final String format, final Object... params )
        {
            this.format = format;
            this.params = params;
        }

        @Override
        public String toString()
        {
            return String.format( format, params );
        }
    }

    private static final class RepoAndTransfer
    {
        private HostedRepository repo;

        private Transfer transfer;
    }

}
