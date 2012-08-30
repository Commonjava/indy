/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.commonjava.aprox.core.rest.util.retrieve;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.copy;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;

import org.commonjava.aprox.core.change.event.FileStorageEvent;
import org.commonjava.aprox.core.io.StorageItem;
import org.commonjava.aprox.core.model.ArtifactStore;
import org.commonjava.aprox.core.model.DeployPoint;
import org.commonjava.aprox.core.model.Group;
import org.commonjava.aprox.core.rest.AproxWorkflowException;
import org.commonjava.aprox.core.rest.util.FileManager;
import org.commonjava.aprox.core.rest.util.MavenMetadataMerger;
import org.commonjava.util.logging.Logger;

@Singleton
public class MavenMetadataHandler
    implements GroupPathHandler
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private FileManager fileManager;

    @Inject
    private MavenMetadataMerger merger;

    @Inject
    private Event<FileStorageEvent> fileEvent;

    @Override
    public boolean canHandle( final String path )
    {
        return path.endsWith( MavenMetadataMerger.METADATA_NAME );
    }

    @Override
    public StorageItem retrieve( final Group group, final List<? extends ArtifactStore> stores, final String path )
        throws AproxWorkflowException
    {
        final File target = fileManager.formatStorageReference( group, path );
        final File targetInfo =
            fileManager.formatStorageReference( group, path + MavenMetadataMerger.METADATA_MERGEINFO_SUFFIX );

        if ( !target.exists() )
        {
            final Set<StorageItem> sources = fileManager.retrieveAll( stores, path );
            final InputStream merged = merger.merge( sources, group, path );
            if ( merged != null )
            {
                FileOutputStream fos = null;
                try
                {
                    final File dir = target.getParentFile();
                    dir.mkdirs();

                    fos = new FileOutputStream( target );
                    copy( merged, fos );

                    if ( fileEvent != null )
                    {
                        fileEvent.fire( new FileStorageEvent( FileStorageEvent.Type.GENERATE, group, path, target ) );
                    }
                }
                catch ( final IOException e )
                {
                    throw new AproxWorkflowException( Response.serverError()
                                                             .build(),
                                                     "Failed to write merged metadata to: %s.\nError: %s", e, target,
                                                     e.getMessage() );
                }
                finally
                {
                    closeQuietly( merged );
                    closeQuietly( fos );
                }

                FileWriter fw = null;
                try
                {
                    fw = new FileWriter( targetInfo );
                    for ( final StorageItem source : sources )
                    {
                        fw.write( source.getStoreKey()
                                        .toString() );
                        fw.write( "\n" );
                    }
                }
                catch ( final IOException e )
                {
                    logger.error( "Failed to write merged metadata information to: %s.\nError: %s", e, targetInfo,
                                  e.getMessage() );
                }
                finally
                {
                    closeQuietly( fw );
                }
            }
        }

        if ( target.exists() )
        {
            try
            {
                return new StorageItem( group.getKey(), path, new BufferedInputStream( new FileInputStream( target ) ) );
            }
            catch ( final FileNotFoundException e )
            {
                throw new AproxWorkflowException(
                                                 Response.serverError()
                                                         .build(),
                                                 "Cannot find file: %s, EVEN THOUGH WE JUST VERIFIED ITS EXISTENCE.\nError: %s",
                                                 e, target, e.getMessage() );
            }
        }

        return null;
    }

    @Override
    public DeployPoint store( final Group group, final List<? extends ArtifactStore> stores, final String path,
                              final InputStream stream )
        throws AproxWorkflowException
    {
        if ( path.endsWith( MavenMetadataMerger.METADATA_NAME ) )
        {
            // delete so it'll be recomputed.
            final File target = fileManager.formatStorageReference( group, path );
            target.delete();
        }

        return fileManager.store( stores, path, stream );
    }

}
