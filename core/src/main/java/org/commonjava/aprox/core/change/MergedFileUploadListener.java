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
package org.commonjava.aprox.core.change;

import static org.commonjava.aprox.util.LocationUtils.getKey;

import java.io.IOException;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.commonjava.aprox.change.event.AproxFileEventManager;
import org.commonjava.aprox.core.rest.group.ArchetypeCatalogMerger;
import org.commonjava.aprox.core.rest.group.MavenMetadataMerger;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.filer.FileManager;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.rest.group.GroupPathHandler;
import org.commonjava.maven.galley.event.FileDeletionEvent;
import org.commonjava.maven.galley.event.FileEvent;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@javax.enterprise.context.ApplicationScoped
public class MergedFileUploadListener
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private StoreDataManager dataManager;

    @Inject
    private FileManager fileManager;

    @Inject
    private AproxFileEventManager fileEvent;

    public void reMergeUploaded( @Observes final FileEvent event )
    {
        final String path = event.getTransfer()
                                 .getPath();

        final StoreKey key = getKey( event );

        if ( !path.endsWith( MavenMetadataMerger.METADATA_NAME ) && !path.endsWith( ArchetypeCatalogMerger.CATALOG_NAME ) )
        {
            return;
        }

        try
        {
            final Set<? extends Group> groups = dataManager.getGroupsContaining( key );

            if ( groups != null )
            {
                for ( final Group group : groups )
                {
                    try
                    {
                        reMerge( group, path );
                    }
                    catch ( final IOException e )
                    {
                        logger.error( "Failed to delete: {} from group: {}. Error: {}", e, path, group, e.getMessage() );
                    }
                }
            }
        }
        catch ( final ProxyDataException e )
        {
            logger.warn( "Failed to regenerate maven-metadata.xml for groups after deployment to: {}" + "\nCannot retrieve associated groups: {}", e,
                         key, e.getMessage() );
        }
    }

    private void reMerge( final Group group, final String path )
        throws IOException
    {
        final Transfer[] toDelete =
            { fileManager.getStorageReference( group, path ), fileManager.getStorageReference( group, path + GroupPathHandler.MERGEINFO_SUFFIX ),
                fileManager.getStorageReference( group, path + GroupPathHandler.SHA_SUFFIX ),
                fileManager.getStorageReference( group, path + GroupPathHandler.MD5_SUFFIX ) };

        for ( final Transfer item : toDelete )
        {
            if ( item.exists() )
            {
                item.delete();

                if ( fileEvent != null )
                {
                    fileEvent.fire( new FileDeletionEvent( item ) );
                }
            }
        }
    }

}
