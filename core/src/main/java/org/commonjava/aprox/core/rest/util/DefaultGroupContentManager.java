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
package org.commonjava.aprox.core.rest.util;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.filer.FileManager;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.aprox.rest.util.GroupContentManager;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.util.logging.Logger;

/**
 * @deprecated Use {@link FileManager} directly instead.
 * @author jdcasey
 */
@javax.enterprise.context.ApplicationScoped
@Deprecated
public class DefaultGroupContentManager
    implements GroupContentManager
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private StoreDataManager storeManager;

    @Inject
    private FileManager fileManager;

    /* (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.util.GroupContentManager#retrieve(java.lang.String, java.lang.String)
     */
    @Override
    public Transfer retrieve( final String name, final String path )
        throws AproxWorkflowException
    {
        // TODO:
        // 1. directory request (ends with "/")...browse somehow??
        // 2. empty path (directory request for proxy root)

        Group group = null;
        try
        {
            group = storeManager.getGroup( name );
            if ( group == null )
            {
                return null;
            }
        }
        catch ( final ProxyDataException e )
        {
            logger.error( "Failed to retrieve repository-group information: %s. Reason: %s", e, name, e.getMessage() );
            throw new AproxWorkflowException( "Failed to retrieve repository-group information: %s. Reason: %s", e, name, e.getMessage() );
        }

        // logger.info( "Download: %s\nFrom: %s", path, stores );
        final Transfer item = fileManager.retrieve( group, path );
        if ( item == null || item.isDirectory() )
        {
            return null;
        }

        return item;
    }

    /* (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.util.GroupContentManager#store(java.lang.String, java.lang.String, java.io.InputStream)
     */
    @Override
    public Transfer store( final String name, final String path, final InputStream stream )
        throws AproxWorkflowException
    {
        Group group = null;

        try
        {
            group = storeManager.getGroup( name );
            if ( group == null )
            {
                return null;
            }
        }
        catch ( final ProxyDataException e )
        {
            logger.error( "Failed to retrieve repository-group information: %s. Reason: %s", e, name, e.getMessage() );
            throw new AproxWorkflowException( "Failed to retrieve repository-group information: %s. Reason: %s", e, name, e.getMessage() );
        }

        return fileManager.store( group, path, stream );
    }

    @Override
    public boolean delete( final String name, final String path )
        throws AproxWorkflowException, IOException
    {
        Group group = null;

        try
        {
            group = storeManager.getGroup( name );
            if ( group == null )
            {
                return false;
            }
        }
        catch ( final ProxyDataException e )
        {
            logger.error( "Failed to retrieve repository-group information: %s. Reason: %s", e, name, e.getMessage() );
            throw new AproxWorkflowException( "Failed to retrieve repository-group information: %s. Reason: %s", e, name, e.getMessage() );
        }

        return fileManager.delete( group, path );
    }

}
