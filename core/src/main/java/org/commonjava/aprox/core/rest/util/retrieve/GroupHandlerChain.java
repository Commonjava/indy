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
