/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
import org.commonjava.cdi.util.weft.DrainingExecutorCompletionService;
import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.cdi.util.weft.Locker;
import org.commonjava.cdi.util.weft.WeftExecutorService;
import org.commonjava.cdi.util.weft.WeftManaged;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.content.ContentManager;
import org.commonjava.indy.content.DownloadManager;
import org.commonjava.indy.core.inject.GroupMembershipLocks;
import org.commonjava.indy.core.inject.StoreContentLocks;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.measure.annotation.Measure;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor;
import org.commonjava.indy.promote.callback.PromotionCallbackHelper;
import org.commonjava.indy.promote.change.event.PathsPromoteCompleteEvent;
import org.commonjava.indy.promote.change.event.PromoteCompleteEvent;
import org.commonjava.indy.promote.conf.PromoteConfig;
import org.commonjava.indy.promote.model.GroupPromoteRequest;
import org.commonjava.indy.promote.model.GroupPromoteResult;
import org.commonjava.indy.promote.model.PathsPromoteRequest;
import org.commonjava.indy.promote.model.PathsPromoteResult;
import org.commonjava.indy.promote.model.ValidationResult;
import org.commonjava.indy.promote.validate.PromotionValidationException;
import org.commonjava.indy.promote.validate.PromotionValidator;
import org.commonjava.indy.promote.validate.model.ValidationRequest;
import org.commonjava.indy.util.LocationUtils;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.commonjava.maven.galley.spi.nfc.NotFoundCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;
import static org.commonjava.indy.change.EventUtils.fireEvent;
import static org.commonjava.indy.core.ctl.PoolUtils.detectOverload;
import static org.commonjava.indy.core.ctl.PoolUtils.detectOverloadVoid;
import static org.commonjava.indy.model.core.StoreType.hosted;

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

    @Inject
    private Event<PromoteCompleteEvent> promoteCompleteEvent;

    @StoreContentLocks
    @Inject
    private Locker<StoreKey> byPathTargetLocks;

    @GroupMembershipLocks
    @Inject
    private Locker<StoreKey> byGroupTargetLocks;

    private Map<String, StoreKey> targetGroupKeyMap = new ConcurrentHashMap<>( 1 );

    @WeftManaged
    @Inject
    @ExecutorConfig( named = "promotion", threads = 8, priority = 8, loadSensitive = ExecutorConfig.BooleanLiteral.TRUE )
    private WeftExecutorService asyncPromotionService;

    @WeftManaged
    @Inject
    @ExecutorConfig( named = "promotion-transfers", threads = 40, priority = 6, loadSensitive = ExecutorConfig.BooleanLiteral.TRUE,
                     maxLoadFactor = 100 )
    private WeftExecutorService transferService;

    @Inject
    private PromotionCallbackHelper callbackHelper;

    @Inject
    private NotFoundCache nfc;

    protected PromotionManager()
    {
    }

    public PromotionManager( PromotionValidator validator, final ContentManager contentManager,
                             final DownloadManager downloadManager, final StoreDataManager storeManager,
                             Locker<StoreKey> byPathTargetLocks, Locker<StoreKey> byGroupTargetLocks,
                             PromoteConfig config, NotFoundCache nfc, WeftExecutorService asyncPromotionService,
                             WeftExecutorService transferService )
    {
        this.validator = validator;
        this.contentManager = contentManager;
        this.downloadManager = downloadManager;
        this.storeManager = storeManager;
        this.byPathTargetLocks = byPathTargetLocks;
        this.byGroupTargetLocks = byGroupTargetLocks;
        this.config = config;
        this.nfc = nfc;
        this.asyncPromotionService = asyncPromotionService;
        this.transferService = transferService;
    }

    @Measure
    public GroupPromoteResult promoteToGroup( GroupPromoteRequest request, String user, String baseUrl )
            throws PromotionException, IndyWorkflowException
    {
        if ( !storeManager.hasArtifactStore( request.getSource() ) )
        {
            String error = String.format( "Cannot promote from missing source: %s", request.getSource() );
            logger.warn( error );

            return new GroupPromoteResult( request, error );
        }

        final StoreKey targetKey = getTargetKey( request.getTargetGroup() );

        if ( !storeManager.hasArtifactStore( targetKey ) )
        {
            String error = String.format( "No such target group: %s.", request.getTargetGroup() );
            logger.warn( error );

            return new GroupPromoteResult( request, error );
        }

        Future<GroupPromoteResult> future = submitGroupPromoteRequest( request, user, baseUrl );
        if ( request.isAsync() )
        {
            return new GroupPromoteResult( request ).accepted();
        }
        else
        {
            try
            {
                return future.get();
            }
            catch ( InterruptedException | ExecutionException e )
            {
                logger.error( "Group prromotion failed: " + request.getSource() + " -> " + request.getTargetKey(), e );
                throw new PromotionException( "Execution of group promotion failed.", e );
            }
        }
    }

    private ValidationResult doValidationAndPromote( GroupPromoteRequest request, AtomicReference<Exception> error,
                                                     String user, String baseUrl )
    {
        ValidationResult validation = new ValidationResult();
        logger.info( "Running validations for promotion of: {} to group: {}", request.getSource(),
                     request.getTargetGroup() );

        final StoreKey targetKey = getTargetKey( request.getTargetGroup() );
        byGroupTargetLocks.lockAnd( targetKey, config.getLockTimeoutSeconds(), k -> {
            Group target;
            try
            {
                target = (Group) storeManager.getArtifactStore( request.getTargetKey() );
            }
            catch ( IndyDataException e )
            {
                error.set( new PromotionException( "Cannot retrieve target group: %s. Reason: %s", e,
                                                   request.getTargetGroup(), e.getMessage() ) );
                return null;
            }

            try
            {
                ValidationRequest validationRequest = validator.validate( request, validation, baseUrl );

                if ( validation.isValid() )
                {
                    if ( !request.isDryRun() && !target.getConstituents().contains( request.getSource() ) )
                    {
                        // give the preUpdate event a different object to compare vs. the original group.
                        target = target.copyOf();
                        target.addConstituent( request.getSource() );
                        try
                        {
                            final ChangeSummary changeSummary = new ChangeSummary( user,
                                                                                   "Promoting " + request.getSource()
                                                                                                   + " into membership of group: "
                                                                                                   + target.getKey() );

                            storeManager.storeArtifactStore( target, changeSummary, false, true, new EventMetadata() );
                            clearStoreNFC( validationRequest.getSourcePaths(), target );

                            if ( hosted == request.getSource().getType() && config.isAutoLockHostedRepos() )
                            {
                                HostedRepository source =
                                                (HostedRepository) storeManager.getArtifactStore( request.getSource() );

                                source.setReadonly( true );

                                final ChangeSummary readOnlySummary = new ChangeSummary( user, "Promoting "
                                                + request.getSource() + " into membership of group: " + target.getKey() );

                                storeManager.storeArtifactStore( source, readOnlySummary, false, true,
                                                                 new EventMetadata() );
                            }
                        }
                        catch ( IndyDataException e )
                        {
                            error.set( new PromotionException(
                                    "Failed to store group: %s with additional member: %s. Reason: %s", e,
                                    target.getKey(), request.getSource(), e.getMessage() ) );
                        }
                    }
                }
            }
            catch ( PromotionValidationException | IndyWorkflowException e )
            {
                error.set( e );
            }

            return null;
        }, ( k, lock ) -> {
            //FIXME: should we consider to repeat the promote process several times when lock failed?
            String errorMsg =
                    String.format( "Failed to acquire group promotion lock on target when promote: %s in %d seconds.",
                                   targetKey, config.getLockTimeoutSeconds() );
            logger.error( errorMsg );
            error.set( new PromotionException( errorMsg ) );

            return Boolean.FALSE;
        } );

        return validation;
    }

    /**
     * Provides target group store key for a given group name. This is meant to provide the same instance of the key
     * for a name to be able to synchronize promotion requests based on this instance.
     *
     * @param targetName the target group name
     * @return the group store key
     */
    private StoreKey getTargetKey( final String targetName )
    {
        return targetGroupKeyMap.computeIfAbsent( targetName,
                                                  k -> new StoreKey( MavenPackageTypeDescriptor.MAVEN_PKG_KEY,
                                                                     StoreType.group, targetName ) );
    }

    public GroupPromoteResult rollbackGroupPromote( GroupPromoteResult result, String user )
            throws PromotionException, IndyWorkflowException
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

        Future<GroupPromoteResult> future = submitGroupPromoteRollback( result, target, user );
        if ( request.isAsync() )
        {
            return new GroupPromoteResult( request ).accepted();
        }
        else
        {
            try
            {
                return future.get();
            }
            catch ( InterruptedException | ExecutionException e )
            {
                logger.error( "Group promotion rollback failed: " + request.getSource() + " -> " + request.getTargetKey(), e );
                throw new PromotionException( "Execution of group promotion rollback failed.", e );
            }
        }
    }

    private GroupPromoteResult doGroupPromoteRollback( GroupPromoteResult result, Group target, String user )
            throws PromotionException
    {
        GroupPromoteResult ret;

        GroupPromoteRequest request = result.getRequest();

        if ( target.getConstituents().contains( request.getSource() ) )
        {
            // give the preUpdate event a different object to compare vs. the original group.
            target = target.copyOf();

            target.removeConstituent( request.getSource() );
            try
            {
                final ChangeSummary changeSummary = new ChangeSummary( user, "Removing " + request.getSource()
                                + " from membership of group: " + target.getKey() );

                storeManager.storeArtifactStore( target, changeSummary, false, true, new EventMetadata() );
            }
            catch ( IndyDataException e )
            {
                throw new PromotionException( "Failed to store group: %s with additional member: %s. Reason: %s", e,
                                              target.getKey(), request.getSource(), e.getMessage() );
            }
            ret = new GroupPromoteResult( request );
        }
        else
        {
            ret = new GroupPromoteResult( request, "Group: " + target.getKey() + " does not contain member: "
                    + request.getSource() );
        }

        return ret.withPromotionId( result.getPromotionId() );
    }

    private Future<GroupPromoteResult> submitGroupPromoteRollback( final GroupPromoteResult result, final Group target, final String user )
            throws IndyWorkflowException
    {
        return detectOverload( () -> asyncPromotionService.submit( () -> {
            GroupPromoteResult ret;
            try
            {
                ret = doGroupPromoteRollback( result, target, user );
            }
            catch ( Exception ex )
            {
                GroupPromoteRequest request = result.getRequest();
                String msg = "Group promotion rollback failed. Target: " + target.getKey() + ", Source: "
                        + request.getSource() + ", Reason: " + getStackTrace( ex );
                logger.warn( msg );
                ret = new GroupPromoteResult( request, msg ).withPromotionId( result.getPromotionId() );
            }

            if ( ret.getRequest().getCallback() != null )
            {
                return callbackHelper.callback( ret.getRequest().getCallback(), ret );
            }

            return ret;
        } ) );
    }

    private Future<GroupPromoteResult> submitGroupPromoteRequest( final GroupPromoteRequest request, final String user, final String baseUrl )
            throws IndyWorkflowException
    {
        return detectOverload( () -> asyncPromotionService.submit( () -> {
            AtomicReference<Exception> error = new AtomicReference<>();
            ValidationResult validation = doValidationAndPromote( request, error, user, baseUrl );

            GroupPromoteResult ret;
            Exception ex = error.get();
            if ( ex != null )
            {
                String msg = "Group promotion failed. Target: " + request.getTargetGroup() + ", Source: "
                        + request.getSource() + ", Reason: " + getStackTrace( ex );
                logger.warn( msg );
                ret = new GroupPromoteResult( request, msg );
            }
            else
            {
                ret = new GroupPromoteResult( request, validation );
            }

            if ( request.getCallback() != null )
            {
                return callbackHelper.callback( ret.getRequest().getCallback(), ret );
            }

            return ret;
        } ) );
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
    @Measure
    public PathsPromoteResult promotePaths( final PathsPromoteRequest request, final String baseUrl )
            throws PromotionException, IndyWorkflowException
    {
        Future<PathsPromoteResult> future = submitPathsPromoteRequest( request, baseUrl );
        if ( request.isAsync() )
        {
            return new PathsPromoteResult( request ).accepted();
        }
        else
        {
            try
            {
                return future.get();
            }
            catch ( InterruptedException | ExecutionException e )
            {
                logger.error( "Path prromotion failed: " + request.getSource() + " -> " + request.getTargetKey(), e );
                throw new PromotionException( "Execution of path promotion failed.", e );
            }
        }
    }

    private PathsPromoteResult doPathsPromotion( PathsPromoteRequest request, String baseUrl )
            throws IndyWorkflowException, PromotionValidationException
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
        ValidationRequest validationRequest = validator.validate( request, validation, baseUrl );
        if ( request.isDryRun() )
        {
            return new PathsPromoteResult( request, pending, Collections.emptySet(), Collections.emptySet(),
                                           validation );
        }
        else if ( validation.isValid() )
        {
            return runPathPromotions( request, pending, Collections.emptySet(), Collections.emptySet(), contents,
                                      validation, validationRequest );
        }
        else
        {
            return new PathsPromoteResult( request, pending, Collections.emptySet(), Collections.emptySet(),
                                           validation );
        }
    }

    private Future<PathsPromoteResult> submitPathsPromoteRequest( PathsPromoteRequest request, final String baseUrl )
            throws IndyWorkflowException
    {
        return detectOverload( () ->
            asyncPromotionService.submit( () -> {
                PathsPromoteResult ret;
                try
                {
                    ret = doPathsPromotion( request, baseUrl );
                }
                catch ( Exception ex )
                {
                    String msg =
                            "Path promotion failed. Target: " + request.getTarget() + ", Source: " + request.getSource()
                                    + ", Reason: " + getStackTrace( ex );
                    logger.warn( msg );
                    ret = new PathsPromoteResult( request, msg );
                }

                if ( ret.getRequest().getCallback() != null )
                {
                    return callbackHelper.callback( ret.getRequest().getCallback(), ret );
                }

                return ret;
            } )
        );
    }

    /**
     * Attempt to resumePathsPromote from a previously failing {@link PathsPromoteResult}. This is meant to handle cases where a transient (or correctable) error
     * occurs on the server side, and promotion can proceed afterward. It works much like the {@link #promotePaths(PathsPromoteRequest, String)} call, using the pending
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
    public PathsPromoteResult resumePathsPromote( final PathsPromoteResult result, final String baseUrl )
            throws PromotionException, IndyWorkflowException
    {
        final PathsPromoteRequest request = result.getRequest();

        Future<PathsPromoteResult> future = submitResumePathsPromote( result, baseUrl );
        if ( request.isAsync() )
        {
            return new PathsPromoteResult( request ).accepted();
        }
        else
        {
            try
            {
                return future.get();
            }
            catch ( InterruptedException | ExecutionException e )
            {
                logger.error( "Path promotion resume failed: " + request.getSource() + " -> " + request.getTargetKey(), e );
                throw new PromotionException( "Execution of path promotion resume failed.", e );
            }
        }
    }

    private Future<PathsPromoteResult> submitResumePathsPromote( PathsPromoteResult result, String baseUrl )
            throws IndyWorkflowException
    {
        return detectOverload( () -> asyncPromotionService.submit( () -> {
            PathsPromoteResult ret;
            try
            {
                ret = doResumePathsPromote( result, baseUrl );
            }
            catch ( Exception ex )
            {
                final PathsPromoteRequest request = result.getRequest();
                String msg =
                        "Path promotion failed. Target: " + request.getTarget() + ", Source: " + request.getSource()
                                + ", Reason: " + getStackTrace( ex );
                logger.warn( msg );
                ret = new PathsPromoteResult( request, msg ).withPromotionId( result.getPromotionId() );
            }

            if ( ret.getRequest().getCallback() != null )
            {
                return callbackHelper.callback( ret.getRequest().getCallback(), ret );
            }

            return ret;
        } ) );
    }

    private PathsPromoteResult doResumePathsPromote( PathsPromoteResult result, String baseUrl )
            throws IndyWorkflowException, PromotionValidationException
    {
        final PathsPromoteRequest request = result.getRequest();

        final List<Transfer> contents = getTransfersForPaths( request.getSource(), result.getPendingPaths() );

        ValidationResult validation = new ValidationResult();
        ValidationRequest validationRequest = validator.validate( request, validation, baseUrl );
        PathsPromoteResult ret = runPathPromotions( request, result.getPendingPaths(), result.getCompletedPaths(),
                                                    result.getSkippedPaths(), contents, result.getValidations(),
                                                    validationRequest );

        return ret.withPromotionId( result.getPromotionId() );
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
        final PathsPromoteRequest request = result.getRequest();

        Future<PathsPromoteResult> future = submitRollbackPathsPromote( result );
        if ( request.isAsync() )
        {
            return new PathsPromoteResult( request ).accepted();
        }
        else
        {
            try
            {
                return future.get();
            }
            catch ( InterruptedException | ExecutionException e )
            {
                logger.error( "Path promotion rollback failed: " + request.getSource() + " -> " + request.getTargetKey(), e );
                throw new PromotionException( "Execution of path promotion rollback failed.", e );
            }
        }
    }

    private Future<PathsPromoteResult> submitRollbackPathsPromote( PathsPromoteResult result )
            throws IndyWorkflowException
    {
        return detectOverload( () -> asyncPromotionService.submit( () -> {
            PathsPromoteResult ret;
            try
            {
                ret = doRollbackPathsPromote( result );
            }
            catch ( Exception ex )
            {
                final PathsPromoteRequest request = result.getRequest();
                String msg = "Rollback path promotion failed. Target: " + request.getTarget() + ", Source: "
                        + request.getSource() + ", Reason: " + getStackTrace( ex );
                logger.warn( msg );
                ret = new PathsPromoteResult( request, msg ).withPromotionId( result.getPromotionId() );
            }

            if ( ret.getRequest().getCallback() != null )
            {
                return callbackHelper.callback( ret.getRequest().getCallback(), ret );
            }

            return ret;
        } ) );
    }

    private PathsPromoteResult doRollbackPathsPromote( PathsPromoteResult result )
            throws IndyWorkflowException
    {
        StoreKey targetKey = result.getRequest().getTarget();

        final List<Transfer> contents = getTransfersForPaths( targetKey, result.getCompletedPaths() );
        final Set<String> completed = result.getCompletedPaths();
        final Set<String> skipped = result.getSkippedPaths();

        if ( completed == null || completed.isEmpty() )
        {
            result.setError( null );
            return result;
        }

        Set<String> pending =
                        result.getPendingPaths() == null ? new HashSet<>() : new HashSet<>( result.getPendingPaths() );

        final AtomicReference<String> error = new AtomicReference<>();
        final boolean copyToSource = result.getRequest().isPurgeSource();

        ArtifactStore source = null;
        try
        {
            source = storeManager.getArtifactStore( result.getRequest().getSource() );
        }
        catch ( final IndyDataException e )
        {
            String msg =
                            String.format( "Failed to retrieve artifact store: %s. Reason: %s", result.getRequest().getSource(),
                                           e.getMessage() );

            logger.error( msg, e );
            error.set( msg );
        }

        AtomicReference<IndyWorkflowException> wfEx = new AtomicReference<>();
        if ( error.get() == null )
        {
            ArtifactStore src = source;
            byPathTargetLocks.lockAnd( targetKey, config.getLockTimeoutSeconds(), k -> {
                for ( final Transfer transfer : contents )
                {
                    if ( transfer != null && transfer.exists() )
                    {
                        try
                        {
                            if ( copyToSource )
                            {
                                final String path = transfer.getPath();
                                try (InputStream stream = transfer.openInputStream( true ))
                                {
                                    contentManager.store( src, path, stream, TransferOperation.UPLOAD,
                                                          new EventMetadata() );
                                }
                                catch ( IndyWorkflowException e )
                                {
                                    wfEx.set( e );
                                    return null;
                                }
                            }

                            transfer.delete( true );
                            completed.remove( transfer.getPath() );
                            pending.add( transfer.getPath() );
                        }
                        catch ( final IOException e )
                        {
                            String msg = String.format( "Failed to rollback path promotion of: %s from: %s. Reason: %s",
                                                        transfer, result.getRequest().getSource(), e.getMessage() );
                            logger.error( msg, e );
                            error.set( msg );
                        }
                    }
                }

                return null;
            }, ( k, lock ) -> {
                String msg = String.format( "Failed to acquire promotion lock on target: %s in %d seconds.", targetKey,
                                            config.getLockTimeoutSeconds() );
                logger.warn( msg );
                error.set( msg );

                return false;
            } );

            IndyWorkflowException wfException = wfEx.get();
            if ( wfException != null )
            {
                throw wfException;
            }

        }

        PathsPromoteResult ret = new PathsPromoteResult( result.getRequest(), pending, completed, skipped, error.get(),
                                                         new ValidationResult() );
        return ret.withPromotionId( result.getPromotionId() );
    }

    private PathsPromoteResult runPathPromotions( final PathsPromoteRequest request, final Set<String> pending,
                                                  final Set<String> prevComplete, final Set<String> prevSkipped,
                                                  final List<Transfer> contents, ValidationResult validation,
                                                  final ValidationRequest validationRequest )
            throws IndyWorkflowException
    {
        if ( pending == null || pending.isEmpty() )
        {
            return new PathsPromoteResult( request, pending, prevComplete, prevSkipped, validation );
        }

        StoreKey targetKey = request.getTarget();

        final Set<String> complete = prevComplete == null ? new HashSet<>() : new HashSet<>( prevComplete );
        final Set<String> skipped = prevSkipped == null ? new HashSet<>() : new HashSet<>( prevSkipped );

        List<String> errors = new ArrayList<>();

        ArtifactStore sourceStore = null;
        try
        {
            sourceStore = storeManager.getArtifactStore( request.getSource() );
        }
        catch ( IndyDataException e )
        {
            String msg = String.format( "Failed to retrieve artifact store: %s. Reason: %s", request.getSource(),
                                        e.getMessage() );
            errors.add( msg );
            logger.error( msg, e );
        }

        ArtifactStore targetStore = null;
        try
        {
            targetStore = storeManager.getArtifactStore( request.getTarget() );
        }
        catch ( IndyDataException e )
        {
            String msg = String.format( "Failed to retrieve artifact store: %s. Reason: %s", request.getTarget(),
                                        e.getMessage() );
            errors.add( msg );
            logger.error( msg, e );
        }

        if ( targetStore == null )
        {
            String msg = String.format( "Failed to retrieve artifact store: %s", request.getTarget() );
            errors.add( msg );
            logger.error( msg );
        }

        if ( errors.isEmpty() )
        {
            ArtifactStore src = sourceStore;
            ArtifactStore tgt = targetStore;

            AtomicReference<IndyWorkflowException> wfError = new AtomicReference<>();

            Set<PathTransferResult> results =
                    byPathTargetLocks.lockAnd( targetKey, config.getLockTimeoutSeconds(), k -> {
                        logger.info( "Running promotions from: {} (key: {})\n  to: {} (key: {})", src,
                                     request.getSource(), tgt, request.getTarget() );

                        DrainingExecutorCompletionService<PathTransferResult> svc =
                                new DrainingExecutorCompletionService<>( transferService );

                        try
                        {
                            detectOverloadVoid( () -> contents.forEach( ( transfer ) -> svc.submit(
                                    newPathsPromoteTransfer( transfer, src, tgt, request ) ) ) );

                            Set<PathTransferResult> pathResults = new HashSet<>();
                            try
                            {
                                svc.drain( ptr -> pathResults.add( ptr ) );
                            }
                            catch ( InterruptedException | ExecutionException e )
                            {
                                Set<String> paths;
                                try
                                {
                                    paths = validationRequest.getSourcePaths();
                                }
                                catch ( PromotionValidationException e1 )
                                {
                                    paths = contents.stream()
                                                    .map( txfr -> txfr.getPath() )
                                                    .collect( Collectors.toSet() );
                                }

                                logger.error(
                                        String.format( "Error waiting for promotion of: %s to: %s\nPaths:\n\n%s\n\n",
                                                       request.getSource(), targetKey, paths ), e );
                            }

                            try
                            {
                                clearStoreNFC( validationRequest.getSourcePaths(), tgt );
                            }
                            catch ( IndyDataException | PromotionValidationException e )
                            {
                                String msg = String.format( "Failed to promote to: %s. Reason: %s", tgt, e.getMessage() );
                                errors.add( msg );
                                logger.error( msg, e );
                            }
                            return pathResults;
                        }
                        catch ( IndyWorkflowException e )
                        {
                            wfError.set( e );
                            return null;
                        }
                    }, ( k, lock ) -> {
                        String error = String.format( "Failed to acquire promotion lock on target: %s in %d seconds.",
                                                      targetKey, config.getLockTimeoutSeconds() );

                        errors.add( error );
                        logger.warn( error );

                        return false;
                    } );

            if ( wfError.get() != null )
            {
                throw wfError.get();
            }

            if ( results != null )
            {
                results.forEach( pathResult -> {
                    if ( pathResult.traversed )
                    {
                        pending.remove( pathResult.path );
                    }

                    if ( pathResult.skipped )
                    {
                        skipped.add( pathResult.path );
                    }

                    if ( pathResult.completed )
                    {
                        complete.add( pathResult.path );
                    }

                    if ( pathResult.error != null )
                    {
                        errors.add( pathResult.error );
                    }
                } );
            }
        }

        String error = null;

        if ( !errors.isEmpty() )
        {
            error = StringUtils.join( errors, "\n" );
        }

        PathsPromoteResult result = new PathsPromoteResult( request, pending, complete, skipped, error, validation );

        if ( request.isFireEvents() )
        {
            PathsPromoteCompleteEvent evt = new PathsPromoteCompleteEvent( result );
            fireEvent( promoteCompleteEvent, evt );
        }
        return result;
    }

    private static final class PathTransferResult
    {
        public String error;

        public boolean traversed = false;

        public boolean skipped = false;

        public boolean completed = false;

        public final String path;

        public PathTransferResult( final String path )
        {
            this.path = path;
        }
    }

    private Callable<PathTransferResult> newPathsPromoteTransfer( final Transfer transfer, final ArtifactStore src,
                                                                  final ArtifactStore tgt,
                                                                  final PathsPromoteRequest request )
    {
        return () -> {
            PathTransferResult result = new PathTransferResult( transfer.getPath() );
            final String path = transfer.getPath();

            if ( !transfer.exists() )
            {
                result.traversed = true;
                result.skipped = true;
            }
            else
            {
                try
                {
                    Transfer target = contentManager.getTransfer( tgt, path, TransferOperation.UPLOAD );
                    //                        synchronized ( target )
                    //                        {
                    // TODO: Should the request object have an overwrite attribute? Is that something the user is qualified to decide?
                    if ( target != null && target.exists() )
                    {
                        logger.warn( "NOT promoting: {} from: {} to: {}. Target file already exists.", path,
                                     request.getSource(), request.getTarget() );

                        // TODO: There's no guarantee that the pre-existing content is the same!
                        result.traversed = true;
                        result.skipped = true;
                    }
                    else
                    {
                        try (InputStream stream = transfer.openInputStream( true ))
                        {
                            contentManager.store( tgt, path, stream, TransferOperation.UPLOAD, new EventMetadata() );

                            result.traversed = true;
                            result.completed = true;

                            stream.close();

                            if ( request.isPurgeSource() )
                            {
                                contentManager.delete( src, path, new EventMetadata() );
                            }
                        }
                        catch ( final IOException e )
                        {
                            String msg = String.format( "Failed to open input stream for: %s. Reason: %s", transfer,
                                                        e.getMessage() );
                            result.error = msg;
                            logger.error( msg, e );
                        }
                    }

                }
                catch ( final IndyWorkflowException e )
                {
                    String msg = String.format( "Failed to promote path: %s to: %s. Reason: %s", transfer, tgt,
                                                e.getMessage() );
                    result.error = msg;
                    logger.error( msg, e );
                }
            }

            return result;
        };
    }

    private List<Transfer> getTransfersForPaths( final StoreKey source, final Set<String> paths )
            throws IndyWorkflowException
    {
        final List<Transfer> contents = new ArrayList<>();
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

    /**
     * NOTE: Adding sourcePaths parameter here to cut down on number of paths for clearing from NFC.
     *
     * @param sourcePaths The set of paths that need to be cleared from the NFC.
     * @param store The store whose affected groups should have their NFC entries cleared
     * @throws IndyDataException
     */
    private void clearStoreNFC( final Set<String> sourcePaths, ArtifactStore store )
            throws IndyDataException
    {
        Set<String> paths = sourcePaths.stream()
                                       .map( sp -> sp.startsWith( "/" ) && sp.length() > 1 ? sp.substring( 1 ) : sp )
                                       .collect( Collectors.toSet() );

        paths.forEach( path -> {
            ConcreteResource resource = new ConcreteResource( LocationUtils.toLocation( store ), path );

            logger.debug( "Clearing NFC path: {} from: {}\n\tResource: {}", path, store.getKey(), resource );
            nfc.clearMissing( resource );
        } );

        Set<Group> groups = storeManager.query().getGroupsAffectedBy( store.getKey() );
        if ( groups != null )
        {
            groups.forEach( group -> paths.forEach( path -> {
                ConcreteResource resource = new ConcreteResource( LocationUtils.toLocation( group ), path );

                logger.debug( "Clearing NFC path: {} from: {}\n\tResource: {}", path, group.getKey(), resource );
                nfc.clearMissing( resource );
            } ) );
        }
    }

}
