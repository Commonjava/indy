/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.core.rest.group;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.filer.FileManager;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.rest.group.GroupPathHandler;
import org.commonjava.aprox.util.ApplicationStatus;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;

@ApplicationScoped
public class MavenMetadataHandler
    implements GroupPathHandler
{

    @Inject
    private GroupMergeHelper helper;

    @Inject
    private FileManager fileManager;

    @Inject
    private MavenMetadataMerger merger;

    @Override
    public boolean canHandle( final String path )
    {
        return path.endsWith( MavenMetadataMerger.METADATA_NAME );
    }

    @Override
    public Transfer retrieve( final Group group, final List<? extends ArtifactStore> stores, final String path )
        throws AproxWorkflowException
    {
        final Transfer target = fileManager.getStorageReference( group, path );

        if ( !target.exists() )
        {
            final Set<Transfer> sources = fileManager.retrieveAll( stores, path );
            final byte[] merged = merger.merge( sources, group, path );
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
                    throw new AproxWorkflowException( "Failed to write merged metadata to: %s.\nError: %s", e, target, e.getMessage() );
                }
                finally
                {
                    closeQuietly( fos );
                }

                helper.writeChecksumsAndMergeInfo( merged, sources, group, path );
            }
        }

        if ( target.exists() )
        {
            return target;
        }

        return null;
    }

    @Override
    public Transfer store( final Group group, final List<? extends ArtifactStore> stores, final String path, final InputStream stream )
        throws AproxWorkflowException
    {
        if ( path.endsWith( MavenMetadataMerger.METADATA_NAME ) )
        {
            // delete so it'll be recomputed.
            final Transfer target = fileManager.getStorageReference( group, path );
            try
            {
                target.delete();
            }
            catch ( final IOException e )
            {
                throw new AproxWorkflowException(

                "Failed to delete generated file (to allow re-generation on demand: %s. Error: %s", e, target.getFullPath(), e.getMessage() );
            }
        }

        return fileManager.store( stores, path, stream );
    }

    @Override
    public boolean delete( final Group group, final List<? extends ArtifactStore> stores, final String path )
        throws AproxWorkflowException
    {
        final Transfer target = fileManager.getStorageReference( group, path );

        if ( target == null )
        {
            return false;
        }

        try
        {
            target.delete();

            helper.deleteChecksumsAndMergeInfo( group, path );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.SERVER_ERROR, "Failed to delete one or more group files for: %s:%s. Reason: %s", e,
                                              group.getKey(), path, e.getMessage() );
        }

        return true;
    }

}
