package org.commonjava.aprox.client.core.helper;

import java.text.ParseException;
import java.util.Date;
import java.util.Map;

import org.commonjava.aprox.model.util.HttpUtils;

public class PathInfo
{
    private final boolean exists;

    private final String contentType;

    private final long contentLength;

    private final Date lastModified;

    public PathInfo( final Map<String, String> headers )
    {
        if ( headers == null )
        {
            exists = false;
            contentType = null;
            contentLength = -1;
            lastModified = null;
        }
        else
        {
            exists = true;
            
            contentType = headers.get( "content-type" );
            
            final String cl = headers.get("content-length");
            contentLength = cl == null ? -1 : Long.parseLong( cl );
            
            final String lm = headers.get( "last-modified" );

            Date lastModified;
            try
            {
                lastModified = lm == null ? null : HttpUtils.parseDateHeader( lm );
            }
            catch ( final ParseException e )
            {
                lastModified = null;
            }

            this.lastModified = lastModified;
        }
    }

    public boolean exists()
    {
        return exists;
    }

    public String getContentType()
    {
        return contentType;
    }

    public long getContentLength()
    {
        return contentLength;
    }

    public Date getLastModified()
    {
        return lastModified;
    }

    @Override
    public String toString()
    {
        return String.format( "PathInfo [exists=%s, contentType=%s, contentLength=%s, lastModified=%s]", exists,
                              contentType, contentLength, lastModified );
    }

}
