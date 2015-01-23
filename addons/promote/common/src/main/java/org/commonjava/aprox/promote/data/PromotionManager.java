package org.commonjava.aprox.promote.data;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.content.ContentManager;
import org.commonjava.aprox.content.DownloadManager;
import org.commonjava.aprox.data.AproxDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.promote.model.PromoteRequest;
import org.commonjava.aprox.promote.model.PromoteResult;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component responsible for orchestrating the transfer of artifacts from one store to another, according to the given {@link PromoteRequest} or 
 * {@link PromoteResult}. Currently provides promote, resume, and rollback (the latter two for dealing with failed promote calls).
 * 
 * @author jdcasey
 */
@ApplicationScoped
public class PromotionManager
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ContentManager contentManager;

    @Inject
    private DownloadManager downloadManager;

    @Inject
    private StoreDataManager storeManager;

    protected PromotionManager()
    {
    }

    public PromotionManager( final ContentManager contentManager, final DownloadManager downloadManager,
                             final StoreDataManager storeManager )
    {
        this.contentManager = contentManager;
        this.downloadManager = downloadManager;
        this.storeManager = storeManager;
    }

    /**
     * Promote artifacts from the source to the target given in the {@link PromoteRequest}. If a set of paths are given, only try to promote those.
     * Otherwise, build a recursive list of available artifacts in the source store, and try to promote them all.
     * 
     * @param request The request containing source and target store keys, and an optional list of paths to promote
     * 
     * @return The result, including the source and target store keys used, the paths completed (promoted successfully), the pending paths (those that 
     * weren't processed due to some error...or null), and a nullable error explaining what (if anything) went wrong with the promotion.
     * 
     * @throws PromotionException
     * @throws AproxWorkflowException
     */
    public PromoteResult promote( final PromoteRequest request )
        throws PromotionException, AproxWorkflowException
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

        if ( request.isDryRun() )
        {
            return new PromoteResult( request, pending, Collections.<String> emptySet(), null );
        }

        return runPromotions( request, pending, new HashSet<String>(), contents );
    }

    /**
     * Attempt to resume from a previously failing {@link PromoteResult}. This is meant to handle cases where a transient (or correctable) error
     * occurs on the server side, and promotion can proceed afterward. It works much like the {@link #promote(PromoteRequest)} call, using the pending
     * paths list from the input result as the list of paths to process. The output {@link PromoteResult} contains all previous completed paths PLUS
     * any additional completed transfers when it is returned, thus providing a cumulative result to the user.
     * 
     * @param result The result to resume
     * 
     * @return The same result, with any successful path promotions moved from the pending to completed paths list, and the error cleared (or set to a 
     * new error)
     * 
     * @throws PromotionException
     * @throws AproxWorkflowException
     */
    public PromoteResult resume( final PromoteResult result )
        throws PromotionException, AproxWorkflowException
    {
        final List<Transfer> contents = getTransfersForPaths( result.getRequest()
                                                                    .getSource(), result.getPendingPaths() );

        return runPromotions( result.getRequest(), result.getPendingPaths(), result.getCompletedPaths(), contents );
    }

    /**
     * Attempt to rollback a previously failing {@link PromoteResult}. This is meant to handle cases where an unrecoverable error
     * occurs on the server side, and promotion can NOT proceed afterward. All paths in the completed paths set are deleted from the target, if 
     * possible. The output {@link PromoteResult} contains the previous content, with any successfully removed target paths moved back from the
     * completed-paths list to the pending-paths list. If an error occurs during rollback, the error field will be set...otherwise, it will be null.
     * 
     * @param result The result to rollback
     * 
     * @return The same result, with any successful deletions moved from the completed to pending paths list, and the error cleared (or set to a 
     * new error)
     * 
     * @throws PromotionException
     * @throws AproxWorkflowException
     */
    public PromoteResult rollback( final PromoteResult result )
        throws PromotionException, AproxWorkflowException
    {
        final List<Transfer> contents = getTransfersForPaths( result.getRequest()
                                                                    .getTarget(), result.getCompletedPaths() );
        final Set<String> completed = result.getCompletedPaths();
        final Set<String> pending = result.getPendingPaths();
        String error = null;
        final boolean copyToSource = result.getRequest()
                                           .isPurgeSource();

        ArtifactStore source = null;
        try
        {
            source = storeManager.getArtifactStore( result.getRequest()
                                                          .getSource() );
        }
        catch ( final AproxDataException e )
        {
            error =
                String.format( "Failed to retrieve artifact store: %s. Reason: %s", result.getRequest()
                                                                                          .getSource(), e.getMessage() );
            logger.error( error, e );
        }

        if ( error == null )
        {
            for ( final Transfer transfer : contents )
            {
                if ( transfer.exists() )
                {
                    InputStream stream = null;
                    try
                    {
                        if ( copyToSource )
                        {
                            stream = transfer.openInputStream( true );
                            final String path = transfer.getPath();
                            contentManager.store( source, path, stream, TransferOperation.UPLOAD );
                        }

                        transfer.delete( true );
                        completed.remove( transfer.getPath() );
                        pending.add( transfer.getPath() );
                    }
                    catch ( final IOException e )
                    {
                        error =
                            String.format( "Failed to rollback promotion of: %s from: %s. Reason: %s", transfer,
                                           result.getRequest()
                                                 .getSource(), e.getMessage() );
                        logger.error( error, e );
                    }
                    finally
                    {
                        closeQuietly( stream );
                    }
                }
            }
        }

        return new PromoteResult( result.getRequest(), pending, completed, error );
    }

    private PromoteResult runPromotions( final PromoteRequest request, final Set<String> pending,
                                         final Set<String> prevComplete, final List<Transfer> contents )
    {
        final Set<String> complete = prevComplete == null ? new HashSet<String>() : new HashSet<>( prevComplete );

        String error = null;
        ArtifactStore sourceStore = null;
        ArtifactStore targetStore = null;
        try
        {
            sourceStore = storeManager.getArtifactStore( request.getSource() );
            targetStore = storeManager.getArtifactStore( request.getTarget() );
        }
        catch ( final AproxDataException e )
        {
            error =
                String.format( "Failed to retrieve artifact store: %s. Reason: %s", request.getSource(), e.getMessage() );
            logger.error( error, e );
        }

        if ( error == null )
        {
            final boolean purgeSource = request.isPurgeSource();
            for ( final Transfer transfer : contents )
            {
                InputStream stream = null;
                try
                {
                    stream = transfer.openInputStream( true );
                    final String path = transfer.getPath();
                    contentManager.store( targetStore, path, stream, TransferOperation.UPLOAD );
                    pending.remove( path );
                    complete.add( path );

                    if ( purgeSource )
                    {
                        contentManager.delete( sourceStore, path );
                    }
                }
                catch ( final IOException e )
                {
                    error = String.format( "Failed to open input stream for: %s. Reason: %s", transfer, e.getMessage() );
                    logger.error( error, e );
                    break;
                }
                catch ( final AproxWorkflowException e )
                {
                    error =
                        String.format( "Failed to promote: %s to: %s. Reason: %s", transfer, targetStore,
                                       e.getMessage() );
                    logger.error( error, e );
                    break;
                }
                finally
                {
                    closeQuietly( stream );
                }
            }
        }

        return new PromoteResult( request, pending, complete, error );
    }

    private List<Transfer> getTransfersForPaths( final StoreKey source, final Set<String> paths )
        throws AproxWorkflowException
    {
        final List<Transfer> contents = new ArrayList<Transfer>();
        for ( final String path : paths )
        {
            final Transfer txfr = downloadManager.getStorageReference( source, path );
            if ( !txfr.exists() )
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
