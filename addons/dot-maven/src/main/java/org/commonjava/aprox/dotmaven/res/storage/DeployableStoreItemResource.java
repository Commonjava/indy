package org.commonjava.aprox.dotmaven.res.storage;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.copy;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.CollectionResource;
import io.milton.resource.MakeCollectionableResource;
import io.milton.resource.PutableResource;
import io.milton.resource.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.commonjava.aprox.dotmaven.data.StorageAdvice;
import org.commonjava.aprox.dotmaven.webctl.RequestInfo;
import org.commonjava.aprox.io.StorageItem;

public class DeployableStoreItemResource
    extends StoreItemResource
    implements PutableResource, MakeCollectionableResource
{

    private final StorageAdvice advice;

    public DeployableStoreItemResource( final StorageItem item, final StorageAdvice advice, final RequestInfo info )
    {
        super( item, info );
        this.advice = advice;
    }

    @Override
    public String checkRedirect( final Request request )
        throws NotAuthorizedException, BadRequestException
    {
        return null;
    }

    @Override
    public Resource child( final String childName )
        throws NotAuthorizedException, BadRequestException
    {
        if ( getItem().isDirectory() )
        {
            return new DeployableStoreItemResource( getItem().getChild( childName ), advice, getRequestInfo() );
        }

        throw new BadRequestException( "Cannot create child of non-directory: " + getName() );
    }

    @Override
    public List<? extends Resource> getChildren()
        throws NotAuthorizedException, BadRequestException
    {
        if ( getItem().isDirectory() )
        {
            final List<Resource> children = new ArrayList<Resource>();

            final String[] listing = getItem().list();
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

        throw new BadRequestException( "Cannot create child of non-directory: " + getName() );
    }

    @Override
    public CollectionResource createCollection( final String newName )
        throws NotAuthorizedException, ConflictException, BadRequestException
    {
        if ( getItem().isDirectory() )
        {
            final StorageItem childItem = getItem().getChild( newName );
            childItem.mkdirs();

            // TODO: Check advice!!
            return new DeployableStoreItemResource( childItem, advice, getRequestInfo() );
        }

        throw new BadRequestException( "Cannot create child of non-directory: " + getName() );
    }

    @Override
    public Resource createNew( final String newName, final InputStream in, final Long length, final String contentType )
        throws IOException, ConflictException, NotAuthorizedException, BadRequestException
    {
        if ( getItem().isDirectory() )
        {
            final StorageItem childItem = getItem().getChild( newName );

            // TODO: Check advice!!
            OutputStream out = null;
            try
            {
                out = childItem.openOutputStream();
                copy( in, out );
            }
            finally
            {
                closeQuietly( out );
            }
        }

        throw new BadRequestException( "Cannot create child of non-directory: " + getName() );
    }

    @Override
    public void sendContent( final OutputStream out, final Range range, final Map<String, String> params,
                             final String contentType )
        throws IOException, NotAuthorizedException, BadRequestException, NotFoundException
    {
        InputStream in = null;
        try
        {
            in = getItem().openInputStream();
            copy( in, out );
        }
        finally
        {
            closeQuietly( in );
        }
    }

}
