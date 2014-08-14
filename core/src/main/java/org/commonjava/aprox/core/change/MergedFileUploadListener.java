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
package org.commonjava.aprox.core.change;

import static org.commonjava.aprox.util.LocationUtils.getKey;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.commonjava.aprox.change.event.AproxFileEventManager;
import org.commonjava.aprox.content.FileManager;
import org.commonjava.aprox.content.group.GroupPathHandler;
import org.commonjava.aprox.core.content.group.ArchetypeCatalogMerger;
import org.commonjava.aprox.core.content.group.MavenMetadataMerger;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.cdi.util.weft.ExecutorConfig;
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

    @Inject
    @ExecutorConfig( daemon = true, priority = 7, named = "aprox-events" )
    private Executor executor;

    public void reMergeUploaded( @Observes final FileEvent event )
    {
        executor.execute( new Runnable()
        {
            @Override
            public void run()
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
                                logger.error( String.format( "Failed to delete: %s from group: %s. Error: %s", path, group, e.getMessage() ), e );
                            }
                        }
                    }
                }
                catch ( final ProxyDataException e )
                {
                    logger.warn( "Failed to regenerate maven-metadata.xml for groups after deployment to: {}"
                        + "\nCannot retrieve associated groups: {}", e, key, e.getMessage() );
                }
            }
        } );
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
