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
package org.commonjava.aprox.core.content;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.commonjava.maven.galley.util.PathUtils.normalize;
import static org.commonjava.maven.galley.util.PathUtils.parentPath;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.content.DownloadManager;
import org.commonjava.aprox.content.StoreResource;
import org.commonjava.aprox.core.content.group.ArchetypeCatalogMerger;
import org.commonjava.aprox.core.content.group.GroupMergeHelper;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.util.LocationUtils;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;

@ApplicationScoped
public class ArchetypeCatalogGenerator
    extends AbstractMergedContentGenerator
{

    private static final Set<String> HANDLED_FILENAMES = Collections.unmodifiableSet( new HashSet<String>()
    {

        {
            add( ArchetypeCatalogMerger.CATALOG_NAME );
            add( ArchetypeCatalogMerger.CATALOG_MD5_NAME );
            add( ArchetypeCatalogMerger.CATALOG_SHA_NAME );
        }

        private static final long serialVersionUID = 1L;

    } );

    @Inject
    private ArchetypeCatalogMerger merger;

    protected ArchetypeCatalogGenerator()
    {
    }

    public ArchetypeCatalogGenerator( final DownloadManager downloadManager, final StoreDataManager storeManager,
                                      final ArchetypeCatalogMerger merger,
                                      final GroupMergeHelper mergeHelper )
    {
        super( downloadManager, storeManager, mergeHelper );
        this.merger = merger;
        this.helper = mergeHelper;
    }

    private boolean canProcess( final String path )
    {
        for ( final String filename : HANDLED_FILENAMES )
        {
            if ( path.endsWith( filename ) )
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public Transfer generateGroupFileContent( final Group group, final List<ArtifactStore> members, final String path )
        throws AproxWorkflowException
    {
        if ( !canProcess( path ) )
        {
            return null;
        }

        final Transfer target = fileManager.getStorageReference( group, path );

        if ( !target.exists() )
        {
            String toMergePath = path;
            if ( !path.endsWith( ArchetypeCatalogMerger.CATALOG_NAME ) )
            {
                toMergePath = normalize( normalize( parentPath( toMergePath ) ), ArchetypeCatalogMerger.CATALOG_NAME );
            }

            final List<Transfer> sources = fileManager.retrieveAll( members, toMergePath );
            final byte[] merged = merger.merge( sources, group, toMergePath );
            if ( merged != null )
            {
                OutputStream fos = null;
                try
                {
                    fos = target.openOutputStream( TransferOperation.GENERATE, true );
                    fos.write( merged );
                }
                catch ( final IOException e )
                {
                    throw new AproxWorkflowException( "Failed to write merged archetype catalog to: {}.\nError: {}", e, target, e.getMessage() );
                }
                finally
                {
                    closeQuietly( fos );
                }

                //                helper.writeChecksumsAndMergeInfo( merged, sources, group, toMergePath );
            }
        }

        if ( target.exists() )
        {
            return target;
        }

        return null;
    }

    @Override
    public List<StoreResource> generateGroupDirectoryContent( final Group group, final List<ArtifactStore> members,
                                                              final String path )
        throws AproxWorkflowException
    {
        final List<StoreResource> result = new ArrayList<StoreResource>();
        for ( final String filename : HANDLED_FILENAMES )
        {
            result.add( new StoreResource( LocationUtils.toLocation( group ), Paths.get( path, filename )
                                                                                   .toString() ) );
        }

        return result;
    }

    @Override
    protected String getMergedMetadataName()
    {
        return ArchetypeCatalogMerger.CATALOG_NAME;
    }

}
