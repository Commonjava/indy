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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.commonjava.aprox.core.change.event.FileStorageEvent;
import org.commonjava.aprox.core.data.ProxyDataException;
import org.commonjava.aprox.core.data.ProxyDataManager;
import org.commonjava.aprox.core.model.ArtifactStore;
import org.commonjava.aprox.core.model.Group;
import org.commonjava.aprox.core.rest.util.FileManager;
import org.commonjava.aprox.core.rest.util.MavenMetadataMerger;
import org.commonjava.util.logging.Logger;

@Singleton
public class MavenMetadataUploadListener
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private ProxyDataManager dataManager;

    @Inject
    private MavenMetadataMerger merger;

    @Inject
    private FileManager fileManager;

    public void reMergeUploadedMetadata( @Observes final FileStorageEvent event )
    {
        String path = event.getPath();
        if ( !path.endsWith( MavenMetadataMerger.METADATA_NAME ) )
        {
            return;
        }

        try
        {
            Set<Group> groups = dataManager.getGroupsContaining( event.getStore().getKey() );

            if ( groups != null )
            {
                for ( Group group : groups )
                {
                    reMergeMavenMetadata( group, event.getPath() );
                }
            }
        }
        catch ( ProxyDataException e )
        {
            logger.warn( "Failed to regenerate maven-metadata.xml for groups after deployment to: %s"
                             + "\nCannot retrieve associated groups: %s", e,
                         event.getStore().getKey(), e.getMessage() );
        }
    }

    private void reMergeMavenMetadata( final Group group, final String path )
    {
        File target = fileManager.formatStorageReference( group, path );

        if ( !target.exists() )
        {
            return;
        }

        try
        {
            List<ArtifactStore> stores =
                dataManager.getOrderedConcreteStoresInGroup( group.getName() );

            Set<File> files = new LinkedHashSet<File>();
            if ( stores != null )
            {
                for ( ArtifactStore store : stores )
                {
                    File file = fileManager.formatStorageReference( store, path );

                    if ( file.exists() )
                    {
                        files.add( file );
                    }
                }
            }

            merger.merge( files, target, group, path );
        }
        catch ( ProxyDataException e )
        {
            logger.warn( "Failed to regenerate maven-metadata.xml for groups after deployment to: %s"
                             + "\nCannot retrieve storage locations associated with: %s", e,
                         group.getKey(), e.getMessage() );
        }
    }

}
