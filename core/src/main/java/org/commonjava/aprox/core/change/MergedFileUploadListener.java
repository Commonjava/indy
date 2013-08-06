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
package org.commonjava.aprox.core.change;

import static org.commonjava.aprox.util.LocationUtils.getKey;

import java.io.IOException;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.commonjava.aprox.change.event.AproxFileEventManager;
import org.commonjava.aprox.core.rest.util.ArchetypeCatalogMerger;
import org.commonjava.aprox.core.rest.util.MavenMetadataMerger;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.filer.FileManager;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.rest.util.retrieve.GroupPathHandler;
import org.commonjava.maven.galley.event.FileDeletionEvent;
import org.commonjava.maven.galley.event.FileEvent;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.util.logging.Logger;

@javax.enterprise.context.ApplicationScoped
public class MergedFileUploadListener
{

    private final Logger logger = new Logger( getClass() );

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

        if ( !path.endsWith( MavenMetadataMerger.METADATA_NAME )
            && !path.endsWith( ArchetypeCatalogMerger.CATALOG_NAME ) )
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
                        logger.error( "Failed to delete: %s from group: %s. Error: %s", e, path, group, e.getMessage() );
                    }
                }
            }
        }
        catch ( final ProxyDataException e )
        {
            logger.warn( "Failed to regenerate maven-metadata.xml for groups after deployment to: %s"
                + "\nCannot retrieve associated groups: %s", e, key, e.getMessage() );
        }
    }

    private void reMerge( final Group group, final String path )
        throws IOException
    {
        final Transfer[] toDelete =
            { fileManager.getStorageReference( group, path ),
                fileManager.getStorageReference( group, path + GroupPathHandler.MERGEINFO_SUFFIX ),
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
