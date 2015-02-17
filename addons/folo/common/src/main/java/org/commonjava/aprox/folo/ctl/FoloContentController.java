/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
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
import org.commonjava.maven.galley.model.TransferOperation;

/**
 * Wrapper around {@link ContentController} that accepts {@link TrackingKey} in place of {@link StoreKey}, and uses the extra tracking ID it contains 
 * to record artifact accesses.
 * 
 * @author jdcasey
 */
@ApplicationScoped
public class FoloContentController
{

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

    public Transfer get( final TrackingKey tracking, final String path )
        throws AproxWorkflowException
    {
        final Transfer item = contentController.get( tracking.getTrackedStore(), path );

        final StoreKey affectedKey = LocationUtils.getKey( item );
        try
        {
            recordManager.recordArtifact( tracking, affectedKey, path, StoreEffect.DONWLOAD );
        }
        catch ( final FoloContentException e )
        {
            throw new AproxWorkflowException( "Failed to record download: %s. Reason: %s", e, path, e.getMessage() );
        }

        return item;
    }

    public Transfer store( final TrackingKey tracking, final String path, final InputStream stream )
        throws AproxWorkflowException
    {
        final Transfer item = contentController.store( tracking.getTrackedStore(), path, stream );

        final StoreKey affectedKey = LocationUtils.getKey( item );
        try
        {
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

    public String renderListing( final String standardAccept, final TrackingKey tk, final String path,
                                 final String baseUri, final UriFormatter uriFormatter )
        throws AproxWorkflowException
    {
        return contentController.renderListing( standardAccept, tk.getTrackedStore(), path, baseUri, uriFormatter );
    }

    public boolean isHtmlContent( final Transfer item )
        throws AproxWorkflowException
    {
        return contentController.isHtmlContent( item );
    }

    public Transfer getTransfer( final TrackingKey trackingKey, final String path, final TransferOperation op )
        throws AproxWorkflowException
    {
        final Transfer item = contentController.getTransfer( trackingKey.getTrackedStore(), path, op );

        final StoreKey affectedKey = LocationUtils.getKey( item );
        try
        {
            recordManager.recordArtifact( trackingKey, affectedKey, path, StoreEffect.UPLOAD );
        }
        catch ( final FoloContentException e )
        {
            throw new AproxWorkflowException( "Failed to record upload: %s. Reason: %s", e, path, e.getMessage() );
        }

        return item;
    }

}
