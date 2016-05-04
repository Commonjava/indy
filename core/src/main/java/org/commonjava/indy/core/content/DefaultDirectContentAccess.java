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
package org.commonjava.indy.core.content;

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.content.DirectContentAccess;
import org.commonjava.indy.content.DownloadManager;
import org.commonjava.indy.content.StoreResource;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jdcasey on 5/2/16.
 */
public class DefaultDirectContentAccess
        implements DirectContentAccess
{

    @Inject
    private DownloadManager downloadManager;

    public DefaultDirectContentAccess(){}

    public DefaultDirectContentAccess( DownloadManager downloadManager )
    {
        this.downloadManager = downloadManager;
    }

    public List<Transfer> retrieveAllRaw( final List<? extends ArtifactStore> stores, final String path,
                                          final EventMetadata eventMetadata )
            throws IndyWorkflowException
    {
        final List<Transfer> txfrs = new ArrayList<Transfer>();
        for ( final ArtifactStore store : stores )
        {
            final Transfer txfr = retrieveRaw( store, path, eventMetadata );
            if ( txfr != null )
            {
                txfrs.add( txfr );
            }
        }

        return txfrs;
    }

    public Transfer retrieveRaw( final ArtifactStore store, final String path, final EventMetadata eventMetadata )
            throws IndyWorkflowException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.info( "Attempting to retrieve: {} from: {}", path, store.getKey() );

        Transfer item = null;
        try
        {
            item = downloadManager.retrieve( store, path, eventMetadata );
        }
        catch ( IndyWorkflowException e )
        {
            e.filterLocationErrors();
        }

        return item;
    }

    @Override
    public Transfer getTransfer( final ArtifactStore store, final String path )
            throws IndyWorkflowException
    {
        return downloadManager.getStorageReference( store, path );
    }

    @Override
    public Transfer getTransfer( final StoreKey key, final String path )
            throws IndyWorkflowException
    {
        return downloadManager.getStorageReference( key, path );
    }

    @Override
    public List<StoreResource> listRaw( ArtifactStore store, String path )
            throws IndyWorkflowException
    {
        return downloadManager.list( store, path );
    }

}
