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
package org.commonjava.indy.relate.content;

import org.commonjava.cdi.util.weft.ThreadContext;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.content.AbstractContentGenerator;
import org.commonjava.indy.content.DirectContentAccess;
import org.commonjava.indy.content.DownloadManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.relate.conf.RelateConfig;
import org.commonjava.indy.relate.util.RelateGenerationManager;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.commonjava.maven.galley.transport.htcli.model.HttpExchangeMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import java.io.IOException;

import static org.commonjava.indy.relate.util.RelateGenerationManager.POM_SUFFIX;
import static org.commonjava.indy.relate.util.RelateGenerationManager.REL_DIRECT_GENERATING;
import static org.commonjava.indy.relate.util.RelateGenerationManager.REL_SUFFIX;

/**
 * Created by ruhan on 3/21/17.
 */
public class RelateContentGenerator
                extends AbstractContentGenerator
{
    @Inject
    private RelateConfig config;

    @Inject
    private RelateGenerationManager relateGenerationManager;

    @Inject
    private DirectContentAccess fileManager;

    @Inject
    private DownloadManager downloadManager;

    @Override
    public Transfer generateFileContent( final ArtifactStore store, final String path,
                                         final EventMetadata eventMetadata ) throws IndyWorkflowException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );

        if ( !config.isEnabled() )
        {
            logger.debug( "Relate Add-on is not enabled." );
            return null;
        }

        logger.info( "Generate .rel for: {}/{}", store.getKey(), path );

        if ( !canProcess( path ) )
        {
            logger.debug( "Not a .rel request" );
            return null;
        }

        logger.debug( "Check for POM matching: {}", path );

        String pomPath = path.substring( 0, path.length() - REL_SUFFIX.length() );
        if ( !pomPath.endsWith( POM_SUFFIX ) )
        {
            logger.debug( "Not a POM {}", pomPath );
            return null;
        }

        Transfer pomTransfer = fileManager.getTransfer( store, path );
        if ( !exists( pomTransfer ) )
        {
            ThreadContext threadContext = ThreadContext.getContext( true );
            threadContext.put( REL_DIRECT_GENERATING, true );

            pomTransfer = downloadManager.retrieve( store, pomPath, eventMetadata );
            if ( !exists( pomTransfer ) )
            {
                logger.debug( "POM not exists for request {}", pomPath );
                return null;
            }
        }

        Transfer result = relateGenerationManager.generateRelationshipFile( pomTransfer, TransferOperation.DOWNLOAD );

        if ( result != null )
        {
            Transfer meta = result.getSiblingMeta( HttpExchangeMetadata.FILE_EXTENSION );
            try
            {
                meta.delete(); // delete *.rel.http-metadata.json, which is created unnecessarily during the unsuccessful get request
                // TODO: If mark .rel as generated in SpecialPathManager, should be able to avoid creating the meta file in the first place...
            }
            catch ( IOException e )
            {
                logger.debug( "Delete meta {} failed", meta, e );
            }
        }

        // Finally, pass the Transfer back.
        return result;
    }

    @Override
    public boolean canProcess( String path )
    {
        if ( path.endsWith( REL_SUFFIX ) )
        {
            return true;
        }
        return false;
    }

    private boolean exists( Transfer transfer )
    {
        return transfer != null && transfer.exists();
    }
}
