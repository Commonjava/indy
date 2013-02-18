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
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import net.sf.webdav.ITransaction;
import net.sf.webdav.IWebdavStore;
import net.sf.webdav.StoredObject;
import net.sf.webdav.exceptions.WebdavException;

import org.commonjava.aprox.dotmaven.webctl.RequestInfo;

@RequestScoped
public class DotMavenStore
    implements IWebdavStore
{
    //    private final Logger logger = new Logger( getClass() );

    @Inject
    private Instance<SubStore> injectedSubstores;

    @Inject
    private RequestInfo requestInfo;

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
        //        logger.info( "start txn: %s", principal );
        return new StoreTxn( principal );
    }

    @Override
    public void checkAuthentication( final ITransaction transaction )
    {
        //        logger.info( "check auth: %s", transaction );
        // TODO
    }

    @Override
    public void commit( final ITransaction transaction )
    {
        //        logger.info( "commit: %s", transaction );
        // TODO
    }

    @Override
    public void rollback( final ITransaction transaction )
    {
        //        logger.info( "rollback: %s", transaction );
        // TODO
    }

    @Override
    public void createFolder( final ITransaction transaction, final String folderUri )
    {
        //        logger.info( "create folder: %s, %s", transaction, folderUri );
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

        //        logger.info( "Select sub-store: %s", uri );
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
    {
        //        logger.info( "create resource: %s, %s", transaction, resourceUri );
        final SubStore store = select( resourceUri );
        if ( store != null )
        {
            store.createResource( transaction, resourceUri );
        }
    }

    @Override
    public InputStream getResourceContent( final ITransaction transaction, final String resourceUri )
    {
        //        logger.info( "get content: %s, %s", transaction, resourceUri );
        final SubStore store = select( resourceUri );
        if ( store != null )
        {
            return store.getResourceContent( transaction, resourceUri );
        }

        return null;
    }

    @Override
    public long setResourceContent( final ITransaction transaction, final String resourceUri,
                                    final InputStream content, final String contentType, final String characterEncoding )
    {
        //        logger.info( "set content: %s, %s", transaction, resourceUri );
        final SubStore store = select( resourceUri );
        if ( store != null )
        {
            return store.setResourceContent( transaction, resourceUri, content, contentType, characterEncoding );
        }

        throw new WebdavException( "Cannot save: " + resourceUri );
    }

    @Override
    public String[] getChildrenNames( final ITransaction transaction, final String folderUri )
    {
        //        logger.info( "get children names: %s, %s", transaction, folderUri );
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
    {
        //        logger.info( "get length: %s, %s", transaction, path );
        final SubStore store = select( path );
        if ( store != null )
        {
            return store.getResourceLength( transaction, path );
        }

        return 0;
    }

    @Override
    public void removeObject( final ITransaction transaction, final String uri )
    {
        //        logger.info( "remove: %s, %s", transaction, uri );
        final SubStore store = select( uri );
        if ( store != null )
        {
            store.removeObject( transaction, uri );
        }
    }

    @Override
    public StoredObject getStoredObject( final ITransaction transaction, final String uri )
    {
        //        final HttpSession session = requestInfo.getRequest()
        //                                               .getSession();
        //        if ( session != null )
        //        {
        //            logger.info( "mount point: %s", session.getAttribute( RequestInfo.MOUNT_POINT ) );
        //        }
        //        else
        //        {
        //            logger.info( "No session available" );
        //        }

        //        logger.info( "get stored object: %s, %s", transaction, uri );
        final SubStore store = select( uri );
        if ( store != null )
        {
            //            logger.info( "Returning stored object from sub-store: %s", store );
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
