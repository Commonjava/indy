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
package org.commonjava.indy.core.change;

import java.io.IOException;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.commonjava.indy.change.event.AbstractStoreDeleteEvent;
import org.commonjava.indy.change.event.ArtifactStoreDeletePreEvent;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listens for {@link ArtifactStoreDeletePreEvent} and remove associated cached/stored artifacts. This is a best effort...if deleting a hosted repo
 * that uses a read-only storage location, delete may fail with only an error entry in the logs.
 * 
 * @author jdcasey
 */
@ApplicationScoped
public class StorageDeletionListener
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public void clearStorage( @Observes final AbstractStoreDeleteEvent event )
    {
        for ( final Map.Entry<ArtifactStore, Transfer> storeRoot : event.getStoreRoots()
                                                                        .entrySet() )
        {
            final Transfer root = storeRoot.getValue();
            logger.info( "Clearing storage for: {}\n  {}", storeRoot.getKey(), root );

            recurseAndDelete( root );
        }
    }

    private void recurseAndDelete( final Transfer txfr )
    {
        try
        {
            final String[] list = txfr.list();
            if ( list == null )
            {
                return;
            }

            for ( final String fname : list )
            {
                final Transfer child = txfr.getChild( fname );
                if ( child.isDirectory() )
                {
                    recurseAndDelete( child );
                }
                else
                {
                    child.delete( true );
                }
            }
        }
        catch ( final IOException e )
        {
            logger.error( String.format( "Failed to list files for deletion under: %s. Reason: %s", txfr,
                                         e.getMessage() ), e );
        }
    }

}
