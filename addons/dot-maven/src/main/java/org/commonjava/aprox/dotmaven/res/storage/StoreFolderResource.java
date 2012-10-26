package org.commonjava.aprox.dotmaven.res.storage;

import static org.commonjava.aprox.dotmaven.util.NameUtils.appendPath;
import io.milton.http.Auth;
import io.milton.http.Request;
import io.milton.http.Request.Method;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.CollectionResource;
import io.milton.resource.MakeCollectionableResource;
import io.milton.resource.PropFindableResource;
import io.milton.resource.Resource;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.dotmaven.data.StorageAdvice;
import org.commonjava.aprox.dotmaven.webctl.RequestInfo;
import org.commonjava.aprox.filer.FileManager;
import org.commonjava.aprox.io.StorageItem;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.util.logging.Logger;

public class StoreFolderResource
    implements MakeCollectionableResource, PropFindableResource
{

    public static final String ROOT = "/";

    private final Logger logger = new Logger( getClass() );

    private final List<ArtifactStore> stores;

    private final String path;

    private final FileManager fileManager;

    private final StoreDataManager dataManager;

    private final RequestInfo info;

    private final StorageAdvice advice;

    public StoreFolderResource( final String path, final FileManager fileManager, final StoreDataManager dataManager,
                                final RequestInfo info, final StorageAdvice advice, final ArtifactStore... stores )
    {
        this( path, fileManager, dataManager, info, advice, Arrays.asList( stores ) );
    }

    public StoreFolderResource( final String path, final FileManager fileManager, final StoreDataManager dataManager,
                                final RequestInfo info, final StorageAdvice advice, final List<ArtifactStore> stores )
    {
        logger.info( "Constructing StoreFolderResource with path of: %s", path );
        this.stores = stores;
        this.path = path;
        this.fileManager = fileManager;
        this.dataManager = dataManager;
        this.info = info;
        this.advice = advice;
    }

    @Override
    public Resource child( final String childName )
        throws NotAuthorizedException, BadRequestException
    {
        final String childPath = appendPath( path, childName );
        StorageItem item;
        try
        {
            item = fileManager.retrieveFirst( stores, childPath );
        }
        catch ( final AproxWorkflowException e )
        {
            throw new BadRequestException( "Cannot retrieve child: " + childName );
        }

        if ( item.isDirectory() )
        {
            return new StoreFolderResource( childPath, fileManager, dataManager, info, advice, stores );
        }
        else
        {
            if ( advice.isDeployable() )
            {
                return new DeployableStoreItemResource( item, advice, info );
            }
            else if ( item.exists() )
            {
                return new StoreItemResource( item, info );
            }
        }

        return null;
    }

    @Override
    public List<? extends Resource> getChildren()
        throws NotAuthorizedException, BadRequestException
    {
        final List<String> allListing = new ArrayList<String>();
        for ( final ArtifactStore store : stores )
        {
            logger.info( "Retrieving children of path: %s from: %s", path, store.getKey() );
            final StorageItem item = fileManager.getStorageReference( store.getKey(), path );

            final String[] listing = item.list();
            if ( listing != null )
            {
                for ( final String fname : listing )
                {
                    if ( !allListing.contains( fname ) )
                    {
                        allListing.add( fname );
                    }
                }
            }
        }

        final List<Resource> children = new ArrayList<Resource>();

        // TODO: This is inefficient, we're giving up the store info only to look it up again! 
        // It could also cause errors. Need to streamline this.
        for ( final String fname : allListing )
        {
            final Resource child = child( fname );
            if ( child != null )
            {
                children.add( child );
            }
        }

        return children;
    }

    @Override
    public String getUniqueId()
    {
        return path;
    }

    @Override
    public String getName()
    {
        return new File( path ).getName();
    }

    @Override
    public Object authenticate( final String user, final String password )
    {
        return "ok";
    }

    @Override
    public boolean authorise( final Request request, final Method method, final Auth auth )
    {
        return true;
    }

    @Override
    public String getRealm()
    {
        return info.getRealm();
    }

    @Override
    public Date getModifiedDate()
    {
        Date d = new Date();
        if ( !ROOT.equals( path ) )
        {
            try
            {
                final StorageItem item = fileManager.retrieveFirst( stores, path );
                d = new Date( item.getDetachedFile()
                                  .lastModified() );
            }
            catch ( final AproxWorkflowException e )
            {
                logger.error( "Failed to retrieve: %s from stores: %s.\nReason: %s", e, path, stores );
            }
        }

        return d;
    }

    @Override
    public String checkRedirect( final Request request )
        throws NotAuthorizedException, BadRequestException
    {
        return null;
    }

    @Override
    public Date getCreateDate()
    {
        return getModifiedDate();
    }

    @Override
    public CollectionResource createCollection( final String newName )
        throws NotAuthorizedException, ConflictException, BadRequestException
    {
        throw new NotAuthorizedException( "Folder is read-only.", this );
    }
}
