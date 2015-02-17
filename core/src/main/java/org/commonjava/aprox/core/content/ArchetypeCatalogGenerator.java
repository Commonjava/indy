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
import org.commonjava.aprox.content.AbstractContentGenerator;
import org.commonjava.aprox.content.DownloadManager;
import org.commonjava.aprox.content.StoreResource;
import org.commonjava.aprox.core.content.group.ArchetypeCatalogMerger;
import org.commonjava.aprox.core.content.group.GroupMergeHelper;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.util.ApplicationStatus;
import org.commonjava.aprox.util.LocationUtils;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;

@ApplicationScoped
public class ArchetypeCatalogGenerator
    extends AbstractContentGenerator
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
    private GroupMergeHelper helper;

    @Inject
    private DownloadManager fileManager;

    @Inject
    private ArchetypeCatalogMerger merger;

    protected ArchetypeCatalogGenerator()
    {
    }

    public ArchetypeCatalogGenerator( final DownloadManager downloadManager, final ArchetypeCatalogMerger merger,
                                      final GroupMergeHelper mergeHelper )
    {
        this.fileManager = downloadManager;
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
    public void handleContentStorage( final ArtifactStore store, final String path, final Transfer result )
        throws AproxWorkflowException
    {
        if ( StoreType.group == store.getKey()
                                     .getType() && path.endsWith( ArchetypeCatalogMerger.CATALOG_NAME ) )
        {
            final Group group = (Group) store;

            // delete so it'll be recomputed.
            final Transfer target = fileManager.getStorageReference( group, path );
            try
            {
                target.delete();
                helper.deleteChecksumsAndMergeInfo( group, path );
            }
            catch ( final IOException e )
            {
                throw new AproxWorkflowException( "Failed to delete generated file (to allow re-generation on demand: {}. Error: {}", e,
                                                  target.getFullPath(), e.getMessage() );
            }
        }
    }

    @Override
    public void handleContentDeletion( final ArtifactStore store, final String path )
        throws AproxWorkflowException
    {
        if ( StoreType.group == store.getKey()
                                     .getType() )
        {
            final Group group = (Group) store;
            final Transfer target = fileManager.getStorageReference( group, path );

            if ( target == null )
            {
                return;
            }

            try
            {
                target.delete();

                helper.deleteChecksumsAndMergeInfo( group, path );
            }
            catch ( final IOException e )
            {
                throw new AproxWorkflowException( ApplicationStatus.SERVER_ERROR.code(),
                                                  "Failed to delete one or more group files for: {}:{}. Reason: {}", e,
                                                  group.getKey(), path, e.getMessage() );
            }
        }
    }

}
