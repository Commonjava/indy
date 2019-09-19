/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.dotmaven.store;

import static org.commonjava.indy.dotmaven.util.NameUtils.isValidResource;

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

            return names.toArray( new String[names.size()] );
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
