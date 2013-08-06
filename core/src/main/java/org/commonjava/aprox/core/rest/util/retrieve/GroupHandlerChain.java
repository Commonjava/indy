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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.commonjava.aprox.filer.FileManager;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.aprox.rest.util.retrieve.GroupPathHandler;
import org.commonjava.maven.galley.model.Transfer;

@javax.enterprise.context.ApplicationScoped
public class GroupHandlerChain
{

    //    private final Logger logger = new Logger( getClass() );

    @Inject
    private Instance<GroupPathHandler> handlers;

    @Inject
    private FileManager downloader;

    public Transfer retrieve( final Group group, final List<? extends ArtifactStore> stores, final String path )
        throws AproxWorkflowException
    {
        for ( final GroupPathHandler handler : handlers )
        {
            if ( handler.canHandle( path ) )
            {
                //                logger.info( "Retrieving path: %s using GroupPathHandler: %s", path, handler.getClass()
                //                                                                                            .getName() );
                return handler.retrieve( group, stores, path );
            }
        }

        //        logger.info( "Retrieving path: %s from first available in: %s", path, join( stores, ", " ) );
        return downloader.retrieveFirst( stores, path );
    }

    public Transfer store( final Group group, final List<? extends ArtifactStore> stores, final String path,
                           final InputStream stream )
        throws AproxWorkflowException
    {
        for ( final GroupPathHandler handler : handlers )
        {
            if ( handler.canHandle( path ) )
            {
                return handler.store( group, stores, path, stream );
            }
        }

        return downloader.store( stores, path, stream );
    }

    public boolean delete( final Group group, final List<? extends ArtifactStore> stores, final String path )
        throws AproxWorkflowException, IOException
    {
        for ( final GroupPathHandler handler : handlers )
        {
            if ( handler.canHandle( path ) )
            {
                return handler.delete( group, stores, path );
            }
        }

        return downloader.deleteAll( stores, path );
    }

}
