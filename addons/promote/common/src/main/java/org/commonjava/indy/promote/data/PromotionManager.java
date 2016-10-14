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
package org.commonjava.indy.promote.data;

import org.apache.commons.lang.StringUtils;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.content.ContentManager;
import org.commonjava.indy.content.DownloadManager;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.promote.conf.PromoteConfig;
import org.commonjava.indy.promote.model.GroupPromoteRequest;
import org.commonjava.indy.promote.model.GroupPromoteResult;
import org.commonjava.indy.promote.model.PathsPromoteRequest;
import org.commonjava.indy.promote.model.PathsPromoteResult;
import org.commonjava.indy.promote.model.ValidationResult;
import org.commonjava.indy.promote.validate.PromotionValidator;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static org.apache.commons.io.IOUtils.closeQuietly;

/**
 * Component responsible for orchestrating the transfer of artifacts from one store to another, according to the given {@link PathsPromoteRequest} or
 * {@link PathsPromoteResult}. Currently provides promotePaths, resumePathsPromote, and rollbackPathsPromote (the latter two for dealing with failed promotePaths calls).
 *
 * @author jdcasey
 */
@ApplicationScoped
public class PromotionManager
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private PromoteConfig config;

    @Inject
    private ContentManager contentManager;

    @Inject
    private DownloadManager downloadManager;

    @Inject
    private StoreDataManager storeManager;

    @Inject
    private PromotionValidator validator;

    private Map<StoreKey, ReentrantLock> byPathTargetLocks = new HashMap<>();

    protected PromotionManager()
    {
    }

    public PromotionManager( PromotionValidator validator, final ContentManager contentManager,
                             final DownloadManager downloadManager, final StoreDataManager storeManager, PromoteConfig config )
    {
        this.validator = validator;
        this.contentManager = contentManager;
        this.downloadManager = downloadManager;
        this.storeManager = storeManager;
        this.config = config;
    }

    public GroupPromoteResult promoteToGroup( GroupPromoteRequest request, String user )
            throws PromotionException
    {
        if ( !storeManager.hasArtifactStore( request.getSource() ) )
        {
            String error = String.format( "Cannot promote from missing source: %s", request.getSource() );
            logger.warn( error );

            return new GroupPromoteResult( request, error );
        }

        Group target;
        try
        {
            target = (Group) storeManager.getArtifactStore( request.getTargetKey() );
        }
        catch ( IndyDataException e )
        {
            throw new PromotionException( "Cannot retrieve target group: %s. Reason: %s", e, request.getTargetGroup(),
                                          e.getMessage() );
        }

        if ( target == null )
        {
            String error = String.format( "No such target group: %s.", request.getTargetGroup() );
            logger.warn( error );

            return new GroupPromoteResult( request, error );
        }

        ValidationResult validation = new ValidationResult();
        logger.info( "Running validations for promotion of: {} to group: {}", request.getSource(),
                     request.getTargetGroup() );

        validator.validate( request, validation );
        if ( validation.isValid() )
        {
            if ( !request.isDryRun() && !target.getConstituents().contains( request.getSource() ) )
            {
                // give the preUpdate event a different object to compare vs. the original group.
                target = target.copyOf();

                target.addConstituent( request.getSource() );
                try
                {
                    storeManager.storeArtifactStore( target, new ChangeSummary( user, "Promoting " + request.getSource()
                            + " into membership of group: " + target.getKey() ), false, new EventMetadata() );
                }
                catch ( IndyDataException e )
                {
                    throw new PromotionException( "Failed to store group: %s with additional member: %s. Reason: %s", e,
                                                  target.getKey(), request.getSource(), e.getMessage() );
                }
            }
        }

        return new GroupPromoteResult( request, validation );
    }

    public GroupPromoteResult rollbackGroupPromote( GroupPromoteResult result, String user )
            throws PromotionException
    {
        GroupPromoteRequest request = result.getRequest();

        if ( !storeManager.hasArtifactStore( request.getSource() ) )
        {
            String error = String.format( "No such source/member store: %s", request.getSource() );
            logger.warn( error );

            return new GroupPromoteResult( request, error );
        }

        Group target;
        try
        {
            target = (Group) storeManager.getArtifactStore( request.getTargetKey() );
        }
        catch ( IndyDataException e )
        {
            throw new PromotionException( "Cannot retrieve target group: %s. Reason: %s", e, request.getTargetGroup(),
                                          e.getMessage() );
        }

        if ( target == null )
        {
            String error = String.format( "No such target group: %s.", request.getTargetGroup() );
            logger.warn( error );

            return new GroupPromoteResult( request, error );
        }

        if ( target.getConstituents().contains( request.getSource() ) )
        {
            // give the preUpdate event a different object to compare vs. the original group.
            target = target.copyOf();

            target.removeConstituent( request.getSource() );
            try
            {
                storeManager.storeArtifactStore( target, new ChangeSummary( user, "Removing " + request.getSource()
                        + " from membership of group: " + target.getKey() ), false, new EventMetadata() );
            }
            catch ( IndyDataException e )
            {
                throw new PromotionException( "Failed to store group: %s with additional member: %s. Reason: %s", e,
                                              target.getKey(), request.getSource(), e.getMessage() );
            }
        }
        else
        {
            return new GroupPromoteResult( request, "Group: " + target.getKey() + " does not contain member: " + request
                    .getSource() );
        }

        return new GroupPromoteResult( request );
    }

    /**
     * Promote artifacts from the source to the target given in the {@link PathsPromoteRequest}. If a set of paths are given, only try to promotePaths those.
     * Otherwise, build a recursive list of available artifacts in the source store, and try to promotePaths them all.
     *
     * @param request The request containing source and target store keys, and an optional list of paths to promotePaths
     *
     * @return The result, including the source and target store keys used, the paths completed (promoted successfully), the pending paths (those that 
     * weren't processed due to some error...or null), and a nullable error explaining what (if anything) went wrong with the promotion.
     *
     * @throws PromotionException
     * @throws IndyWorkflowException
     */
    public PathsPromoteResult promotePaths( final PathsPromoteRequest request )
            throws PromotionException, IndyWorkflowException
    {
        final Set<String> paths = request.getPaths();
        final StoreKey source = request.getSource();

        List<Transfer> contents;
        if ( paths == null || paths.isEmpty() )
        {
            contents = downloadManager.listRecursively( source, DownloadManager.ROOT_PATH );
        }
        else
        {
            contents = getTransfersForPaths( source, paths );
        }

        final Set<String> pending = new HashSet<>();

        for ( final Transfer transfer : contents )
        {
            pending.add( transfer.getPath() );
        }

        ValidationResult validation = new ValidationResult();
        validator.validate( request, validation );
        if ( request.isDryRun() )
        {
            return new PathsPromoteResult( request, pending, Collections.emptySet(), Collections.emptySet(),
                                           validation );
        }
        else if ( validation.isValid() )
        {
            return runPathPromotions( request, pending, Collections.emptySet(), Collections.emptySet(), contents );
        }
        else
        {
            return new PathsPromoteResult( request, pending, Collections.emptySet(), Collections.emptySet(),
                                           validation );
        }
    }

    /**
     * Attempt to resumePathsPromote from a previously failing {@link PathsPromoteResult}. This is meant to handle cases where a transient (or correctable) error
     * occurs on the server side, and promotion can proceed afterward. It works much like the {@link #promotePaths(PathsPromoteRequest)} call, using the pending
     * paths list from the input result as the list of paths to process. The output {@link PathsPromoteResult} contains all previous completed paths PLUS
     * any additional completed transfers when it is returned, thus providing a cumulative result to the user.
     *
     * @param result The result to resumePathsPromote
     *
     * @return The same result, with any successful path promotions moved from the pending to completed paths list, and the error cleared (or set to a 
     * new error)
     *
     * @throws PromotionException
     * @throws IndyWorkflowException
     */
    public PathsPromoteResult resumePathsPromote( final PathsPromoteResult result )
            throws PromotionException, IndyWorkflowException
    {
        final List<Transfer> contents =
                getTransfersForPaths( result.getRequest().getSource(), result.getPendingPaths() );

        return runPathPromotions( result.getRequest(), result.getPendingPaths(), result.getCompletedPaths(),
                                  result.getSkippedPaths(), contents );
    }

    /**
     * Attempt to rollbackPathsPromote a previously failing {@link PathsPromoteResult}. This is meant to handle cases where an unrecoverable error
     * occurs on the server side, and promotion can NOT proceed afterward. All paths in the completed paths set are deleted from the target, if 
     * possible. The output {@link PathsPromoteResult} contains the previous content, with any successfully removed target paths moved back from the
     * completed-paths list to the pending-paths list. If an error occurs during rollbackPathsPromote, the error field will be set...otherwise, it will be null.
     *
     * @param result The result to rollbackPathsPromote
     *
     * @return The same result, with any successful deletions moved from the completed to pending paths list, and the error cleared (or set to a 
     * new error)
     *
     * @throws PromotionException
     * @throws IndyWorkflowException
     */
    public PathsPromoteResult rollbackPathsPromote( final PathsPromoteResult result )
            throws PromotionException, IndyWorkflowException
    {
        StoreKey targetKey = result.getRequest().getTarget();

        ReentrantLock lock;
        synchronized ( byPathTargetLocks )
        {
            lock = byPathTargetLocks.get( targetKey );
            if ( lock == null )
            {
                lock = new ReentrantLock();
                byPathTargetLocks.put( targetKey, lock );
            }
        }

        final List<Transfer> contents = getTransfersForPaths( targetKey, result.getCompletedPaths() );
        final Set<String> completed = result.getCompletedPaths();
        final Set<String> skipped = result.getSkippedPaths();

        if ( completed == null || completed.isEmpty() )
        {
            result.setError( null );
            return result;
        }

        Set<String> pending = result.getPendingPaths();
        pending = pending == null ? new HashSet<String>() : new HashSet<String>( pending );

        String error = null;
        final boolean copyToSource = result.getRequest().isPurgeSource();

        ArtifactStore source = null;
        try
        {
            source = storeManager.getArtifactStore( result.getRequest().getSource() );
        }
        catch ( final IndyDataException e )
        {
            error = String.format( "Failed to retrieve artifact store: %s. Reason: %s",
                                   result.getRequest().getSource(), e.getMessage() );
            logger.error( error, e );
        }

        try
        {
            if ( error == null )
            {
                boolean locked= lock.tryLock( config.getLockTimeoutSeconds(), TimeUnit.SECONDS );
                if ( !locked )
                {
                    error = String.format( "Failed to acquire promotion lock on target: %s in %d seconds.", targetKey,
                                           config.getLockTimeoutSeconds() );
                    logger.warn( error );
                }
            }

            if ( error == null )
            {
                for ( final Transfer transfer : contents )
                {
                    if ( transfer != null && transfer.exists() )
                    {
                        InputStream stream = null;
                        try
                        {
                            if ( copyToSource )
                            {
                                stream = transfer.openInputStream( true );
                                final String path = transfer.getPath();
                                contentManager.store( source, path, stream, TransferOperation.UPLOAD,
                                                      new EventMetadata() );
                                stream.close();
                            }

                            transfer.delete( true );
                            completed.remove( transfer.getPath() );
                            pending.add( transfer.getPath() );
                        }
                        catch ( final IOException e )
                        {
                            error = String.format( "Failed to rollback path promotion of: %s from: %s. Reason: %s",
                                                   transfer, result.getRequest().getSource(), e.getMessage() );
                            logger.error( error, e );
                        }
                        finally
                        {
                            closeQuietly( stream );
                        }
                    }
                }
            }
        }
        catch ( InterruptedException e )
        {
            error = String.format( "Interrupted waiting for promotion lock on target: %s", targetKey );
            logger.warn( error );
        }
        finally
        {
            lock.unlock();
        }

        return new PathsPromoteResult( result.getRequest(), pending, completed, skipped, error );
    }

    private PathsPromoteResult runPathPromotions( final PathsPromoteRequest request, final Set<String> pending,
                                                  final Set<String> prevComplete, final Set<String> prevSkipped,
                                                  final List<Transfer> contents )
    {
        if ( pending == null || pending.isEmpty() )
        {
            return new PathsPromoteResult( request, pending, prevComplete, prevSkipped, new ValidationResult() );
        }

        StoreKey targetKey= request.getTarget();

        ReentrantLock lock;
        synchronized ( byPathTargetLocks )
        {
            lock = byPathTargetLocks.get( targetKey );
            if ( lock == null )
            {
                lock = new ReentrantLock();
                byPathTargetLocks.put( targetKey, lock );
            }
        }

        final Set<String> complete = prevComplete == null ? new HashSet<>() : new HashSet<>( prevComplete );
        final Set<String> skipped = prevSkipped == null ? new HashSet<>() : new HashSet<>( prevSkipped );

        List<String> errors = new ArrayList<>();
        ArtifactStore sourceStore = null;
        ArtifactStore targetStore = null;
        try
        {
            sourceStore = storeManager.getArtifactStore( request.getSource() );
            targetStore = storeManager.getArtifactStore( request.getTarget() );
        }
        catch ( final IndyDataException e )
        {
            String msg = String.format( "Failed to retrieve artifact store: %s. Reason: %s", request.getSource(),
                                        e.getMessage() );
            errors.add( msg );
            logger.error( msg, e );
        }

        try
        {
            if ( errors.isEmpty() )
            {
                boolean locked= lock.tryLock( config.getLockTimeoutSeconds(), TimeUnit.SECONDS );
                if ( !locked )
                {
                    String error= String.format( "Failed to acquire promotion lock on target: %s in %d seconds.", targetKey,
                                                 config.getLockTimeoutSeconds() );

                    errors.add( error );
                    logger.warn( error );
                }
            }

            if ( errors.isEmpty() )
            {
                logger.info( "Running promotions from: {} (key: {})\n  to: {} (key: {})", sourceStore, request.getSource(),
                             targetStore, request.getTarget() );

                final boolean purgeSource = request.isPurgeSource();
                for ( final Transfer transfer : contents )
                {
                    try
                    {
                        final String path = transfer.getPath();

                        Transfer target = contentManager.getTransfer( targetStore, path, TransferOperation.UPLOAD );
                        //                        synchronized ( target )
                        //                        {
                        // TODO: Should the request object have an overwrite attribute? Is that something the user is qualified to decide?
                        if ( target != null && target.exists() )
                        {
                            logger.warn( "NOT promoting: {} from: {} to: {}. Target file already exists.", path,
                                         request.getSource(), request.getTarget() );

                            // TODO: There's no guarantee that the pre-existing content is the same!
                            pending.remove( path );
                            skipped.add( path );

                            continue;
                        }

                        try (InputStream stream = transfer.openInputStream( true ))
                        {
                            contentManager.store( targetStore, path, stream, TransferOperation.UPLOAD,
                                                  new EventMetadata() );

                            pending.remove( path );
                            complete.add( path );

                            stream.close();

                            if ( purgeSource )
                            {
                                contentManager.delete( sourceStore, path, new EventMetadata() );
                            }
                        }
                        catch ( final IOException e )
                        {
                            String msg = String.format( "Failed to open input stream for: %s. Reason: %s", transfer,
                                                        e.getMessage() );
                            errors.add( msg );
                            logger.error( msg, e );
                        }
                        //                        }
                    }
                    catch ( final IndyWorkflowException e )
                    {
                        String msg =
                                String.format( "Failed to promote path: %s to: %s. Reason: %s", transfer, targetStore,
                                               e.getMessage() );
                        errors.add( msg );
                        logger.error( msg, e );
                    }
                }
            }

        }
        catch ( InterruptedException e )
        {
            String error = String.format( "Interrupted waiting for promotion lock on target: %s", targetKey );
            errors.add( error );
            logger.warn( error );
        }
        finally
        {
            lock.unlock();
        }

        String error = null;

        if ( !errors.isEmpty() )
        {
            error = StringUtils.join( errors, "\n" );
        }

        return new PathsPromoteResult( request, pending, complete, skipped, error );
    }

    private List<Transfer> getTransfersForPaths( final StoreKey source, final Set<String> paths )
            throws IndyWorkflowException
    {
        final List<Transfer> contents = new ArrayList<Transfer>();
        for ( final String path : paths )
        {
            final Transfer txfr = downloadManager.getStorageReference( source, path );
            if ( txfr == null || !txfr.exists() )
            {
                logger.warn( "Cannot promote path: '{}' from source: '{}'. It does not exist!", path, source );
                // TODO: Fail??
                continue;
            }

            contents.add( txfr );
        }

        return contents;
    }

}
