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
package org.commonjava.aprox.dotmaven.store;

import static org.commonjava.aprox.dotmaven.util.NameUtils.isValidResource;

import java.io.InputStream;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import net.sf.webdav.StoredObject;
import net.sf.webdav.exceptions.WebdavException;
import net.sf.webdav.spi.ITransaction;
import net.sf.webdav.spi.IWebdavStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class DotMavenStore
    implements IWebdavStore
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private Instance<SubStore> injectedSubstores;

    private List<SubStore> substores;

    @PostConstruct
    public void initialize()
    {
        substores = new ArrayList<SubStore>();
        if ( injectedSubstores != null )
        {
            for ( final SubStore sub : injectedSubstores )
            {
                substores.add( sub );
            }
        }
    }

    @Override
    public ITransaction begin( final Principal principal )
    {
        //        logger.info( "start txn: {}", principal );
        return new StoreTxn( principal );
    }

    @Override
    public void checkAuthentication( final ITransaction transaction )
    {
        //        logger.info( "check auth: {}", transaction );
        // TODO
    }

    @Override
    public void commit( final ITransaction transaction )
    {
        //        logger.info( "commit: {}", transaction );
        // TODO
    }

    @Override
    public void rollback( final ITransaction transaction )
    {
        //        logger.info( "rollback: {}", transaction );
        // TODO
    }

    @Override
    public void createFolder( final ITransaction transaction, final String folderUri )
        throws WebdavException
    {
        logger.info( "create folder: {}, {}", transaction, folderUri );
        final SubStore store = select( folderUri );
        if ( store != null )
        {
            store.createFolder( transaction, folderUri );
        }
    }

    private SubStore select( final String uri )
    {
        if ( !isValidResource( uri ) )
        {
            return null;
        }

        logger.info( "Select sub-store: {}", uri );
        for ( final SubStore sub : substores )
        {
            if ( sub.matchesUri( uri ) )
            {
                return sub;
            }
        }

        return null;
    }

    @Override
    public void createResource( final ITransaction transaction, final String resourceUri )
        throws WebdavException
    {
        //        logger.info( "create resource: {}, {}", transaction, resourceUri );
        final SubStore store = select( resourceUri );
        if ( store != null )
        {
            store.createResource( transaction, resourceUri );
        }
    }

    @Override
    public InputStream getResourceContent( final ITransaction transaction, final String resourceUri )
        throws WebdavException
    {
        logger.info( "get content: {}, {}", transaction, resourceUri );
        final SubStore store = select( resourceUri );
        if ( store != null )
        {
            return store.getResourceContent( transaction, resourceUri );
        }

        return null;
    }

    @Override
    public long setResourceContent( final ITransaction transaction, final String resourceUri, final InputStream content, final String contentType,
                                    final String characterEncoding )
        throws WebdavException
    {
        //        logger.info( "set content: {}, {}", transaction, resourceUri );
        final SubStore store = select( resourceUri );
        if ( store != null )
        {
            return store.setResourceContent( transaction, resourceUri, content, contentType, characterEncoding );
        }

        throw new WebdavException( "Cannot save: " + resourceUri );
    }

    @Override
    public String[] getChildrenNames( final ITransaction transaction, final String folderUri )
        throws WebdavException
    {
        logger.info( "get children names: {}, {}", transaction, folderUri );
        if ( "/".equals( folderUri ) )
        {
            final Set<String> names = new TreeSet<String>();
            for ( final SubStore ss : substores )
            {
                names.addAll( Arrays.asList( ss.getRootResourceNames() ) );
            }

            return names.toArray( new String[] {} );
        }
        else
        {
            final SubStore store = select( folderUri );
            if ( store != null )
            {
                return store.getChildrenNames( transaction, folderUri );
            }
        }

        return new String[] {};
    }

    @Override
    public long getResourceLength( final ITransaction transaction, final String path )
        throws WebdavException
    {
        logger.info( "get length: {}, {}", transaction, path );
        final SubStore store = select( path );
        if ( store != null )
        {
            return store.getResourceLength( transaction, path );
        }

        return 0;
    }

    @Override
    public void removeObject( final ITransaction transaction, final String uri )
        throws WebdavException
    {
        //        logger.info( "remove: {}, {}", transaction, uri );
        final SubStore store = select( uri );
        if ( store != null )
        {
            store.removeObject( transaction, uri );
        }
    }

    @Override
    public StoredObject getStoredObject( final ITransaction transaction, final String uri )
        throws WebdavException
    {
        //        final HttpSession session = requestInfo.getRequest()
        //                                               .getSession();
        //        if ( session != null )
        //        {
        //            logger.info( "mount point: {}", session.getAttribute( RequestInfo.MOUNT_POINT ) );
        //        }
        //        else
        //        {
        //            logger.info( "No session available" );
        //        }

        logger.info( "get stored object: {}, {}", transaction, uri );
        final SubStore store = select( uri );
        if ( store != null )
        {
            logger.info( "Returning stored object from sub-store: {}", store );
            return store.getStoredObject( transaction, uri );
        }

        final StoredObject so = new StoredObject();
        so.setFolder( true );
        final Date d = new Date();
        so.setCreationDate( d );
        so.setLastModified( d );

        return so;
    }

}
