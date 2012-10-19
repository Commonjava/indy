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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.dotmaven.data.StorageAdvice;
import org.commonjava.aprox.dotmaven.webctl.RequestInfo;
import org.commonjava.aprox.filer.FileManager;
import org.commonjava.aprox.io.StorageItem;
import org.commonjava.aprox.model.ArtifactStore;

public class StoreFolderResource
    implements MakeCollectionableResource, PropFindableResource
{

    public static final String ROOT = "/";

    private final ArtifactStore store;

    private final String path;

    private final FileManager fileManager;

    private final StoreDataManager dataManager;

    private final RequestInfo info;

    private final StorageAdvice advice;

    public StoreFolderResource( final ArtifactStore store, final String path, final FileManager fileManager,
                                final StoreDataManager dataManager, final RequestInfo info, final StorageAdvice advice )
    {
        this.store = store;
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
        final StorageItem item = fileManager.getStorageReference( store.getKey(), childPath );
        if ( item.isDirectory() )
        {
            return new StoreFolderResource( store, childPath, fileManager, dataManager, info, advice );
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
        // FIXME: If this is a group, get all constituents and perform getChildren on all...

        final StorageItem item = fileManager.getStorageReference( store.getKey(), path );

        final List<Resource> children = new ArrayList<Resource>();

        final String[] listing = item.list();
        if ( listing != null )
        {
            for ( final String fname : listing )
            {
                final Resource child = child( fname );
                if ( child != null )
                {
                    children.add( child );
                }
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
        final StorageItem item = fileManager.getStorageReference( store.getKey(), path );
        return item.getDetachedFile()
                   .getName();
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
        final StorageItem item = fileManager.getStorageReference( store.getKey(), path );
        return new Date( item.getDetachedFile()
                             .lastModified() );
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
