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
package org.commonjava.aprox.folo.ctl;

import java.io.InputStream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.core.ctl.ContentController;
import org.commonjava.aprox.folo.data.FoloContentException;
import org.commonjava.aprox.folo.data.FoloRecordManager;
import org.commonjava.aprox.folo.model.StoreEffect;
import org.commonjava.aprox.folo.model.TrackedContentRecord;
import org.commonjava.aprox.folo.model.TrackingKey;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.util.LocationUtils;
import org.commonjava.aprox.util.UriFormatter;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper around {@link ContentController} that accepts {@link TrackingKey} in place of {@link StoreKey}, and uses the extra tracking ID it contains 
 * to record artifact accesses.
 * 
 * @author jdcasey
 */
@ApplicationScoped
public class FoloContentController
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ContentController contentController;

    @Inject
    private FoloRecordManager recordManager;

    protected FoloContentController()
    {
    }

    public FoloContentController( final ContentController contentController, final FoloRecordManager recordManager )
    {
        this.contentController = contentController;
        this.recordManager = recordManager;
    }

    public TrackedContentRecord getRecord( final TrackingKey key )
        throws AproxWorkflowException
    {
        try
        {
            return recordManager.getRecord( key );
        }
        catch ( final FoloContentException e )
        {
            throw new AproxWorkflowException( "Failed to retrieve tracking record for: %s. Reason: %s", e, key,
                                              e.getMessage() );
        }
    }

    // provided for consistent interface / layering, rather than requiring binding classes to inject the record manager itself.
    public void clearRecord( final TrackingKey key )
    {
        recordManager.clearRecord( key );
    }

    // provided for consistent interface / layering, rather than requiring binding classes to inject the record manager itself.
    public boolean hasRecord( final TrackingKey key )
    {
        return recordManager.hasRecord( key );
    }

    public Transfer get( final TrackingKey tracking, final StoreKey key, final String path )
        throws AproxWorkflowException
    {
        final Transfer item = contentController.get( key, path );

        final StoreKey affectedKey = LocationUtils.getKey( item );
        try
        {
            logger.debug( "Tracking download of: {} from: {} in report: {}", path, key, tracking );
            recordManager.recordArtifact( tracking, affectedKey, path, StoreEffect.DOWNLOAD );
        }
        catch ( final FoloContentException e )
        {
            throw new AproxWorkflowException( "Failed to record download: %s. Reason: %s", e, path, e.getMessage() );
        }

        return item;
    }

    public Transfer store( final TrackingKey tracking, final StoreKey key, final String path, final InputStream stream )
        throws AproxWorkflowException
    {
        final Transfer item = contentController.store( key, path, stream );

        final StoreKey affectedKey = LocationUtils.getKey( item );
        try
        {
            logger.debug( "Tracking upload of: {} to: {} in report: {}", path, key, tracking );
            recordManager.recordArtifact( tracking, affectedKey, path, StoreEffect.UPLOAD );
        }
        catch ( final FoloContentException e )
        {
            throw new AproxWorkflowException( "Failed to record upload: %s. Reason: %s", e, path, e.getMessage() );
        }

        return item;
    }

    public String getContentType( final String path )
    {
        return contentController.getContentType( path );
    }

    public String renderListing( final String standardAccept, final TrackingKey tk, final StoreKey key,
                                 final String path,
                                 final String baseUri, final UriFormatter uriFormatter )
        throws AproxWorkflowException
    {
        return contentController.renderListing( standardAccept, key, path, baseUri, uriFormatter );
    }

    public boolean isHtmlContent( final Transfer item )
        throws AproxWorkflowException
    {
        return contentController.isHtmlContent( item );
    }

    //    public Transfer getTransfer( final TrackingKey tracking, final String path, final TransferOperation op )
    //        throws AproxWorkflowException
    //    {
    //        final Transfer item = contentController.getTransfer( tracking.getTrackedStore(), path, op );
    //
    //        final StoreKey affectedKey = LocationUtils.getKey( item );
    //        try
    //        {
    //            logger.debug( "Tracking upload of: {} to: {} in report: {}", path, tracking.getTrackedStore(), tracking.getId() );
    //            recordManager.recordArtifact( tracking, affectedKey, path, StoreEffect.UPLOAD );
    //        }
    //        catch ( final FoloContentException e )
    //        {
    //            throw new AproxWorkflowException( "Failed to record upload: %s. Reason: %s", e, path, e.getMessage() );
    //        }
    //
    //        return item;
    //    }

}
