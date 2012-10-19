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

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.dotmaven.data.StorageAdvisor;
import org.commonjava.aprox.dotmaven.res.DotMavenResourceFactory;
import org.commonjava.aprox.dotmaven.webctl.RequestInfo;
import org.commonjava.aprox.filer.FileManager;
import org.commonjava.aprox.model.StoreType;

@Named( "stores-folder" )
@RequestScoped
public class AllStoresFolderResource
    implements MakeCollectionableResource, PropFindableResource
{

    @Inject
    private FileManager fileManager;

    @Inject
    private StoreDataManager dataManager;

    @Inject
    private StorageAdvisor advisor;

    @Inject
    private RequestInfo info;

    @Override
    public Resource child( final String childName )
        throws NotAuthorizedException, BadRequestException
    {
        return getChild( childName );
    }

    public StoreTypeFolderResource getChild( final String childName )
    {
        final String name = trimLeadingSlash( childName );

        final StoreType type = StoreType.get( name );
        return new StoreTypeFolderResource( type, fileManager, dataManager, info, advisor );
    }

    @Override
    public List<? extends Resource> getChildren()
        throws NotAuthorizedException, BadRequestException
    {
        final List<StoreTypeFolderResource> children = new ArrayList<StoreTypeFolderResource>( 3 );
        children.add( new StoreTypeFolderResource( StoreType.deploy_point, fileManager, dataManager, info, advisor ) );
        children.add( new StoreTypeFolderResource( StoreType.repository, fileManager, dataManager, info, advisor ) );
        children.add( new StoreTypeFolderResource( StoreType.group, fileManager, dataManager, info, advisor ) );

        return children;
    }

    @Override
    public String getUniqueId()
    {
        return DotMavenResourceFactory.STORES_BASE;
    }

    @Override
    public String getName()
    {
        return DotMavenResourceFactory.STORES_BASE;
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
        throw new NotAuthorizedException( "List of ArtifactStore types is read-only.", this );
    }

}
