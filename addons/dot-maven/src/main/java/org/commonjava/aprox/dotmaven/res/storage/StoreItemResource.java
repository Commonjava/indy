package org.commonjava.aprox.dotmaven.res.storage;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.copy;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.Request.Method;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.GetableResource;
import io.milton.resource.PropFindableResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Map;

import javax.activation.MimetypesFileTypeMap;

import org.commonjava.aprox.dotmaven.webctl.RequestInfo;
import org.commonjava.aprox.io.StorageItem;

public class StoreItemResource
    implements GetableResource, PropFindableResource
{

    private final StorageItem item;

    private final RequestInfo info;

    public StoreItemResource( final StorageItem item, final RequestInfo info )
    {
        this.item = item;
        this.info = info;
    }

    protected StorageItem getItem()
    {
        return item;
    }

    protected RequestInfo getRequestInfo()
    {
        return info;
    }

    @Override
    public String getUniqueId()
    {
        return item.getPath();
    }

    @Override
    public String getName()
    {
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
        // TODO: How do I get an accurate date for this??
        return getModifiedDate();
    }

    @Override
    public void sendContent( final OutputStream out, final Range range, final Map<String, String> params,
                             final String contentType )
        throws IOException, NotAuthorizedException, BadRequestException, NotFoundException
    {
        if ( !item.exists() )
        {
            throw new NotFoundException( "File: " + getName() + " does not exist" );
        }

        InputStream in = null;
        try
        {
            in = item.openInputStream();
            copy( in, out );
        }
        finally
        {
            closeQuietly( in );
        }
    }

    @Override
    public Long getMaxAgeSeconds( final Auth auth )
    {
        return 1L;
    }

    @Override
    public String getContentType( final String accepts )
    {
        final MimetypesFileTypeMap typeMap = new MimetypesFileTypeMap();
        return typeMap.getContentType( item.getDetachedFile() );
    }

    @Override
    public Long getContentLength()
    {
        return item.getDetachedFile()
                   .length();
    }

}
