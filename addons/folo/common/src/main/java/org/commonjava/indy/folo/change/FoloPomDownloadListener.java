/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.folo.change;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.content.ContentManager;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.atlas.maven.ident.util.ArtifactPathInfo;
import org.commonjava.maven.galley.event.FileStorageEvent;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.commonjava.maven.galley.spi.cache.CacheProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a listener that tracks file storage events. If a non-pom artifact download occurs it ensures that also
 * associated pom artifact gets downloaded even if it was not requested.
 *
 * @author pkocandr
 */
@ApplicationScoped
public class FoloPomDownloadListener
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ContentManager contentManager;

    @Inject
    private CacheProvider cacheProvider;

    @Inject
    private StoreDataManager storeManager;

    public void onFileUpload( @Observes final FileStorageEvent event )
    {
        // check for a TransferOperation of DOWNLOAD
        final TransferOperation op = event.getType();
        if ( op != TransferOperation.DOWNLOAD )
        {
            logger.trace( "Not a download transfer operation. No pom existence check performed." );
            return;
        }

        // check if it is a path that doesn't end in with ".pom"
        final Transfer transfer = event.getTransfer();
        if ( transfer == null )
        {
            logger.trace( "No transfer. No pom existence check performed." );
            return;
        }

        String txfrPath = transfer.getPath();
        if ( txfrPath.endsWith( ".pom" ) )
        {
            logger.trace( "This is a pom download." );
            return;
        }

        // use ArtifactPathInfo to parse into a GAV, just to verify that it's looking at an artifact download
        ArtifactPathInfo artPathInfo = ArtifactPathInfo.parse( txfrPath );
        if ( artPathInfo == null )
        {
            logger.trace( "Not an artifact download ({}). No pom existence check performed.", txfrPath );
            return;
        }

        // verify that the associated .pom file exists
        String pomFilename = String.format( "%s-%s.pom", artPathInfo.getArtifactId(), artPathInfo.getVersion() );
        ConcreteResource pomResource = transfer.getResource().getParent().getChild( pomFilename );
        if ( cacheProvider.exists( pomResource ) )
        {
            logger.trace( "Pom {} already exists.", cacheProvider.getFilePath( pomResource ) );
            return;
        }

        // trigger pom download by requesting it from the same repository as the original artifact
        StoreKey storeKey = StoreKey.fromString( transfer.getLocation().getName() );
        ArtifactStore store;
        try
        {
            store = storeManager.getArtifactStore( storeKey );
        }
        catch ( final IndyDataException ex )
        {
            logger.error( "Error retrieving artifactStore with key " + storeKey, ex );
            return;
        }
        try
        {
            logger.debug( "Downloading POM as automatic response to associated artifact download: {}/{}", storeKey,
                          pomResource.getPath() );
            contentManager.retrieve( store, pomResource.getPath(), event.getEventMetadata() );
        }
        catch ( final IndyWorkflowException ex )
        {
            logger.error( "Error while retrieving pom artifact " + pomResource.getPath() + " from store " + store, ex );
            return;
        }
    }

}
