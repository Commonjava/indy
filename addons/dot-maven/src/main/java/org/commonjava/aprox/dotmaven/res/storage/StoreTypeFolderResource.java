package org.commonjava.aprox.dotmaven.res.storage;

import static org.commonjava.aprox.dotmaven.util.NameUtils.trimLeadingSlash;
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

import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.dotmaven.DotMavenException;
import org.commonjava.aprox.dotmaven.data.StorageAdvice;
import org.commonjava.aprox.dotmaven.data.StorageAdvisor;
import org.commonjava.aprox.dotmaven.webctl.RequestInfo;
import org.commonjava.aprox.filer.FileManager;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.util.logging.Logger;

public class StoreTypeFolderResource
    implements MakeCollectionableResource, PropFindableResource
{

    private final Logger logger = new Logger( getClass() );

    private final StoreType type;

    private final FileManager fileManager;

    private final StoreDataManager dataManager;

    private final StorageAdvisor advisor;

    private final RequestInfo info;

    public StoreTypeFolderResource( final StoreType type, final FileManager fileManager,
                                    final StoreDataManager dataManager, final RequestInfo info,
                                    final StorageAdvisor advisor )
    {
        this.type = type;
        this.fileManager = fileManager;
        this.dataManager = dataManager;
        this.info = info;
        this.advisor = advisor;
    }

    @Override
    public Resource child( final String childName )
        throws NotAuthorizedException, BadRequestException
    {
        return getChild( childName );
    }

    public StoreFolderResource getChild( final String childName )
        throws BadRequestException
    {
        final StoreKey key = new StoreKey( type, trimLeadingSlash( childName ) );
        try
        {
            final ArtifactStore store = dataManager.getArtifactStore( key );
            if ( store != null )
            {
                final StorageAdvice advice = advisor.getStorageAdvice( store );
                return new StoreFolderResource( store, StoreFolderResource.ROOT, fileManager, dataManager, info, advice );
            }
        }
        catch ( final ProxyDataException e )
        {
            logger.error( "Failed to retrieve ArtifactStore: %s. Reason: %s", e, key, e.getMessage() );
            throw new BadRequestException( "Failed to retrieve child: " + childName );
        }
        catch ( final DotMavenException e )
        {
            logger.error( "Failed to generate storage advice for: %s. Reason: %s", e, key, e.getMessage() );
            throw new BadRequestException( "Failed to retrieve child: " + childName );
        }

        return null;
    }

    @Override
    public List<? extends Resource> getChildren()
        throws NotAuthorizedException, BadRequestException
    {
        List<? extends ArtifactStore> stores = null;
        try
        {
            switch ( type )
            {
                case deploy_point:
                {
                    stores = dataManager.getAllDeployPoints();
                    break;
                }
                case group:
                {
                    stores = dataManager.getAllGroups();
                    break;
                }
                case repository:
                {
                    stores = dataManager.getAllRepositories();
                    break;
                }
            }
        }
        catch ( final ProxyDataException e )
        {
            logger.error( "Failed to retrieve ArtifactStores of type: %s. Reason: %s", e, type, e.getMessage() );
            throw new BadRequestException( "Failed to retrieve children" );
        }

        final List<StoreFolderResource> children = new ArrayList<StoreFolderResource>();
        if ( stores != null )
        {
            for ( final ArtifactStore store : stores )
            {
                StorageAdvice advice;
                try
                {
                    advice = advisor.getStorageAdvice( store );
                }
                catch ( final DotMavenException e )
                {
                    logger.error( "Failed to retrieve storage advice for: %s. Reason: %s", e, store.getKey(),
                                  e.getMessage() );
                    throw new BadRequestException( "Failed to retrieve children" );
                }

                children.add( new StoreFolderResource( store, store.getName(), fileManager, dataManager, info, advice ) );
            }
        }

        return children;
    }

    @Override
    public String getUniqueId()
    {
        return type.pluralEndpointName();
    }

    @Override
    public String getName()
    {
        return type.pluralEndpointName();
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
        return new Date();
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
        return new Date();
    }

    @Override
    public CollectionResource createCollection( final String newName )
        throws NotAuthorizedException, ConflictException, BadRequestException
    {
        throw new NotAuthorizedException( "Create artifact stores through the UI, then try again.", this );
    }
}
