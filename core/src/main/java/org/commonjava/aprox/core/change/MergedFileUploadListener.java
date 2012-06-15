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

import java.io.File;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.commonjava.aprox.core.change.event.FileStorageEvent;
import org.commonjava.aprox.core.data.ProxyDataException;
import org.commonjava.aprox.core.data.StoreDataManager;
import org.commonjava.aprox.core.model.Group;
import org.commonjava.aprox.core.rest.util.ArchetypeCatalogMerger;
import org.commonjava.aprox.core.rest.util.FileManager;
import org.commonjava.aprox.core.rest.util.MavenMetadataMerger;
import org.commonjava.util.logging.Logger;

@Singleton
public class MergedFileUploadListener
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private StoreDataManager dataManager;

    @Inject
    private FileManager fileManager;

    public void reMergeUploaded( @Observes final FileStorageEvent event )
    {
        final String path = event.getPath();
        if ( !path.endsWith( MavenMetadataMerger.METADATA_NAME )
            && !path.endsWith( ArchetypeCatalogMerger.CATALOG_NAME ) )
        {
            return;
        }

        try
        {
            final Set<? extends Group> groups = dataManager.getGroupsContaining( event.getStore()
                                                                                      .getKey() );

            if ( groups != null )
            {
                for ( final Group group : groups )
                {
                    reMerge( group, event.getPath() );
                }
            }
        }
        catch ( final ProxyDataException e )
        {
            logger.warn( "Failed to regenerate maven-metadata.xml for groups after deployment to: %s"
                + "\nCannot retrieve associated groups: %s", e, event.getStore()
                                                                     .getKey(), e.getMessage() );
        }
    }

    private void reMerge( final Group group, final String path )
    {
        final File target = fileManager.formatStorageReference( group, path );
        final File targetInfo;
        if ( path.endsWith( MavenMetadataMerger.METADATA_NAME ) )
        {
            targetInfo =
                fileManager.formatStorageReference( group, path + MavenMetadataMerger.METADATA_MERGEINFO_SUFFIX );
        }
        else if ( path.endsWith( ArchetypeCatalogMerger.CATALOG_NAME ) )
        {
            targetInfo =
                fileManager.formatStorageReference( group, path + ArchetypeCatalogMerger.CATALOG_MERGEINFO_SUFFIX );
        }
        else
        {
            return;
        }

        if ( target.exists() )
        {
            // allow it to regenerate on the next call.
            target.delete();
        }

        if ( targetInfo.exists() )
        {
            targetInfo.delete();
        }
    }

}
