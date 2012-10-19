package org.commonjava.aprox.dotmaven.res;

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

import org.commonjava.aprox.dotmaven.webctl.RequestInfo;

@Named( "everything-folder" )
@RequestScoped
public class EverythingResource
    implements MakeCollectionableResource, PropFindableResource
{

    @Inject
    @Named( "settings-folder" )
    private Resource settingsFolderResource;

    @Inject
    @Named( "stores-folder" )
    private Resource storesFolderResource;

    @Inject
    private RequestInfo requestInfo;

    @Override
    public Resource child( final String childName )
        throws NotAuthorizedException, BadRequestException
    {
        final String name = trimLeadingSlash( childName );
        if ( DotMavenResourceFactory.SETTINGS_BASE.equals( name ) )
        {
            return settingsFolderResource;
        }
        else if ( DotMavenResourceFactory.STORES_BASE.equals( name ) )
        {
            return storesFolderResource;
        }

        throw new BadRequestException( "No such child." );
    }

    @Override
    public List<? extends Resource> getChildren()
        throws NotAuthorizedException, BadRequestException
    {
        final List<Resource> children = new ArrayList<Resource>();
        children.add( settingsFolderResource );
        children.add( storesFolderResource );

        return children;
    }

    @Override
    public String getUniqueId()
    {
        return "/";
    }

    @Override
    public String getName()
    {
        return "/";
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
        return requestInfo.getRealm();
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
        throw new NotAuthorizedException( "This folder is read-only", this );
    }

}
